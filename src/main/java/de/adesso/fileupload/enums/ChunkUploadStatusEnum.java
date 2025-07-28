package de.adesso.fileupload.enums;

public enum ChunkUploadStatusEnum {

  UPLOADED("uploaded"),
  SUSPENDED("suspended");

  private final String value;

  ChunkUploadStatusEnum(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
