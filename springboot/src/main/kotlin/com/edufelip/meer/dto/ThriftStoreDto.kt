package com.edufelip.meer.dto

data class ThriftStoreDto(
    val id: Int,
    val name: String,
    val tagline: String,
    val coverImageUrl: String,
    val galleryUrls: List<String>?,
    val addressLine: String,
    val latitude: Double?,
    val longitude: Double?,
    val mapImageUrl: String?,
    val openingHours: String,
    val openingHoursNotes: String?,
    val facebook: String?,
    val instagram: String?,
    val website: String?,
    val whatsapp: String?,
    val categories: List<String>,
    val distanceKm: Double?,
    val walkTimeMinutes: Int?,
    val neighborhood: String?,
    val badgeLabel: String?,
    val isFavorite: Boolean?,
    val description: String?,
    val contents: List<GuideContentDto>? = null
)
