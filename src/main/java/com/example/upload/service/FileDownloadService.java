package com.example.upload.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.postgresql.PGConnection;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
public class FileDownloadService {

  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  public FileDownloadService(JdbcTemplate jdbcTemplate, DataSource dataSource) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
  }

  public StreamingResponseBody streamFile(UUID sessionId) {
    return outputStream -> {
      Connection connection = null;
      try {
        connection = dataSource.getConnection(); // ✅ avoid Spring-managed connection
        connection.setAutoCommit(false);         // ✅ required for PostgreSQL large objects

        PGConnection pgConnection = connection.unwrap(PGConnection.class);
        LargeObjectManager lobj = pgConnection.getLargeObjectAPI();

        PreparedStatement stmt = connection.prepareStatement(
            "SELECT chunk_data FROM upload_chunk WHERE upload_session_id = ? ORDER BY chunk_index ASC"
        );
        stmt.setObject(1, sessionId);
        ResultSet rs = stmt.executeQuery();

        byte[] buffer = new byte[8192];
        long totalBytes = 0;

        while (rs.next()) {
          long oid = rs.getLong("chunk_data");
          System.out.println("Streaming chunk with OID: " + oid);

          try (LargeObject obj = lobj.open(oid, LargeObjectManager.READ)) {
            int bytesRead;
            while ((bytesRead = obj.read(buffer, 0, buffer.length)) > 0) {
              outputStream.write(buffer, 0, bytesRead);
              outputStream.flush(); // ✅ force send every chunk
              totalBytes += bytesRead;
            }
          }
        }

        outputStream.flush();
        connection.commit();

        System.out.println("✅ Streaming complete. Sent " + totalBytes + " bytes.");

      } catch (Exception e) {
        if (connection != null) {
          try {
            connection.rollback();
          } catch (SQLException ex) {
            ex.printStackTrace();
          }
        }
        throw new RuntimeException(e);
      } finally {
        if (connection != null) {
          try {
            connection.setAutoCommit(true);
            connection.close();
          } catch (SQLException ex) {
            ex.printStackTrace();
          }
        }
      }
    };
  }



}