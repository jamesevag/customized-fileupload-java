package com.example.upload.model;

import jakarta.persistence.*;
import java.sql.Blob;
import java.util.UUID;

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

    public UUID getId() { return id; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public UploadSession getUploadSession() { return uploadSession; }
    public void setUploadSession(UploadSession uploadSession) { this.uploadSession = uploadSession; }

    public  byte[] getChunkData() { return chunkData; }
    public void setChunkData( byte[] chunkData) { this.chunkData = chunkData; }
}
