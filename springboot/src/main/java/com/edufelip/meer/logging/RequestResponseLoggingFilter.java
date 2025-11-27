package com.edufelip.meer.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;

/**
 * Logs request headers/body and response status/body for debugging.
 * Masks Authorization header and truncates large payloads to avoid log bloat.
 */
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    private static final int MAX_LOG_BYTES = 4000; // per body

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        var wrappedRequest = new ContentCachingRequestWrapper(request, MAX_LOG_BYTES);
        var wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.nanoTime();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
            logRequestAndResponse(wrappedRequest, wrappedResponse, elapsedMs);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequestAndResponse(ContentCachingRequestWrapper request,
                                        ContentCachingResponseWrapper response,
                                        long elapsedMs) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        String reqHeaders = headersToString(Collections.list(request.getHeaderNames()), request);
        String reqBody = bodyToString(request.getContentAsByteArray(), request.getCharacterEncoding(), request.getContentType());

        String resHeaders = headersToString(response.getHeaderNames(), response);
        String resBody = bodyToString(response.getContentAsByteArray(), response.getCharacterEncoding(), response.getContentType());

        log.info("HTTP {} {} | status={} | {} ms\nreqHeaders={}\nreqBody={}\nresHeaders={}\nresBody={}",
                method, uri, response.getStatus(), elapsedMs, reqHeaders, reqBody, resHeaders, resBody);
    }

    private String headersToString(Collection<String> names, HttpServletRequest request) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : names) {
            String value = request.getHeader(name);
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                value = "***masked***";
            }
            map.put(name, value);
        }
        return map.toString();
    }

    private String headersToString(Collection<String> names, HttpServletResponse response) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : names) {
            String value = String.join(",", response.getHeaders(name));
            map.put(name, value);
        }
        return map.toString();
    }

    private String bodyToString(byte[] content, String encoding, String contentType) {
        if (content == null || content.length == 0) return "<empty>";
        if (contentType != null) {
            // Avoid logging multipart/binary bodies
            if (contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
                return "<multipart skipped>";
            }
        }
        int length = Math.min(content.length, MAX_LOG_BYTES);
        String charset = StringUtils.hasText(encoding) ? encoding : Charset.defaultCharset().name();
        String body = new String(content, 0, length, Charset.forName(charset));
        if (content.length > MAX_LOG_BYTES) {
            body = body + "...<truncated>";
        }
        return body;
    }
}
