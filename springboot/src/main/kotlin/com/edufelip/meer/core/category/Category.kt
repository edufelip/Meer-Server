package com.edufelip.meer.core.category

import com.edufelip.meer.core.CategoryId
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class Category(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: CategoryId = 0,
    val name: String,
    val imageUrl: String
)
