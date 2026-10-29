package com.pelypets.todolist.server

import com.pelypets.todolist.core.api.ApiRoutes
import com.pelypets.todolist.core.dto.EmailCredentialsDto
import com.pelypets.todolist.core.dto.ErrorResponse
import com.pelypets.todolist.core.dto.TodoDto
import com.pelypets.todolist.core.dto.TodoInputDto
import com.pelypets.todolist.core.dto.TodoStatusDto
import com.pelypets.todolist.core.dto.TokenPairDto
import com.pelypets.todolist.server.api.plugins.AppJson
import com.pelypets.todolist.server.config.AppConfig
import com.pelypets.todolist.server.config.AppEnvironment
import com.pelypets.todolist.server.config.DatabaseConfig
import com.pelypets.todolist.server.config.GoogleConfig
import com.pelypets.todolist.server.config.JwtConfig
import com.pelypets.todolist.server.config.RateLimitConfig
import com.pelypets.todolist.server.domain.model.GoogleIdentity
import com.pelypets.todolist.server.domain.port.GoogleTokenVerifier
import com.pelypets.todolist.server.infrastructure.db.AppDatabase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.time.Instant

object TestDatabase {
    private val container: PostgreSQLContainer by lazy {
        PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine")).apply { start() }
    }

    val config: DatabaseConfig by lazy {
        DatabaseConfig(container.jdbcUrl, container.username, container.password, maxPoolSize = 4)
            .also { AppDatabase.connect(it).close() }
    }

    fun truncateAll() {
        val config = config
        DriverManager.getConnection(config.url, config.user, config.password).use { connection ->
            connection.createStatement().use { it.execute("TRUNCATE users CASCADE") }
        }
    }
}

class FakeGoogleTokenVerifier : GoogleTokenVerifier {
    val identities = mutableMapOf<String, GoogleIdentity>()

    override suspend fun verify(idToken: String): GoogleIdentity? = identities[idToken]
}

fun testAppConfig(rateLimit: RateLimitConfig = RateLimitConfig(authRequestsPerMinute = 1_000)) = AppConfig(
    environment = AppEnvironment.DEVELOPMENT,
    database = TestDatabase.config,
    jwt = JwtConfig(
        secret = "test-secret-test-secret-test-secret",
        issuer = "test-issuer",
        audience = "test-audience",
        realm = "test",
        accessTokenTtlSeconds = 900,
        refreshTokenTtlDays = 30,
    ),
    google = GoogleConfig(clientIds = emptyList()),
    rateLimit = rateLimit,
    trustProxy = false,
)

fun apiTest(
    google: FakeGoogleTokenVerifier = FakeGoogleTokenVerifier(),
    config: AppConfig = testAppConfig(),
    block: suspend ApiTestScope.() -> Unit,
) = testApplication {
    environment { this.config = MapApplicationConfig() }
    application {
        configureApp(config, listOf(module { single<GoogleTokenVerifier> { google } }))
    }
    val client = createClient { install(ContentNegotiation) { json(AppJson) } }
    ApiTestScope(client).block()
}

class ApiTestScope(val client: HttpClient) {

    suspend fun register(email: String = "user@example.com", password: String = "password123"): TokenPairDto {
        val response = client.post(ApiRoutes.AUTH_REGISTER) { json(EmailCredentialsDto(email, password)) }
        assertEquals(HttpStatusCode.Created, response.status)
        return response.body()
    }

    suspend fun createTodo(
        token: String,
        title: String,
        dueAt: String,
        status: TodoStatusDto = TodoStatusDto.TODO,
    ): TodoDto {
        val response = client.post(ApiRoutes.TODOS) {
            bearerAuth(token)
            json(TodoInputDto(title = title, description = "", dueAt = Instant.parse(dueAt), status = status))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return response.body()
    }

    suspend fun HttpResponse.assertError(status: HttpStatusCode, code: String) {
        assertEquals(status, this.status)
        assertEquals(code, body<ErrorResponse>().code)
    }
}

fun HttpRequestBuilder.json(body: Any) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
