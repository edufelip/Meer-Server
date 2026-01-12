package com.edufelip.meer.web;

import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.FeedbackRequest;
import com.edufelip.meer.dto.FeedbackResponse;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.service.StoreFeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/stores/{storeId}/feedback")
public class StoreFeedbackController {

  private final AuthUserResolver authUserResolver;
  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreFeedbackService storeFeedbackService;

  public StoreFeedbackController(
      AuthUserResolver authUserResolver,
      ThriftStoreRepository thriftStoreRepository,
      StoreFeedbackService storeFeedbackService) {
    this.authUserResolver = authUserResolver;
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeFeedbackService = storeFeedbackService;
  }

  // Upsert (create/update) feedback for current user on a store
  @PostMapping
  public FeedbackResponse upsert(
      @PathVariable java.util.UUID storeId,
      @RequestBody @Valid FeedbackRequest body,
      @RequestHeader("Authorization") String authHeader) {
    var user = authUserResolver.requireUser(authHeader);
    var store =
        thriftStoreRepository
            .findById(storeId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    var fb = storeFeedbackService.upsert(user, store, body.score(), body.body());
    return new FeedbackResponse(fb.getScore(), fb.getBody());
  }

  // Get current user's feedback on a store
  @GetMapping
  public ResponseEntity<FeedbackResponse> getMine(
      @PathVariable java.util.UUID storeId, @RequestHeader("Authorization") String authHeader) {
    var user = authUserResolver.requireUser(authHeader);
    return storeFeedbackService
        .find(user.getId(), storeId)
        .map(fb -> ResponseEntity.ok(new FeedbackResponse(fb.getScore(), fb.getBody())))
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  // Delete current user's feedback on a store (idempotent)
  @DeleteMapping
  public ResponseEntity<Void> delete(
      @PathVariable java.util.UUID storeId, @RequestHeader("Authorization") String authHeader) {
    var user = authUserResolver.requireUser(authHeader);
    // ensure store exists for proper 404 semantics
    thriftStoreRepository
        .findById(storeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
    storeFeedbackService.delete(user.getId(), storeId);
    return ResponseEntity.noContent().build();
  }
}
