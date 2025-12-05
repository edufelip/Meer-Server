package com.edufelip.meer.web;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.ContentCreateRequest;
import com.edufelip.meer.dto.ContentUploadSlotResponse;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.GuideTopDto;
import com.edufelip.meer.dto.PhotoUploadSlot;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.service.GcsStorageService;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/contents")
public class GuideContentController {

    private final GetGuideContentUseCase getGuideContentUseCase;
    private final GuideContentRepository guideContentRepository;
    private final AuthUserRepository authUserRepository;
    private final TokenProvider tokenProvider;
    private final GcsStorageService gcsStorageService;
    private final ThriftStoreRepository thriftStoreRepository;

    public GuideContentController(GetGuideContentUseCase getGuideContentUseCase,
                                  GuideContentRepository guideContentRepository,
                                  AuthUserRepository authUserRepository,
                                  TokenProvider tokenProvider,
                                  GcsStorageService gcsStorageService,
                                  ThriftStoreRepository thriftStoreRepository) {
        this.getGuideContentUseCase = getGuideContentUseCase;
        this.guideContentRepository = guideContentRepository;
        this.authUserRepository = authUserRepository;
        this.tokenProvider = tokenProvider;
        this.gcsStorageService = gcsStorageService;
        this.thriftStoreRepository = thriftStoreRepository;
    }

    @GetMapping("/top")
    public com.edufelip.meer.dto.PageResponse<GuideTopDto> top(@RequestHeader("Authorization") String authHeader,
                                                               @RequestParam(name = "page", defaultValue = "0") int page,
                                                               @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                                               @RequestParam(name = "storeId", required = false) java.util.UUID storeId) {
        var user = currentUser(authHeader); // enforce auth
        java.util.UUID targetStoreId = storeId != null ? storeId
                : (user.getOwnedThriftStore() != null ? user.getOwnedThriftStore().getId() : null);
        if (targetStoreId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storeId is required (or own a store)");
        }
        var pageRes = getGuideContentUseCase.executeByStorePaged(targetStoreId, page, pageSize);
        var items = pageRes.getContent().stream()
                .map(gc -> new GuideTopDto(
                        gc.getId(),
                        gc.getTitle(),
                        gc.getDescription(),
                        gc.getImageUrl(),
                        gc.getThriftStore() != null ? gc.getThriftStore().getId() : null,
                        gc.getThriftStore() != null ? gc.getThriftStore().getName() : null,
                        gc.getCreatedAt()))
                .toList();
        return new com.edufelip.meer.dto.PageResponse<>(items, page + 1, pageRes.hasNext());
    }

    @GetMapping("/{id}")
    public GuideContentDto getById(@PathVariable Integer id, @RequestHeader("Authorization") String authHeader) {
        currentUser(authHeader); // just to enforce auth
        var content = getGuideContentUseCase.execute(id);
        return content != null ? Mappers.toDto(content) : null;
    }

    @PostMapping
    public GuideContentDto create(@RequestBody ContentCreateRequest body,
                                  @RequestHeader("Authorization") String authHeader) {
        var user = currentUser(authHeader);
        if (body == null || body.title() == null || body.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (body.description() == null || body.description().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
        }
        if (body.storeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storeId is required");
        }
        var thriftStore = thriftStoreRepository.findById(body.storeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        if (user.getOwnedThriftStore() == null || !user.getOwnedThriftStore().getId().equals(thriftStore.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must own this store to add content");
        }
        String defaultCategory = "general";
        String defaultType = "article";
        var content = new GuideContent(
                null,
                body.title(),
                body.description(),
                defaultCategory,
                defaultType,
                "",
                thriftStore
        );
        return Mappers.toDto(guideContentRepository.save(content));
    }

    @PostMapping("/{contentId}/image/upload")
    public ContentUploadSlotResponse requestImageSlot(@PathVariable Integer contentId,
                                                      @RequestHeader("Authorization") String authHeader,
                                                      @RequestBody(required = false) java.util.Map<String, String> body) {
        var user = currentUser(authHeader);
        var content = guideContentRepository.findById(contentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        if (content.getThriftStore() == null || user.getOwnedThriftStore() == null ||
                !content.getThriftStore().getId().equals(user.getOwnedThriftStore().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must own this store to upload content images");
        }
        String ctype = body != null ? body.get("contentType") : null;
        if (ctype != null && !(ctype.equalsIgnoreCase("image/jpeg") || ctype.equalsIgnoreCase("image/png") || ctype.equalsIgnoreCase("image/webp"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported content type");
        }
        UUID storeId = content.getThriftStore().getId();
        PhotoUploadSlot slot = gcsStorageService.createUploadSlots(storeId, 1, java.util.List.of(ctype)).get(0);
        return new ContentUploadSlotResponse(slot.getUploadUrl(), slot.getFileKey(), slot.getContentType());
    }

    @PutMapping("/{id}")
    public GuideContentDto update(@PathVariable Integer id,
                                  @RequestBody GuideContent body,
                                  @RequestHeader("Authorization") String authHeader) {
        var user = currentUser(authHeader);
        var content = guideContentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        if (user.getOwnedThriftStore() == null || !user.getOwnedThriftStore().getId().equals(content.getThriftStore().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must own this store to update content");
        }
        if (body.getTitle() != null) content.setTitle(body.getTitle());
        if (body.getDescription() != null) content.setDescription(body.getDescription());
        if (body.getImageUrl() != null) content.setImageUrl(body.getImageUrl());
        // keep default category/type if still null
        if (content.getCategoryLabel() == null) content.setCategoryLabel("general");
        if (content.getType() == null) content.setType("article");
        guideContentRepository.save(content);
        return Mappers.toDto(content);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id, @RequestHeader("Authorization") String authHeader) {
        var user = currentUser(authHeader);
        var content = guideContentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        if (user.getOwnedThriftStore() == null || !user.getOwnedThriftStore().getId().equals(content.getThriftStore().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must own this store to delete content");
        }
        guideContentRepository.delete(content);
    }

    private com.edufelip.meer.core.auth.AuthUser currentUser(String authHeader) {
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
