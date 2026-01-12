package com.edufelip.meer.core.push;

public enum PushEnvironment {
  DEV,
  STAGING,
  PROD;

  public static PushEnvironment parse(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("environment is required");
    }
    String normalized = value.trim().toUpperCase();
    return switch (normalized) {
      case "DEV" -> DEV;
      case "STAGING" -> STAGING;
      case "PROD" -> PROD;
      default ->
          throw new IllegalArgumentException("environment must be one of: DEV, STAGING, PROD");
    };
  }
}
