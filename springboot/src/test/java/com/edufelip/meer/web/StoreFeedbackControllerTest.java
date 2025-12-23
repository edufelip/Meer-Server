package com.edufelip.meer.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.store.StoreFeedback;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.StoreFeedbackService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoreFeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class StoreFeedbackControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TokenProvider tokenProvider;
  @MockitoBean private AuthUserRepository authUserRepository;
  @MockitoBean private ThriftStoreRepository thriftStoreRepository;
  @MockitoBean private StoreFeedbackService storeFeedbackService;

  @Test
  void deleteIsIdempotent() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");

    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setName("Store");
    store.setAddressLine("123 Road");

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(store));

    mockMvc
        .perform(
            delete("/stores/{storeId}/feedback", storeId).header("Authorization", "Bearer token"))
        .andExpect(status().isNoContent());

    verify(storeFeedbackService).delete(userId, storeId);
  }

  @Test
  void deleteReturnsNotFoundWhenStoreMissing() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            delete("/stores/{storeId}/feedback", storeId).header("Authorization", "Bearer token"))
        .andExpect(status().isNotFound());

    verifyNoInteractions(storeFeedbackService);
  }

  @Test
  void deleteRequiresAuth() throws Exception {
    mockMvc
        .perform(delete("/stores/{storeId}/feedback", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(tokenProvider, authUserRepository, thriftStoreRepository);
    verifyNoInteractions(storeFeedbackService);
  }

  @Test
  void upsertRejectsScoreOutsideRange() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");

    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setName("Store");
    store.setAddressLine("123 Road");

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeFeedbackService.upsert(eq(user), eq(store), eq(6), eq("bad")))
        .thenThrow(new IllegalArgumentException("score must be between 1 and 5"));

    mockMvc
        .perform(
            post("/stores/{storeId}/feedback", storeId)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":6,\"body\":\"bad\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void upsertRejectsOverlongBody() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");

    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setName("Store");
    store.setAddressLine("123 Road");

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(store));
    when(storeFeedbackService.upsert(eq(user), eq(store), eq(5), any()))
        .thenReturn(new StoreFeedback(user, store, 5, "ok"));

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 2001; i++) {
      sb.append('a');
    }

    mockMvc
        .perform(
            post("/stores/{storeId}/feedback", storeId)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":5,\"body\":\"" + sb + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteReturnsUnauthorizedWhenTokenExpired() throws Exception {
    UUID storeId = UUID.randomUUID();

    when(tokenProvider.parseAccessToken("expired-token")).thenThrow(new InvalidTokenException());

    mockMvc
        .perform(
            delete("/stores/{storeId}/feedback", storeId)
                .header("Authorization", "Bearer expired-token"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(authUserRepository, thriftStoreRepository);
    verifyNoInteractions(storeFeedbackService);
  }

  @Test
  void deleteReturnsUnauthorizedForMalformedBearer() throws Exception {
    UUID storeId = UUID.randomUUID();

    mockMvc
        .perform(
            delete("/stores/{storeId}/feedback", storeId).header("Authorization", "Token abc123"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(tokenProvider, authUserRepository, thriftStoreRepository);
    verifyNoInteractions(storeFeedbackService);
  }
}
