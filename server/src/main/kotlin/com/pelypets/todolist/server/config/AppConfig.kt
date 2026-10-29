package com.pelypets.todolist.server.config

import io.ktor.server.config.ApplicationConfig

/** Signing key used only when no JWT_SECRET is set during local development. */
internal const val DEV_JWT_SECRET = "dev-only-secret-change-me-please-32b"

/** HS256 needs a key of at least 256 bits. */
internal const val MIN_JWT_SECRET_LENGTH = 32

enum class AppEnvironment {
    DEVELOPMENT,
    PRODUCTION,
    ;

    companion object {
        fun parse(value: String?): AppEnvironment = entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
            ?: error("APP_ENV must be 'development' or 'production', got '$value'")
    }
}

data class AppConfig(
    val environment: AppEnvironment,
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val google: GoogleConfig,
    val rateLimit: RateLimitConfig,
    /** Trust X-Forwarded-* headers; enable only behind a reverse proxy that sets them. */
    val trustProxy: Boolean,
    /** Bearer token for /metrics; without it production does not expose metrics. */
    val metricsToken: String? = null,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig {
            val environment = AppEnvironment.parse(config.propertyOrNull("app.environment")?.getString())
            return AppConfig(
                environment = environment,
                database = DatabaseConfig(
                    url = config.property("database.url").getString(),
                    user = config.property("database.user").getString(),
                    password = config.property("database.password").getString(),
                    maxPoolSize = config.propertyOrNull("database.maxPoolSize")?.getString()?.toInt() ?: 10,
                ),
                jwt = JwtConfig(
                    secret = resolveJwtSecret(config.propertyOrNull("jwt.secret")?.getString(), environment),
                    issuer = config.property("jwt.issuer").getString(),
                    audience = config.property("jwt.audience").getString(),
                    realm = config.property("jwt.realm").getString(),
                    accessTokenTtlSeconds = config.property("jwt.accessTokenTtlSeconds").getString().toLong(),
                    refreshTokenTtlDays = config.property("jwt.refreshTokenTtlDays").getString().toLong(),
                ),
                google = GoogleConfig(
                    clientIds = config.propertyOrNull("google.clientIds")?.getString()
                        ?.split(',')
                        ?.map(String::trim)
                        ?.filter(String::isNotEmpty)
                        .orEmpty(),
                ),
                rateLimit = RateLimitConfig(
                    authRequestsPerMinute = config.propertyOrNull("rateLimit.authRequestsPerMinute")
                        ?.getString()?.toInt() ?: RateLimitConfig.DEFAULT_AUTH_REQUESTS_PER_MINUTE,
                ),
                trustProxy = config.propertyOrNull("app.trustProxy")?.getString()?.toBooleanStrict() ?: false,
                metricsToken = config.propertyOrNull("metrics.token")?.getString()?.takeIf(String::isNotBlank),
            )
        }
    }
}

/**
 * Production must never fall back to the development key: anyone who has read this repository could sign tokens.
 */
internal fun resolveJwtSecret(value: String?, environment: AppEnvironment): String {
    if (value.isNullOrBlank()) {
        check(environment == AppEnvironment.DEVELOPMENT) { "JWT_SECRET must be set when APP_ENV=production" }
        return DEV_JWT_SECRET
    }
    require(value.length >= MIN_JWT_SECRET_LENGTH) { "JWT_SECRET must be at least $MIN_JWT_SECRET_LENGTH characters" }
    require(environment == AppEnvironment.DEVELOPMENT || value != DEV_JWT_SECRET) {
        "JWT_SECRET must not be the development key when APP_ENV=production"
    }
    return value
}

data class DatabaseConfig(val url: String, val user: String, val password: String, val maxPoolSize: Int = 10)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val accessTokenTtlSeconds: Long,
    val refreshTokenTtlDays: Long,
)

data class GoogleConfig(val clientIds: List<String>)

data class RateLimitConfig(
    /** Requests per client IP per minute across all /auth endpoints. */
    val authRequestsPerMinute: Int = DEFAULT_AUTH_REQUESTS_PER_MINUTE,
) {
    companion object {
        const val DEFAULT_AUTH_REQUESTS_PER_MINUTE = 20
    }
}
