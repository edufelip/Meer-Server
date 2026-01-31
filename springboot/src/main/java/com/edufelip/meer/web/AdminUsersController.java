package com.edufelip.meer.web;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.auth.Role;
import com.edufelip.meer.core.push.PushToken;
import com.edufelip.meer.domain.auth.DeleteUserUseCase;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.PushTokenRepository;
import com.edufelip.meer.dto.AdminProfileDto;
import com.edufelip.meer.dto.AdminUserDto;
import com.edufelip.meer.dto.PageResponse;
import com.edufelip.meer.dto.PushTokenDto;
import com.edufelip.meer.mapper.ProfileMapper;
import com.edufelip.meer.security.DashboardAdminAuthorizer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/dashboard/users")
public class AdminUsersController {

  private static final char LIKE_ESCAPE = '!';

  private final DashboardAdminAuthorizer adminAuthorizer;
  private final AuthUserRepository authUserRepository;
  private final PushTokenRepository pushTokenRepository;
  private final DeleteUserUseCase deleteUserUseCase;

  public AdminUsersController(
      DashboardAdminAuthorizer adminAuthorizer,
      AuthUserRepository authUserRepository,
      PushTokenRepository pushTokenRepository,
      DeleteUserUseCase deleteUserUseCase) {
    this.adminAuthorizer = adminAuthorizer;
    this.authUserRepository = authUserRepository;
    this.pushTokenRepository = pushTokenRepository;
    this.deleteUserUseCase = deleteUserUseCase;
  }

  @GetMapping
  public PageResponse<AdminUserDto> listUsers(
      @RequestHeader("Authorization") String authHeader,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "newest") String sort) {
    adminAuthorizer.requireAdmin(authHeader);
    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination params");
    }
    Sort s =
        "oldest".equalsIgnoreCase(sort)
            ? Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "createdAt")
            : Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
    Pageable pageable = PageRequest.of(page, pageSize, s);
    String term = (q != null && !q.isBlank()) ? q.trim() : null;
    var pageRes =
        (term != null)
            ? authUserRepository.searchNonAdminUsers(escapeLikeTerm(term), Role.ADMIN, pageable)
            : authUserRepository.findNonAdminUsers(Role.ADMIN, pageable);

    List<UUID> userIds = pageRes.getContent().stream().map(AuthUser::getId).toList();
    Map<UUID, List<PushToken>> tokensByUser =
        userIds.isEmpty()
            ? Map.of()
            : pushTokenRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(PushToken::getUserId));

    List<AdminUserDto> items =
        pageRes.getContent().stream()
            .map(
                u -> {
                  List<PushTokenDto> tokens =
                      tokensByUser.getOrDefault(u.getId(), List.of()).stream()
                          .map(
                              t ->
                                  new PushTokenDto(
                                      t.getId().toString(),
                                      t.getDeviceId(),
                                      t.getPlatform() != null ? t.getPlatform().name() : null,
                                      t.getEnvironment() != null ? t.getEnvironment().name() : null,
                                      t.getAppVersion(),
                                      t.getLastSeenAt(),
                                      t.getCreatedAt()))
                          .toList();
                  return new AdminUserDto(
                      u.getId().toString(),
                      u.getDisplayName(),
                      u.getEmail(),
                      (u.getRole() != null ? u.getRole() : Role.USER).name(),
                      u.getCreatedAt(),
                      u.getPhotoUrl(),
                      tokens);
                })
            .toList();
    return new PageResponse<>(items, page, pageRes.hasNext());
  }

  @GetMapping("/{id}")
  public AdminProfileDto getUser(
      @RequestHeader("Authorization") String authHeader, @PathVariable UUID id) {
    adminAuthorizer.requireAdmin(authHeader);
    var user =
        authUserRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    return ProfileMapper.toAdminProfileDto(user, true);
  }

  @DeleteMapping("/{id}")
  public org.springframework.http.ResponseEntity<Void> deleteUser(
      @RequestHeader("Authorization") String authHeader, @PathVariable UUID id) {
    AuthUser admin = adminAuthorizer.requireAdmin(authHeader);
    var target =
        authUserRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    // Clear owned thrift store links before deleting stores to satisfy FK constraints
    if (target.getOwnedThriftStore() != null) {
      target.setOwnedThriftStore(null);
      authUserRepository.save(target);
    }

    deleteUserUseCase.execute(target, "ADMIN_DELETE");
    return org.springframework.http.ResponseEntity.noContent().build();
  }

  private String escapeLikeTerm(String term) {
    return term.replace(String.valueOf(LIKE_ESCAPE), String.valueOf(LIKE_ESCAPE) + LIKE_ESCAPE)
        .replace("%", LIKE_ESCAPE + "%")
        .replace("_", LIKE_ESCAPE + "_");
  }
}
