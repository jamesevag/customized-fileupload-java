package com.example.upload.model;

import static jakarta.persistence.GenerationType.AUTO;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ZipEntryMetadata {

  @Id
  @GeneratedValue(strategy = AUTO)
  private Long id;

  private String path; // path inside the zip

  private boolean directory;

  private long size;

  private long compressedSize;

  @ManyToOne
  @JoinColumn(name = "upload_session_id")
  private UploadSession uploadSession;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String name) {
    this.path = name;
  }

  public boolean isDirectory() {
    return directory;
  }

  public void setDirectory(boolean directory) {
    this.directory = directory;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public long getCompressedSize() {
    return compressedSize;
  }

  public void setCompressedSize(long compressedSize) {
    this.compressedSize = compressedSize;
  }

  public UploadSession getUploadSession() {
    return uploadSession;
  }

  public void setUploadSession(UploadSession uploadSession) {
    this.uploadSession = uploadSession;
  }


}