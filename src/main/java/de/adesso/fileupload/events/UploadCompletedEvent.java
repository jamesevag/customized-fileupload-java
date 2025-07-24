package de.adesso.fileupload.events;

import de.adesso.fileupload.model.UploadSession;
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