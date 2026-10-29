package com.pelypets.todolist.server.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val google: GoogleConfig,
) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig = AppConfig(
            database = DatabaseConfig(
                url = config.property("database.url").getString(),
                user = config.property("database.user").getString(),
                password = config.property("database.password").getString(),
                maxPoolSize = config.propertyOrNull("database.maxPoolSize")?.getString()?.toInt() ?: 10,
            ),
            jwt = JwtConfig(
                secret = config.property("jwt.secret").getString(),
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
        )
    }
}

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = 10,
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val accessTokenTtlSeconds: Long,
    val refreshTokenTtlDays: Long,
)

data class GoogleConfig(
    val clientIds: List<String>,
)
