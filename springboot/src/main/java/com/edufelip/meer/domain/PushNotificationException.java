package com.edufelip.meer.domain;

public class PushNotificationException extends Exception {
  private final PushNotificationFailureReason failureReason;
  private final String providerErrorCode;

  public PushNotificationException(String message, Throwable cause) {
    this(PushNotificationFailureReason.UNKNOWN, null, message, cause);
  }

  public PushNotificationException(String message) {
    this(PushNotificationFailureReason.UNKNOWN, null, message, null);
  }

  public PushNotificationException(PushNotificationFailureReason failureReason, String message) {
    this(failureReason, null, message, null);
  }

  public PushNotificationException(
      PushNotificationFailureReason failureReason,
      String providerErrorCode,
      String message,
      Throwable cause) {
    super(message, cause);
    this.failureReason =
        failureReason != null ? failureReason : PushNotificationFailureReason.UNKNOWN;
    this.providerErrorCode = providerErrorCode;
  }

  public PushNotificationFailureReason getFailureReason() {
    return failureReason;
  }

  public String getProviderErrorCode() {
    return providerErrorCode;
  }
}
