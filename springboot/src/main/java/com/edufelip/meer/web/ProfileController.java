package com.edufelip.meer.web;

import com.edufelip.meer.domain.auth.GetProfileUseCase;
import com.edufelip.meer.domain.auth.UpdateProfileUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.dto.DeleteAccountRequest;
import com.edufelip.meer.dto.ProfileDto;
import com.edufelip.meer.dto.UpdateProfileRequest;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.security.token.InvalidTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final AuthUserRepository authUserRepository;
    private final StoreFeedbackRepository storeFeedbackRepository;

    public ProfileController(GetProfileUseCase getProfileUseCase,
                             UpdateProfileUseCase updateProfileUseCase,
                             AuthUserRepository authUserRepository,
                             StoreFeedbackRepository storeFeedbackRepository) {
        this.getProfileUseCase = getProfileUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.authUserRepository = authUserRepository;
        this.storeFeedbackRepository = storeFeedbackRepository;
    }

    @GetMapping
    public ProfileDto getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearer(authHeader);
        var user = getProfileUseCase.execute(token);
        return Mappers.toProfileDto(user, true);
    }

    // Alias for clients expecting /auth/me
    @GetMapping("/auth/me")
    public ProfileDto getProfileAlias(@RequestHeader("Authorization") String authHeader) {
        return getProfile(authHeader);
    }

    @PutMapping
    public ProfileDto updateProfile(@RequestHeader("Authorization") String authHeader,
                                    @RequestBody UpdateProfileRequest body) {
        String token = extractBearer(authHeader);
        if (body == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body required");
        var user = updateProfileUseCase.execute(token, body);
        return Mappers.toProfileDto(user, true);
    }

    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileDto patchProfile(@RequestHeader("Authorization") String authHeader,
                                   @RequestPart(name = "name", required = false) String name,
                                   @RequestPart(name = "bio", required = false) String bio,
                                   @RequestPart(name = "notifyNewStores", required = false) String notifyNewStores,
                                   @RequestPart(name = "notifyPromotions", required = false) String notifyPromotions,
                                   @RequestPart(name = "avatar", required = false) MultipartFile avatar) {
        String token = extractBearer(authHeader);

        if (bio != null && bio.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bio exceeds 200 characters");
        }

        String avatarUrl = null;
        if (avatar != null && !avatar.isEmpty()) {
            var ctype = avatar.getContentType();
            if (ctype == null || !(ctype.equalsIgnoreCase("image/jpeg") || ctype.equalsIgnoreCase("image/png"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "avatar must be jpeg or png");
            }
            if (avatar.getSize() > 5 * 1024 * 1024) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "avatar too large (max 5MB)");
            }
            try {
                Path uploadDir = Path.of("uploads", "avatars");
                Files.createDirectories(uploadDir);
                String ext = ctype.equalsIgnoreCase("image/png") ? ".png" : ".jpg";
                String filename = UUID.randomUUID() + ext;
                Path target = uploadDir.resolve(filename);
                Files.write(target, avatar.getBytes());
                avatarUrl = "/uploads/avatars/" + filename;
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save avatar");
            }
        }

        var req = new UpdateProfileRequest(
                name,
                avatarUrl,
                bio,
                notifyNewStores != null ? Boolean.valueOf(notifyNewStores) : null,
                notifyPromotions != null ? Boolean.valueOf(notifyPromotions) : null
        );
        var user = updateProfileUseCase.execute(token, req);
        return Mappers.toProfileDto(user, true);
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@RequestHeader("Authorization") String authHeader,
                                              @RequestBody DeleteAccountRequest body) {
        String token = extractBearer(authHeader);
        var user = getProfileUseCase.execute(token);
        if (body == null || body.email() == null || !body.email().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email confirmation does not match");
        }
        // cleanup: favorites and feedbacks
        user.getFavorites().clear();
        authUserRepository.save(user);
        storeFeedbackRepository.deleteByUserId(user.getId());
        authUserRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    private String extractBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) throw new InvalidTokenException();
        return header.substring("Bearer ".length()).trim();
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<?> handleInvalid(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new com.edufelip.meer.dto.ErrorDto(ex.getMessage()));
    }
}
