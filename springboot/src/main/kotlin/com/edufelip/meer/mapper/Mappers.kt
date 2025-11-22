package com.edufelip.meer.mapper

import com.edufelip.meer.core.category.Category
import com.edufelip.meer.core.content.GuideContent
import com.edufelip.meer.core.store.ThriftStore
import com.edufelip.meer.dto.CategoryDto
import com.edufelip.meer.dto.GuideContentDto
import com.edufelip.meer.dto.ThriftStoreDto

fun Category.toDto() = CategoryDto(
    id = id,
    name = name,
    imageUrl = imageUrl
)

fun GuideContent.toDto() = GuideContentDto(
    id = id,
    title = title,
    description = description,
    categoryLabel = categoryLabel,
    imageUrl = imageUrl,
    thriftStoreId = thriftStore?.id
)

fun ThriftStore.toDto(includeContents: Boolean = false) = ThriftStoreDto(
    id = id,
    name = name,
    tagline = tagline,
    coverImageUrl = coverImageUrl,
    galleryUrls = galleryUrls,
    addressLine = addressLine,
    latitude = latitude,
    longitude = longitude,
    mapImageUrl = mapImageUrl,
    openingHours = openingHours,
    openingHoursNotes = openingHoursNotes,
    facebook = social?.facebook,
    instagram = social?.instagram,
    website = social?.website,
    whatsapp = social?.whatsapp,
    categories = categories,
    distanceKm = distanceKm,
    walkTimeMinutes = walkTimeMinutes,
    neighborhood = neighborhood,
    badgeLabel = badgeLabel,
    isFavorite = isFavorite,
    description = description,
    contents = if (includeContents) contents?.map { it.toDto() } else null
)
