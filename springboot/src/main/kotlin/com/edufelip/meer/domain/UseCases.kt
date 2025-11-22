package com.edufelip.meer.domain

import com.edufelip.meer.core.ThriftStoreId
import com.edufelip.meer.core.content.GuideContent
import com.edufelip.meer.core.store.ThriftStore

class GetThriftStoreUseCase(private val thriftStoreRepository: ThriftStoreRepository) {
    fun execute(id: Int): ThriftStore? {
        return thriftStoreRepository.findById(id).orElse(null)
    }
}

class GetThriftStoresUseCase(private val thriftStoreRepository: ThriftStoreRepository) {
    fun execute(): List<ThriftStore> {
        return thriftStoreRepository.findAll()
    }
}

class CreateThriftStoreUseCase(private val thriftStoreRepository: ThriftStoreRepository) {
    fun execute(thriftStore: ThriftStore): ThriftStore {
        return thriftStoreRepository.save(thriftStore)
    }
}

class GetGuideContentUseCase(private val guideContentRepository: GuideContentRepository) {
    fun execute(id: Int): GuideContent? {
        return guideContentRepository.findById(id).orElse(null)
    }
}

class GetGuideContentsByThriftStoreUseCase(private val guideContentRepository: GuideContentRepository) {
    fun execute(thriftStoreId: ThriftStoreId): List<GuideContent> {
        return guideContentRepository.findByThriftStoreId(thriftStoreId)
    }
}

class CreateGuideContentUseCase(
    private val guideContentRepository: GuideContentRepository,
    private val thriftStoreRepository: ThriftStoreRepository
) {
    fun execute(guideContent: GuideContent): GuideContent {
        val thriftStore = guideContent.thriftStore?.id?.let { thriftStoreRepository.findById(it) }
        if (thriftStore != null) {
            return guideContentRepository.save(guideContent)
        } else {
            throw Exception("Thrift store not found")
        }
    }
}
