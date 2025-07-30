package de.adesso.fileupload.entity;

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
@Table(name = UploadSession.TABLE_NAME)
public class UploadSession {

  protected static final String TABLE_NAME = "upload_session";
  private static final String FILE_NAME = "file_name";
  private static final String TOTAL_SIZE = "total_size";
  private static final String UPLOADED_SIZE = "uploaded_size";
  private static final String COMPLETED = "completed";
  private static final String CREATED_AT = "created_at";
  private static final String CREATED_BY = "created_by";
  private static final String INITIATING_IP = "initiating_ip";

  @Id
  private UUID id;

  @Column(name = FILE_NAME)
  private String fileName;

  @Column(name = TOTAL_SIZE, nullable = false)
  private long totalSize;

  @Column(name = UPLOADED_SIZE, nullable = false)
  private long uploadedSize;

  @Column(name = COMPLETED)
  private boolean completed;

  @Column(name = INITIATING_IP)
  private String initiatingIp;

  @Column(name = CREATED_AT, nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = CREATED_BY, nullable = false, updatable = false)
  private String createdBy;
}
