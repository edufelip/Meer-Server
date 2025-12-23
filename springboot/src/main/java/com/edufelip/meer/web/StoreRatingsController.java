package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.dto.StoreRatingDto;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/stores/{storeId}/ratings")
public class StoreRatingsController {

    private final TokenProvider tokenProvider;
    private final AuthUserRepository authUserRepository;
    private final ThriftStoreRepository thriftStoreRepository;
    private final StoreFeedbackRepository storeFeedbackRepository;

    public StoreRatingsController(TokenProvider tokenProvider,
                                  AuthUserRepository authUserRepository,
                                  ThriftStoreRepository thriftStoreRepository,
                                  StoreFeedbackRepository storeFeedbackRepository) {
        this.tokenProvider = tokenProvider;
        this.authUserRepository = authUserRepository;
        this.thriftStoreRepository = thriftStoreRepository;
        this.storeFeedbackRepository = storeFeedbackRepository;
    }

    @GetMapping
    public PageResponse<StoreRatingDto> list(@PathVariable java.util.UUID storeId,
                                             @RequestHeader("Authorization") String authHeader,
                                             @RequestParam(name = "page", defaultValue = "1") int page,
                                             @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
        }
        currentUser(authHeader);
        thriftStoreRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));

        var pageable = PageRequest.of(page - 1, pageSize);
        var slice = storeFeedbackRepository.findRatingsByStoreId(storeId, pageable);
        return new PageResponse<>(slice.getContent(), page, slice.hasNext());
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
