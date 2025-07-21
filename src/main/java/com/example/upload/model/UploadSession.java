package com.example.upload.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class UploadSession {
    @Id
    private UUID id;

    private String fileName;
    private long totalSize;
    private long uploadedSize;
    @Column(nullable = false)
    private boolean completed = false;

    @Column(length = 50)
    private String encoding;

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

    public long getUploadedSize() { return uploadedSize; }
    public void setUploadedSize(long uploadedSize) { this.uploadedSize = uploadedSize; }

    public boolean isComplete() { return completed; }
    public void setComplete(boolean completed) { this.completed = completed; }
}
