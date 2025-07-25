package de.adesso.fileupload.repository;

import de.adesso.fileupload.entity.UploadChunk;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadChunkRepository extends JpaRepository<UploadChunk, UUID> {
  @Query("SELECT c.chunkIndex FROM UploadChunk c WHERE c.uploadSession.id = :sessionId")
  List<Integer> findUploadedChunkIndexes(UUID sessionId);

  @Query(value = "SELECT chunk_data FROM upload_chunk WHERE upload_session_id = :id ORDER BY chunk_index ASC", nativeQuery = true)
  List<byte[]> findByUploadSessionIdOrderByChunkIndexAsc(@Param("id") UUID sessionId);
}
