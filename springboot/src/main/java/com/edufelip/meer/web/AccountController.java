package com.edufelip.meer.web;

import com.edufelip.meer.domain.auth.DeleteUserUseCase;
import com.edufelip.meer.domain.auth.GetProfileUseCase;
import com.edufelip.meer.dto.DeleteAccountRequest;
import com.edufelip.meer.security.AuthUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountController {
  private final GetProfileUseCase getProfileUseCase;
  private final DeleteUserUseCase deleteUserUseCase;
  private final AuthUserResolver authUserResolver;

  public AccountController(
      GetProfileUseCase getProfileUseCase,
      DeleteUserUseCase deleteUserUseCase,
      AuthUserResolver authUserResolver) {
    this.getProfileUseCase = getProfileUseCase;
    this.deleteUserUseCase = deleteUserUseCase;
    this.authUserResolver = authUserResolver;
  }

  @DeleteMapping("/account")
  public ResponseEntity<Void> deleteAccount(
      @RequestHeader("Authorization") String authHeader, @RequestBody DeleteAccountRequest body) {
    String token = authUserResolver.requireBearer(authHeader);
    var user = getProfileUseCase.execute(token);
    if (body == null || body.email() == null || !body.email().equalsIgnoreCase(user.getEmail())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email confirmation does not match");
    }
    deleteUserUseCase.execute(user, "ACCOUNT_DELETE");
    return ResponseEntity.noContent().build();
  }
}
