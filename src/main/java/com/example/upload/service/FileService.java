package com.example.upload.service;

import com.example.upload.model.UploadChunk;
import com.example.upload.model.UploadSession;
import com.example.upload.repository.UploadChunkRepository;
import com.example.upload.repository.UploadSessionRepository;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@Service
public class FileService {

  private final UploadSessionRepository sessionRepo;
  private final UploadChunkRepository chunkRepo;
  private final DataSource dataSource;

  public FileService(DataSource dataSource, UploadSessionRepository sessionRepo
      , UploadChunkRepository chunkRepo) {
    this.dataSource = dataSource;
    this.sessionRepo = sessionRepo;
    this.chunkRepo = chunkRepo;

  }

  public StreamingResponseBody streamFile(UUID sessionId) {
    return outputStream -> {
      byte[] buffer = new byte[8192];
      long totalBytes = 0;

      try (
          Connection connection = dataSource.getConnection()
      ) {
        connection.setAutoCommit(false); // Required for PostgreSQL Large Object API

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

                  // Optional: only flush every 10MB or at end
                  if (totalBytes % (10 * 1024 * 1024) < buffer.length) {
                    outputStream.flush();
                    log.info("Streamed {} MB", totalBytes / (1024 * 1024));
                  }
                }
              }
            }

            outputStream.flush(); // Final flush
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

        // Prepare the ZIP output stream
        try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(outputStream)) {
          zipOut.setMethod(ZipArchiveOutputStream.DEFLATED);

          // You can give any file name inside the zip
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

          zipOut.closeArchiveEntry(); // important
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
      conn.setAutoCommit(false); // Needed for PostgreSQL large object access

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


  public void detectAndStoreEncodingForZip(UUID id, UploadSession session, boolean anyUtf8,
      boolean anyNonUtf8) {
    if (session.getFileName().toLowerCase().endsWith(".zip")) {
      try (InputStream mergedStream = getMergedInputStream(id)) {
        ZipArchiveInputStream zipIn = new ZipArchiveInputStream(mergedStream);

        ZipArchiveEntry entry;
        while ((entry = zipIn.getNextZipEntry()) != null) {
          String name = entry.getName();

          if (entry.isDirectory()) {
            log.info("Directory: {}", entry.getName());
          } else {
            // Check if this file is inside a subdirectory
            if (name.contains("/")) {
              String folder = name.substring(0, name.lastIndexOf("/"));
              log.info("[Implied Directory]: {}", folder);
            }

            log.info("File: {}", name);
          }
          if (entry.getGeneralPurposeBit().usesUTF8ForNames()) {
            anyUtf8 = true;
          } else {
            anyNonUtf8 = true;
          }
        }
      } catch (IOException e) {
        session.setEncoding("unknown");
      }
    }
    if (anyUtf8 && !anyNonUtf8) {
      session.setEncoding("UTF-8 (filenames)");
    } else if (!anyUtf8 && anyNonUtf8) {
      session.setEncoding("CP437 (default or unknown)");
    } else if (anyUtf8 && anyNonUtf8) {
      session.setEncoding("Mixed (UTF-8 and CP437)");
    } else {
      session.setEncoding("unknown");
    }
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
    sessionRepo.save(session);
  }

  public UploadSession saveUploadSession(String fileName, long totalSize) {
    UploadSession session = new UploadSession();
    session.setId(UUID.randomUUID());
    session.setFileName(fileName);
    session.setTotalSize(totalSize);
    session.setComplete(false);
    session.setUploadedSize(0);
    sessionRepo.save(session);
    return session;
  }

  public void saveSessionAsCompleted(UUID id) {
    UploadSession session = sessionRepo.findById(id).orElseThrow();
    boolean anyUtf8 = false;
    boolean anyNonUtf8 = false;

    detectAndStoreEncodingForZip(id, session, anyUtf8, anyNonUtf8);

    session.setComplete(true);
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
}