package com.edufelip.meer.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcsConfig {

  private static final Logger log = LoggerFactory.getLogger(GcsConfig.class);
  private static final List<String> STORAGE_SCOPES =
      List.of("https://www.googleapis.com/auth/devstorage.read_write");

  @Bean
  public Storage storage(@Value("${storage.gcs.credentials-path:}") String credentialsPath)
      throws IOException {
    StorageOptions.Builder optionsBuilder = StorageOptions.newBuilder();

    if (credentialsPath != null && !credentialsPath.isBlank()) {
      log.info("Initializing GCS Storage with credentials from: {}", credentialsPath);
      GoogleCredentials credentials =
          GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
      credentials = applyScopes(credentials);
      logCredentialDetails(credentials);
      optionsBuilder.setCredentials(credentials);
    } else {
      log.warn(
          "STORAGE_GCS_CREDENTIALS_PATH not set - falling back to Application Default Credentials (ADC)");
      GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
      credentials = applyScopes(credentials);
      logCredentialDetails(credentials);
      optionsBuilder.setCredentials(credentials);
    }

    return optionsBuilder.build().getService();
  }

  private GoogleCredentials applyScopes(GoogleCredentials credentials) {
    if (credentials == null) {
      return null;
    }
    if (credentials.createScopedRequired()) {
      log.info("Applying GCS Storage OAuth scopes: {}", STORAGE_SCOPES);
      return credentials.createScoped(STORAGE_SCOPES);
    }
    return credentials;
  }

  private void logCredentialDetails(GoogleCredentials credentials) {
    if (credentials instanceof ServiceAccountCredentials serviceAccountCredentials) {
      log.info(
          "GCS Storage service account clientEmail={}, projectId={}",
          serviceAccountCredentials.getClientEmail(),
          serviceAccountCredentials.getProjectId());
      return;
    }
    if (credentials != null) {
      log.info("GCS Storage credentials type={}", credentials.getClass().getSimpleName());
    } else {
      log.warn("GCS Storage credentials are NULL - will use unauthenticated access");
    }
  }
}
