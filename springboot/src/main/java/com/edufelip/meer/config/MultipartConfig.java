package com.edufelip.meer.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * Central multipart limits for file uploads.
 * We keep the same 25 MB per-file / 75 MB per-request caps and explicitly
 * raise the file-count ceiling so multi-photo store creations do not fail
 * with Tomcat's default count limit.
 */
@Configuration
public class MultipartConfig {

    static {
        // Bump Tomcat / Commons FileUpload file-count ceiling to allow multi-photo uploads.
        System.setProperty("org.apache.tomcat.util.http.fileupload.FileUploadBase.fileCountMax", "30");
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        // location = "" (use default temp), per-file 25 MB, per-request 75 MB, threshold 0.
        return new MultipartConfigElement(
                "",
                DataSize.ofMegabytes(25).toBytes(),
                DataSize.ofMegabytes(75).toBytes(),
                0
        );
    }
}
