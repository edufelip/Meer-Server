package com.edufelip.meer.web;

import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.core.push.PushPlatform;
import com.edufelip.meer.domain.DeletePushTokenUseCase;
import com.edufelip.meer.domain.UpsertPushTokenUseCase;
import com.edufelip.meer.dto.PushTokenRequest;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.security.token.TokenPayload;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/push-tokens")
public class PushTokenController {

  private final AuthUserResolver authUserResolver;
  private final UpsertPushTokenUseCase upsertPushTokenUseCase;
  private final DeletePushTokenUseCase deletePushTokenUseCase;

  public PushTokenController(
      AuthUserResolver authUserResolver,
      UpsertPushTokenUseCase upsertPushTokenUseCase,
      DeletePushTokenUseCase deletePushTokenUseCase) {
    this.authUserResolver = authUserResolver;
    this.upsertPushTokenUseCase = upsertPushTokenUseCase;
    this.deletePushTokenUseCase = deletePushTokenUseCase;
  }

  @PostMapping
  public ResponseEntity<Void> upsert(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody @Valid PushTokenRequest body) {
    TokenPayload payload = authUserResolver.requirePayload(authHeader);
    PushPlatform platform = PushPlatform.parse(body.platform());
    PushEnvironment environment = PushEnvironment.parse(body.environment());
    upsertPushTokenUseCase.execute(
        payload.getUserId(),
        body.deviceId(),
        body.fcmToken(),
        platform,
        body.appVersion(),
        environment);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{deviceId}")
  public ResponseEntity<Void> delete(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String deviceId,
      @RequestParam(name = "environment", required = false) String environment) {
    TokenPayload payload = authUserResolver.requirePayload(authHeader);
    PushEnvironment env =
        environment != null && !environment.isBlank() ? PushEnvironment.parse(environment) : null;
    deletePushTokenUseCase.execute(payload.getUserId(), deviceId, env);
    return ResponseEntity.noContent().build();
  }
}
