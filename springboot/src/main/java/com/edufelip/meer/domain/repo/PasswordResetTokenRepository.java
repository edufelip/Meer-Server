package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.auth.PasswordResetToken;
import jakarta.transaction.Transactional;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
  @Modifying
  @Transactional
  @Query("delete from PasswordResetToken t where t.user.id = :userId")
  void deleteByUserId(@Param("userId") UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from PasswordResetToken t join fetch t.user where t.token = :token")
  Optional<PasswordResetToken> findForUpdateByToken(@Param("token") UUID token);
}
