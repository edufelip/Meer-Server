package com.edufelip.meer.dto

data class GuideContentDto(
    val id: Int,
    val title: String,
    val description: String,
    val categoryLabel: String,
    val imageUrl: String,
    val thriftStoreId: Int?
)
