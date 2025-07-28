package de.adesso.fileupload.controller;

import de.adesso.fileupload.entity.UploadSession;
import de.adesso.fileupload.service.DownloadService;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/download")
public class DownloadController {

  private final DownloadService downloadService;

  public DownloadController(
      DownloadService downloadService) {
    this.downloadService = downloadService;
  }

  @Deprecated
  @GetMapping("/{id}/deprecated")
  public ResponseEntity<StreamingResponseBody> downloadFromDatabase(@PathVariable UUID id) {
    UploadSession session = downloadService.findById(id).orElseThrow();

    StreamingResponseBody response = downloadService.streamFile(session.getId());

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
    UploadSession session = downloadService.findById(id).orElseThrow();
    String zipName = session.getFileName().replaceAll("(?i)\\.txt$", ""); // case-insensitive .txt

    StreamingResponseBody body = downloadService.downloadAsZip(id, session.getFileName());

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + ".zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(body);
  }


  @GetMapping("/{id}")
  public ResponseEntity<StreamingResponseBody> downloadWithRangeSupport(
      @PathVariable UUID id,
      @RequestHeader(value = "Range", required = false) String rangeHeader) {

    UploadSession session = downloadService.findById(id).orElseThrow();
    long totalSize = session.getTotalSize();
    long rangeStart = 0;
    long rangeEnd = totalSize - 1;

    if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
      String[] parts = rangeHeader.replace("bytes=", "").split("-");
      rangeStart = Long.parseLong(parts[0]);
      if (parts.length > 1 && !parts[1].isBlank()) {
        rangeEnd = Long.parseLong(parts[1]);
      }
    }

    final long finalStart = rangeStart;
    final long finalEnd = rangeEnd;
    log.info("Download request: id={}, range={}, totalSize={}", id, rangeStart + "-" + rangeEnd,
        totalSize);

    StreamingResponseBody responseBody = outputStream ->
        downloadService.streamFileRange(id, finalStart, finalEnd, outputStream);

    return ResponseEntity.status((rangeHeader != null) ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + session.getFileName() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(finalEnd - finalStart + 1))
        .header(HttpHeaders.CONTENT_RANGE, "bytes " + finalStart + "-" + finalEnd + "/" + totalSize)
        .header(HttpHeaders.ETAG, "\"" + session.getId().toString() + "\"")
        .header(HttpHeaders.LAST_MODIFIED, session.getCreatedAt().toString())
        .body(responseBody);
  }


}
