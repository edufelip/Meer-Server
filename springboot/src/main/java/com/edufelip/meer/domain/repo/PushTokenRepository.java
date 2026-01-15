package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.core.push.PushToken;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {
  Optional<PushToken> findByUserIdAndDeviceIdAndEnvironment(
      UUID userId, String deviceId, PushEnvironment environment);

  List<PushToken> findByUserIdAndEnvironment(UUID userId, PushEnvironment environment);

  @Modifying
  @Transactional
  @Query(
      "delete from PushToken t where t.userId = :userId and t.deviceId = :deviceId and t.environment = :environment")
  int deleteByUserDeviceAndEnvironment(
      @Param("userId") UUID userId,
      @Param("deviceId") String deviceId,
      @Param("environment") PushEnvironment environment);

  @Modifying
  @Transactional
  @Query("delete from PushToken t where t.userId = :userId and t.deviceId = :deviceId")
  int deleteByUserAndDevice(@Param("userId") UUID userId, @Param("deviceId") String deviceId);

  @Modifying
  @Transactional
  @Query("delete from PushToken t where t.userId = :userId")
  int deleteByUserId(@Param("userId") UUID userId);

  List<PushToken> findByUserIdIn(List<UUID> userIds);
}
