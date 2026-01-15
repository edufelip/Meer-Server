package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.auth.AuthUser;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
  AuthUser findByEmail(String email);

  Page<AuthUser> findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
      String email, String name, Pageable pageable);

  @Modifying
  @Transactional
  @Query(
      value = "delete from auth_user_favorites where thrift_store_id = :storeId",
      nativeQuery = true)
  void deleteFavoritesByStoreId(@Param("storeId") UUID storeId);

  @Modifying
  @Transactional
  @Query(
      value = "delete from auth_user_favorites where auth_user_id = :userId",
      nativeQuery = true)
  void deleteFavoritesByUserId(@Param("userId") UUID userId);
}
