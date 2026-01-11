package com.edufelip.meer.domain.port;

import java.util.List;
import java.util.UUID;

public interface PhotoStoragePort {

  record UploadSlot(String uploadUrl, String fileKey, String contentType) {}

  record StoredObject(String contentType, Long size) {}

  List<UploadSlot> createUploadSlots(UUID storeId, int count, List<String> contentTypes);

  StoredObject fetchRequired(String fileKey);

  String publicUrl(String fileKey);

  String extractFileKey(String url);

  void deleteByUrl(String url);
}
