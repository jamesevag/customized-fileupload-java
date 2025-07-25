package de.adesso.fileupload.entity;

import static de.adesso.fileupload.entity.UploadChunk.TABLE_NAME;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = TABLE_NAME)
public class UploadChunk {

  protected static final String TABLE_NAME = "upload_chunk";
  private static final String CHUNK_INDEX = "chunk_index";
  private static final String CHUNK_DATA = "chunk_data";
  private static final String UPLOAD_SESSION_ID = "upload_session_id";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = CHUNK_INDEX)
  private Integer chunkIndex;

  @Lob
  @Column(name = CHUNK_DATA, nullable = false)
  private byte[] chunkData;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = UPLOAD_SESSION_ID)
  private UploadSession uploadSession;

}
