package com.edufelip.meer.bootstrap;

import com.edufelip.meer.core.category.Category;
import com.edufelip.meer.domain.repo.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"default", "local-db", "prod", "local"})
public class CategorySeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(CategorySeeder.class);

    private final CategoryRepository categoryRepository;
    private final boolean enabled;

    public CategorySeeder(CategoryRepository categoryRepository,
                          @Value("${meer.seed.categories:true}") boolean enabled) {
        this.categoryRepository = categoryRepository;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Category seeding disabled (meer.seed.categories=false)");
            return;
        }

        List<Category> seeds = List.of(
                new Category("casa", "Brechó de Casa", "brecho-categories-house"),
                new Category("masculino", "Brechó Masculino", "categories-masculino"),
                new Category("feminino", "Brechó Feminino", "categories-feminino"),
                new Category("infantil", "Brechó Infantil", "categories-infantil"),
                new Category("luxo", "Brechó de Luxo", "categories-luxo"),
                new Category("designer", "Brechó de Designer", "categories-designer"),
                new Category("desapego", "Brechó de Desapego", "categories-desapego"),
                new Category("geral", "Brechós Gerais", "categories-geral")
        );

        seeds.forEach(seed -> categoryRepository.findById(seed.getId())
                .map(existing -> {
                    if (!existing.getName().equals(seed.getName()) || !existing.getImageResId().equals(seed.getImageResId())) {
                        existing.setName(seed.getName());
                        existing.setImageResId(seed.getImageResId());
                        return categoryRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> categoryRepository.save(seed))
        );

        log.info("Category seeding complete ({} records ensured)", seeds.size());
    }
}
