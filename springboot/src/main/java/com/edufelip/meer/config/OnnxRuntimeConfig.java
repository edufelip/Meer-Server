package com.edufelip.meer.config;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
@ConditionalOnProperty(
    name = "moderation.nsfw.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OnnxRuntimeConfig {

  private static final Logger log = LoggerFactory.getLogger(OnnxRuntimeConfig.class);

  private final ModerationProperties moderationProperties;
  private final ResourceLoader resourceLoader;

  public OnnxRuntimeConfig(
      ModerationProperties moderationProperties, ResourceLoader resourceLoader) {
    this.moderationProperties = moderationProperties;
    this.resourceLoader = resourceLoader;
  }

  @Bean(destroyMethod = "close")
  public OrtEnvironment ortEnvironment() {
    log.info("Creating OrtEnvironment for NSFW moderation");
    return OrtEnvironment.getEnvironment();
  }

  @Bean(destroyMethod = "close")
  public OrtSession ortSession(OrtEnvironment env) throws Exception {
    String modelPath = moderationProperties.getModelPath();
    log.info("Loading ONNX model from path: {}", modelPath);

    OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
    // Use conservative thread settings for low-priority background tasks
    opts.setIntraOpNumThreads(1);
    opts.setInterOpNumThreads(1);

    // Load model - handle both classpath and file system paths
    if (modelPath.startsWith("classpath:")) {
      String resourcePath = modelPath.substring("classpath:".length());
      Resource resource = resourceLoader.getResource("classpath:" + resourcePath);

      if (!resource.exists()) {
        throw new IllegalStateException("ONNX model not found at: " + modelPath);
      }

      // Copy to temp file since ORT requires a file path
      Path tempFile = Files.createTempFile("nsfw-model-", ".onnx");
      tempFile.toFile().deleteOnExit();

      try (InputStream is = resource.getInputStream()) {
        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
      }

      log.info("Model copied to temporary location: {}", tempFile);
      OrtSession session = env.createSession(tempFile.toString(), opts);
      logModelInfo(session);
      return session;
    } else {
      // Direct file path
      OrtSession session = env.createSession(modelPath, opts);
      logModelInfo(session);
      return session;
    }
  }

  private void logModelInfo(OrtSession session) {
    log.info("=== ONNX Model Information ===");
    try {
      session
          .getInputInfo()
          .forEach(
              (name, info) -> {
                log.info("INPUT: {} -> {}", name, info.getInfo());
              });
      session
          .getOutputInfo()
          .forEach(
              (name, info) -> {
                log.info("OUTPUT: {} -> {}", name, info.getInfo());
              });
    } catch (Exception e) {
      log.warn("Failed to log model info", e);
    }
    log.info("==============================");
  }
}
