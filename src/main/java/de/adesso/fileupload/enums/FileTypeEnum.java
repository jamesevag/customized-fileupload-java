package de.adesso.fileupload.enums;

public enum FileTypeEnum {

  ZIP(".zip"),
  TXT(".txt"),
  OTHER("Other");

  private final String value;

  FileTypeEnum(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}