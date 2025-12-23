package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.dto.StoreRatingDto;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoreRatingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class StoreRatingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private TokenProvider tokenProvider;
    @MockitoBean private AuthUserRepository authUserRepository;
    @MockitoBean private ThriftStoreRepository thriftStoreRepository;
    @MockitoBean private StoreFeedbackRepository storeFeedbackRepository;

    @Test
    void listReturnsPageWithRatings() throws Exception {
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
        when(thriftStoreRepository.findById(storeId)).thenReturn(Optional.of(new com.edufelip.meer.core.store.ThriftStore()));

        Instant createdAt = Instant.parse("2024-01-02T10:15:30Z");
        var dto = new StoreRatingDto(1, storeId, 5, "Great place", "Ana", "https://img", createdAt);
        var slice = new SliceImpl<>(List.of(dto), PageRequest.of(0, 10), true);
        when(storeFeedbackRepository.findRatingsByStoreId(eq(storeId), any())).thenReturn(slice);

        mockMvc.perform(get("/stores/{storeId}/ratings", storeId)
                        .header("Authorization", "Bearer token")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.items[0].score").value(5))
                .andExpect(jsonPath("$.items[0].body").value("Great place"))
                .andExpect(jsonPath("$.items[0].authorName").value("Ana"))
                .andExpect(jsonPath("$.items[0].authorAvatarUrl").value("https://img"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2024-01-02T10:15:30Z"));
    }

    @Test
    void listReturnsNotFoundWhenStoreMissing() throws Exception {
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

        mockMvc.perform(get("/stores/{storeId}/ratings", storeId)
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verifyNoInteractions(storeFeedbackRepository);
    }
}
