package de.adesso.fileupload.dao;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

public interface ChunkDao {
   List<Long> findChunkOidsBySessionId(UUID sessionId, Connection connection);
}
