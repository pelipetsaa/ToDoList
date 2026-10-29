package com.pelypets.todolist.server.application

import com.pelypets.todolist.server.domain.EmailTakenException
import com.pelypets.todolist.server.domain.UnauthorizedException
import com.pelypets.todolist.server.domain.Validation
import com.pelypets.todolist.server.domain.model.RefreshTokenRecord
import com.pelypets.todolist.server.domain.model.TokenPair
import com.pelypets.todolist.server.domain.model.User
import com.pelypets.todolist.server.domain.port.AccessTokenIssuer
import com.pelypets.todolist.server.domain.port.GoogleTokenVerifier
import com.pelypets.todolist.server.domain.port.PasswordHasher
import com.pelypets.todolist.server.domain.port.RefreshTokenGenerator
import com.pelypets.todolist.server.domain.port.RefreshTokenRepository
import com.pelypets.todolist.server.domain.port.UserRepository
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration

class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwordHasher: PasswordHasher,
    private val accessTokenIssuer: AccessTokenIssuer,
    private val refreshTokenGenerator: RefreshTokenGenerator,
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val refreshTokenTtl: Duration,
    private val clock: Clock,
) {

    suspend fun register(email: String, password: String): TokenPair {
        val normalized = Validation.normalizeEmail(email)
        Validation.require(Validation.emailError(normalized))
        Validation.require(Validation.passwordError(password))
        if (users.findByEmail(normalized) != null) throw EmailTakenException()

        val user = users.create(
            User(
                id = UUID.randomUUID(),
                email = normalized,
                passwordHash = passwordHasher.hash(password),
                googleSubject = null,
                createdAt = clock.now(),
            ),
        )
        return issueTokens(user.id)
    }

    suspend fun login(email: String, password: String): TokenPair {
        val user = users.findByEmail(Validation.normalizeEmail(email)) ?: throw invalidCredentials()
        val hash = user.passwordHash ?: throw invalidCredentials()
        if (!passwordHasher.verify(password, hash)) throw invalidCredentials()
        return issueTokens(user.id)
    }

    suspend fun loginWithGoogle(idToken: String): TokenPair {
        val identity = googleTokenVerifier.verify(idToken)
            ?: throw UnauthorizedException("Invalid Google token")
        val email = Validation.normalizeEmail(identity.email)

        val user = users.findByGoogleSubject(identity.subject)
            ?: users.findByEmail(email)?.also { users.linkGoogleSubject(it.id, identity.subject) }
            ?: users.create(
                User(
                    id = UUID.randomUUID(),
                    email = email,
                    passwordHash = null,
                    googleSubject = identity.subject,
                    createdAt = clock.now(),
                ),
            )
        return issueTokens(user.id)
    }

    suspend fun refresh(refreshToken: String): TokenPair {
        val hash = refreshTokenGenerator.hash(refreshToken)
        val record = refreshTokens.findByHash(hash) ?: throw invalidRefreshToken()
        // Deleting first makes rotation single-use even under concurrent refresh attempts.
        if (!refreshTokens.deleteByHash(hash)) throw invalidRefreshToken()
        if (record.expiresAt <= clock.now()) throw invalidRefreshToken()
        return issueTokens(record.userId)
    }

    suspend fun logout(refreshToken: String) {
        refreshTokens.deleteByHash(refreshTokenGenerator.hash(refreshToken))
    }

    suspend fun currentUser(userId: UUID): User =
        users.findById(userId) ?: throw UnauthorizedException("User no longer exists")

    private suspend fun issueTokens(userId: UUID): TokenPair {
        val refreshToken = refreshTokenGenerator.generate()
        refreshTokens.save(
            RefreshTokenRecord(
                id = UUID.randomUUID(),
                userId = userId,
                tokenHash = refreshTokenGenerator.hash(refreshToken),
                expiresAt = clock.now() + refreshTokenTtl,
            ),
        )
        return TokenPair(
            accessToken = accessTokenIssuer.issue(userId),
            refreshToken = refreshToken,
            expiresInSeconds = accessTokenIssuer.ttlSeconds,
        )
    }

    private fun invalidCredentials() = UnauthorizedException("Invalid email or password")

    private fun invalidRefreshToken() = UnauthorizedException("Invalid or expired refresh token")
}
