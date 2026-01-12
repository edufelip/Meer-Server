package com.edufelip.meer.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final List<String> allowedOriginPatterns;
  private final boolean allowCredentials;

  public CorsConfig(@Value("${meer.cors.allowed-origins:}") String originsProp) {
    if (StringUtils.hasText(originsProp)) {
      this.allowedOriginPatterns =
          Arrays.stream(originsProp.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .toList();
      this.allowCredentials = true; // only set when explicit origins are provided
    } else {
      this.allowedOriginPatterns = List.of("*");
      this.allowCredentials = false;
    }
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        // We use origin patterns so dev/staging can safely allow wildcard subdomains
        // (e.g. https://*.guiabrecho.com.br) via MEER_CORS_ALLOWED_ORIGINS.
        .allowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new))
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        // Allow all headers to avoid preflight failures from browser client-hints (sec-ch-ua, etc.)
        .allowedHeaders("*")
        .allowCredentials(allowCredentials)
        .maxAge(3600);
  }
}
