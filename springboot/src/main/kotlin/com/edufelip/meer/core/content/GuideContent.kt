package com.edufelip.meer.core.content

import com.edufelip.meer.core.GuideContentId
import com.edufelip.meer.core.store.ThriftStore
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
data class GuideContent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: GuideContentId = 0,
    val title: String,
    val description: String,
    val categoryLabel: String,
    val imageUrl: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thrift_store_id")
    val thriftStore: ThriftStore? = null
)
