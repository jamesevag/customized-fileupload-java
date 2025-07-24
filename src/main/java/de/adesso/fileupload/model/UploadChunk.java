package de.adesso.fileupload.model;

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
@Table(name = "upload_chunk")
public class UploadChunk {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "chunk_index")
  private Integer chunkIndex;

  @Lob
  @Column(name = "chunk_data", nullable = false)
  private byte[] chunkData;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "upload_session_id")
  private UploadSession uploadSession;

}
