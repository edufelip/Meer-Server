package com.edufelip.meer.domain;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class StoreCategoryNormalizer {
  private StoreCategoryNormalizer() {}

  public static List<String> normalize(List<String> categories) {
    if (categories == null) return null;
    return categories.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(String::toLowerCase)
        .distinct()
        .collect(Collectors.toCollection(java.util.ArrayList::new));
  }
}
