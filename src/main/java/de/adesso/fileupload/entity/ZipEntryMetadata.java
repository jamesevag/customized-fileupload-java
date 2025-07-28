package de.adesso.fileupload.entity;

import static jakarta.persistence.GenerationType.AUTO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = ZipEntryMetadata.TABLE_NAME)
public class ZipEntryMetadata {

  protected static final String TABLE_NAME = "zip_entry_metadata";
  private static final String PATH = "path";
  private static final String DIRECTORY = "directory";
  private static final String SIZE = "size";
  private static final String COMPRESSED_SIZE = "compressedSize";
  private static final String ENCODING = "encoding";
  private static final String UPLOAD_SESSION_ID = "upload_session_id";
  private static final String STATUS = "status";
  private static final String FAILURE_REASON = "failure_reason";

  @Id
  @GeneratedValue(strategy = AUTO)
  private Long id;

  @Column(name = PATH, nullable = true)
  private String path;

  @Column(name = DIRECTORY, nullable = true)
  private boolean directory;

  @Column(name = SIZE, nullable = true)
  private long size;

  @Column(name = COMPRESSED_SIZE, nullable = true)
  private long compressedSize;

  @Column(name = ENCODING, length = 50, nullable = true)
  private String encoding;

  @Column(name = STATUS, nullable = false)
  private String status;

  @Column(name = FAILURE_REASON, length = 1024)
  private String failureReason;

  @ManyToOne
  @JoinColumn(name = UPLOAD_SESSION_ID)
  private UploadSession uploadSession;

}