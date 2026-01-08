package com.edufelip.meer.dto;

import com.edufelip.meer.core.store.ThriftStore;
import java.util.UUID;

public record FeaturedStoreDto(UUID id, String name, String coverImageUrl) {
  public FeaturedStoreDto(ThriftStore store) {
    this(store.getId(), store.getName(), StoreDtoCalculations.firstPhotoOrCover(store));
  }
}
