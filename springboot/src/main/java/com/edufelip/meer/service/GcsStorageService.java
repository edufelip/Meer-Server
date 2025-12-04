package com.edufelip.meer.service;

import com.edufelip.meer.dto.PhotoUploadSlot;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class GcsStorageService {

    private final Storage storage;
    private final String bucket;
    private final Duration signedUrlTtl;
    private final String publicBaseUrl;
    private final String avatarsPrefix;

    public GcsStorageService(Storage storage,
                             @Value("${storage.gcs.bucket}") String bucket,
                             @Value("${storage.gcs.signed-url-ttl-minutes:120}") long signedUrlTtlMinutes,
                             @Value("${storage.gcs.public-base-url:}") String publicBaseUrl,
                             @Value("${storage.gcs.avatars-prefix:avatars}") String avatarsPrefix) {
        this.storage = storage;
        this.bucket = bucket;
        this.signedUrlTtl = Duration.ofMinutes(signedUrlTtlMinutes);
        this.publicBaseUrl = (publicBaseUrl == null || publicBaseUrl.isBlank())
                ? "https://storage.googleapis.com/" + bucket
                : publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        this.avatarsPrefix = avatarsPrefix;
    }

    public List<PhotoUploadSlot> createUploadSlots(UUID storeId, int count, List<String> contentTypes) {
        List<PhotoUploadSlot> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String ctype = contentTypes != null && contentTypes.size() > i && contentTypes.get(i) != null
                    ? contentTypes.get(i)
                    : "image/jpeg";
            String objectName = "stores/%s/photos/%s".formatted(storeId, UUID.randomUUID());
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName)
                    .setContentType(ctype)
                    .build();
            URL url = storage.signUrl(
                    blobInfo,
                    signedUrlTtl.toMinutes(),
                    TimeUnit.MINUTES,
                    SignUrlOption.httpMethod(HttpMethod.PUT),
                    SignUrlOption.withV4Signature(),
                    SignUrlOption.withContentType());
            slots.add(new PhotoUploadSlot(url.toString(), objectName, ctype));
        }
        return slots;
    }

    public PhotoUploadSlot createAvatarSlot(String userId, String contentType) {
        String ctype = contentType != null ? contentType : "image/jpeg";
        String objectName = "%s/%s-%s".formatted(avatarsPrefix, userId, UUID.randomUUID());
        BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName)
                .setContentType(ctype)
                .build();
        URL url = storage.signUrl(
                blobInfo,
                signedUrlTtl.toMinutes(),
                TimeUnit.MINUTES,
                SignUrlOption.httpMethod(HttpMethod.PUT),
                SignUrlOption.withV4Signature(),
                SignUrlOption.withContentType());
        return new PhotoUploadSlot(url.toString(), objectName, ctype);
    }

    public Blob fetchRequiredObject(String fileKey) {
        Blob blob = storage.get(BlobId.of(bucket, fileKey));
        if (blob == null || !blob.exists()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file not found: " + fileKey);
        }
        return blob;
    }

    public String publicUrl(String fileKey) {
        return publicBaseUrl + "/" + fileKey;
    }

    public String publicBaseUrl() {
        return publicBaseUrl;
    }

    public String getBucket() {
        return bucket;
    }

    public void deleteByFileKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) return;
        storage.delete(BlobId.of(bucket, fileKey));
    }

    public void deleteByUrl(String url) {
        if (url == null || url.isBlank()) return;
        String key = deriveKey(url);
        deleteByFileKey(key);
    }

    private String deriveKey(String url) {
        if (url == null) return null;
        // Strip bucket-hosted URL patterns
        if (url.startsWith(publicBaseUrl + "/")) {
            return url.substring((publicBaseUrl + "/").length());
        }
        String gsBase = "https://storage.googleapis.com/" + bucket + "/";
        if (url.startsWith(gsBase)) {
            return url.substring(gsBase.length());
        }
        return null;
    }
}
