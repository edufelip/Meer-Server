package com.edufelip.meer.domain.port;

import com.edufelip.meer.core.push.PushEnvironment;
import com.edufelip.meer.domain.PushNotificationException;
import java.util.Map;
import java.util.UUID;

public interface PushNotificationPort {

  String sendTestPush(String token, String title, String body, String type, String id)
      throws PushNotificationException;

  String sendToTopic(String topic, String title, String body, Map<String, String> data)
      throws PushNotificationException;

  int sendToUser(
      UUID userId,
      PushEnvironment environment,
      String title,
      String body,
      Map<String, String> data);
}
