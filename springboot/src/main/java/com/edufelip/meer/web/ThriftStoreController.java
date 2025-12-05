package com.edufelip.meer.web;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.CreateGuideContentUseCase;
import com.edufelip.meer.domain.GetGuideContentsByThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.domain.repo.CategoryRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.core.store.Social;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.dto.ThriftStoreDto;
import com.edufelip.meer.dto.PhotoUploadRequest;
import com.edufelip.meer.dto.PhotoUploadResponse;
import com.edufelip.meer.dto.PhotoRegisterRequest;
import com.edufelip.meer.dto.PhotoUploadSlot;
import com.edufelip.meer.dto.StoreRequest;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.service.StoreFeedbackService;
import com.edufelip.meer.service.GcsStorageService;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.edufelip.meer.core.store.ThriftStorePhoto;

@RestController
@RequestMapping("/stores")
public class ThriftStoreController {

    private final GetThriftStoreUseCase getThriftStoreUseCase;
    private final GetThriftStoresUseCase getThriftStoresUseCase;
    private final GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase;
    private final CreateGuideContentUseCase createGuideContentUseCase;
    private final ThriftStoreRepository thriftStoreRepository;
    private final CategoryRepository categoryRepository;
    private final com.edufelip.meer.domain.repo.AuthUserRepository authUserRepository;
    private final TokenProvider tokenProvider;
    private final StoreFeedbackService storeFeedbackService;
    private final GcsStorageService gcsStorageService;
    private static final int MAX_PHOTO_COUNT = 10;
    private static final long MAX_PHOTO_BYTES = 2 * 1024 * 1024L; // 2MB cap to align with client compression requirement
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/pjpeg", "image/webp", "image/x-webp"
    );

    public ThriftStoreController(GetThriftStoreUseCase getThriftStoreUseCase,
                                 GetThriftStoresUseCase getThriftStoresUseCase,
                                 GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase,
                                 CreateGuideContentUseCase createGuideContentUseCase,
                                 ThriftStoreRepository thriftStoreRepository,
                                 CategoryRepository categoryRepository,
                                 com.edufelip.meer.domain.repo.AuthUserRepository authUserRepository,
                                 TokenProvider tokenProvider,
                                 StoreFeedbackService storeFeedbackService,
                                 GcsStorageService gcsStorageService) {
        this.getThriftStoreUseCase = getThriftStoreUseCase;
        this.getThriftStoresUseCase = getThriftStoresUseCase;
        this.getGuideContentsByThriftStoreUseCase = getGuideContentsByThriftStoreUseCase;
        this.createGuideContentUseCase = createGuideContentUseCase;
        this.thriftStoreRepository = thriftStoreRepository;
        this.categoryRepository = categoryRepository;
        this.authUserRepository = authUserRepository;
        this.tokenProvider = tokenProvider;
        this.storeFeedbackService = storeFeedbackService;
        this.gcsStorageService = gcsStorageService;
    }

    @GetMapping
    public PageResponse<ThriftStoreDto> getStores(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng
    ) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
        }
        var user = currentUser(authHeader);
        var favorites = user.getFavorites();
        var pageable = PageRequest.of(page - 1, pageSize);
        java.util.List<ThriftStore> storesPage;
        boolean hasNext;

        if ("nearby".equalsIgnoreCase(type)) {
            if (lat == null || lng == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat and lng are required for nearby search");
            }
            var result = getThriftStoresUseCase.executeNearby(lat, lng, page - 1, pageSize);
            storesPage = result.getContent();
            hasNext = result.hasNext();
        } else if (categoryId != null) {
            if (categoryRepository.findById(categoryId).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
            }
            var result = thriftStoreRepository.findByCategoryId(categoryId, pageable);
            storesPage = result.getContent();
            hasNext = result.hasNext();
        } else {
            var result = getThriftStoresUseCase.executePaged(page - 1, pageSize);
            storesPage = result.getContent();
            hasNext = result.hasNext();
        }

        var ids = storesPage.stream().map(ThriftStore::getId).toList();
        var summaries = storeFeedbackService.getSummaries(ids);

        var items = storesPage.stream()
                .map(store -> {
                    var summary = summaries.get(store.getId());
                    Double rating = summary != null ? summary.rating() : null;
                    Integer reviewCount = summary != null && summary.reviewCount() != null ? summary.reviewCount().intValue() : null;
                    boolean isFav = favorites.stream().anyMatch(f -> f.getId().equals(store.getId()));
                    Double distanceMeters = (lat != null && lng != null && store.getLatitude() != null && store.getLongitude() != null)
                            ? distanceKm(lat, lng, store.getLatitude(), store.getLongitude()) * 1000
                            : null;
                    return Mappers.toDto(store, false, isFav, rating, reviewCount, distanceMeters);
                })
                .toList();
        return new PageResponse<>(items, page, hasNext);
    }

    @GetMapping("/{id}")
    public ThriftStoreDto getById(@PathVariable java.util.UUID id, @RequestHeader("Authorization") String authHeader) {
        var user = currentUser(authHeader);
        var store = getThriftStoreUseCase.execute(id);
        if (store == null) return null;
        var summary = storeFeedbackService.getSummaries(java.util.List.of(store.getId())).get(store.getId());
        Double rating = summary != null ? summary.rating() : null;
        Integer reviewCount = summary != null && summary.reviewCount() != null ? summary.reviewCount().intValue() : null;
        boolean isFav = user.getFavorites().stream().anyMatch(f -> f.getId().equals(store.getId()));
        return Mappers.toDto(store, true, isFav, rating, reviewCount, null);
    }

    @PostMapping
    public ResponseEntity<ThriftStoreDto> create(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody StoreRequest body
    ) {
        var user = currentUser(authHeader);
        validateCreate(body);

        ThriftStore store = new ThriftStore();
        store.setName(body.getName());
        store.setDescription(body.getDescription());
        store.setOpeningHours(body.getOpeningHours());
        store.setAddressLine(body.getAddressLine());
        store.setLatitude(body.getLatitude());
        store.setLongitude(body.getLongitude());
        store.setOwner(user);
        store.setSocial(body.getSocial());
        store.setPhone(body.getPhone());
        store.setEmail(body.getEmail());
        store.setTagline(body.getTagline());
        store.setNeighborhood(body.getNeighborhood());
        store.setCategories(normalizeCategories(body.getCategories()));

        var saved = thriftStoreRepository.save(store);

        // keep user linkage for profile responses
        user.setOwnedThriftStore(saved);
        authUserRepository.save(user);

        var dto = Mappers.toDto(saved, true, false, null, null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{storeId}/photos/uploads")
    public PhotoUploadResponse requestPhotoUploadSlots(
            @PathVariable UUID storeId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PhotoUploadRequest request
    ) {
        var user = currentUser(authHeader);
        var store = thriftStoreRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        ensureOwner(user, store);

        int count = request.getCount() != null ? request.getCount() : 0;
        if (count <= 0 || count > MAX_PHOTO_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "count must be between 1 and " + MAX_PHOTO_COUNT);
        }
        if (request.getContentTypes() != null) {
            for (String ct : request.getContentTypes()) {
                if (ct != null && !isSupportedContentType(ct)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported content type: " + ct);
                }
            }
        }
        int existing = store.getPhotos() == null ? 0 : store.getPhotos().size();
        if (existing + count > MAX_PHOTO_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Store already has " + existing + " photos; max " + MAX_PHOTO_COUNT);
        }
        var slots = gcsStorageService.createUploadSlots(storeId, count, request.getContentTypes());
        return new PhotoUploadResponse(slots);
    }

    @PutMapping("/{storeId}/photos")
    public ThriftStoreDto replacePhotos(
            @PathVariable UUID storeId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PhotoRegisterRequest request
    ) {
        var user = currentUser(authHeader);
        var store = thriftStoreRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        ensureOwner(user, store);
        List<PhotoRegisterRequest.Item> items = request.getPhotos();
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photos array is required");
        }
        if (items.size() > MAX_PHOTO_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many photos; max " + MAX_PHOTO_COUNT);
        }

        // validate unique contiguous positions
        var positions = new java.util.HashSet<Integer>();
        for (var i : items) {
            if (i.getPosition() == null || i.getPosition() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "position must be non-negative");
            }
            if (!positions.add(i.getPosition())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicate position " + i.getPosition());
            }
        }
        if (!positions.isEmpty()) {
            int max = positions.stream().mapToInt(Integer::intValue).max().orElse(0);
            if (max != items.size() - 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "positions must be contiguous starting at 0");
            }
        }

        var existingPhotos = store.getPhotos() == null ? new ArrayList<ThriftStorePhoto>() : new ArrayList<>(store.getPhotos());
        var existingById = new java.util.HashMap<Integer, ThriftStorePhoto>();
        for (var p : existingPhotos) {
            existingById.put(p.getId(), p);
        }

        // handle deletions
        List<Integer> deleteIds = request.getDeletePhotoIds();
        if (deleteIds != null && !deleteIds.isEmpty()) {
            for (Integer delId : deleteIds) {
                var removed = existingById.remove(delId);
                if (removed != null) {
                    gcsStorageService.deleteByUrl(removed.getUrl());
                }
            }
        }

        List<ThriftStorePhoto> finalPhotos = new ArrayList<>();
        items.sort(Comparator.comparing(PhotoRegisterRequest.Item::getPosition));

        for (var item : items) {
            boolean hasPhotoId = item.getPhotoId() != null;
            boolean hasFileKey = item.getFileKey() != null && !item.getFileKey().isBlank();
            if (!hasPhotoId && !hasFileKey) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each photo must have photoId or fileKey");
            }

            if (hasPhotoId) {
                var existing = existingById.remove(item.getPhotoId());
                if (existing == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photoId " + item.getPhotoId() + " not found on this store");
                }
                existing.setDisplayOrder(item.getPosition());
                finalPhotos.add(existing);
                continue;
            }

            // new photo via fileKey
            if (!item.getFileKey().startsWith("stores/" + storeId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileKey does not belong to this store");
            }
            var blob = gcsStorageService.fetchRequiredObject(item.getFileKey());
            validateBlob(blob, item.getFileKey());
            String viewUrl = gcsStorageService.publicUrl(item.getFileKey());
            finalPhotos.add(new ThriftStorePhoto(store, viewUrl, item.getPosition()));
        }

        // Mutate managed collection in place to avoid orphanRemoval issues
        var photos = store.getPhotos();
        if (photos == null) {
            photos = new ArrayList<>();
            store.setPhotos(photos);
        } else {
            photos.clear();
        }
        photos.addAll(finalPhotos);
        photos.sort(Comparator.comparing(p -> p.getDisplayOrder() == null ? Integer.MAX_VALUE : p.getDisplayOrder()));
        if (!photos.isEmpty()) {
            store.setCoverImageUrl(photos.get(0).getUrl());
        } else {
            store.setCoverImageUrl(null);
        }
        thriftStoreRepository.save(store);

        var refreshed = thriftStoreRepository.findById(storeId).orElseThrow();
        boolean isFav = user.getFavorites().stream().anyMatch(f -> f.getId().equals(storeId));
        return Mappers.toDto(refreshed, true, isFav, null, null, null);
    }

    @GetMapping("/{storeId}/contents")
    public List<GuideContentDto> getContentsByThriftStoreId(@PathVariable java.util.UUID storeId) {
        return getGuideContentsByThriftStoreUseCase.execute(storeId).stream().map(Mappers::toDto).toList();
    }

    @PostMapping("/{storeId}/contents")
    public GuideContentDto createGuideContent(
            @PathVariable java.util.UUID storeId,
            @RequestBody GuideContent guideContent,
            @RequestHeader("Authorization") String authHeader
    ) {
        var user = currentUser(authHeader);
        if (user.getOwnedThriftStore() == null || !user.getOwnedThriftStore().getId().equals(storeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must own this store to add content");
        }
        var thriftStore = thriftStoreRepository.findById(storeId).orElseThrow(() -> new RuntimeException("Thrift store not found"));
        var contentWithStore = new GuideContent(
                guideContent.getId(),
                guideContent.getTitle(),
                guideContent.getDescription(),
                guideContent.getCategoryLabel(),
                guideContent.getType(),
                guideContent.getImageUrl(),
                thriftStore);
        return Mappers.toDto(createGuideContentUseCase.execute(contentWithStore));
    }

    @PutMapping("/{id}")
    public ThriftStoreDto updateStore(
            @PathVariable java.util.UUID id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody StoreRequest body
    ) {
        var user = currentUser(authHeader);
        var store = thriftStoreRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        ensureOwner(user, store);

        if (body.getName() != null && body.getName().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be blank");
        if (body.getAddressLine() != null && body.getAddressLine().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "addressLine cannot be blank");

        if (body.getName() != null) store.setName(body.getName());
        if (body.getDescription() != null) store.setDescription(body.getDescription());
        if (body.getOpeningHours() != null) store.setOpeningHours(body.getOpeningHours());
        if (body.getAddressLine() != null) store.setAddressLine(body.getAddressLine());
        if (body.getPhone() != null) {
            var socialObj = store.getSocial() != null ? store.getSocial() : new Social();
            store.setSocial(socialObj);
            store.setPhone(body.getPhone());
        }
        if (body.getEmail() != null) store.setEmail(body.getEmail());
        if (body.getTagline() != null) store.setTagline(body.getTagline());
        if (body.getNeighborhood() != null) store.setNeighborhood(body.getNeighborhood());
        if (body.getCategories() != null) store.setCategories(normalizeCategories(body.getCategories()));
        if (body.getSocial() != null) store.setSocial(body.getSocial());

        if (body.getLatitude() != null && body.getLongitude() != null) {
            store.setLatitude(body.getLatitude());
            store.setLongitude(body.getLongitude());
        } else if ((body.getAddressLine() != null) && (store.getLatitude() == null || store.getLongitude() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude/longitude required or geocoding failed");
        }

        thriftStoreRepository.save(store);

        var refreshed = thriftStoreRepository.findById(id).orElseThrow();
        boolean isFav = user.getFavorites().stream().anyMatch(f -> f.getId().equals(id));
        var dto = Mappers.toDto(refreshed, true, isFav, null, null, null);
        return dto;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable java.util.UUID id, @RequestHeader("Authorization") String authHeader) {
        var user = currentUser(authHeader);
        var store = thriftStoreRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        if (user.getOwnedThriftStore() == null || !user.getOwnedThriftStore().getId().equals(store.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner");
        }
        if (store.getPhotos() != null) {
            store.getPhotos().forEach(p -> {
                if (!deleteLocalIfUploads(p.getUrl())) {
                    gcsStorageService.deleteByUrl(p.getUrl());
                }
            });
        }
        thriftStoreRepository.delete(store);
        return ResponseEntity.noContent().build();
    }


    private java.util.UUID extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new InvalidTokenException();
        String token = authHeader.substring("Bearer ".length()).trim();
        try {
            TokenPayload payload = tokenProvider.parseAccessToken(token);
            return payload.getUserId();
        } catch (RuntimeException ex) {
            throw new InvalidTokenException();
        }
    }

    private com.edufelip.meer.core.auth.AuthUser currentUser(String authHeader) {
        java.util.UUID userId = extractUserId(authHeader);
        return authUserRepository.findById(userId).orElseThrow(InvalidTokenException::new);
    }

    private void validateCreate(StoreRequest body) {
        if (body == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        if (body.getName() == null || body.getName().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        if (body.getAddressLine() == null || body.getAddressLine().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "addressLine is required");
        if (body.getDescription() == null || body.getDescription().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
        if (body.getPhone() == null || body.getPhone().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone is required");
        if (body.getLatitude() == null || body.getLongitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude/longitude required or geocoding failed");
        }
    }

    private void ensureOwner(com.edufelip.meer.core.auth.AuthUser user, ThriftStore store) {
        boolean userOwnsByLink = user.getOwnedThriftStore() != null
                && store.getId().equals(user.getOwnedThriftStore().getId());
        boolean userOwnsByStoreOwner = store.getOwner() != null
                && store.getOwner().getId().equals(user.getId());

        // If the links are missing but this user should own the store, repair the linkage eagerly.
        if (!userOwnsByLink && userOwnsByStoreOwner) {
            user.setOwnedThriftStore(store);
            authUserRepository.save(user);
            userOwnsByLink = true;
        } else if (!userOwnsByStoreOwner && userOwnsByLink) {
            store.setOwner(user);
            thriftStoreRepository.save(store);
            userOwnsByStoreOwner = true;
        } else if (!userOwnsByLink && !userOwnsByStoreOwner && store.getOwner() == null) {
            // freshly created store, set both sides
            store.setOwner(user);
            user.setOwnedThriftStore(store);
            thriftStoreRepository.save(store);
            authUserRepository.save(user);
            userOwnsByLink = true;
            userOwnsByStoreOwner = true;
        }

        if (!(userOwnsByLink || userOwnsByStoreOwner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner");
        }
    }

    private double distanceKm(double lat1, double lon1, Double lat2, Double lon2) {
        if (lat2 == null || lon2 == null) return Double.MAX_VALUE;
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private boolean isSupportedContentType(String ctype) {
        if (ctype == null) return false;
        return SUPPORTED_CONTENT_TYPES.contains(ctype.toLowerCase());
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

    private List<String> normalizeCategories(List<String> categories) {
        if (categories == null) return null;
        return categories.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    private void validateBlob(com.google.cloud.storage.Blob blob, String fileKey) {
        String ctype = blob.getContentType();
        if (!isSupportedContentType(ctype)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported content type for " + fileKey);
        }
        if (blob.getSize() != null && blob.getSize() > MAX_PHOTO_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File too large (>2MB) for " + fileKey);
        }
    }
}
