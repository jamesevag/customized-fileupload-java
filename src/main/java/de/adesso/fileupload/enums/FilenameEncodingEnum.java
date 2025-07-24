package de.adesso.fileupload.enums;

public enum FilenameEncodingEnum {

  UTF8("UTF8"),
  CP437("CP437"),
  MIXED("Mixed UTF-8 and CP437"),
  UNKNOWN("Unknown");

  private final String value;

  FilenameEncodingEnum(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}