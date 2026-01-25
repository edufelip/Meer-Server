package com.edufelip.meer.testutil;

import com.edufelip.meer.domain.port.PhotoStoragePort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LocalPhotoStorageFake implements PhotoStoragePort {
  private final Path baseDir;
  private final Map<String, StoredObject> objects = new HashMap<>();

  public LocalPhotoStorageFake(Path baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public List<PhotoStoragePort.UploadSlot> createUploadSlots(
      UUID storeId, int count, List<String> contentTypes) {
    List<PhotoStoragePort.UploadSlot> slots = new ArrayList<>();
    String prefix = storeId != null ? "stores/%s".formatted(storeId) : "contents";
    for (int i = 0; i < count; i++) {
      String ctype =
          contentTypes != null && contentTypes.size() > i && contentTypes.get(i) != null
              ? contentTypes.get(i)
              : "image/jpeg";
      String fileKey = "%s/%s".formatted(prefix, UUID.randomUUID());
      objects.put(fileKey, new StoredObject(ctype, 0L));
      slots.add(new PhotoStoragePort.UploadSlot("/uploads/" + fileKey, fileKey, ctype));
      try {
        Path path = baseDir.resolve(fileKey);
        Files.createDirectories(path.getParent());
        Files.write(path, new byte[0]);
      } catch (Exception ignored) {
      }
    }
    return slots;
  }

  @Override
  public StoredObject fetchRequired(String fileKey) {
    StoredObject obj = objects.get(fileKey);
    if (obj == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Uploaded file not found: " + fileKey);
    }
    return obj;
  }

  @Override
  public String publicUrl(String fileKey) {
    return "/uploads/" + fileKey;
  }

  @Override
  public String extractFileKey(String url) {
    if (url == null) return null;
    return url.startsWith("/uploads/") ? url.substring("/uploads/".length()) : null;
  }

  @Override
  public void deleteByUrl(String url) {
    if (url == null) return;
    if (!url.startsWith("/uploads/")) return;
    String key = url.substring("/uploads/".length());
    objects.remove(key);
    try {
      Files.deleteIfExists(baseDir.resolve(key));
    } catch (Exception ignored) {
    }
  }

  public void storeObject(String fileKey, String contentType, byte[] bytes) {
    objects.put(fileKey, new StoredObject(contentType, (long) bytes.length));
    try {
      Path path = baseDir.resolve(fileKey);
      Files.createDirectories(path.getParent());
      Files.write(path, bytes);
    } catch (Exception ignored) {
    }
  }
}
