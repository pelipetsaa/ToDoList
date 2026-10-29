package com.pelypets.todolist.data

import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.local.TokenStorage
import com.pelypets.todolist.data.remote.ApiConfig
import com.pelypets.todolist.data.remote.HttpClientFactory
import com.pelypets.todolist.data.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

class InMemoryTokenStorage(initial: StoredTokens? = null) : TokenStorage {
    var tokens: StoredTokens? = initial
        private set

    override fun read(): StoredTokens? = tokens
    override fun save(tokens: StoredTokens) {
        this.tokens = tokens
    }
    override fun clear() {
        tokens = null
    }
}

class TestEnvironment(initialTokens: StoredTokens? = null, handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
    val requests = mutableListOf<HttpRequestData>()
    val storage = InMemoryTokenStorage(initialTokens)
    val session = SessionManager(storage)
    val engine = MockEngine { request ->
        requests += request
        handler(request)
    }
    val client = HttpClientFactory.create(engine, ApiConfig(baseUrl = "http://test.local"), session)
}

fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

fun MockRequestHandleScope.respondUnauthorized() = respond(
    """{"code":"UNAUTHORIZED","message":"expired"}""",
    HttpStatusCode.Unauthorized,
    headersOf(
        HttpHeaders.ContentType to listOf("application/json"),
        HttpHeaders.WWWAuthenticate to listOf("Bearer realm=\"todolist\""),
    ),
)

const val TODO_JSON = """
{"id":"t1","title":"Buy milk","description":"","dueAt":"2026-10-29T10:00:00Z","status":"IN_PROGRESS",
 "createdAt":"2026-10-28T10:00:00Z","updatedAt":"2026-10-28T10:00:00Z"}
"""

const val TOKENS_JSON = """{"accessToken":"new-access","refreshToken":"new-refresh","expiresInSeconds":900}"""
