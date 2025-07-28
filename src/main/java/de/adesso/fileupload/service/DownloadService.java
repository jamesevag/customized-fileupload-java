package de.adesso.fileupload.service;

import de.adesso.fileupload.entity.UploadSession;
import de.adesso.fileupload.repository.UploadSessionRepository;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DownloadService {

  private final UploadSessionRepository sessionRepo;
  private final DataSource dataSource;

  public DownloadService(DataSource dataSource, UploadSessionRepository sessionRepo) {
    this.dataSource = dataSource;
    this.sessionRepo = sessionRepo;
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
