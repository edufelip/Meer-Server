package com.edufelip.meer.domain;

import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.UUID;

public class GetThriftStoreUseCase {
    private final ThriftStoreRepository thriftStoreRepository;

    public GetThriftStoreUseCase(ThriftStoreRepository thriftStoreRepository) {
        this.thriftStoreRepository = thriftStoreRepository;
    }

    public ThriftStore execute(UUID id) {
        return thriftStoreRepository.findById(id).orElse(null);
    }
}
