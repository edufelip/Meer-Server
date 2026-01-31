package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
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

  @Query("select u from AuthUser u where (u.role <> :admin or u.role is null)")
  Page<AuthUser> findNonAdminUsers(@Param("admin") Role admin, Pageable pageable);

  @Query(
      "select u from AuthUser u where (u.role <> :admin or u.role is null) and "
          + "(lower(u.email) like lower(concat('%', :term, '%')) escape '!' "
          + "or lower(u.displayName) like lower(concat('%', :term, '%')) escape '!')")
  Page<AuthUser> searchNonAdminUsers(
      @Param("term") String term, @Param("admin") Role admin, Pageable pageable);

  @Modifying
  @Transactional
  @Query(
      value = "delete from auth_user_favorites where thrift_store_id = :storeId",
      nativeQuery = true)
  void deleteFavoritesByStoreId(@Param("storeId") UUID storeId);

  @Modifying
  @Transactional
  @Query(value = "delete from auth_user_favorites where auth_user_id = :userId", nativeQuery = true)
  void deleteFavoritesByUserId(@Param("userId") UUID userId);
}
