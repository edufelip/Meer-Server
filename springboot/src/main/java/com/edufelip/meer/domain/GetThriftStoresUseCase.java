package com.edufelip.meer.domain;

import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.List;

public class GetThriftStoresUseCase {
    private final ThriftStoreRepository thriftStoreRepository;

    public GetThriftStoresUseCase(ThriftStoreRepository thriftStoreRepository) {
        this.thriftStoreRepository = thriftStoreRepository;
    }

    public List<ThriftStore> execute() {
        return thriftStoreRepository.findAll();
    }
}
