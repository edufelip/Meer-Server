package com.edufelip.meer.domain.auth;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.token.InvalidTokenException;
import com.edufelip.meer.security.token.TokenPayload;
import com.edufelip.meer.security.token.TokenProvider;
import org.springframework.util.StringUtils;

public class UpdateProfileUseCase {
  private final TokenProvider tokenProvider;
  private final AuthUserRepository authUserRepository;

  public record Command(
      String name,
      String avatarUrl,
      String bio,
      Boolean notifyNewStores,
      Boolean notifyPromos) {}

  public UpdateProfileUseCase(TokenProvider tokenProvider, AuthUserRepository authUserRepository) {
    this.tokenProvider = tokenProvider;
    this.authUserRepository = authUserRepository;
  }

  public AuthUser execute(String accessToken, Command command) {
    TokenPayload payload;
    try {
      payload = tokenProvider.parseAccessToken(accessToken);
    } catch (RuntimeException ex) {
      throw new InvalidTokenException();
    }

    AuthUser user =
        authUserRepository.findById(payload.getUserId()).orElseThrow(InvalidTokenException::new);

    if (StringUtils.hasText(command.name())) user.setDisplayName(command.name());
    if (command.avatarUrl() != null) user.setPhotoUrl(command.avatarUrl());
    if (command.bio() != null) user.setBio(command.bio());
    if (command.notifyNewStores() != null) user.setNotifyNewStores(command.notifyNewStores());
    if (command.notifyPromos() != null) user.setNotifyPromos(command.notifyPromos());

    return authUserRepository.save(user);
  }
}
