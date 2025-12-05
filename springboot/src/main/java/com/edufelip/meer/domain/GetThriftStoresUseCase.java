package com.edufelip.meer.domain;

import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public class GetThriftStoresUseCase {
    private final ThriftStoreRepository thriftStoreRepository;
    private final boolean preferPostgres;
    private final boolean postgisEnabled;

    public GetThriftStoresUseCase(ThriftStoreRepository thriftStoreRepository,
                                  @Value("${spring.datasource.url:}") String datasourceUrl,
                                  @Value("${meer.postgis.enabled:false}") boolean postgisEnabled) {
        this.thriftStoreRepository = thriftStoreRepository;
        this.preferPostgres = datasourceUrl != null && datasourceUrl.contains("postgresql");
        this.postgisEnabled = postgisEnabled;
    }

    public List<ThriftStore> execute() {
        return thriftStoreRepository.findAll();
    }

    public Page<ThriftStore> executePaged(int page, int pageSize) {
        return thriftStoreRepository.findAll(PageRequest.of(page, pageSize));
    }

    @Cacheable("featuredTop10")
    public List<ThriftStore> executeRecentTop10() {
        return thriftStoreRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public Page<ThriftStore> executeNearby(double lat, double lng, int page, int pageSize) {
        if (preferPostgres && postgisEnabled) {
            try {
                // Prefer PostGIS geography KNN if extension + index are present
                return thriftStoreRepository.findNearbyGeography(lat, lng, PageRequest.of(page, pageSize));
            } catch (DataAccessException ex) {
                // fall through to point KNN if geography unavailable
            }
            try {
                return thriftStoreRepository.findNearbyKnn(lat, lng, PageRequest.of(page, pageSize));
            } catch (DataAccessException ex) {
                // fall through to portable query if index missing
            }
        }
        return thriftStoreRepository.findNearbyHaversine(lat, lng, PageRequest.of(page, pageSize));
    }
}
