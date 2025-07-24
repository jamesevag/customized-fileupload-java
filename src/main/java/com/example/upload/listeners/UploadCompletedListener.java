package com.example.upload.listeners;

import com.example.upload.model.UploadSession;
import com.example.upload.service.FileService;
import com.example.upload.events.UploadCompletedEvent;
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