package com.edufelip.meer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CreateThriftStoreUseCaseTest {

  @Test
  void createsStoreWithNormalizedCategoriesAndSocial() {
    ThriftStoreRepository repo = Mockito.mock(ThriftStoreRepository.class);
    AuthUserRepository authUserRepository = Mockito.mock(AuthUserRepository.class);
    CreateThriftStoreUseCase useCase = new CreateThriftStoreUseCase(repo, authUserRepository);

    when(repo.save(Mockito.any(ThriftStore.class)))
        .thenAnswer(inv -> inv.getArgument(0, ThriftStore.class));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    CreateThriftStoreUseCase.Command command =
        new CreateThriftStoreUseCase.Command(
            "Name",
            "Desc",
            null,
            "123 Road",
            10.0,
            20.0,
            "555-1111",
            "test@example.com",
            "Neighborhood",
            false,
            List.of(" Vintage ", "vintage", "Kids"),
            new CreateThriftStoreUseCase.SocialInput(
                null, "insta", "https://example.com", "https://wa.me/123"));

    ThriftStore saved = useCase.execute(user, command);

    assertThat(saved.getCategories()).containsExactly("vintage", "kids");
    assertThat(saved.getSocial().getWebsite()).isEqualTo("https://example.com");
    assertThat(saved.getSocial().getWhatsapp()).isEqualTo("https://wa.me/123");
    assertThat(user.getOwnedThriftStore()).isEqualTo(saved);
    verify(authUserRepository).save(user);
  }

  @Test
  void createsStoreWithoutPhone() {
    ThriftStoreRepository repo = Mockito.mock(ThriftStoreRepository.class);
    AuthUserRepository authUserRepository = Mockito.mock(AuthUserRepository.class);
    CreateThriftStoreUseCase useCase = new CreateThriftStoreUseCase(repo, authUserRepository);

    when(repo.save(Mockito.any(ThriftStore.class)))
        .thenAnswer(inv -> inv.getArgument(0, ThriftStore.class));

    AuthUser user = new AuthUser();
    user.setId(UUID.randomUUID());

    CreateThriftStoreUseCase.Command command =
        new CreateThriftStoreUseCase.Command(
            "Name",
            "Desc",
            null,
            "123 Road",
            10.0,
            20.0,
            null,
            null,
            "Neighborhood",
            false,
            List.of("vintage"),
            null);

    ThriftStore saved = useCase.execute(user, command);

    assertThat(saved.getPhone()).isNull();
    verify(authUserRepository).save(user);
  }
}
