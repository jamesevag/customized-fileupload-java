package com.example.upload.controller;

import com.example.upload.model.UploadChunk;
import com.example.upload.model.UploadSession;
import com.example.upload.repository.UploadChunkRepository;
import com.example.upload.repository.UploadSessionRepository;
import com.example.upload.service.FileService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class UploadController {

  private final UploadSessionRepository sessionRepo;
  private final UploadChunkRepository chunkRepo;
  private final FileService fileService;

  public UploadController(UploadSessionRepository sessionRepo,
      UploadChunkRepository chunkRepo, FileService downloadService) {
    this.sessionRepo = sessionRepo;
    this.chunkRepo = chunkRepo;
    this.fileService = downloadService;
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
    boolean anyUtf8 = false;
    boolean anyNonUtf8 = false;

    if (session.getFileName().toLowerCase().endsWith(".zip")) {
      try (InputStream mergedStream = fileService.getMergedInputStream(id)) {
        ZipArchiveInputStream zipIn = new ZipArchiveInputStream(mergedStream);

        ZipArchiveEntry entry;
        while ((entry = zipIn.getNextZipEntry()) != null) {
          if (entry.getGeneralPurposeBit().usesUTF8ForNames()) {
            anyUtf8 = true;
          } else {
            anyNonUtf8 = true;
          }
        }
      } catch (IOException e) {
        session.setEncoding("unknown");
      }
    }
    if (anyUtf8 && !anyNonUtf8) {
      session.setEncoding("UTF-8 (filenames)");
    } else if (!anyUtf8 && anyNonUtf8) {
      session.setEncoding("CP437 (default or unknown)");
    } else if (anyUtf8 && anyNonUtf8) {
      session.setEncoding("Mixed (UTF-8 and CP437)");
    } else {
      session.setEncoding("unknown");
    }

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

}
