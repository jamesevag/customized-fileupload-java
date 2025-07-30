package de.adesso.fileupload.service;

import de.adesso.fileupload.dao.ChunkDao;
import de.adesso.fileupload.entity.UploadChunk;
import de.adesso.fileupload.entity.UploadSession;
import de.adesso.fileupload.entity.ZipEntryMetadata;
import de.adesso.fileupload.enums.ChunkUploadStatusEnum;
import de.adesso.fileupload.enums.FileTypeEnum;
import de.adesso.fileupload.enums.FilenameEncodingEnum;
import de.adesso.fileupload.enums.ZipMetaDataStatusEnum;
import de.adesso.fileupload.event.UploadCompletedEvent;
import de.adesso.fileupload.repository.UploadChunkRepository;
import de.adesso.fileupload.repository.UploadSessionRepository;
import de.adesso.fileupload.repository.ZipEntryMetadataRepository;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class UploadService {

  private final UploadSessionRepository sessionRepo;
  private final UploadChunkRepository chunkRepo;
  private final ChunkDao chunkDao;
  private final DataSource dataSource;
  private final ZipEntryMetadataRepository zipEntryMetadataRepository;
  private final ApplicationEventPublisher eventPublisher;

  public UploadService(DataSource dataSource, UploadSessionRepository sessionRepo
      , UploadChunkRepository chunkRepo, ZipEntryMetadataRepository zipEntryMetadataRepository,
      ApplicationEventPublisher eventPublisher, ChunkDao downloadRangeDao) {
    this.dataSource = dataSource;
    this.sessionRepo = sessionRepo;
    this.chunkRepo = chunkRepo;
    this.zipEntryMetadataRepository = zipEntryMetadataRepository;
    this.eventPublisher = eventPublisher;
    this.chunkDao = downloadRangeDao;
  }

  public UploadSession saveUploadSession(String fileName, long totalSize) {
    UploadSession session = new UploadSession();
    session.setId(UUID.randomUUID());
    session.setFileName(fileName);
    session.setTotalSize(totalSize);
    session.setCompleted(false);
    session.setUploadedSize(0);
    session.setCreatedBy("TestUser");
    session.setCreatedAt(Instant.now());

    sessionRepo.save(session);
    return session;
  }

  public void saveUploadedChunk(UUID id, Integer chunkIndex, MultipartFile chunk)
      throws IOException {
    UploadSession session = sessionRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));

    byte[] chunkBytes = new byte[0];
    try {
      chunkBytes = chunk.getBytes();
    } catch (IOException e) {
      handleErrorUploadingBytes(chunkIndex, e, session, chunkBytes);
    }

    try {
      UploadChunk uploadChunk = new UploadChunk();
      uploadChunk.setUploadSession(session);
      uploadChunk.setChunkIndex(chunkIndex);
      uploadChunk.setChunkData(chunkBytes);
      uploadChunk.setStatus(ChunkUploadStatusEnum.UPLOADED.getValue());

      chunkRepo.save(uploadChunk);

      session.setUploadedSize(session.getUploadedSize() + chunk.getSize());
      sessionRepo.save(session);

    } catch (Exception e) {
      handleErrorStoringUploadedChunk(chunkIndex, e, session, chunkBytes);
    }

    triggerEventWhenCompleted(session);

    sessionRepo.save(session);
  }

  private void triggerEventWhenCompleted(UploadSession session) {
    if (session.getUploadedSize() == session.getTotalSize()) {
      session.setCompleted(true);
      eventPublisher.publishEvent(new UploadCompletedEvent(session));
    }
  }

  public void storeZipFileMetadata(UploadSession session) {
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
      } catch (ZipException e) {
        log.error("Invalid ZIP format for session {}: {}", session.getId(), e.getMessage());
        handleZipError(e, session);
      } catch (IOException e) {
        log.error("I/O error reading ZIP for session {}: {}", session.getId(), e.getMessage());
        handleZipError(e, session);
      } catch (Exception e) {
        log.error("Unexpected error while parsing ZIP for session {}: {}", session.getId(),
            e.getMessage());
        handleZipError(e, session);
      }
    }
  }

  private void handleZipError(Throwable t, UploadSession session) {
    String reason = "ZIP parsing failed: " + t.getClass().getSimpleName() + ": " + t.getMessage();

    ZipEntryMetadata meta = new ZipEntryMetadata();
    meta.setUploadSession(session);
    meta.setStatus(ZipMetaDataStatusEnum.FAILED.getValue());
    meta.setFailureReason(reason);
    zipEntryMetadataRepository.save(meta);
  }

  private InputStream getMergedInputStream(UUID sessionId) {
    try {
      Connection conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      PGConnection pgConn = conn.unwrap(PGConnection.class);
      LargeObjectManager lobj = pgConn.getLargeObjectAPI();

      List<Long> chunkOids = chunkDao.findChunkOidsBySessionId(sessionId, conn);

      List<InputStream> streams = new ArrayList<>();

      for (long oid : chunkOids) {
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

      // Ensure connection is closed when all InputStreams are closed
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

  @Transactional(readOnly = true)
  public List<Integer> getChunkIndexes(UUID id) {
    return chunkRepo.findUploadedChunkIndexes(id);
  }

  public List<UploadSession> getFinishedUploadSessions() {
    return sessionRepo.findByCompletedTrue();
  }

  public List<UploadSession> getUnfinishedUploadSessions() {
    return sessionRepo.findByCompletedFalse();
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
    meta.setStatus(ZipMetaDataStatusEnum.SUCCESSFUL.getValue());
    zipEntryMetadataRepository.save(meta);
  }


  private void handleErrorStoringUploadedChunk(Integer chunkIndex, Exception exception,
      UploadSession session,
      byte[] chunkBytes) {
    String reason = exception.getClass().getSimpleName() + ": " + exception.getMessage();

    log.error("Failed to persist uploaded chunk (index: {}) for session {}", chunkIndex,
        session.getId(), exception);
    storeFailedChunk(chunkIndex, session, chunkBytes, reason);

    throw new RuntimeException("Failed to save uploaded chunk", exception);
  }

  private void handleErrorUploadingBytes(Integer chunkIndex, IOException e, UploadSession session,
      byte[] chunkBytes) throws IOException {
    String reason = e.getClass().getSimpleName() + ": " + e.getMessage();

    storeFailedChunk(chunkIndex, session, chunkBytes, reason);

    log.error("Failed to read bytes from uploaded chunk with chunk index: {}", chunkIndex, e);
    throw new IOException("Failed to read chunk data", e);
  }

  private void storeFailedChunk(Integer chunkIndex, UploadSession session, byte[] chunkBytes,
      String reason) {
    UploadChunk uploadChunk = new UploadChunk();
    uploadChunk.setUploadSession(session);
    uploadChunk.setChunkIndex(chunkIndex);
    uploadChunk.setChunkData(chunkBytes);
    uploadChunk.setStatus(ChunkUploadStatusEnum.SUSPENDED.getValue());
    uploadChunk.setFailureReason(
        reason.length() > 1024 ? reason.substring(0, 1020) + "..." : reason);
    chunkRepo.save(uploadChunk);
  }

}
