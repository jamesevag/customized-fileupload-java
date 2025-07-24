package de.adesso.fileupload.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "upload_session")
public class UploadSession {

  @Id
  private UUID id;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "total_size", nullable = false)
  private long totalSize;

  @Column(name = "uploaded_size", nullable = false)
  private long uploadedSize;

  @Column(name = "completed")
  private boolean completed;

  @Column(name = "encoding", length = 50)
  private String encoding;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

}
