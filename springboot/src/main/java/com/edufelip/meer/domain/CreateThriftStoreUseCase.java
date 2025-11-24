package com.edufelip.meer.domain;

import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;

public class CreateThriftStoreUseCase {
    private final ThriftStoreRepository thriftStoreRepository;

    public CreateThriftStoreUseCase(ThriftStoreRepository thriftStoreRepository) {
        this.thriftStoreRepository = thriftStoreRepository;
    }

    public ThriftStore execute(ThriftStore thriftStore) {
        return thriftStoreRepository.save(thriftStore);
    }
}
