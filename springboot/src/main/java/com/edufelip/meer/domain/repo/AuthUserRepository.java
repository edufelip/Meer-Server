package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.auth.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
    AuthUser findByEmail(String email);
}
