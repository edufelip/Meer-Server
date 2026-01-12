package com.edufelip.meer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.push.PushToken;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PushNotificationServiceTest {

  @Test
  void sendToStoredTokenReturnsTrueOnSuccess() throws Exception {
    FirebaseMessaging firebaseMessaging = Mockito.mock(FirebaseMessaging.class);
    PushTokenRepository repository = Mockito.mock(PushTokenRepository.class);
    PushNotificationService service = new PushNotificationService(firebaseMessaging, repository);

    PushToken token = new PushToken();
    token.setId(UUID.randomUUID());
    token.setUserId(UUID.randomUUID());
    token.setFcmToken("token-1");

    when(firebaseMessaging.send(any(Message.class))).thenReturn("msg-1");

    boolean result =
        service.sendToStoredToken(
            token, "Title", "Body", Map.of("type", "guide_content", "id", "123"));

    assertThat(result).isTrue();
    verify(repository, never()).deleteById(any());
  }

  @Test
  void sendToStoredTokenDeletesOnUnregistered() throws Exception {
    FirebaseMessaging firebaseMessaging = Mockito.mock(FirebaseMessaging.class);
    PushTokenRepository repository = Mockito.mock(PushTokenRepository.class);
    PushNotificationService service = new PushNotificationService(firebaseMessaging, repository);

    PushToken token = new PushToken();
    UUID tokenId = UUID.randomUUID();
    token.setId(tokenId);
    token.setUserId(UUID.randomUUID());
    token.setFcmToken("token-2");

    FirebaseMessagingException ex = Mockito.mock(FirebaseMessagingException.class);
    when(ex.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
    when(firebaseMessaging.send(any(Message.class))).thenThrow(ex);

    boolean result =
        service.sendToStoredToken(token, "Title", "Body", Map.of("type", "store", "id", "abc"));

    assertThat(result).isFalse();
    verify(repository).deleteById(tokenId);
  }

  @Test
  void sendToStoredTokenKeepsTokenOnOtherErrors() throws Exception {
    FirebaseMessaging firebaseMessaging = Mockito.mock(FirebaseMessaging.class);
    PushTokenRepository repository = Mockito.mock(PushTokenRepository.class);
    PushNotificationService service = new PushNotificationService(firebaseMessaging, repository);

    PushToken token = new PushToken();
    token.setId(UUID.randomUUID());
    token.setUserId(UUID.randomUUID());
    token.setFcmToken("token-3");

    FirebaseMessagingException ex = Mockito.mock(FirebaseMessagingException.class);
    when(ex.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
    when(firebaseMessaging.send(any(Message.class))).thenThrow(ex);

    boolean result =
        service.sendToStoredToken(token, "Title", "Body", Map.of("type", "store", "id", "xyz"));

    assertThat(result).isFalse();
    verify(repository, never()).deleteById(any());
  }
}
