package com.example.upload.repository;

import com.example.upload.model.UploadSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

  List<UploadSession> findByCompletedFalse();
  List<UploadSession> findByCompletedTrue();

}
