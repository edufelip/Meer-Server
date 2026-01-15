package com.edufelip.meer.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.auth.DeleteUserUseCase;
import com.edufelip.meer.domain.auth.GetProfileUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.AuthUserResolver;
import com.edufelip.meer.security.token.TokenProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, AuthUserResolver.class, TestClockConfig.class})
class AccountControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetProfileUseCase getProfileUseCase;
  @MockitoBean private DeleteUserUseCase deleteUserUseCase;
  @MockitoBean private TokenProvider tokenProvider;
  @MockitoBean private AuthUserRepository authUserRepository;

  @Test
  void deleteAccountDeletesUser() throws Exception {
    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");

    when(getProfileUseCase.execute("token")).thenReturn(user);

    mockMvc
        .perform(
            delete("/account")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}"))
        .andExpect(status().isNoContent());

    verify(deleteUserUseCase).execute(user, "ACCOUNT_DELETE");
  }

  @Test
  void deleteAccountRejectsEmailMismatch() throws Exception {
    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());
    user.setEmail("user@example.com");
    user.setDisplayName("User");
    user.setPasswordHash("hash");

    when(getProfileUseCase.execute("token")).thenReturn(user);

    mockMvc
        .perform(
            delete("/account")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"other@example.com\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(deleteUserUseCase);
  }
}
