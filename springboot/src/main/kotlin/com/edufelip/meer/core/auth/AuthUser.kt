package com.edufelip.meer.core.auth

import com.edufelip.meer.core.AuthUserId
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class AuthUser(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: AuthUserId = 0,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)
