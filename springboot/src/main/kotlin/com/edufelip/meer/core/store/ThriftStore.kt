package com.edufelip.meer.core.store

import com.edufelip.meer.core.ThriftStoreId
import com.edufelip.meer.core.content.GuideContent
import jakarta.persistence.CascadeType
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity
data class ThriftStore(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: ThriftStoreId = 0,
    val name: String,
    val tagline: String,
    val coverImageUrl: String,

    @ElementCollection
    val galleryUrls: List<String>? = null,
    val addressLine: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapImageUrl: String? = null,
    val openingHours: String,
    val openingHoursNotes: String? = null,

    @Embedded
    val social: Social? = null,

    @ElementCollection
    val categories: List<String>,
    val distanceKm: Double? = null,
    val walkTimeMinutes: Int? = null,
    val neighborhood: String? = null,
    val badgeLabel: String? = null,
    val isFavorite: Boolean? = null,
    val description: String? = null,

    @OneToMany(mappedBy = "thriftStore", cascade = [CascadeType.ALL], orphanRemoval = true)
    val contents: List<GuideContent>? = null
)
