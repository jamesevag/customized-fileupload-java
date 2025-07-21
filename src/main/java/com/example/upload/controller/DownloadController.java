package com.example.upload.controller;

import com.example.upload.model.UploadSession;
import com.example.upload.repository.UploadSessionRepository;
import com.example.upload.service.FileService;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/download")
public class DownloadController {

  private final UploadSessionRepository sessionRepo;
  private final FileService fileService;

  public DownloadController(UploadSessionRepository sessionRepo,
      FileService downloadService) {
    this.sessionRepo = sessionRepo;
    this.fileService = downloadService;
  }

  @GetMapping("/{id}")
  public ResponseEntity<StreamingResponseBody> downloadFromDatabase(@PathVariable UUID id) {
    UploadSession session = sessionRepo.findById(id).orElseThrow();

    StreamingResponseBody response = fileService.streamFile(session.getId());

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + session.getFileName() + "\"")
        .header(HttpHeaders.CONTENT_ENCODING, "identity") // disables compression
        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(response);
  }

  @GetMapping("/{id}/zip")
  public ResponseEntity<StreamingResponseBody> downloadAsZip(@PathVariable UUID id) {
    UploadSession session = sessionRepo.findById(id).orElseThrow();
    String zipName = session.getFileName().replaceAll("(?i)\\.txt$", ""); // case-insensitive .txt

    StreamingResponseBody body = fileService.downloadAsZip(id, session.getFileName());

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + ".zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(body);
  }

}
