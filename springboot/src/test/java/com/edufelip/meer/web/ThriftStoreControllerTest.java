package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.core.store.ThriftStorePhoto;
import com.edufelip.meer.domain.CreateGuideContentUseCase;
import com.edufelip.meer.domain.GetGuideContentsByThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoreUseCase;
import com.edufelip.meer.domain.GetThriftStoresUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.CategoryRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.GcsStorageService;
import com.edufelip.meer.service.StoreFeedbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ThriftStoreController.class)
@AutoConfigureMockMvc(addFilters = false)
class ThriftStoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GetThriftStoreUseCase getThriftStoreUseCase;
    @MockitoBean private GetThriftStoresUseCase getThriftStoresUseCase;
    @MockitoBean private GetGuideContentsByThriftStoreUseCase getGuideContentsByThriftStoreUseCase;
    @MockitoBean private CreateGuideContentUseCase createGuideContentUseCase;
    @MockitoBean private ThriftStoreRepository thriftStoreRepository;
    @MockitoBean private CategoryRepository categoryRepository;
    @MockitoBean private AuthUserRepository authUserRepository;
    @MockitoBean private TokenProvider tokenProvider;
    @MockitoBean private StoreFeedbackService storeFeedbackService;
    @MockitoBean private GcsStorageService gcsStorageService;
    @MockitoBean private StoreFeedbackRepository storeFeedbackRepository;

    @Test
    void adminDeleteStoreCleansRelationsAndAssets() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        AuthUser admin = new AuthUser();
        admin.setId(adminId);
        admin.setEmail("admin@example.com");
        admin.setDisplayName("Admin");
        admin.setPasswordHash("hash");
        admin.setRole(Role.ADMIN);

        AuthUser owner = new AuthUser();
        owner.setId(ownerId);
        owner.setEmail("owner@example.com");
        owner.setDisplayName("Owner");
        owner.setPasswordHash("hash");

        ThriftStore store = new ThriftStore();
        store.setId(storeId);
        store.setOwner(owner);
        owner.setOwnedThriftStore(store);

        ThriftStorePhoto photo = new ThriftStorePhoto(store, "https://storage.googleapis.com/bucket/photo.jpg", 0);
        store.setPhotos(List.of(photo));

        when(tokenProvider.parseAccessToken("admin-token")).thenReturn(new TokenPayload(adminId, "admin@example.com", "Admin", Role.ADMIN));
        when(authUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(store));

        mockMvc.perform(delete("/stores/{id}", storeId)
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(authUserRepository, times(1)).deleteFavoritesByStoreId(storeId);
        verify(storeFeedbackRepository, times(1)).deleteByThriftStoreId(storeId);
        verify(authUserRepository, times(1)).save(owner);
        verify(gcsStorageService, times(1)).deleteByUrl("https://storage.googleapis.com/bucket/photo.jpg");
        verify(thriftStoreRepository, times(1)).delete(store);
    }
}
