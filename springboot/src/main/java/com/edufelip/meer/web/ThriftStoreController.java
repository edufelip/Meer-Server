package com.edufelip.meer.web;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.CreateGuideContentUseCase;
import com.edufelip.meer.domain.CreateThriftStoreUseCase;
import com.edufelip.meer.domain.GetGuideContentsByThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.domain.repo.CategoryRepository;
import com.edufelip.meer.domain.repo.ThriftStorePhotoRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.core.store.Social;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.dto.ThriftStoreDto;
import com.edufelip.meer.mapper.Mappers;
import com.edufelip.meer.service.StoreFeedbackService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import net.coobird.thumbnailator.Thumbnails;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.edufelip.meer.core.store.ThriftStorePhoto;

@RestController
@RequestMapping("/stores")
public class ThriftStoreController {

    private final GetThriftStoreUseCase getThriftStoreUseCase;
    private final GetThriftStoresUseCase getThriftStoresUseCase;
    private final CreateThriftStoreUseCase createThriftStoreUseCase;
    private final GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase;
    private final CreateGuideContentUseCase createGuideContentUseCase;
    private final ThriftStoreRepository thriftStoreRepository;
    private final ThriftStorePhotoRepository thriftStorePhotoRepository;
    private final CategoryRepository categoryRepository;
    private final com.edufelip.meer.domain.repo.AuthUserRepository authUserRepository;
    private final TokenProvider tokenProvider;
    private final StoreFeedbackService storeFeedbackService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ThriftStoreController(GetThriftStoreUseCase getThriftStoreUseCase,
                                 GetThriftStoresUseCase getThriftStoresUseCase,
                                 CreateThriftStoreUseCase createThriftStoreUseCase,
                                 GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase,
                                 CreateGuideContentUseCase createGuideContentUseCase,
                                 ThriftStoreRepository thriftStoreRepository,
                                 ThriftStorePhotoRepository thriftStorePhotoRepository,
                                 CategoryRepository categoryRepository,
                                 com.edufelip.meer.domain.repo.AuthUserRepository authUserRepository,
                                 TokenProvider tokenProvider,
                                 StoreFeedbackService storeFeedbackService) {
        this.getThriftStoreUseCase = getThriftStoreUseCase;
        this.getThriftStoresUseCase = getThriftStoresUseCase;
        this.createThriftStoreUseCase = createThriftStoreUseCase;
        this.getGuideContentsByThriftStoreUseCase = getGuideContentsByThriftStoreUseCase;
        this.createGuideContentUseCase = createGuideContentUseCase;
        this.thriftStoreRepository = thriftStoreRepository;
        this.thriftStorePhotoRepository = thriftStorePhotoRepository;
        this.categoryRepository = categoryRepository;
        this.authUserRepository = authUserRepository;
        this.tokenProvider = tokenProvider;
        this.storeFeedbackService = storeFeedbackService;
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
    public ThriftStoreDto create(@RequestBody ThriftStore thriftStore) {
        return Mappers.toDto(createThriftStoreUseCase.execute(thriftStore), true);
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

    @PostMapping(path = "", consumes = "multipart/form-data")
    public ResponseEntity<ThriftStoreDto> createStore(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String openingHours,
            @RequestParam(required = false) String addressLine,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String social,
            @RequestParam(required = false) String categories,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String photoOrder,
            @RequestPart(required = false) List<MultipartFile> newPhotos
    ) {
        var user = currentUser(authHeader);
        if (name == null || name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        if (addressLine == null || addressLine.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "addressLine is required");
        if (description == null || description.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
        if (phone == null || phone.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone is required");
        if (latitude == null || longitude == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude/longitude required or geocoding failed");
        }

        ThriftStore store = new ThriftStore();
        store.setName(name);
        store.setDescription(description);
        store.setOpeningHours(openingHours);
        store.setAddressLine(addressLine);
        store.setLatitude(latitude);
        store.setLongitude(longitude);
        store.setOwner(user);
        var socialObj = store.getSocial() != null ? store.getSocial() : new Social();
        store.setSocial(socialObj);
        store.setPhone(phone);
        store.setEmail(email);
        store.setTagline(null);
        store.setNeighborhood(neighborhood);
        store.setCategories(parseStringArray(categories));
        store.setSocial(parseSocial(social));

        var saved = thriftStoreRepository.save(store);

        handlePhotos(saved, null, newPhotos, photoOrder);

        // keep user linkage for profile responses
        user.setOwnedThriftStore(saved);
        authUserRepository.save(user);

        var dto = Mappers.toDto(saved, true, false, null, null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping(path = "/{id}", consumes = "multipart/form-data")
    public ThriftStoreDto updateStore(
            @PathVariable java.util.UUID id,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String openingHours,
            @RequestParam(required = false) String addressLine,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String social,
            @RequestParam(required = false) String categories,
            @RequestParam(required = false) String tagline,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String photoOrder,
            @RequestPart(required = false) List<MultipartFile> newPhotos
    ) {
        var user = currentUser(authHeader);
        var store = thriftStoreRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));
        if (user.getOwnedThriftStore() == null || !user.getOwnedThriftStore().getId().equals(store.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not owner");
        }

        if (name != null && name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be blank");
        if (addressLine != null && addressLine.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "addressLine cannot be blank");

        if (name != null) store.setName(name);
        if (description != null) store.setDescription(description);
        if (openingHours != null) store.setOpeningHours(openingHours);
        if (addressLine != null) store.setAddressLine(addressLine);
        if (phone != null) {
        var socialObj = store.getSocial() != null ? store.getSocial() : new Social();
        store.setSocial(socialObj);
        store.setPhone(phone);
        }
        if (email != null) store.setEmail(email);
        if (tagline != null) store.setTagline(tagline);
        if (neighborhood != null) store.setNeighborhood(neighborhood);
        if (categories != null) store.setCategories(parseStringArray(categories));
        if (social != null) store.setSocial(parseSocial(social));

        if (latitude != null && longitude != null) {
            store.setLatitude(latitude);
            store.setLongitude(longitude);
        } else if ((addressLine != null) && (store.getLatitude() == null || store.getLongitude() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude/longitude required or geocoding failed");
        }

        thriftStoreRepository.save(store);
        handlePhotos(store, null, newPhotos, photoOrder);

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

    private List<String> parseStringArray(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON array");
        }
    }

    private List<Integer> parseIntArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON array");
        }
    }

    private Social parseSocial(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            Map<String, String> map = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
            return new Social(
                    map.get("facebook"), map.get("instagram"), map.get("website")
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid social JSON");
        }
    }

    private void handlePhotos(ThriftStore store,
                              String deletePhotoIdsJson,
                              List<MultipartFile> newPhotos,
                              String photoOrderJson) {
        var photos = store.getPhotos();

        // delete
        var deleteIds = parseIntArray(deletePhotoIdsJson);
        if (!deleteIds.isEmpty()) {
            photos.removeIf(p -> deleteIds.contains(p.getId()));
        }

        // map new photo temp keys
        Map<String, MultipartFile> newPhotoMap = new HashMap<>();
        if (newPhotos != null) {
            int idx = 0;
            for (MultipartFile f : newPhotos) {
                newPhotoMap.put("new" + idx, f);
                idx++;
            }
        }

        List<String> orderKeys = parseStringArray(photoOrderJson);
        if (orderKeys == null) orderKeys = new ArrayList<>();

        List<ThriftStorePhoto> additions = new ArrayList<>();
        int order = 0;
        for (String key : orderKeys) {
            final int currentOrder = order;
            // existing
            try {
                Integer existingId = Integer.valueOf(key);
                photos.stream().filter(p -> p.getId().equals(existingId)).findFirst()
                        .ifPresent(p -> p.setDisplayOrder(currentOrder));
                order++;
                continue;
            } catch (NumberFormatException ignored) {}

            // new
            MultipartFile f = newPhotoMap.remove(key);
            if (f != null) {
                var url = savePhotoFile(store.getId(), f);
                additions.add(new ThriftStorePhoto(store, url, currentOrder));
                order++;
            }
        }

        // append leftover new photos
        for (MultipartFile f : newPhotoMap.values()) {
            var url = savePhotoFile(store.getId(), f);
            additions.add(new ThriftStorePhoto(store, url, order++));
        }

        photos.addAll(additions);
        // ensure cover points to first photo in order
        photos.sort(Comparator.comparing(p -> p.getDisplayOrder() == null ? Integer.MAX_VALUE : p.getDisplayOrder()));
        if (!photos.isEmpty()) {
            store.setCoverImageUrl(photos.get(0).getUrl());
        }
        thriftStoreRepository.save(store);
    }

    private String savePhotoFile(java.util.UUID storeId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty photo file");
        }
        var ctype = file.getContentType();
        if (ctype == null || !(ctype.equalsIgnoreCase("image/jpeg") || ctype.equalsIgnoreCase("image/png"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be jpeg or png");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Photo too large (max 10MB)");
        }
        try {
            Path dir = Path.of("uploads", "stores", storeId.toString());
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ".jpg";
            Path target = dir.resolve(filename);
            // Resize to max 1600px longest side and compress to ~75% quality as JPEG
            try (var in = file.getInputStream()) {
                Thumbnails.of(in)
                        .size(1600, 1600)
                        .outputFormat("jpg")
                        .outputQuality(0.75)
                        .toFile(target.toFile());
            }
            return "/uploads/stores/" + storeId + "/" + filename;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save photo");
        }
    }
}
