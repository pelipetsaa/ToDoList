package com.pelypets.todolist.server

import com.pelypets.todolist.core.api.ApiRoutes
import com.pelypets.todolist.core.dto.EmailCredentialsDto
import com.pelypets.todolist.core.dto.ErrorCodes
import com.pelypets.todolist.core.dto.GoogleLoginDto
import com.pelypets.todolist.core.dto.HealthDto
import com.pelypets.todolist.core.dto.RefreshTokenDto
import com.pelypets.todolist.core.dto.TokenPairDto
import com.pelypets.todolist.core.dto.UserDto
import com.pelypets.todolist.server.config.RateLimitConfig
import com.pelypets.todolist.server.domain.model.GoogleIdentity
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthApiTest {

    @BeforeTest
    fun cleanDatabase() = TestDatabase.truncateAll()

    @Test
    fun `health endpoint is public`() = apiTest {
        val response = client.get(ApiRoutes.HEALTH)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("UP", response.body<HealthDto>().status)
    }

    @Test
    fun `register returns tokens and me returns the normalized email`() = apiTest {
        val tokens = register(email = "  New.User@Example.COM ")

        assertTrue(tokens.accessToken.isNotBlank())
        assertTrue(tokens.refreshToken.isNotBlank())
        assertEquals(900, tokens.expiresInSeconds)

        val me = client.get(ApiRoutes.ME) { bearerAuth(tokens.accessToken) }
        assertEquals(HttpStatusCode.OK, me.status)
        assertEquals("new.user@example.com", me.body<UserDto>().email)
    }

    @Test
    fun `register with an existing email returns 409`() = apiTest {
        register(email = "dup@example.com")

        client.post(ApiRoutes.AUTH_REGISTER) { json(EmailCredentialsDto("DUP@example.com", "password123")) }
            .assertError(HttpStatusCode.Conflict, ErrorCodes.EMAIL_TAKEN)
    }

    @Test
    fun `register validates email and password`() = apiTest {
        client.post(ApiRoutes.AUTH_REGISTER) { json(EmailCredentialsDto("not-an-email", "password123")) }
            .assertError(HttpStatusCode.BadRequest, ErrorCodes.VALIDATION_ERROR)
        client.post(ApiRoutes.AUTH_REGISTER) { json(EmailCredentialsDto("ok@example.com", "short")) }
            .assertError(HttpStatusCode.BadRequest, ErrorCodes.VALIDATION_ERROR)
    }

    @Test
    fun `malformed json returns 400`() = apiTest {
        client.post(ApiRoutes.AUTH_LOGIN) { json(mapOf("email" to "x")) }
            .assertError(HttpStatusCode.BadRequest, ErrorCodes.VALIDATION_ERROR)
    }

    @Test
    fun `login succeeds with the right password and fails otherwise`() = apiTest {
        register(email = "login@example.com", password = "password123")

        val ok = client.post(ApiRoutes.AUTH_LOGIN) { json(EmailCredentialsDto("login@example.com", "password123")) }
        assertEquals(HttpStatusCode.OK, ok.status)

        client.post(ApiRoutes.AUTH_LOGIN) { json(EmailCredentialsDto("login@example.com", "wrong-password")) }
            .assertError(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED)
        client.post(ApiRoutes.AUTH_LOGIN) { json(EmailCredentialsDto("nobody@example.com", "password123")) }
            .assertError(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED)
    }

    @Test
    fun `protected endpoints require a valid access token`() = apiTest {
        client.get(ApiRoutes.ME).assertError(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED)
        client.get(ApiRoutes.ME) { bearerAuth("garbage") }
            .assertError(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED)
    }

    @Test
    fun `refresh rotates the refresh token and the old one stops working`() = apiTest {
        val initial = register()

        val refreshed = client.post(ApiRoutes.AUTH_REFRESH) { json(RefreshTokenDto(initial.refreshToken)) }
        assertEquals(HttpStatusCode.OK, refreshed.status)
        val pair = refreshed.body<TokenPairDto>()
        assertNotEquals(initial.refreshToken, pair.refreshToken)

        assertEquals(HttpStatusCode.OK, client.get(ApiRoutes.ME) { bearerAuth(pair.accessToken) }.status)

        client.post(ApiRoutes.AUTH_REFRESH) { json(RefreshTokenDto(initial.refreshToken)) }
            .assertError(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED)
    }

    @Test
    fun `logout revokes the refresh token`() = apiTest {
        val tokens = register()

        val logout = client.post(ApiRoutes.AUTH_LOGOUT) { json(RefreshTokenDto(tokens.refreshToken)) }
        assertEquals(HttpStatusCode.NoContent, logout.status)

        client.post(ApiRoutes.AUTH_REFRESH) { json(RefreshTokenDto(tokens.refreshToken)) }
            .assertError(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED)
    }

    @Test
    fun `google sign in creates an account and links an existing email`() {
        val google = FakeGoogleTokenVerifier().apply {
            identities["new-user-token"] = GoogleIdentity(subject = "g-1", email = "google@example.com")
            identities["existing-user-token"] = GoogleIdentity(subject = "g-2", email = "Existing@example.com")
        }
        apiTest(google) {
            val created = client.post(ApiRoutes.AUTH_GOOGLE) { json(GoogleLoginDto("new-user-token")) }
            assertEquals(HttpStatusCode.OK, created.status)
            val createdMe = client.get(ApiRoutes.ME) { bearerAuth(created.body<TokenPairDto>().accessToken) }
            assertEquals("google@example.com", createdMe.body<UserDto>().email)

            val existing = register(email = "existing@example.com")
            val existingId = client.get(ApiRoutes.ME) { bearerAuth(existing.accessToken) }.body<UserDto>().id

            val linked = client.post(ApiRoutes.AUTH_GOOGLE) { json(GoogleLoginDto("existing-user-token")) }
            val linkedId = client.get(ApiRoutes.ME) { bearerAuth(linked.body<TokenPairDto>().accessToken) }
                .body<UserDto>().id
            assertEquals(existingId, linkedId)

            val again = client.post(ApiRoutes.AUTH_GOOGLE) { json(GoogleLoginDto("existing-user-token")) }
            val againId = client.get(ApiRoutes.ME) { bearerAuth(again.body<TokenPairDto>().accessToken) }
                .body<UserDto>().id
            assertEquals(existingId, againId)
        }
    }

    @Test
    fun `google sign in rejects invalid tokens`() = apiTest {
        client.post(ApiRoutes.AUTH_GOOGLE) { json(GoogleLoginDto("unknown")) }
            .assertError(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED)
        client.post(ApiRoutes.AUTH_GOOGLE) { json(GoogleLoginDto(" ")) }
            .assertError(HttpStatusCode.BadRequest, ErrorCodes.VALIDATION_ERROR)
    }

    @Test
    fun `auth endpoints are rate limited while the rest of the api is not`() =
        apiTest(config = testAppConfig(rateLimit = RateLimitConfig(authRequestsPerMinute = 3))) {
            val token = register().accessToken
            repeat(2) {
                client.post(ApiRoutes.AUTH_LOGIN) { json(EmailCredentialsDto("user@example.com", "password123")) }
                    .also { assertEquals(HttpStatusCode.OK, it.status) }
            }

            client.post(ApiRoutes.AUTH_LOGIN) { json(EmailCredentialsDto("user@example.com", "password123")) }
                .assertError(HttpStatusCode.TooManyRequests, ErrorCodes.TOO_MANY_REQUESTS)

            assertEquals(HttpStatusCode.OK, client.get(ApiRoutes.ME) { bearerAuth(token) }.status)
        }
}
