package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.core.store.ThriftStorePhoto;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.GuideContentRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.service.GcsStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private TokenProvider tokenProvider;
    @MockBean private AuthUserRepository authUserRepository;
    @MockBean private ThriftStoreRepository thriftStoreRepository;
    @MockBean private GuideContentRepository guideContentRepository;
    @MockBean private StoreFeedbackRepository storeFeedbackRepository;
    @MockBean private GcsStorageService gcsStorageService;

    @Test
    void deleteUserRemovesStoresAssetsFavoritesAndFeedback() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        AuthUser admin = new AuthUser();
        admin.setId(adminId);
        admin.setRole(Role.ADMIN);
        admin.setEmail("admin@example.com");

        AuthUser target = new AuthUser();
        target.setId(targetId);
        target.setEmail("user@example.com");
        target.setDisplayName("Target User");
        target.setPasswordHash("hash");
        target.setPhotoUrl("https://storage.googleapis.com/bucket/avatar.png");

        ThriftStore store = new ThriftStore();
        store.setId(storeId);
        store.setOwner(target);
        ThriftStorePhoto photo = new ThriftStorePhoto(store, "https://storage.googleapis.com/bucket/photo-1.jpg", 0);
        store.setPhotos(List.of(photo));
        target.setOwnedThriftStore(store);

        Set<ThriftStore> favorites = new HashSet<>();
        favorites.add(store);
        target.setFavorites(favorites);

        when(authUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(thriftStoreRepository.findByOwnerId(targetId)).thenReturn(List.of(store));

        mockMvc.perform(delete("/dashboard/users/{id}", targetId)
                        .header("Authorization", "Bearer admin-token")
                        .requestAttr("adminUser", admin)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(thriftStoreRepository, times(1)).delete(argThat(ts -> ts.getId().equals(storeId)));
        verify(storeFeedbackRepository, times(1)).deleteByUserId(targetId);
        verify(gcsStorageService, times(1)).deleteByUrl("https://storage.googleapis.com/bucket/avatar.png");
        verify(gcsStorageService, times(1)).deleteByUrl("https://storage.googleapis.com/bucket/photo-1.jpg");
        verify(authUserRepository, times(1)).delete(target);
        verify(authUserRepository, times(2)).save(target);
    }

    @Test
    void adminCanDeleteSelf() throws Exception {
        UUID adminId = UUID.randomUUID();

        AuthUser admin = new AuthUser();
        admin.setId(adminId);
        admin.setRole(Role.ADMIN);
        admin.setEmail("admin@example.com");
        admin.setDisplayName("Admin");
        admin.setPasswordHash("hash");

        when(authUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(thriftStoreRepository.findByOwnerId(adminId)).thenReturn(List.of());

        mockMvc.perform(delete("/dashboard/users/{id}", adminId)
                        .header("Authorization", "Bearer admin-token")
                        .requestAttr("adminUser", admin)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(storeFeedbackRepository, times(1)).deleteByUserId(adminId);
        verify(authUserRepository, times(1)).delete(admin);
    }
}
