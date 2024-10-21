package com.probely.api;

public enum FindingSeverity {
  NONE("Never"),
  LOW("Low"),
  MEDIUM("Medium"),
  HIGH("High");

  private String value;

  FindingSeverity(final String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return this.getValue();
  }

  public static FindingSeverity fromString(String text) {
    for (FindingSeverity fs : FindingSeverity.values()) {
      if (fs.value.equalsIgnoreCase(text)) {
        return fs;
      }
    }
    throw new IllegalArgumentException("No severity with text " + text + " found");
  }
}
