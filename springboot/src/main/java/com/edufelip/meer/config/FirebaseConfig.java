package com.edufelip.meer.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {
  private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
  private static final String FIREBASE_APP_NAME = "meer-fcm";
  private static final List<String> FIREBASE_SCOPES =
      List.of("https://www.googleapis.com/auth/firebase.messaging");

  @Bean
  public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
    FirebaseOptions.Builder builder = FirebaseOptions.builder();
    GoogleCredentials credentials = resolveCredentials(properties);
    logCredentialRefresh(credentials);
    builder.setCredentials(credentials);
    String projectId = properties.getProjectId();
    String trimmedProjectId = projectId != null ? projectId.trim() : "";
    if (!trimmedProjectId.isBlank()) {
      builder.setProjectId(trimmedProjectId);
    }
    log.info(
        "Initializing FirebaseApp (projectId={})",
        trimmedProjectId.isBlank() ? "<default>" : trimmedProjectId);

    FirebaseOptions options = builder.build();
    FirebaseApp app = getOrCreateApp(options);
    log.info("Using FirebaseApp '{}' with projectId={}", app.getName(), options.getProjectId());
    return app;
  }

  @Bean
  public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
    return FirebaseMessaging.getInstance(app);
  }

  private FirebaseApp getOrCreateApp(FirebaseOptions options) {
    for (FirebaseApp app : FirebaseApp.getApps()) {
      if (FIREBASE_APP_NAME.equals(app.getName())) {
        log.info("Reusing FirebaseApp: {}", app.getName());
        return app;
      }
    }
    FirebaseApp app = FirebaseApp.initializeApp(options, FIREBASE_APP_NAME);
    log.info("FirebaseApp initialized: {}", app.getName());
    return app;
  }

  private GoogleCredentials resolveCredentials(FirebaseProperties properties) throws IOException {
    String json = properties.getCredentialsJson();
    if (json != null && !json.isBlank()) {
      log.info(
          "Using Firebase credentials from FIREBASE_CREDENTIALS_JSON (length={})",
          json.trim().length());
      try (InputStream stream =
          new ByteArrayInputStream(json.trim().getBytes(StandardCharsets.UTF_8))) {
        GoogleCredentials credentials = applyScopes(GoogleCredentials.fromStream(stream));
        logCredentialDetails(credentials);
        return credentials;
      }
    }
    String path = properties.getCredentialsPath();
    if (path != null && !path.isBlank()) {
      Path credentialsPath = Path.of(path.trim());
      log.info(
          "Using Firebase credentials from FIREBASE_CREDENTIALS_PATH={} (exists={}, readable={})",
          credentialsPath,
          Files.exists(credentialsPath),
          Files.isReadable(credentialsPath));
      try (InputStream stream = new FileInputStream(credentialsPath.toFile())) {
        GoogleCredentials credentials = applyScopes(GoogleCredentials.fromStream(stream));
        logCredentialDetails(credentials);
        return credentials;
      }
    }
    log.info("Using Firebase Application Default Credentials");
    GoogleCredentials credentials = applyScopes(GoogleCredentials.getApplicationDefault());
    logCredentialDetails(credentials);
    return credentials;
  }

  private GoogleCredentials applyScopes(GoogleCredentials credentials) {
    if (credentials == null) {
      return null;
    }
    if (credentials.createScopedRequired()) {
      log.info("Applying Firebase OAuth scopes: {}", FIREBASE_SCOPES);
      return credentials.createScoped(FIREBASE_SCOPES);
    }
    return credentials;
  }

  private void logCredentialDetails(GoogleCredentials credentials) {
    if (credentials instanceof ServiceAccountCredentials serviceAccountCredentials) {
      log.info(
          "Firebase service account clientEmail={}, projectId={}",
          serviceAccountCredentials.getClientEmail(),
          serviceAccountCredentials.getProjectId());
      return;
    }
    if (credentials != null) {
      log.info("Firebase credentials type={}", credentials.getClass().getSimpleName());
    }
  }

  private void logCredentialRefresh(GoogleCredentials credentials) {
    if (credentials == null) {
      log.warn("Firebase credentials are null; skipping token refresh diagnostics");
      return;
    }
    try {
      credentials.refreshIfExpired();
      if (credentials.getAccessToken() == null) {
        log.warn("Firebase credentials refresh completed but access token is null");
        return;
      }
      log.info(
          "Firebase access token obtained (expiresAt={})",
          credentials.getAccessToken().getExpirationTime());
    } catch (IOException ex) {
      log.warn("Failed to refresh Firebase access token", ex);
    }
  }
}
