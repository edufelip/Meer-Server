package com.edufelip.meer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Lightweight request/response logger for dev environments.
 * Logs method, path (with query), status and duration.
 */
@Configuration
@Profile({"local-db", "default", "local"})
public class RequestResponseLoggingConfig {

    @Bean
    public OncePerRequestFilter requestResponseLoggingFilter() {
        return new OncePerRequestFilter() {
            private final Logger log = LoggerFactory.getLogger("HTTP");

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                long start = System.currentTimeMillis();
                ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
                try {
                    filterChain.doFilter(request, wrapper);
                } finally {
                    long duration = System.currentTimeMillis() - start;
                    String path = request.getRequestURI();
                    if (request.getQueryString() != null) {
                        path += "?" + request.getQueryString();
                    }
                    log.info("[HTTP] {} {} -> {} ({} ms)", request.getMethod(), path, wrapper.getStatus(), duration);
                    wrapper.copyBodyToResponse();
                }
            }
        };
    }
}
