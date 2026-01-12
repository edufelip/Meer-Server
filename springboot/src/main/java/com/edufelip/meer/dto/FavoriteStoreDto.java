package com.edufelip.meer.dto;

import com.edufelip.meer.core.store.ThriftStore;
import java.util.UUID;

public record FavoriteStoreDto(
    UUID id,
    String name,
    String description,
    String coverImageUrl,
    Double latitude,
    Double longitude,
    Boolean isFavorite,
    Double distanceMeters) {

  public FavoriteStoreDto(
      ThriftStore store, Double originLat, Double originLng, Boolean isFavorite) {
    this(
        store.getId(),
        store.getName(),
        store.getDescription(),
        StoreDtoCalculations.firstPhotoOrCover(store),
        store.getLatitude(),
        store.getLongitude(),
        isFavorite,
        StoreDtoCalculations.distanceMeters(
            originLat, originLng, store.getLatitude(), store.getLongitude()));
  }
}
