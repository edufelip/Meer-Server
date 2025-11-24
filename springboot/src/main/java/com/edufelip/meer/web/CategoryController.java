package com.edufelip.meer.web;

import com.edufelip.meer.domain.repo.CategoryRepository;
import com.edufelip.meer.dto.CategoryDto;
import com.edufelip.meer.mapper.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll().stream().map(Mappers::toDto).toList();
    }
}
