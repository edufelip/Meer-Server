package com.edufelip.meer.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.content.GuideContent;
import com.edufelip.meer.core.store.Social;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.CreateStoreGuideContentUseCase;
import com.edufelip.meer.domain.CreateThriftStoreUseCase;
import com.edufelip.meer.domain.DeleteThriftStoreUseCase;
import com.edufelip.meer.domain.GetStoreContentsUseCase;
import com.edufelip.meer.domain.GetStoreDetailsUseCase;
import com.edufelip.meer.domain.GetStoreListingsUseCase;
import com.edufelip.meer.domain.ReplaceStorePhotosUseCase;
import com.edufelip.meer.domain.RequestStorePhotoUploadsUseCase;
import com.edufelip.meer.domain.UpdateThriftStoreUseCase;
import com.edufelip.meer.domain.port.PhotoStoragePort;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ThriftStoreController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TestClockConfig.class, AuthUserResolver.class})
class ThriftStoreControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetStoreListingsUseCase getStoreListingsUseCase;
  @MockitoBean private GetStoreDetailsUseCase getStoreDetailsUseCase;
  @MockitoBean private GetStoreContentsUseCase getStoreContentsUseCase;
  @MockitoBean private CreateThriftStoreUseCase createThriftStoreUseCase;
  @MockitoBean private UpdateThriftStoreUseCase updateThriftStoreUseCase;
  @MockitoBean private DeleteThriftStoreUseCase deleteThriftStoreUseCase;
  @MockitoBean private RequestStorePhotoUploadsUseCase requestStorePhotoUploadsUseCase;
  @MockitoBean private ReplaceStorePhotosUseCase replaceStorePhotosUseCase;
  @MockitoBean private CreateStoreGuideContentUseCase createStoreGuideContentUseCase;
  @MockitoBean private AuthUserRepository authUserRepository;
  @MockitoBean private TokenProvider tokenProvider;

  @Test
  void adminDeleteStoreDelegatesToUseCase() throws Exception {
    UUID adminId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser admin = new AuthUser();
    admin.setId(adminId);
    admin.setEmail("admin@example.com");
    admin.setDisplayName("Admin");
    admin.setPasswordHash("hash");
    admin.setRole(Role.ADMIN);

    when(tokenProvider.parseAccessToken("admin-token"))
        .thenReturn(new TokenPayload(adminId, "admin@example.com", "Admin", Role.ADMIN));
    when(authUserRepository.findById(adminId)).thenReturn(Optional.of(admin));

    mockMvc
        .perform(
            delete("/stores/{id}", storeId)
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(deleteThriftStoreUseCase).execute(admin, storeId);
  }

  @Test
  void createStoreIncludesWebsiteAndWhatsapp() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser user = new AuthUser();
    user.setId(userId);
    user.setEmail("owner@example.com");
    user.setDisplayName("Owner");
    user.setPasswordHash("hash");
    user.setRole(Role.USER);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "owner@example.com", "Owner", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(user));
    ThriftStore created = new ThriftStore();
    created.setId(storeId);
    created.setSocial(new Social(null, null, "https://example.com", "https://wa.me/5511999999999"));
    when(createThriftStoreUseCase.execute(ArgumentMatchers.eq(user), ArgumentMatchers.any()))
        .thenReturn(created);

    String body =
        """
        {
          "name": "Social Store",
          "description": "Nice",
          "addressLine": "123 Road",
          "phone": "555-1111",
          "latitude": -23.0,
          "longitude": -46.0,
          "social": {
            "website": "https://example.com",
            "whatsapp": "https://wa.me/5511999999999"
          }
        }
        """;

    mockMvc
        .perform(
            post("/stores")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.website").value("https://example.com"))
        .andExpect(jsonPath("$.whatsapp").value("https://wa.me/5511999999999"));
  }

  @Test
  void updateStoreReturnsUpdatedSocial() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser owner = new AuthUser();
    owner.setId(userId);
    owner.setEmail("owner@example.com");
    owner.setDisplayName("Owner");
    owner.setPasswordHash("hash");
    owner.setRole(Role.USER);

    ThriftStore updated = new ThriftStore();
    updated.setId(storeId);
    updated.setSocial(
        new Social("https://facebook.com/store", "insta", "https://new.com", "wa123"));

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "owner@example.com", "Owner", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(owner));
    when(updateThriftStoreUseCase.execute(
            ArgumentMatchers.eq(owner), ArgumentMatchers.eq(storeId), ArgumentMatchers.any()))
        .thenReturn(updated);

    String body =
        """
        {
          "social": {
            "website": "https://new.com"
          }
        }
        """;

    mockMvc
        .perform(
            put("/stores/{id}", storeId)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.website").value("https://new.com"))
        .andExpect(jsonPath("$.facebook").value("https://facebook.com/store"))
        .andExpect(jsonPath("$.whatsapp").value("wa123"));
  }

  @Test
  void requestPhotoUploadSlotsDelegatesToUseCase() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser owner = new AuthUser();
    owner.setId(userId);
    owner.setEmail("owner@example.com");
    owner.setDisplayName("Owner");
    owner.setPasswordHash("hash");
    owner.setRole(Role.USER);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "owner@example.com", "Owner", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(owner));

    PhotoStoragePort.UploadSlot slot =
        new PhotoStoragePort.UploadSlot(
            "https://uploads.example.com/slot", "stores/file-key", "image/jpeg");
    when(requestStorePhotoUploadsUseCase.execute(
            ArgumentMatchers.eq(owner),
            ArgumentMatchers.eq(storeId),
            ArgumentMatchers.eq(1),
            ArgumentMatchers.eq(List.of("image/jpeg"))))
        .thenReturn(List.of(slot));

    String body =
        """
        {
          "count": 1,
          "contentTypes": ["image/jpeg"]
        }
        """;

    mockMvc
        .perform(
            post("/stores/{storeId}/photos/uploads", storeId)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.uploads[0].uploadUrl").value("https://uploads.example.com/slot"))
        .andExpect(jsonPath("$.uploads[0].fileKey").value("stores/file-key"))
        .andExpect(jsonPath("$.uploads[0].contentType").value("image/jpeg"));

    verify(requestStorePhotoUploadsUseCase).execute(owner, storeId, 1, List.of("image/jpeg"));
  }

  @Test
  void createGuideContentDelegatesToUseCase() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    AuthUser owner = new AuthUser();
    owner.setId(userId);
    owner.setEmail("owner@example.com");
    owner.setDisplayName("Owner");
    owner.setPasswordHash("hash");
    owner.setRole(Role.USER);

    when(tokenProvider.parseAccessToken("token"))
        .thenReturn(new TokenPayload(userId, "owner@example.com", "Owner", Role.USER));
    when(authUserRepository.findById(userId)).thenReturn(Optional.of(owner));

    ThriftStore store = new ThriftStore();
    store.setId(storeId);
    store.setName("Guide Store");
    store.setCoverImageUrl("cover");

    GuideContent saved = new GuideContent(1, "Title", "Desc", "cat", "type", "image", store);
    when(createStoreGuideContentUseCase.execute(
            ArgumentMatchers.eq(owner),
            ArgumentMatchers.eq(storeId),
            ArgumentMatchers.any(GuideContent.class)))
        .thenReturn(saved);

    String body =
        """
        {
          "title": "Title",
          "description": "Desc",
          "categoryLabel": "cat",
          "type": "type",
          "imageUrl": "image"
        }
        """;

    mockMvc
        .perform(
            post("/stores/{storeId}/contents", storeId)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.thriftStoreId").value(storeId.toString()))
        .andExpect(jsonPath("$.thriftStoreName").value("Guide Store"));

    verify(createStoreGuideContentUseCase)
        .execute(
            ArgumentMatchers.eq(owner),
            ArgumentMatchers.eq(storeId),
            ArgumentMatchers.any(GuideContent.class));
  }
}
