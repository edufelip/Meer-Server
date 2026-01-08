package com.edufelip.meer.domain;

public class PushNotificationException extends Exception {
  public PushNotificationException(String message, Throwable cause) {
    super(message, cause);
  }

  public PushNotificationException(String message) {
    super(message);
  }
}
