package com.edufelip.meer.core.push;

public enum PushPlatform {
  ANDROID,
  IOS;

  public static PushPlatform parse(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("platform is required");
    }
    String normalized = value.trim().toUpperCase();
    return switch (normalized) {
      case "ANDROID" -> ANDROID;
      case "IOS" -> IOS;
      default -> throw new IllegalArgumentException("platform must be one of: ANDROID, IOS");
    };
  }
}
