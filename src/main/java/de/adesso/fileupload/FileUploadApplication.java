package de.adesso.fileupload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FileUploadApplication {

  public static void main(String[] args) {
    SpringApplication.run(FileUploadApplication.class, args);
  }
}
