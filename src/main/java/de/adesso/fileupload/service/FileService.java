package de.adesso.fileupload.service;

import de.adesso.fileupload.entity.UploadChunk;
import de.adesso.fileupload.entity.UploadSession;
import de.adesso.fileupload.entity.ZipEntryMetadata;
import de.adesso.fileupload.enums.FileTypeEnum;
import de.adesso.fileupload.enums.FilenameEncodingEnum;
import de.adesso.fileupload.events.UploadCompletedEvent;
import de.adesso.fileupload.repository.UploadChunkRepository;
import de.adesso.fileupload.repository.UploadSessionRepository;
import de.adesso.fileupload.repository.ZipEntryMetadataRepository;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.GeneralPurposeBit;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@Service
public class FileService {

  private final UploadSessionRepository sessionRepo;
  private final UploadChunkRepository chunkRepo;
  private final DataSource dataSource;
  private final ZipEntryMetadataRepository zipEntryMetadataRepository;
  private final ApplicationEventPublisher eventPublisher;

  public FileService(DataSource dataSource, UploadSessionRepository sessionRepo
      , UploadChunkRepository chunkRepo, ZipEntryMetadataRepository zipEntryMetadataRepository,
      ApplicationEventPublisher eventPublisher) {
    this.dataSource = dataSource;
    this.sessionRepo = sessionRepo;
    this.chunkRepo = chunkRepo;
    this.zipEntryMetadataRepository = zipEntryMetadataRepository;
    this.eventPublisher = eventPublisher;
  }

  public StreamingResponseBody streamFile(UUID sessionId) {
    return outputStream -> {
      byte[] buffer = new byte[8192];
      long totalBytes = 0;

      try (
          Connection connection = dataSource.getConnection()
      ) {
        connection.setAutoCommit(false);

        PGConnection pgConnection = connection.unwrap(PGConnection.class);
        LargeObjectManager lobj = pgConnection.getLargeObjectAPI();

        try (
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT chunk_data FROM upload_chunk WHERE upload_session_id = ? ORDER BY chunk_index ASC"
            )
        ) {
          stmt.setObject(1, sessionId);
          try (ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
              long oid = rs.getLong("chunk_data");
              log.info("Streaming chunk with OID: {}", oid);

              try (LargeObject obj = lobj.open(oid, LargeObjectManager.READ)) {
                int bytesRead;
                while ((bytesRead = obj.read(buffer, 0, buffer.length)) > 0) {
                  outputStream.write(buffer, 0, bytesRead);
                  totalBytes += bytesRead;

                  if (totalBytes % (10 * 1024 * 1024) < buffer.length) {
                    outputStream.flush();
                    log.info("Streamed {} MB", totalBytes / (1024 * 1024));
                  }
                }
              }
            }

            outputStream.flush();
            connection.commit();
            log.info("Streaming complete. Total bytes: {}", totalBytes);
          }
        }
      } catch (Exception e) {
        log.info("Streaming failed: {}", e.getMessage());
        e.printStackTrace();
        throw new RuntimeException("Error while streaming file", e);
      }
    };
  }


  public StreamingResponseBody downloadAsZip(UUID sessionId, String originalFileName) {
    return outputStream -> {
      try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        PGConnection pgConn = conn.unwrap(PGConnection.class);
        LargeObjectManager lobj = pgConn.getLargeObjectAPI();

        PreparedStatement stmt = conn.prepareStatement("""
                SELECT chunk_data FROM upload_chunk
                WHERE upload_session_id = ?
                ORDER BY chunk_index ASC
            """);
        stmt.setObject(1, sessionId);

        ResultSet rs = stmt.executeQuery();

        try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(outputStream)) {
          zipOut.setMethod(ZipArchiveOutputStream.DEFLATED);

          ZipArchiveEntry entry = new ZipArchiveEntry(originalFileName);
          GeneralPurposeBit gpb = new GeneralPurposeBit();
          gpb.useUTF8ForNames(true);
          entry.setGeneralPurposeBit(gpb);
          zipOut.putArchiveEntry(entry);

          byte[] buffer = new byte[8192];
          while (rs.next()) {
            long oid = rs.getLong("chunk_data");
            try (LargeObject obj = lobj.open(oid, LargeObjectManager.READ)) {
              int len;
              while ((len = obj.read(buffer, 0, buffer.length)) > 0) {
                zipOut.write(buffer, 0, len);
              }
            }
          }

          zipOut.closeArchiveEntry();
          zipOut.finish();
        }

        conn.commit();
      } catch (Exception e) {
        throw new RuntimeException("Failed to stream ZIP", e);
      }
    };
  }

  public InputStream getMergedInputStream(UUID sessionId) {
    try {
      Connection conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      PGConnection pgConn = conn.unwrap(PGConnection.class);
      LargeObjectManager lobj = pgConn.getLargeObjectAPI();

      PreparedStatement stmt = conn.prepareStatement("""
              SELECT chunk_data FROM upload_chunk
              WHERE upload_session_id = ?
              ORDER BY chunk_index ASC
          """);
      stmt.setObject(1, sessionId);
      ResultSet rs = stmt.executeQuery();

      List<InputStream> streams = new ArrayList<>();

      while (rs.next()) {
        long oid = rs.getLong("chunk_data");

        LargeObject obj = lobj.open(oid, LargeObjectManager.READ);
        InputStream stream = new FilterInputStream(obj.getInputStream()) {
          @Override
          public void close() {
            try {
              obj.close();
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        };

        streams.add(stream);
      }

      // Ensure connection is closed when InputStream is closed
      return new SequenceInputStream(Collections.enumeration(streams)) {
        @Override
        public void close() throws IOException {
          super.close();
          try {
            conn.close();
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        }
      };

    } catch (Exception e) {
      throw new RuntimeException("Failed to create merged InputStream from chunks", e);
    }
  }

  public void detectAndStoreEncodingForZip(UploadSession session) {
    if (session.getFileName().toLowerCase().endsWith(FileTypeEnum.ZIP.getValue())) {
      try (InputStream mergedStream = getMergedInputStream(session.getId())) {
        ZipArchiveInputStream zipIn = new ZipArchiveInputStream(mergedStream);

        ZipArchiveEntry entry;
        while ((entry = zipIn.getNextEntry()) != null) {

          String pathEncoded;
          String encoding;
          if (entry.getGeneralPurposeBit().usesUTF8ForNames()) {
            pathEncoded = new String(entry.getRawName(), StandardCharsets.UTF_8);
            encoding = FilenameEncodingEnum.UTF8.getValue();
          } else {
            pathEncoded = new String(entry.getRawName(),
                Charset.forName(FilenameEncodingEnum.CP437.getValue()));
            encoding = FilenameEncodingEnum.CP437.getValue();
          }

          storeZipMetadata(session, entry, pathEncoded, encoding);
        }
      } catch (IOException e) {
        log.error("Failed to store ZIP entry", e);
      }
    }
  }


  private void storeZipMetadata(UploadSession session, ZipArchiveEntry entry, String pathEncoded,
      String encoding) {
    ZipEntryMetadata meta = new ZipEntryMetadata();
    meta.setPath(pathEncoded);
    meta.setDirectory(entry.isDirectory());
    meta.setSize(entry.getSize());
    meta.setCompressedSize(entry.getCompressedSize());
    meta.setUploadSession(session);
    meta.setEncoding(encoding);
    zipEntryMetadataRepository.save(meta);
  }

  public void saveUploadedChunk(UUID id, Integer chunkIndex, MultipartFile chunk)
      throws IOException {
    UploadSession session = sessionRepo.findById(id).orElseThrow();

    UploadChunk uploadChunk = new UploadChunk();
    uploadChunk.setUploadSession(session);
    uploadChunk.setChunkIndex(chunkIndex);
    uploadChunk.setChunkData(chunk.getBytes());

    chunkRepo.save(uploadChunk);

    session.setUploadedSize(session.getUploadedSize() + chunk.getSize());

    if (session.getUploadedSize() == session.getTotalSize()) {
      session.setCompleted(true);
      eventPublisher.publishEvent(new UploadCompletedEvent(session));
    }

    sessionRepo.save(session);
  }

  public UploadSession saveUploadSession(String fileName, long totalSize) {
    UploadSession session = new UploadSession();
    session.setId(UUID.randomUUID());
    session.setFileName(fileName);
    session.setTotalSize(totalSize);
    session.setCompleted(false);
    session.setUploadedSize(0);
    sessionRepo.save(session);
    return session;
  }

  public void saveSessionAsCompleted(UUID id) {
    UploadSession session = sessionRepo.findById(id).orElseThrow();
    detectAndStoreEncodingForZip(session);
    sessionRepo.save(session);
  }

  public List<Integer> getChunkIndexes(UUID id) {
    return chunkRepo.findUploadedChunkIndexes(id);
  }

  public List<UploadSession> getFinishedUploadSessions() {
    return sessionRepo.findByCompletedTrue();
  }

  public List<UploadSession> getUnfinishedUploadSessions() {
    return sessionRepo.findByCompletedFalse();
  }

  public Optional<UploadSession> findById(UUID id) {
    return sessionRepo.findById(id);
  }


  public void streamFileRange(UUID sessionId, long rangeStart, long rangeEnd,
      OutputStream outputStream) {

    byte[] buffer = new byte[8192];
    long currentPosition = 0;

    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);

      PGConnection pgConnection = connection.unwrap(PGConnection.class);
      LargeObjectManager lobj = pgConnection.getLargeObjectAPI();

      try (PreparedStatement stmt = connection.prepareStatement(
          "SELECT chunk_data FROM upload_chunk WHERE upload_session_id = ? ORDER BY chunk_index ASC"
      )) {
        stmt.setObject(1, sessionId);

        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next() && currentPosition <= rangeEnd) {
            log.info("Requested: {} - {} | Current pos: {}", rangeStart, rangeEnd, currentPosition);

            long oid = rs.getLong("chunk_data");
            try (LargeObject obj = lobj.open(oid, LargeObjectManager.READ)) {
              int chunkSize = obj.size();

              // Skip this chunk entirely
              if (currentPosition + chunkSize <= rangeStart) {
                currentPosition += chunkSize;
                continue;
              }

              // Seek to the starting byte inside this chunk
              long skipBytes = Math.max(0, rangeStart - currentPosition);
              if (skipBytes > 0) {
                obj.seek((int) skipBytes);
                currentPosition += skipBytes;
              }

              int bytesRead;
              while ((bytesRead = obj.read(buffer, 0, buffer.length)) > 0) {
                long remaining = rangeEnd - currentPosition + 1;
                if (bytesRead > remaining) {
                  bytesRead = (int) remaining;
                }

                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();

                currentPosition += bytesRead;

                if (currentPosition > rangeEnd) {
                  break;
                }
              }
            }
          }
        }
      }

      connection.commit();
      log.info("Range streamed: {} to {}", rangeStart, rangeEnd);
    } catch (Exception e) {
      log.error("Error streaming file range", e);
      throw new RuntimeException("Streaming failed", e);
    }
  }

}