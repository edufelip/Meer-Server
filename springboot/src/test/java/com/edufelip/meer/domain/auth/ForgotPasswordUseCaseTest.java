package com.edufelip.meer.domain.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.PasswordResetProperties;
import com.edufelip.meer.service.PasswordResetTokenService;
import com.edufelip.meer.support.TestFixtures;
import org.junit.jupiter.api.Test;

class ForgotPasswordUseCaseTest {

  @Test
  void doesNotSendResetEmailWhenUserDoesNotExist() {
    AuthUserRepository repo = mock(AuthUserRepository.class);
    PasswordResetTokenService tokenService = mock(PasswordResetTokenService.class);
    PasswordResetNotifier notifier = mock(PasswordResetNotifier.class);
    PasswordResetProperties props = new PasswordResetProperties();

    when(repo.findByEmail(eq("missing@example.com"))).thenReturn(null);

    ForgotPasswordUseCase useCase = new ForgotPasswordUseCase(repo, tokenService, notifier, props);
    useCase.execute("missing@example.com");

    verifyNoInteractions(tokenService);
    verifyNoInteractions(notifier);
  }

  @Test
  void trimsEmailBeforeCheckingDb() {
    AuthUserRepository repo = mock(AuthUserRepository.class);
    PasswordResetTokenService tokenService = mock(PasswordResetTokenService.class);
    PasswordResetNotifier notifier = mock(PasswordResetNotifier.class);
    PasswordResetProperties props = new PasswordResetProperties();

    AuthUser user = TestFixtures.user("jane@example.com", "Jane");
    when(repo.findByEmail(eq("jane@example.com"))).thenReturn(user);

    ForgotPasswordUseCase useCase = new ForgotPasswordUseCase(repo, tokenService, notifier, props);
    useCase.execute("  jane@example.com  ");

    var order = inOrder(tokenService, notifier);
    order.verify(tokenService).createNewToken(eq(user), any(), anyLong());
    order.verify(notifier).sendResetLink(eq(user.getEmail()), anyString(), anyLong());
  }
}
