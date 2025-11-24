package com.edufelip.meer.web;

import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.CreateGuideContentUseCase;
import com.edufelip.meer.domain.CreateThriftStoreUseCase;
import com.edufelip.meer.domain.GetGuideContentsByThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.GuideContentDto;
import com.edufelip.meer.dto.ThriftStoreDto;
import com.edufelip.meer.mapper.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/thrift-stores")
public class ThriftStoreController {

    private final GetThriftStoreUseCase getThriftStoreUseCase;
    private final GetThriftStoresUseCase getThriftStoresUseCase;
    private final CreateThriftStoreUseCase createThriftStoreUseCase;
    private final GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase;
    private final CreateGuideContentUseCase createGuideContentUseCase;
    private final ThriftStoreRepository thriftStoreRepository;

    public ThriftStoreController(GetThriftStoreUseCase getThriftStoreUseCase,
                                 GetThriftStoresUseCase getThriftStoresUseCase,
                                 CreateThriftStoreUseCase createThriftStoreUseCase,
                                 GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase,
                                 CreateGuideContentUseCase createGuideContentUseCase,
                                 ThriftStoreRepository thriftStoreRepository) {
        this.getThriftStoreUseCase = getThriftStoreUseCase;
        this.getThriftStoresUseCase = getThriftStoresUseCase;
        this.createThriftStoreUseCase = createThriftStoreUseCase;
        this.getGuideContentsByThriftStoreUseCase = getGuideContentsByThriftStoreUseCase;
        this.createGuideContentUseCase = createGuideContentUseCase;
        this.thriftStoreRepository = thriftStoreRepository;
    }

    @GetMapping
    public List<ThriftStoreDto> getAll() {
        return getThriftStoresUseCase.execute().stream().map(store -> Mappers.toDto(store, false)).toList();
    }

    @GetMapping("/{id}")
    public ThriftStoreDto getById(@PathVariable Integer id) {
        var store = getThriftStoreUseCase.execute(id);
        return store != null ? Mappers.toDto(store, true) : null;
    }

    @PostMapping
    public ThriftStoreDto create(@RequestBody ThriftStore thriftStore) {
        return Mappers.toDto(createThriftStoreUseCase.execute(thriftStore), true);
    }

    @GetMapping("/{storeId}/contents")
    public List<GuideContentDto> getContentsByThriftStoreId(@PathVariable Integer storeId) {
        return getGuideContentsByThriftStoreUseCase.execute(storeId).stream().map(Mappers::toDto).toList();
    }

    @PostMapping("/{storeId}/contents")
    public GuideContentDto createGuideContent(
            @PathVariable Integer storeId,
            @RequestBody GuideContent guideContent
    ) {
        var thriftStore = thriftStoreRepository.findById(storeId).orElseThrow(() -> new RuntimeException("Thrift store not found"));
        var contentWithStore = new GuideContent(guideContent.getId(), guideContent.getTitle(), guideContent.getDescription(), guideContent.getCategoryLabel(), guideContent.getImageUrl(), thriftStore);
        return Mappers.toDto(createGuideContentUseCase.execute(contentWithStore));
    }
}
