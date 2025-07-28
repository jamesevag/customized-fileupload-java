package de.adesso.fileupload.event;

import de.adesso.fileupload.entity.UploadSession;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UploadCompletedEvent extends ApplicationEvent {
  private final UploadSession session;

  public UploadCompletedEvent(UploadSession session) {
    super(session);
    this.session = session;
  }
}