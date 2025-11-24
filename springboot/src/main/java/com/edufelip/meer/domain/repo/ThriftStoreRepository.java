package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.store.ThriftStore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThriftStoreRepository extends JpaRepository<ThriftStore, Integer> {}
