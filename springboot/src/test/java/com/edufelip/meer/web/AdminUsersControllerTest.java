package com.edufelip.meer.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.auth.DeleteUserUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.security.DashboardAdminAuthorizer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUsersController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({DashboardAdminAuthorizer.class, TestClockConfig.class})
class AdminUsersControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthUserRepository authUserRepository;
  @MockitoBean private PushTokenRepository pushTokenRepository;
  @MockitoBean private DeleteUserUseCase deleteUserUseCase;

  @Test
  void deleteUserInvokesUseCase() throws Exception {
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
    target.setOwnedThriftStore(store);

    when(authUserRepository.findById(targetId)).thenReturn(Optional.of(target));
    mockMvc
        .perform(
            delete("/dashboard/users/{id}", targetId)
                .header("Authorization", "Bearer admin-token")
                .requestAttr("adminUser", admin)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(authUserRepository, times(1)).save(target);
    verify(deleteUserUseCase, times(1)).execute(target, "ADMIN_DELETE");
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

    mockMvc
        .perform(
            delete("/dashboard/users/{id}", adminId)
                .header("Authorization", "Bearer admin-token")
                .requestAttr("adminUser", admin)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(deleteUserUseCase, times(1)).execute(admin, "ADMIN_DELETE");
  }

  @Test
  void listUsersExcludesAdminsWhenNoSearch() throws Exception {
    AuthUser admin = new AuthUser();
    admin.setId(UUID.randomUUID());
    admin.setRole(Role.ADMIN);
    admin.setEmail("admin@example.com");

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");
    user.setRole(null);

    when(authUserRepository.findNonAdminUsers(eq(Role.ADMIN), any()))
        .thenReturn(new PageImpl<>(List.of(user)));
    when(pushTokenRepository.findByUserIdIn(any())).thenReturn(List.of());

    mockMvc
        .perform(
            get("/dashboard/users")
                .header("Authorization", "Bearer admin-token")
                .requestAttr("adminUser", admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(user.getId().toString()));

    verify(authUserRepository, times(1)).findNonAdminUsers(eq(Role.ADMIN), any());
    verify(authUserRepository, never()).searchNonAdminUsers(any(), any(), any());
  }

  @Test
  void listUsersEscapesSearchTerm() throws Exception {
    AuthUser admin = new AuthUser();
    admin.setId(UUID.randomUUID());
    admin.setRole(Role.ADMIN);
    admin.setEmail("admin@example.com");

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setEmail("jane@example.com");
    user.setDisplayName("Jane");
    user.setPasswordHash("hash");
    user.setRole(Role.USER);

    when(authUserRepository.searchNonAdminUsers(any(), eq(Role.ADMIN), any()))
        .thenReturn(new PageImpl<>(List.of(user)));
    when(pushTokenRepository.findByUserIdIn(any())).thenReturn(List.of());

    mockMvc
        .perform(
            get("/dashboard/users")
                .header("Authorization", "Bearer admin-token")
                .requestAttr("adminUser", admin)
                .param("q", "  ja%ne_!  "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(user.getId().toString()));

    ArgumentCaptor<String> termCaptor = ArgumentCaptor.forClass(String.class);
    verify(authUserRepository, times(1))
        .searchNonAdminUsers(termCaptor.capture(), eq(Role.ADMIN), any());
    Assertions.assertEquals("ja!%ne!_!!", termCaptor.getValue());
    verify(authUserRepository, never()).findNonAdminUsers(any(), any());
  }
}
