package com.example.upload.repository;

import com.example.upload.model.UploadSession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

  List<UploadSession> findByCompletedFalse();

  List<UploadSession> findByCompletedTrue();

  UploadSession findByFileName(String fileName);
}
