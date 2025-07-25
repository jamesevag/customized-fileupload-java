package de.adesso.fileupload.controller;

import de.adesso.fileupload.entity.UploadSession;
import de.adesso.fileupload.service.FileService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

  private final FileService fileService;

  public UploadController(FileService downloadService) {
    this.fileService = downloadService;
  }

  @PostMapping("/init")
  public ResponseEntity<?> initUpload(@RequestParam String fileName, @RequestParam long totalSize) {
    UploadSession session = fileService.saveUploadSession(fileName, totalSize);
    return ResponseEntity.ok(Map.of("uploadId", session.getId()));
  }

  @PatchMapping("/{id}/chunk")
  public ResponseEntity<?> uploadChunk(@PathVariable UUID id,
      @RequestParam("chunkIndex") Integer chunkIndex,
      @RequestParam("chunk") MultipartFile chunk) throws Exception {
    fileService.saveUploadedChunk(id, chunkIndex, chunk);

    return ResponseEntity.ok("Chunk " + chunkIndex + " uploaded");
  }


  @Deprecated
  @PostMapping("/{id}/complete")
  public ResponseEntity<?> completeUpload(@PathVariable UUID id) {
    fileService.saveSessionAsCompleted(id);
    return ResponseEntity.ok("Upload complete");
  }


  @Transactional(readOnly = true)
  @GetMapping("/{id}/uploadedChunks")
  public ResponseEntity<List<Integer>> getUploadedChunks(@PathVariable UUID id) {
    return ResponseEntity.ok(fileService.getChunkIndexes(id));
  }

  @GetMapping("/unfinished")
  public List<UploadSession> getUnfinishedSessions() {
    return fileService.getUnfinishedUploadSessions();
  }

  @GetMapping("/finished")
  public List<UploadSession> getFinishedSessions() {
    return fileService.getFinishedUploadSessions();
  }

}
