package de.adesso.fileupload.listener;

import de.adesso.fileupload.entity.UploadSession;
import de.adesso.fileupload.event.UploadCompletedEvent;
import de.adesso.fileupload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadCompletedListener {

  private final UploadService uploadService;

  @Async
  @EventListener
  public void handleUploadComplete(UploadCompletedEvent event) {
    UploadSession session = event.getSession();
    log.info("📦 Upload complete for session: {} — running encoding detection", session.getId());
    uploadService.storeZipFileMetadata(session);
  }
}