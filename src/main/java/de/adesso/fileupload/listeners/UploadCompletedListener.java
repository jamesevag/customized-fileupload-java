package de.adesso.fileupload.listeners;

import de.adesso.fileupload.model.UploadSession;
import de.adesso.fileupload.service.FileService;
import de.adesso.fileupload.events.UploadCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadCompletedListener {

  private final FileService fileService;

  @Async
  @EventListener
  public void handleUploadComplete(UploadCompletedEvent event) {
    UploadSession session = event.getSession();
    log.info("📦 Upload complete for session: {} — running encoding detection", session.getId());
    fileService.detectAndStoreEncodingForZip(session);
  }
}