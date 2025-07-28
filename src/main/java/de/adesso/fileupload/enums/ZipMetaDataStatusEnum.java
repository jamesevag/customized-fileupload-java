package de.adesso.fileupload.enums;

public enum ZipMetaDataStatusEnum {

  SUCCESSFUL("successful"),
  FAILED("failed");

  private final String value;

  ZipMetaDataStatusEnum(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

}
