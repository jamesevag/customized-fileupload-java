package com.example.upload.model;

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
@Table(name = "zip_entry_metadata")
public class ZipEntryMetadata {

  @Id
  @GeneratedValue(strategy = AUTO)
  private Long id;

  @Column(name = "path", nullable = false)
  private String path;

  @Column(name = "directory")
  private boolean directory;

  @Column(name = "size")
  private long size;

  @Column(name = "compressedSize")
  private long compressedSize;

  @ManyToOne
  @JoinColumn(name = "upload_session_id")
  private UploadSession uploadSession;

}