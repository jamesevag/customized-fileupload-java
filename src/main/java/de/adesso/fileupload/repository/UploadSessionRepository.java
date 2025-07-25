package de.adesso.fileupload.repository;

import de.adesso.fileupload.entity.UploadSession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

  List<UploadSession> findByCompletedFalse();

  List<UploadSession> findByCompletedTrue();

  @Query("SELECT u.totalSize FROM UploadSession u WHERE u.id = :id")
  long getTotalSize(@Param("id") UUID id);

  UploadSession findByFileName(String fileName);
}
