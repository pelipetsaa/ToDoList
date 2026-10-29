package com.pelypets.todolist.server

import com.pelypets.todolist.server.config.AppConfig
import com.pelypets.todolist.server.config.AppEnvironment
import com.pelypets.todolist.server.config.DEV_JWT_SECRET
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AppConfigTest {

    private val strongSecret = "a-production-secret-that-is-long-enough"

    private fun config(vararg values: Pair<String, String>): ApplicationConfig = MapApplicationConfig(
        "database.url" to "jdbc:postgresql://localhost/test",
        "database.user" to "user",
        "database.password" to "password",
        "jwt.issuer" to "issuer",
        "jwt.audience" to "audience",
        "jwt.realm" to "realm",
        "jwt.accessTokenTtlSeconds" to "900",
        "jwt.refreshTokenTtlDays" to "30",
        *values,
    )

    @Test
    fun `development falls back to the built-in key`() {
        val appConfig = AppConfig.from(config("app.environment" to "development"))

        assertEquals(AppEnvironment.DEVELOPMENT, appConfig.environment)
        assertEquals(DEV_JWT_SECRET, appConfig.jwt.secret)
        assertFalse(appConfig.trustProxy)
    }

    @Test
    fun `production requires a jwt secret`() {
        assertFailsWith<IllegalStateException> { AppConfig.from(config("app.environment" to "production")) }
    }

    @Test
    fun `production rejects the development key`() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig.from(config("app.environment" to "production", "jwt.secret" to DEV_JWT_SECRET))
        }
    }

    @Test
    fun `short secrets are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig.from(config("app.environment" to "production", "jwt.secret" to "too-short"))
        }
    }

    @Test
    fun `production accepts a strong secret and reads proxy and rate limit settings`() {
        val appConfig = AppConfig.from(
            config(
                "app.environment" to "PRODUCTION",
                "app.trustProxy" to "true",
                "jwt.secret" to strongSecret,
                "rateLimit.authRequestsPerMinute" to "5",
            ),
        )

        assertEquals(strongSecret, appConfig.jwt.secret)
        assertEquals(true, appConfig.trustProxy)
        assertEquals(5, appConfig.rateLimit.authRequestsPerMinute)
    }

    @Test
    fun `unknown environments are rejected`() {
        assertFailsWith<IllegalStateException> { AppConfig.from(config("app.environment" to "staging")) }
    }
}
