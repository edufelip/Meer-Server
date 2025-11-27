package com.edufelip.meer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(@Value("classpath:caffeine-cache.properties") org.springframework.core.io.Resource config) {
        Properties props = new Properties();
        try (var in = config.getInputStream()) {
            props.load(in);
        } catch (Exception ignored) { }

        var featuredTtl = parseDuration(props.getProperty("featuredTop10", "expireAfterWrite=10m"));
        var guideTtl = parseDuration(props.getProperty("guideTop10", "expireAfterWrite=10m"));
        var ratingsTtl = parseDuration(props.getProperty("storeRatings", "expireAfterWrite=5m"));
        var categoriesTtl = parseDuration(props.getProperty("categoriesAll", "expireAfterWrite=60m"));

        var manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                new CaffeineCache("featuredTop10", Caffeine.newBuilder()
                        .expireAfterWrite(featuredTtl)
                        .maximumSize(10)
                        .build()),
                new CaffeineCache("guideTop10", Caffeine.newBuilder()
                        .expireAfterWrite(guideTtl)
                        .maximumSize(10)
                        .build()),
                new CaffeineCache("storeRatings", Caffeine.newBuilder()
                        .expireAfterWrite(ratingsTtl)
                        .maximumSize(200)
                        .build()),
                new CaffeineCache("categoriesAll", Caffeine.newBuilder()
                        .expireAfterWrite(categoriesTtl)
                        .maximumSize(5)
                        .build())
        ));
        return manager;
    }

    private Duration parseDuration(String spec) {
        // expects format expireAfterWrite=Nm or Ns
        var parts = spec.split("=");
        if (parts.length != 2) return Duration.ofMinutes(10);
        String v = parts[1].trim();
        if (v.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(v.substring(0, v.length() - 1)));
        }
        if (v.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(v.substring(0, v.length() - 1)));
        }
        return Duration.ofMinutes(10);
    }
}
