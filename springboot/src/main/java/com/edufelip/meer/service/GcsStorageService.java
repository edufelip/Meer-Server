package com.edufelip.meer.service;

import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GcsStorageService implements PhotoStoragePort {

  private static final Logger log = LoggerFactory.getLogger(GcsStorageService.class);

  private final Storage storage;
  private final String bucket;
  private final Duration signedUrlTtl;
  private final String publicBaseUrl;
  private final String avatarsPrefix;

  public GcsStorageService(
      Storage storage,
      @Value("${storage.gcs.bucket}") String bucket,
      @Value("${storage.gcs.signed-url-ttl-minutes:120}") long signedUrlTtlMinutes,
      @Value("${storage.gcs.public-base-url:}") String publicBaseUrl,
      @Value("${storage.gcs.avatars-prefix:avatars}") String avatarsPrefix) {
    this.storage = storage;
    this.bucket = bucket;
    this.signedUrlTtl = Duration.ofMinutes(signedUrlTtlMinutes);
    this.publicBaseUrl =
        (publicBaseUrl == null || publicBaseUrl.isBlank())
            ? "https://storage.googleapis.com/" + bucket
            : publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    this.avatarsPrefix = avatarsPrefix;
  }

  public List<PhotoStoragePort.UploadSlot> createUploadSlots(
      UUID storeId, int count, List<String> contentTypes) {
    List<PhotoStoragePort.UploadSlot> slots = new ArrayList<>();
    String prefix = storeId != null ? "stores/%s".formatted(storeId) : "contents";
    for (int i = 0; i < count; i++) {
      String ctype =
          contentTypes != null && contentTypes.size() > i && contentTypes.get(i) != null
              ? contentTypes.get(i)
              : "image/jpeg";
      String objectName = "%s/%s".formatted(prefix, UUID.randomUUID());
      BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName).setContentType(ctype).build();
      URL url =
          storage.signUrl(
              blobInfo,
              signedUrlTtl.toMinutes(),
              TimeUnit.MINUTES,
              SignUrlOption.httpMethod(HttpMethod.PUT),
              SignUrlOption.withV4Signature(),
              SignUrlOption.withContentType());
      slots.add(new PhotoStoragePort.UploadSlot(url.toString(), objectName, ctype));
    }
    return slots;
  }

  public PhotoStoragePort.UploadSlot createAvatarSlot(String userId, String contentType) {
    String ctype = contentType != null ? contentType : "image/jpeg";
    String objectName = "%s/%s-%s".formatted(avatarsPrefix, userId, UUID.randomUUID());
    BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName).setContentType(ctype).build();
    URL url =
        storage.signUrl(
            blobInfo,
            signedUrlTtl.toMinutes(),
            TimeUnit.MINUTES,
            SignUrlOption.httpMethod(HttpMethod.PUT),
            SignUrlOption.withV4Signature(),
            SignUrlOption.withContentType());
    return new PhotoStoragePort.UploadSlot(url.toString(), objectName, ctype);
  }

  public Blob fetchRequiredObject(String fileKey) {
    Blob blob = storage.get(BlobId.of(bucket, fileKey));
    if (blob == null || !blob.exists()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Uploaded file not found: " + fileKey);
    }
    return blob;
  }

  @Override
  public StoredObject fetchRequired(String fileKey) {
    Blob blob = fetchRequiredObject(fileKey);
    return new StoredObject(blob.getContentType(), blob.getSize());
  }

  public String publicUrl(String fileKey) {
    return publicBaseUrl + "/" + fileKey;
  }

  public String publicBaseUrl() {
    return publicBaseUrl;
  }

  public String getAvatarsPrefix() {
    return avatarsPrefix;
  }

  /**
   * Extract the object key from a GCS URL (signed or public). Returns null when it does not point
   * to our bucket.
   */
  @Override
  public String extractFileKey(String url) {
    return deriveKey(url);
  }

  public String getBucket() {
    return bucket;
  }

  public void deleteByFileKey(String fileKey) {
    if (fileKey == null || fileKey.isBlank()) return;
    boolean deleted = storage.delete(BlobId.of(bucket, fileKey));
    if (deleted) {
      log.info("GCS delete succeeded bucket={} key={}", bucket, fileKey);
    } else {
      log.warn("GCS delete reported not found/failed bucket={} key={}", bucket, fileKey);
    }
  }

  public void deleteByUrl(String url) {
    if (url == null || url.isBlank()) return;
    if (deleteLocalIfUploads(url)) {
      return;
    }
    String key = deriveKey(url);
    if (key == null) {
      log.warn("GCS delete skipped (URL not in configured bucket): {}", url);
      return;
    }
    deleteByFileKey(key);
  }

  private String deriveKey(String url) {
    if (url == null) return null;
    // Strip bucket-hosted URL patterns
    if (url.startsWith(publicBaseUrl + "/")) {
      return stripQuery(url.substring((publicBaseUrl + "/").length()));
    }
    String gsBase = "https://storage.googleapis.com/" + bucket + "/";
    if (url.startsWith(gsBase)) {
      return stripQuery(url.substring(gsBase.length()));
    }
    return null;
  }

  private String stripQuery(String path) {
    int idx = path.indexOf('?');
    return idx >= 0 ? path.substring(0, idx) : path;
  }

  private boolean deleteLocalIfUploads(String url) {
    if (url == null || !url.startsWith("/uploads/")) return false;
    try {
      Path path = Paths.get("springboot", url.substring(1));
      Files.deleteIfExists(path);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }
}
