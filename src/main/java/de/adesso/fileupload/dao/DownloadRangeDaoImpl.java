package de.adesso.fileupload.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class DownloadRangeDaoImpl implements DownloadRangeDao {

  @Override
  public List<Long> findChunkOidsBySessionId(UUID sessionId, Connection connection) {
    List<Long> oids = new ArrayList<>();

    try (PreparedStatement stmt = connection.prepareStatement(
        "SELECT chunk_data FROM upload_chunk WHERE upload_session_id = ? ORDER BY chunk_index ASC"
    )) {
      stmt.setObject(1, sessionId);

      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          oids.add(rs.getLong("chunk_data"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return oids;
  }
}
