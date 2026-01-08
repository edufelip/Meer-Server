package com.edufelip.meer.web;

import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.dto.StoreRatingDto;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.AuthUserResolver;
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

  private final AuthUserResolver authUserResolver;
  private final ThriftStoreRepository thriftStoreRepository;
  private final StoreFeedbackRepository storeFeedbackRepository;

  public StoreRatingsController(
      AuthUserResolver authUserResolver,
      ThriftStoreRepository thriftStoreRepository,
      StoreFeedbackRepository storeFeedbackRepository) {
    this.authUserResolver = authUserResolver;
    this.thriftStoreRepository = thriftStoreRepository;
    this.storeFeedbackRepository = storeFeedbackRepository;
  }

  @GetMapping
  public PageResponse<StoreRatingDto> list(
      @PathVariable java.util.UUID storeId,
      @RequestHeader(name = "Authorization", required = false) String authHeader,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
    if (page < 1 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    authUserResolver.optionalUser(authHeader);
    thriftStoreRepository
        .findById(storeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));

    var pageable = PageRequest.of(page - 1, pageSize);
    var slice = storeFeedbackRepository.findRatingsByStoreId(storeId, pageable);
    var mapped = slice.map(Mappers::toDto);
    return new PageResponse<>(mapped.getContent(), page, mapped.hasNext());
  }

}
