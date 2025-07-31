package de.adesso.fileupload.controller;

import static de.adesso.fileupload.util.ClientIpResolver.getClientsIp;

import de.adesso.fileupload.entity.UploadSession;
import de.adesso.fileupload.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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

  private final UploadService uploadService;

  public UploadController(final UploadService uploadService) {
    this.uploadService = uploadService;
  }

  @PostMapping("/init")
  public ResponseEntity<?> initUpload(@RequestParam String fileName, @RequestParam long totalSize,
      HttpServletRequest request) {
    UploadSession session = uploadService.saveUploadSession(fileName, totalSize,
        getClientsIp(request));
    return ResponseEntity.ok(Map.of("uploadId", session.getId()));
  }

  @PatchMapping("/{id}/chunk")
  public ResponseEntity<?> uploadChunk(@PathVariable UUID id,
      @RequestParam("chunkIndex") Integer chunkIndex,
      @RequestParam("chunk") MultipartFile chunk) throws Exception {
    uploadService.saveUploadedChunk(id, chunkIndex, chunk);

    return ResponseEntity.ok("Chunk " + chunkIndex + " uploaded");
  }


  @GetMapping("/{id}/uploadedChunks")
  public ResponseEntity<List<Integer>> getUploadedChunks(@PathVariable UUID id) {
    return ResponseEntity.ok(uploadService.getChunkIndexes(id));
  }

  @GetMapping("/unfinished")
  public List<UploadSession> getUnfinishedSessions() {
    return uploadService.getUnfinishedUploadSessions();
  }

  @GetMapping("/finished")
  public List<UploadSession> getFinishedSessions() {
    return uploadService.getFinishedUploadSessions();
  }

}
