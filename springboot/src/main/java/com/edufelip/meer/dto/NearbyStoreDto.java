package com.edufelip.meer.dto;

import com.edufelip.meer.core.store.ThriftStore;
import java.util.List;
import java.util.UUID;

public record NearbyStoreDto(
    UUID id,
    String name,
    String description,
    String coverImageUrl,
    String addressLine,
    Double latitude,
    Double longitude,
    String neighborhood,
    Boolean isFavorite,
    List<String> categories,
    Double rating,
    Integer reviewCount,
    Double distanceMeters,
    Integer walkTimeMinutes) {

  private record NearbyStoreParts(
      UUID id,
      String name,
      String description,
      String coverImageUrl,
      String addressLine,
      Double latitude,
      Double longitude,
      String neighborhood,
      Boolean isFavorite,
      List<String> categories,
      Double rating,
      Integer reviewCount,
      Double distanceMeters,
      Integer walkTimeMinutes) {}

  public NearbyStoreDto(
      ThriftStore store,
      Double originLat,
      Double originLng,
      Boolean isFavorite,
      Double rating,
      Integer reviewCount) {
    this(buildParts(store, originLat, originLng, isFavorite, rating, reviewCount));
  }

  private NearbyStoreDto(NearbyStoreParts parts) {
    this(
        parts.id(),
        parts.name(),
        parts.description(),
        parts.coverImageUrl(),
        parts.addressLine(),
        parts.latitude(),
        parts.longitude(),
        parts.neighborhood(),
        parts.isFavorite(),
        parts.categories(),
        parts.rating(),
        parts.reviewCount(),
        parts.distanceMeters(),
        parts.walkTimeMinutes());
  }

  private static NearbyStoreParts buildParts(
      ThriftStore store,
      Double originLat,
      Double originLng,
      Boolean isFavorite,
      Double rating,
      Integer reviewCount) {
    Double distanceMeters =
        StoreDtoCalculations.distanceMeters(
            originLat, originLng, store.getLatitude(), store.getLongitude());
    return new NearbyStoreParts(
        store.getId(),
        store.getName(),
        store.getDescription(),
        StoreDtoCalculations.firstPhotoOrCover(store),
        StoreDtoCalculations.maskedAddressLine(store),
        store.getLatitude(),
        store.getLongitude(),
        store.getNeighborhood(),
        isFavorite,
        store.getCategories(),
        rating,
        reviewCount,
        distanceMeters,
        StoreDtoCalculations.walkMinutes(distanceMeters));
  }
}
