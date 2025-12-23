package com.edufelip.meer.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.GetGuideContentUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.GcsStorageService;
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

@WebMvcTest(GuideContentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, TestClockConfig.class})
class GuideContentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetGuideContentUseCase getGuideContentUseCase;
  @MockitoBean private GuideContentRepository guideContentRepository;
  @MockitoBean private AuthUserRepository authUserRepository;
  @MockitoBean private TokenProvider tokenProvider;
  @MockitoBean private GcsStorageService gcsStorageService;
  @MockitoBean private ThriftStoreRepository thriftStoreRepository;

  @Test
  void requestImageSlotRejectsUnsupportedContentType() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");
    user.setRole(Role.USER);

    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setName("Store");
    store.setAddressLine("123 Road");
    user.setOwnedThriftStore(store);

    GuideContent content = new GuideContent();
    content.setId(10);
    content.setThriftStore(store);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "user@example.com", "User", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(guideContentRepository.findById(10)).thenReturn(Optional.of(content));

    mockMvc
        .perform(
            post("/contents/{contentId}/image/upload", 10)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentType\":\"image/gif\"}"))
        .andExpect(status().isBadRequest());
  }
}
