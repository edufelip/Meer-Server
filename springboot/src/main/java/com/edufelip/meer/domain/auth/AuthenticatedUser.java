package com.edufelip.meer.domain.auth;

import com.edufelip.meer.core.auth.Role;
import java.util.UUID;

public class AuthenticatedUser {
  private final UUID id;
  private final String name;
  private final String email;
  private final Role role;

  public AuthenticatedUser(UUID id, String name, String email, Role role) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.role = role;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public Role getRole() {
    return role;
  }
}
