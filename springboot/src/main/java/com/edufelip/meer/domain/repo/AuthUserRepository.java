package com.edufelip.meer.domain.repo;

import com.edufelip.meer.core.auth.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRepository extends JpaRepository<AuthUser, Integer> {
    AuthUser findByEmail(String email);
}
