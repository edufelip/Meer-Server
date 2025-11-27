package com.edufelip.meer.web;

import com.edufelip.meer.domain.repo.CategoryRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.CategoryDto;
import com.edufelip.meer.dto.CategoryStoreItemDto;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.mapper.Mappers;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ThriftStoreRepository thriftStoreRepository;

    public CategoryController(CategoryRepository categoryRepository, ThriftStoreRepository thriftStoreRepository) {
        this.categoryRepository = categoryRepository;
        this.thriftStoreRepository = thriftStoreRepository;
    }

    @GetMapping
    @Cacheable("categoriesAll")
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll().stream().map(Mappers::toDto).toList();
    }

    @GetMapping("/{categoryId}/stores")
    public PageResponse<CategoryStoreItemDto> getStoresByCategory(
            @PathVariable String categoryId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
        }

        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }

        var pageable = PageRequest.of(page - 1, pageSize);
        var result = thriftStoreRepository.findByCategoryId(categoryId, pageable);

        var items = result.getContent().stream()
                .map(store -> new CategoryStoreItemDto(
                        store.getId(),
                        store.getName(),
                        store.getCoverImageUrl(),
                        store.getAddressLine(),
                        null,
                        null,
                        store.getCategories(),
                        store.getIsFavorite()
                ))
                .toList();

        return new PageResponse<>(items, page, result.hasNext());
    }
}
