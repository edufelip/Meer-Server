package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {}
