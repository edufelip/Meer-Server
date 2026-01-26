package com.edufelip.meer.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.dto.NearbyStoreDto;
import com.edufelip.meer.dto.ThriftStoreDto;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MappersTest {

  @Test
  void nearbyStoreDtoMasksAddressForOnlineStore() {
    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());
    store.setName("Online Boutique");
    store.setAddressLine("Rua Secret, 123");
    store.setNeighborhood("Centro");
    store.setIsOnlineStore(true);
    store.setLatitude(1.0);
    store.setLongitude(1.0);

    NearbyStoreDto dto = new NearbyStoreDto(store, 0.0, 0.0, false, null, null);

    assertThat(dto.addressLine()).isEqualTo("Centro");
  }

  @Test
  void toDtoMasksAddressForOnlineStore() {
    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());
    store.setName("Online Boutique");
    store.setAddressLine("Rua Secret, 123, Centro, City - SP");
    store.setNeighborhood("Centro");
    store.setIsOnlineStore(true);
    store.setLatitude(1.0);
    store.setLongitude(1.0);

    ThriftStoreDto dto = Mappers.toDto(store, false);

    assertThat(dto.isOnlineStore()).isTrue();
    assertThat(dto.addressLine()).isEqualTo("Centro");
    assertThat(dto.neighborhood()).isEqualTo("Centro");
  }

  @Test
  void toDtoShowsFullAddressForPhysicalStore() {
    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());
    store.setName("Physical Store");
    store.setAddressLine("Rua Public, 123, Centro, City - SP");
    store.setNeighborhood("Centro");
    store.setIsOnlineStore(false);
    store.setLatitude(1.0);
    store.setLongitude(1.0);

    ThriftStoreDto dto = Mappers.toDto(store, false);

    assertThat(dto.isOnlineStore()).isFalse();
    assertThat(dto.addressLine()).isEqualTo("Rua Public, 123, Centro, City - SP");
  }

  @Test
  void toDtoMasksAddressWithFallbackIfNeighborhoodMissing() {
    ThriftStore store = new ThriftStore();
    store.setId(UUID.randomUUID());
    store.setName("Online Boutique");
    store.setAddressLine("Rua Secret, 123");
    store.setNeighborhood(null); // No neighborhood
    store.setIsOnlineStore(true);
    store.setLatitude(1.0);
    store.setLongitude(1.0);

    ThriftStoreDto dto = Mappers.toDto(store, false);

    assertThat(dto.isOnlineStore()).isTrue();
    assertThat(dto.addressLine()).isEqualTo("Loja Online");
  }
}
