package com.edufelip.meer.domain

import com.edufelip.meer.core.AuthUserId
import com.edufelip.meer.core.CategoryId
import com.edufelip.meer.core.GuideContentId
import com.edufelip.meer.core.ThriftStoreId
import com.edufelip.meer.core.auth.AuthUser
import com.edufelip.meer.core.category.Category
import com.edufelip.meer.core.content.GuideContent
import com.edufelip.meer.core.store.ThriftStore
import org.springframework.data.jpa.repository.JpaRepository

interface AuthUserRepository : JpaRepository<AuthUser, AuthUserId>

interface CategoryRepository : JpaRepository<Category, CategoryId>

interface GuideContentRepository : JpaRepository<GuideContent, GuideContentId> {
    fun findByThriftStoreId(thriftStoreId: ThriftStoreId): List<GuideContent>
}

interface ThriftStoreRepository : JpaRepository<ThriftStore, ThriftStoreId>
