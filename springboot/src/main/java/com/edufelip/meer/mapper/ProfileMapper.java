package com.edufelip.meer.mapper;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.dto.AdminProfileDto;
import com.edufelip.meer.dto.ProfileDto;
import com.edufelip.meer.dto.ThriftStoreDto;

public class ProfileMapper {

  private ProfileMapper() {}

  public static ProfileDto toProfileDto(
      AuthUser user, boolean includeOwnedStore, String termsRequiredVersion, String termsUrl) {
    ThriftStoreDto owned = null;
    if (includeOwnedStore && user.getOwnedThriftStore() != null) {
      owned = Mappers.toDto(user.getOwnedThriftStore(), false);
    }
    return new ProfileDto(
        user.getId(),
        user.getDisplayName(),
        user.getEmail(),
        user.getTermsVersion(),
        user.getTermsAcceptedAt(),
        termsRequiredVersion,
        termsUrl,
        user.getPhotoUrl(),
        user.getBio(),
        (user.getRole() != null ? user.getRole() : Role.USER).name(),
        user.isNotifyNewStores(),
        user.isNotifyPromos(),
        owned,
        user.getCreatedAt());
  }

  public static AdminProfileDto toAdminProfileDto(AuthUser user, boolean includeOwnedStore) {
    ThriftStoreDto owned = null;
    if (includeOwnedStore && user.getOwnedThriftStore() != null) {
      owned = Mappers.toDto(user.getOwnedThriftStore(), false);
    }
    return new AdminProfileDto(
        user.getId(),
        user.getDisplayName(),
        user.getEmail(),
        user.getPhotoUrl(),
        user.getBio(),
        (user.getRole() != null ? user.getRole() : Role.USER).name(),
        user.isNotifyNewStores(),
        user.isNotifyPromos(),
        owned,
        user.getCreatedAt());
  }
}
