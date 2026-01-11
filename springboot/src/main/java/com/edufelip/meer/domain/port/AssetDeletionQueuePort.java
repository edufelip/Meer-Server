package com.edufelip.meer.domain.port;

import java.util.List;

public interface AssetDeletionQueuePort {

  void enqueueAll(List<String> urls, String sourceType, String sourceId);
}
