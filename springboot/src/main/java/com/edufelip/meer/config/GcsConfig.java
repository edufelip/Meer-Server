package com.edufelip.meer.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcsConfig {

  @Bean
  public Storage storage(@Value("${storage.gcs.credentials-path:}") String credentialsPath)
      throws IOException {
    StorageOptions.Builder optionsBuilder = StorageOptions.newBuilder();

    if (credentialsPath != null && !credentialsPath.isBlank()) {
      GoogleCredentials credentials =
          GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
      optionsBuilder.setCredentials(credentials);
    }

    return optionsBuilder.build().getService();
  }
}
