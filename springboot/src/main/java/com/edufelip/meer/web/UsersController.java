package com.edufelip.meer.web;

import com.edufelip.meer.domain.auth.AcceptTermsUseCase;
import com.edufelip.meer.domain.auth.TermsVersionMismatchException;
import com.edufelip.meer.dto.AcceptTermsRequest;
import com.edufelip.meer.dto.ProfileDto;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.service.TermsPolicy;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestController
@RequestMapping("/users")
public class UsersController {

  private final AuthUserResolver authUserResolver;
  private final AcceptTermsUseCase acceptTermsUseCase;
  private final TermsPolicy termsPolicy;
  private final ProfileAssembler profileAssembler;

  public UsersController(
      AuthUserResolver authUserResolver,
      AcceptTermsUseCase acceptTermsUseCase,
      TermsPolicy termsPolicy,
      ProfileAssembler profileAssembler) {
    this.authUserResolver = authUserResolver;
    this.acceptTermsUseCase = acceptTermsUseCase;
    this.termsPolicy = termsPolicy;
    this.profileAssembler = profileAssembler;
  }

  @PostMapping("/me/accept-terms")
  public ResponseEntity<Map<String, Object>> acceptTerms(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody @Valid AcceptTermsRequest body) {
    var user = authUserResolver.requireUser(authHeader);
    String requiredVersion = termsPolicy.requiredVersionOrNull();
    var updated =
        acceptTermsUseCase.execute(
            user, new AcceptTermsUseCase.Command(body.termsVersion(), requiredVersion));
    ProfileDto profile = profileAssembler.toProfileDto(updated, true);
    return ResponseEntity.ok(Map.of("user", profile));
  }

  @ExceptionHandler(TermsVersionMismatchException.class)
  public ResponseEntity<Map<String, String>> handleTermsMismatch(
      TermsVersionMismatchException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .orElse("Invalid request");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
  }
}
