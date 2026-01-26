package com.edufelip.meer.dto;

import com.edufelip.meer.core.store.ThriftStore;

public final class StoreDtoCalculations {
  private StoreDtoCalculations() {}

  public static String firstPhotoOrCover(ThriftStore store) {
    if (store == null) return null;
    if (store.getPhotos() != null && !store.getPhotos().isEmpty()) {
      return store.getPhotos().get(0).getUrl();
    }
    return store.getCoverImageUrl();
  }

  public static String maskedAddressLine(ThriftStore store) {
    if (store == null) return null;
    if (Boolean.TRUE.equals(store.getIsOnlineStore())) {
      // Requirement: "Cidade, Bairro"
      // Since we don't have structured city, we use neighborhood.
      String neighborhood = store.getNeighborhood();
      if (neighborhood != null && !neighborhood.isBlank()) {
        return neighborhood;
      } else {
        return "Loja Online";
      }
    }
    return store.getAddressLine();
  }

  public static Double distanceMeters(Double lat1, Double lon1, Double lat2, Double lon2) {
    if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;
    double R = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c * 1000.0;
  }

  static Integer walkMinutes(Double distanceMeters) {
    return distanceMeters != null ? (int) Math.round(distanceMeters / 80.0) : null;
  }
}
