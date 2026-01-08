package com.edufelip.meer.web;

import com.edufelip.meer.domain.auth.GetProfileUseCase;
import com.edufelip.meer.domain.auth.UpdateProfileUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.AvatarUploadResponse;
import com.edufelip.meer.dto.DeleteAccountRequest;
import com.edufelip.meer.dto.ErrorDto;
import com.edufelip.meer.dto.ProfileDto;
import com.edufelip.meer.dto.UpdateProfileRequest;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.service.GcsStorageService;
import com.edufelip.meer.util.UrlValidatorUtil;
import jakarta.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/profile")
public class ProfileController {

  private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

  private final GetProfileUseCase getProfileUseCase;
  private final UpdateProfileUseCase updateProfileUseCase;
  private final AuthUserRepository authUserRepository;
  private final StoreFeedbackRepository storeFeedbackRepository;
  private final ThriftStoreRepository thriftStoreRepository;
  private final GcsStorageService gcsStorageService;
  private final AuthUserResolver authUserResolver;
  private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");
  private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024;

  public ProfileController(
      GetProfileUseCase getProfileUseCase,
      UpdateProfileUseCase updateProfileUseCase,
      AuthUserRepository authUserRepository,
      StoreFeedbackRepository storeFeedbackRepository,
      ThriftStoreRepository thriftStoreRepository,
      GcsStorageService gcsStorageService,
      AuthUserResolver authUserResolver) {
    this.getProfileUseCase = getProfileUseCase;
    this.updateProfileUseCase = updateProfileUseCase;
    this.authUserRepository = authUserRepository;
    this.storeFeedbackRepository = storeFeedbackRepository;
    this.thriftStoreRepository = thriftStoreRepository;
    this.gcsStorageService = gcsStorageService;
    this.authUserResolver = authUserResolver;
  }

  @GetMapping
  public ProfileDto getProfile(@RequestHeader("Authorization") String authHeader) {
    String token = authUserResolver.requireBearer(authHeader);
    var user = getProfileUseCase.execute(token);
    return Mappers.toProfileDto(user, true);
  }

  @PostMapping(value = "/avatar/upload", consumes = MediaType.APPLICATION_JSON_VALUE)
  public AvatarUploadResponse requestAvatarUpload(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody(required = false) java.util.Map<String, String> body) {
    String token = authUserResolver.requireBearer(authHeader);
    var user = getProfileUseCase.execute(token);
    String contentType = body != null ? body.get("contentType") : null;
    if (contentType != null
        && !(contentType.equalsIgnoreCase("image/jpeg")
            || contentType.equalsIgnoreCase("image/png")
            || contentType.equalsIgnoreCase("image/webp"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported content type");
    }
    var slot = gcsStorageService.createAvatarSlot(user.getId().toString(), contentType);
    return new AvatarUploadResponse(slot.uploadUrl(), slot.fileKey(), slot.contentType());
  }

  // Alias for clients expecting /auth/me
  @GetMapping("/auth/me")
  public ProfileDto getProfileAlias(@RequestHeader("Authorization") String authHeader) {
    return getProfile(authHeader);
  }

  @PutMapping
  public ProfileDto updateProfile(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody @Valid UpdateProfileRequest body) {
    String token = authUserResolver.requireBearer(authHeader);
    if (body == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body required");
    var currentUser = getProfileUseCase.execute(token);
    String normalizedAvatar = normalizeAvatarUrl(body.avatarUrl(), currentUser.getId());
    var user =
        updateProfileUseCase.execute(
            token,
            new UpdateProfileUseCase.Command(
                body.name(),
                normalizedAvatar,
                body.bio(),
                body.notifyNewStores(),
                body.notifyPromos()));
    return Mappers.toProfileDto(user, true);
  }

  @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ProfileDto patchProfile(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody(required = false) @Valid UpdateProfileRequest body) {
    String token = authUserResolver.requireBearer(authHeader);

    if (body == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body required");
    }
    if (body.bio() != null && body.bio().length() > 200) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bio exceeds 200 characters");
    }

    var currentUser = getProfileUseCase.execute(token);
    String previousAvatar = currentUser.getPhotoUrl();
    String normalizedAvatar = normalizeAvatarUrl(body.avatarUrl(), currentUser.getId());

    var command =
        new UpdateProfileUseCase.Command(
            body.name(), normalizedAvatar, body.bio(), body.notifyNewStores(), body.notifyPromos());

    var user = updateProfileUseCase.execute(token, command);
    deleteOldAvatarIfReplaced(previousAvatar, user.getPhotoUrl());
    return Mappers.toProfileDto(user, true);
  }

  @DeleteMapping("/account")
  public ResponseEntity<Void> deleteAccount(
      @RequestHeader("Authorization") String authHeader, @RequestBody DeleteAccountRequest body) {
    String token = authUserResolver.requireBearer(authHeader);
    var user = getProfileUseCase.execute(token);
    if (body == null || body.email() == null || !body.email().equalsIgnoreCase(user.getEmail())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Email confirmation does not match");
    }
    // delete owned store + photos in GCS
    Optional.ofNullable(user.getOwnedThriftStore())
        .flatMap(store -> thriftStoreRepository.findById(store.getId()))
        .ifPresent(
            store -> {
              if (store.getPhotos() != null) {
                store
                    .getPhotos()
                    .forEach(
                        p -> {
                          if (!deleteLocalIfUploads(p.getUrl())) {
                            gcsStorageService.deleteByUrl(p.getUrl());
                          }
                        });
              }
              thriftStoreRepository.delete(store);
            });
    // cleanup: favorites and feedbacks
    user.getFavorites().clear();
    // delete avatar if locally stored or GCS
    if (user.getPhotoUrl() != null) {
      if (!deleteLocalIfUploads(user.getPhotoUrl())) {
        gcsStorageService.deleteByUrl(user.getPhotoUrl());
      }
    }
    authUserRepository.save(user);
    storeFeedbackRepository.deleteByUserId(user.getId());
    authUserRepository.delete(user);
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<?> handleInvalid(InvalidTokenException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorDto(ex.getMessage()));
  }

  private void deleteOldAvatarIfReplaced(String oldUrl, String newUrl) {
    if (oldUrl == null || newUrl == null) return;
    if (oldUrl.equals(newUrl)) return;
    // Only delete avatars stored under our avatars prefix for safety
    String oldKey = gcsStorageService.extractFileKey(oldUrl);
    if (oldKey == null) return;
    if (!oldKey.startsWith(gcsStorageService.getAvatarsPrefix() + "/")) return;
    log.info("Deleting previous avatar key={} oldUrl={}", oldKey, oldUrl);
    if (!deleteLocalIfUploads(oldUrl)) {
      try {
        gcsStorageService.deleteByUrl(oldUrl);
      } catch (Exception ex) {
        log.warn(
            "Failed to delete previous avatar key={} url={} error={}",
            oldKey,
            oldUrl,
            ex.getMessage());
      }
    }
  }

  private boolean deleteLocalIfUploads(String url) {
    if (url == null || !url.startsWith("/uploads/")) return false;
    try {
      Path path = Paths.get("springboot", url.substring(1));
      Files.deleteIfExists(path);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private String normalizeAvatarUrl(String avatarUrl, UUID userId) {
    if (avatarUrl == null || avatarUrl.isBlank()) return null;
    String fileKey = gcsStorageService.extractFileKey(avatarUrl);
    if (fileKey == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid avatar URL");
    }
    String expectedPrefix = gcsStorageService.getAvatarsPrefix() + "/" + userId;
    if (!fileKey.startsWith(expectedPrefix)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "avatar must belong to the requesting user");
    }
    var blob = gcsStorageService.fetchRequiredObject(fileKey);
    String ctype = blob.getContentType();
    if (ctype == null || ALLOWED_AVATAR_CONTENT_TYPES.stream().noneMatch(ctype::equalsIgnoreCase)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "avatar must be jpeg, png or webp");
    }
    if (blob.getSize() > MAX_AVATAR_BYTES) {
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "avatar too large (max 5MB)");
    }
    String publicUrl = gcsStorageService.publicUrl(fileKey);
    try {
      UrlValidatorUtil.ensureHttpUrl(publicUrl, "avatarUrl");
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    return publicUrl;
  }
}
