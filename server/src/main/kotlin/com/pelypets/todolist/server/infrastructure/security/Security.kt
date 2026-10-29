package com.pelypets.todolist.server.infrastructure.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.pelypets.todolist.server.config.JwtConfig
import com.pelypets.todolist.server.domain.model.GoogleIdentity
import com.pelypets.todolist.server.domain.port.AccessTokenIssuer
import com.pelypets.todolist.server.domain.port.GoogleTokenVerifier
import com.pelypets.todolist.server.domain.port.PasswordHasher
import com.pelypets.todolist.server.domain.port.RefreshTokenGenerator
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaInstant

class BcryptPasswordHasher(private val cost: Int = 12) : PasswordHasher {

    override suspend fun hash(raw: String): String = withContext(Dispatchers.Default) {
        BCrypt.withDefaults().hashToString(cost, raw.toCharArray())
    }

    override suspend fun verify(raw: String, hash: String): Boolean = withContext(Dispatchers.Default) {
        BCrypt.verifyer().verify(raw.toCharArray(), hash).verified
    }
}

class JwtAccessTokenIssuer(
    private val config: JwtConfig,
    private val clock: Clock,
) : AccessTokenIssuer {

    private val algorithm = Algorithm.HMAC256(config.secret)

    override val ttlSeconds: Long = config.accessTokenTtlSeconds

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    override fun issue(userId: UUID): String {
        val now = clock.now()
        return JWT.create()
            .withSubject(userId.toString())
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withIssuedAt(Date.from(now.toJavaInstant()))
            .withExpiresAt(Date.from((now + ttlSeconds.seconds).toJavaInstant()))
            .sign(algorithm)
    }
}

class SecureRefreshTokenGenerator : RefreshTokenGenerator {

    private val random = SecureRandom()

    override fun generate(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    override fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

class GoogleApiTokenVerifier(private val clientIds: List<String>) : GoogleTokenVerifier {

    private val logger = LoggerFactory.getLogger(GoogleApiTokenVerifier::class.java)

    private val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
        .setAudience(clientIds)
        .build()

    override suspend fun verify(idToken: String): GoogleIdentity? {
        if (clientIds.isEmpty()) {
            logger.warn("Google sign-in rejected: GOOGLE_CLIENT_IDS is not configured")
            return null
        }
        val token = withContext(Dispatchers.IO) {
            runCatching { verifier.verify(idToken) }
                .onFailure { logger.info("Google ID token verification failed: {}", it.message) }
                .getOrNull()
        } ?: return null

        val payload = token.payload
        val email = payload.email ?: return null
        if (payload.emailVerified != true) return null
        return GoogleIdentity(subject = payload.subject, email = email)
    }
}
