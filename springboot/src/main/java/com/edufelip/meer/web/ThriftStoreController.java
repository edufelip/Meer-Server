package com.edufelip.meer.web;

import com.edufelip.meer.domain.CreateThriftStoreUseCase;
import com.edufelip.meer.domain.DeleteThriftStoreUseCase;
import com.edufelip.meer.domain.GetStoreDetailsUseCase;
import com.edufelip.meer.domain.GetStoreListingsUseCase;
import com.edufelip.meer.domain.ReplaceStorePhotosUseCase;
import com.edufelip.meer.domain.RequestStorePhotoUploadsUseCase;
import com.edufelip.meer.domain.UpdateThriftStoreUseCase;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.dto.PhotoRegisterRequest;
import com.edufelip.meer.dto.PhotoUploadRequest;
import com.edufelip.meer.dto.PhotoUploadResponse;
import com.edufelip.meer.dto.StoreRequest;
import com.edufelip.meer.dto.ThriftStoreDto;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.AuthUserResolver;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/stores")
public class ThriftStoreController {

  private final GetStoreListingsUseCase getStoreListingsUseCase;
  private final GetStoreDetailsUseCase getStoreDetailsUseCase;
  private final CreateThriftStoreUseCase createThriftStoreUseCase;
  private final UpdateThriftStoreUseCase updateThriftStoreUseCase;
  private final DeleteThriftStoreUseCase deleteThriftStoreUseCase;
  private final RequestStorePhotoUploadsUseCase requestStorePhotoUploadsUseCase;
  private final ReplaceStorePhotosUseCase replaceStorePhotosUseCase;
  private final AuthUserResolver authUserResolver;

  public ThriftStoreController(
      GetStoreListingsUseCase getStoreListingsUseCase,
      GetStoreDetailsUseCase getStoreDetailsUseCase,
      CreateThriftStoreUseCase createThriftStoreUseCase,
      UpdateThriftStoreUseCase updateThriftStoreUseCase,
      DeleteThriftStoreUseCase deleteThriftStoreUseCase,
      RequestStorePhotoUploadsUseCase requestStorePhotoUploadsUseCase,
      ReplaceStorePhotosUseCase replaceStorePhotosUseCase,
      AuthUserResolver authUserResolver) {
    this.getStoreListingsUseCase = getStoreListingsUseCase;
    this.getStoreDetailsUseCase = getStoreDetailsUseCase;
    this.createThriftStoreUseCase = createThriftStoreUseCase;
    this.updateThriftStoreUseCase = updateThriftStoreUseCase;
    this.deleteThriftStoreUseCase = deleteThriftStoreUseCase;
    this.requestStorePhotoUploadsUseCase = requestStorePhotoUploadsUseCase;
    this.replaceStorePhotosUseCase = replaceStorePhotosUseCase;
    this.authUserResolver = authUserResolver;
  }

  @GetMapping
  public PageResponse<ThriftStoreDto> getStores(
      @RequestParam(name = "type", required = false) String type,
      @RequestParam(name = "categoryId", required = false) String categoryId,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
      @RequestHeader(name = "Authorization", required = false) String authHeader,
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "lat", required = false) Double lat,
      @RequestParam(name = "lng", required = false) Double lng) {
    var user = authUserResolver.optionalUser(authHeader);
    var result =
        getStoreListingsUseCase.execute(
            new GetStoreListingsUseCase.ListingQuery(type, categoryId, q, lat, lng, page, pageSize),
            user);
    var items =
        result.items().stream()
            .map(
                item ->
                    Mappers.toDto(
                        item.store(),
                        false,
                        item.isFavorite(),
                        item.rating(),
                        item.reviewCount(),
                        item.distanceMeters()))
            .toList();
    return new PageResponse<>(items, page, result.hasNext());
  }

  @GetMapping("/{id}")
  public ThriftStoreDto getById(
      @PathVariable java.util.UUID id,
      @RequestHeader(name = "Authorization", required = false) String authHeader) {
    var user = authUserResolver.optionalUser(authHeader);
    var details = getStoreDetailsUseCase.execute(id, user);
    if (details == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found");
    }

    var contentDtos =
        details.contents().stream()
            .map(
                item ->
                    Mappers.toDto(
                        item.content(),
                        item.engagement().likeCount(),
                        item.engagement().commentCount(),
                        item.engagement().likedByMe()))
            .toList();

    return Mappers.toDto(
        details.store(),
        true,
        details.isFavorite(),
        details.rating(),
        details.reviewCount(),
        null,
        details.myRating(),
        contentDtos);
  }

  @PostMapping
  public ResponseEntity<ThriftStoreDto> create(
      @RequestHeader("Authorization") String authHeader, @RequestBody @Valid StoreRequest body) {
    var user = authUserResolver.requireUser(authHeader);
    var command = Mappers.toCreateCommand(body);

    var saved = createThriftStoreUseCase.execute(user, command);

    var dto = Mappers.toDto(saved, true, false, null, null, null);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @PostMapping("/{storeId}/photos/uploads")
  public PhotoUploadResponse requestPhotoUploadSlots(
      @PathVariable UUID storeId,
      @RequestHeader("Authorization") String authHeader,
      @RequestBody PhotoUploadRequest request) {
    var user = authUserResolver.requireUser(authHeader);
    var slots =
        requestStorePhotoUploadsUseCase.execute(
            user, storeId, request.getCount(), request.getContentTypes());
    return Mappers.toPhotoUploadResponse(slots);
  }

  @PutMapping("/{storeId}/photos")
  public ThriftStoreDto replacePhotos(
      @PathVariable UUID storeId,
      @RequestHeader("Authorization") String authHeader,
      @RequestBody PhotoRegisterRequest request) {
    var user = authUserResolver.requireUser(authHeader);
    var refreshed =
        replaceStorePhotosUseCase.execute(user, storeId, Mappers.toReplacePhotosCommand(request));
    return Mappers.toDtoForUser(refreshed, user, true);
  }

  @PutMapping("/{id}")
  public ThriftStoreDto updateStore(
      @PathVariable java.util.UUID id,
      @RequestHeader("Authorization") String authHeader,
      @RequestBody @Valid StoreRequest body) {
    var user = authUserResolver.requireUser(authHeader);
    var command = Mappers.toUpdateCommand(body);
    var refreshed = updateThriftStoreUseCase.execute(user, id, command);
    return Mappers.toDtoForUser(refreshed, user, true);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteStore(
      @PathVariable java.util.UUID id, @RequestHeader("Authorization") String authHeader) {
    var user = authUserResolver.requireUser(authHeader);
    deleteThriftStoreUseCase.execute(user, id);
    return ResponseEntity.noContent().build();
  }
}
