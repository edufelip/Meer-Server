package com.edufelip.meer.core.store

import jakarta.persistence.Embeddable

@Embeddable
data class Social(
    val facebook: String? = null,
    val instagram: String? = null,
    val website: String? = null,
    val whatsapp: String? = null
)
