package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.dto.ProfileDto;
import com.edufelip.meer.mapper.ProfileMapper;
import com.edufelip.meer.service.TermsPolicy;
import org.springframework.stereotype.Component;

@Component
public class ProfileAssembler {
  private final TermsPolicy termsPolicy;

  public ProfileAssembler(TermsPolicy termsPolicy) {
    this.termsPolicy = termsPolicy;
  }

  public ProfileDto toProfileDto(AuthUser user, boolean includeOwnedStore) {
    return ProfileMapper.toProfileDto(
        user, includeOwnedStore, termsPolicy.requiredVersionOrNull(), termsPolicy.urlOrNull());
  }
}
