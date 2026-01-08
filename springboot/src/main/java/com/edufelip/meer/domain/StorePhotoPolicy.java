package com.edufelip.meer.domain;

import java.util.Set;

public final class StorePhotoPolicy {
  public static final int MAX_PHOTO_COUNT = 10;
  public static final long MAX_PHOTO_BYTES = 2 * 1024 * 1024L;
  public static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/jpg", "image/pjpeg", "image/webp", "image/x-webp");

  private StorePhotoPolicy() {}

  public static boolean isSupportedContentType(String contentType) {
    if (contentType == null) return false;
    return SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase());
  }
}
