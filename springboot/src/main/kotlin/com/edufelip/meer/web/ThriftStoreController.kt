package com.edufelip.meer.web

import com.edufelip.meer.core.content.GuideContent
import com.edufelip.meer.domain.*
import com.edufelip.meer.dto.GuideContentDto
import com.edufelip.meer.dto.ThriftStoreDto
import com.edufelip.meer.mapper.toDto
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/thrift-stores")
class ThriftStoreController(
    private val getThriftStoreUseCase: GetThriftStoreUseCase,
    private val getThriftStoresUseCase: GetThriftStoresUseCase,
    private val createThriftStoreUseCase: CreateThriftStoreUseCase,
    private val getGuideContentsByThriftStoreUseCase: GetGuideContentsByThriftStoreUseCase,
    private val createGuideContentUseCase: CreateGuideContentUseCase,
    private val thriftStoreRepository: ThriftStoreRepository
) {

    @GetMapping
    fun getAll(): List<ThriftStoreDto> {
        return getThriftStoresUseCase.execute().map { it.toDto(includeContents = false) }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): ThriftStoreDto? {
        return getThriftStoreUseCase.execute(id)?.toDto(includeContents = true)
    }

    @PostMapping
    fun create(@RequestBody thriftStore: com.edufelip.meer.core.store.ThriftStore): ThriftStoreDto {
        return createThriftStoreUseCase.execute(thriftStore).toDto(includeContents = true)
    }

    @GetMapping("/{storeId}/contents")
    fun getContentsByThriftStoreId(@PathVariable storeId: Int): List<GuideContentDto> {
        return getGuideContentsByThriftStoreUseCase.execute(storeId).map { it.toDto() }
    }

    @PostMapping("/{storeId}/contents")
    fun createGuideContent(
        @PathVariable storeId: Int,
        @RequestBody guideContent: GuideContent
    ): GuideContentDto {
        val thriftStore = thriftStoreRepository.findById(storeId).orElseThrow { Exception("Thrift store not found") }
        val contentWithStore = guideContent.copy(thriftStore = thriftStore)
        return createGuideContentUseCase.execute(contentWithStore).toDto()
    }
}
