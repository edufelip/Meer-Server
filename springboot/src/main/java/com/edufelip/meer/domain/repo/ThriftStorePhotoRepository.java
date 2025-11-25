package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.store.ThriftStorePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThriftStorePhotoRepository extends JpaRepository<ThriftStorePhoto, Integer> {}
