package com.pelypets.todolist.core.dto

import kotlinx.serialization.Serializable

@Serializable
data class EmailCredentialsDto(
    val email: String,
    val password: String,
)

@Serializable
data class GoogleLoginDto(
    val idToken: String,
)

@Serializable
data class RefreshTokenDto(
    val refreshToken: String,
)

@Serializable
data class TokenPairDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
)
