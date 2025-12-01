package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.FeedbackRequest;
import com.edufelip.meer.dto.FeedbackResponse;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.StoreFeedbackService;
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

    private final TokenProvider tokenProvider;
    private final AuthUserRepository authUserRepository;
    private final ThriftStoreRepository thriftStoreRepository;
    private final StoreFeedbackService storeFeedbackService;

    public StoreFeedbackController(TokenProvider tokenProvider,
                                   AuthUserRepository authUserRepository,
                                   ThriftStoreRepository thriftStoreRepository,
                                   StoreFeedbackService storeFeedbackService) {
        this.tokenProvider = tokenProvider;
        this.authUserRepository = authUserRepository;
        this.thriftStoreRepository = thriftStoreRepository;
        this.storeFeedbackService = storeFeedbackService;
    }

    // Upsert (create/update) feedback for current user on a store
    @PostMapping
    public FeedbackResponse upsert(@PathVariable java.util.UUID storeId,
                                   @RequestBody FeedbackRequest body,
                                   @RequestHeader("Authorization") String authHeader) {
        var user = currentUser(authHeader);
        var store = thriftStoreRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        var fb = storeFeedbackService.upsert(user, store, body.score(), body.body());
        return new FeedbackResponse(fb.getScore(), fb.getBody());
    }

    // Get current user's feedback on a store
    @GetMapping
    public ResponseEntity<FeedbackResponse> getMine(@PathVariable java.util.UUID storeId,
                                                    @RequestHeader("Authorization") String authHeader) {
        var user = currentUser(authHeader);
        return storeFeedbackService.find(user.getId(), storeId)
                .map(fb -> ResponseEntity.ok(new FeedbackResponse(fb.getScore(), fb.getBody())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Delete current user's feedback on a store (idempotent)
    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable java.util.UUID storeId,
                                       @RequestHeader("Authorization") String authHeader) {
        var user = currentUser(authHeader);
        // ensure store exists for proper 404 semantics
        thriftStoreRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        storeFeedbackService.delete(user.getId(), storeId);
        return ResponseEntity.noContent().build();
    }

    private AuthUser currentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new InvalidTokenException();
        String token = authHeader.substring("Bearer ".length()).trim();
        TokenPayload payload;
        try {
            payload = tokenProvider.parseAccessToken(token);
        } catch (RuntimeException ex) {
            throw new InvalidTokenException();
        }
        return authUserRepository.findById(payload.getUserId()).orElseThrow(InvalidTokenException::new);
    }
}
