package com.example.upload.controller;

import com.example.upload.model.UploadChunk;
import com.example.upload.model.UploadSession;
import com.example.upload.repository.UploadChunkRepository;
import com.example.upload.repository.UploadSessionRepository;
import com.example.upload.service.FileDownloadService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.rowset.serial.SerialBlob;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/upload")
public class UploadController {

  private final UploadSessionRepository sessionRepo;
  private final UploadChunkRepository chunkRepo;
  private final FileDownloadService downloadService;
  public UploadController(UploadSessionRepository sessionRepo, UploadChunkRepository chunkRepo,FileDownloadService downloadService) {
    this.sessionRepo = sessionRepo;
    this.chunkRepo = chunkRepo;

    this.downloadService = downloadService;
  }

  @PostMapping("/init")
  public ResponseEntity<?> initUpload(@RequestParam String fileName, @RequestParam long totalSize) {
    UploadSession session = new UploadSession();
    session.setId(UUID.randomUUID());
    session.setFileName(fileName);
    session.setTotalSize(totalSize);
    session.setComplete(false);
    session.setUploadedSize(0);
    sessionRepo.save(session);
    return ResponseEntity.ok(Map.of("uploadId", session.getId()));
  }

  @PatchMapping("/{id}/chunk")
  public ResponseEntity<?> uploadChunk(@PathVariable UUID id,
      @RequestParam("chunkIndex") Integer chunkIndex,
      @RequestParam("chunk") MultipartFile chunk) throws Exception {
    UploadSession session = sessionRepo.findById(id).orElseThrow();

    UploadChunk uploadChunk = new UploadChunk();
    uploadChunk.setUploadSession(session);
    uploadChunk.setChunkIndex(chunkIndex);
    uploadChunk.setChunkData(chunk.getBytes());

    chunkRepo.save(uploadChunk);

    session.setUploadedSize(session.getUploadedSize() + chunk.getSize());
    sessionRepo.save(session);

    return ResponseEntity.ok("Chunk " + chunkIndex + " uploaded");
  }

  @PostMapping("/{id}/complete")
  public ResponseEntity<?> completeUpload(@PathVariable UUID id) {
    UploadSession session = sessionRepo.findById(id).orElseThrow();
    session.setComplete(true);
    sessionRepo.save(session);
    return ResponseEntity.ok("Upload complete");
  }


  @Transactional(readOnly = true)
  @GetMapping("/{id}/uploadedChunks")
  public ResponseEntity<List<Integer>> getUploadedChunks(@PathVariable UUID id) {
    return ResponseEntity.ok(chunkRepo.findUploadedChunkIndexes(id));
  }

  @GetMapping("/unfinished")
  public List<UploadSession> getUnfinishedSessions() {
    return sessionRepo.findByCompletedFalse();
  }

  @GetMapping("/finished")
  public List<UploadSession> getFinishedSessions() {
    return sessionRepo.findByCompletedTrue();
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<StreamingResponseBody> downloadFromDatabase(@PathVariable UUID id) {
    UploadSession session = sessionRepo.findById(id).orElseThrow();

    StreamingResponseBody response= downloadService.streamFile(session.getId());


    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_ENCODING, "identity") // <- disable compression
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + session.getFileName() + "\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(session.getTotalSize())
        .body(response);
  }
}
