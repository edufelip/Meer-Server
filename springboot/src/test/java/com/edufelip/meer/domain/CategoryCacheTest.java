package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.config.CacheConfig;
import com.edufelip.meer.core.category.Category;
import com.edufelip.meer.domain.repo.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(CategoryCacheTest.CacheTestConfig.class)
class CategoryCacheTest {

  @Configuration
  @EnableCaching
  @Import(CacheConfig.class)
  static class CacheTestConfig {
    @Bean
    CategoryRepository categoryRepository() {
      return Mockito.mock(CategoryRepository.class);
    }

    @Bean
    GetCategoriesUseCase getCategoriesUseCase(CategoryRepository repo) {
      return new GetCategoriesUseCase(repo);
    }

    @Bean
    CreateCategoryUseCase createCategoryUseCase(CategoryRepository repo) {
      return new CreateCategoryUseCase(repo);
    }

    @Bean
    UpdateCategoryUseCase updateCategoryUseCase(CategoryRepository repo) {
      return new UpdateCategoryUseCase(repo);
    }

    @Bean
    DeleteCategoryUseCase deleteCategoryUseCase(CategoryRepository repo) {
      return new DeleteCategoryUseCase(repo);
    }
  }

  @Autowired private GetCategoriesUseCase getCategoriesUseCase;
  @Autowired private CreateCategoryUseCase createCategoryUseCase;
  @Autowired private UpdateCategoryUseCase updateCategoryUseCase;
  @Autowired private DeleteCategoryUseCase deleteCategoryUseCase;
  @Autowired private CacheManager cacheManager;
  @Autowired private CategoryRepository categoryRepository;

  @BeforeEach
  void clearCache() {
    Mockito.reset(categoryRepository);
    Cache cache = cacheManager.getCache("categoriesAll");
    if (cache != null) {
      cache.clear();
    }
  }

  @Test
  void getCategoriesCachesResults() {
    Category category = new Category("women", "women", "cat_women");
    when(categoryRepository.findAll()).thenReturn(List.of(category));

    var first = getCategoriesUseCase.executeAll();
    var second = getCategoriesUseCase.executeAll();

    assertThat(first).hasSize(1);
    assertThat(second).hasSize(1);
    verify(categoryRepository, times(1)).findAll();

    Cache cache = cacheManager.getCache("categoriesAll");
    assertThat(cache).isNotNull();
    assertThat(cache.get(SimpleKey.EMPTY)).isNotNull();
  }

  @Test
  void createEvictsCategoriesCache() {
    primeCache();
    when(categoryRepository.save(org.mockito.ArgumentMatchers.any(Category.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    createCategoryUseCase.execute(new Category("new", "new", "cat_new"));

    Cache cache = cacheManager.getCache("categoriesAll");
    assertThat(cache).isNotNull();
    assertThat(cache.get(SimpleKey.EMPTY)).isNull();
  }

  @Test
  void updateEvictsCategoriesCache() {
    primeCache();
    when(categoryRepository.save(org.mockito.ArgumentMatchers.any(Category.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    updateCategoryUseCase.execute(new Category("women", "women_updated", "cat_women_updated"));

    Cache cache = cacheManager.getCache("categoriesAll");
    assertThat(cache).isNotNull();
    assertThat(cache.get(SimpleKey.EMPTY)).isNull();
  }

  @Test
  void deleteEvictsCategoriesCache() {
    primeCache();

    deleteCategoryUseCase.execute("women");

    Cache cache = cacheManager.getCache("categoriesAll");
    assertThat(cache).isNotNull();
    assertThat(cache.get(SimpleKey.EMPTY)).isNull();
  }

  private void primeCache() {
    Category category = new Category("women", "women", "cat_women");
    when(categoryRepository.findAll()).thenReturn(List.of(category));
    getCategoriesUseCase.executeAll();
    Cache cache = cacheManager.getCache("categoriesAll");
    assertThat(cache).isNotNull();
    assertThat(cache.get(SimpleKey.EMPTY)).isNotNull();
  }
}
