package com.edufelip.meer.web

import com.edufelip.meer.domain.CategoryRepository
import com.edufelip.meer.dto.CategoryDto
import com.edufelip.meer.mapper.toDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/categories")
class CategoryController(
    private val categoryRepository: CategoryRepository
) {

    @GetMapping
    fun getAll(): List<CategoryDto> {
        return categoryRepository.findAll().map { it.toDto() }
    }
}
