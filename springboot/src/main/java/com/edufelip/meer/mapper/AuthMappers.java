package com.edufelip.meer.mapper;

import com.edufelip.meer.domain.auth.AuthenticatedUser;
import com.edufelip.meer.dto.AuthDtos;

public class AuthMappers {
    public static AuthDtos.UserDto toDto(AuthenticatedUser user) {
        return new AuthDtos.UserDto(String.valueOf(user.getId()), user.getName(), user.getEmail());
    }
}
