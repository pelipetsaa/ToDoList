package com.pelypets.todolist.data

import com.pelypets.todolist.core.api.ApiRoutes
import com.pelypets.todolist.data.local.StoredTokens
import com.pelypets.todolist.data.remote.AuthApi
import com.pelypets.todolist.data.remote.TodoApi
import com.pelypets.todolist.data.repository.AuthRepositoryImpl
import com.pelypets.todolist.data.repository.TodoRepositoryImpl
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.model.TimeRange
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoStatus
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

class RepositoriesTest {

    private val loggedIn = StoredTokens("old-access", "old-refresh")
    private val range = TimeRange(Instant.parse("2026-10-29T00:00:00Z"), Instant.parse("2026-10-30T00:00:00Z"))

    @Test
    fun getTodosSendsTheRangeAndBearerTokenAndMapsTheResponse() = runTest {
        val env = TestEnvironment(loggedIn) { respondJson("[$TODO_JSON]") }
        val repository = TodoRepositoryImpl(TodoApi(env.client))

        val result = repository.getTodos(range)

        val todos = assertIs<DomainResult.Success<List<Todo>>>(result).value
        assertEquals(
            listOf(Todo("t1", "Buy milk", "", Instant.parse("2026-10-29T10:00:00Z"), TodoStatus.IN_PROGRESS)),
            todos,
        )
        val request = env.requests.single()
        assertEquals(ApiRoutes.TODOS, request.url.encodedPath)
        assertEquals("2026-10-29T00:00:00Z", request.url.parameters[ApiRoutes.QUERY_FROM])
        assertEquals("2026-10-30T00:00:00Z", request.url.parameters[ApiRoutes.QUERY_TO])
        assertEquals("Bearer old-access", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun httpErrorsAreMappedToDomainErrors() = runTest {
        val cases = mapOf(
            HttpStatusCode.NotFound to DomainError.NotFound,
            HttpStatusCode.Conflict to DomainError.Conflict,
            HttpStatusCode.BadRequest to DomainError.Validation(serverMessage = "bad"),
            HttpStatusCode.InternalServerError to DomainError.Unknown("bad"),
        )
        for ((status, expected) in cases) {
            val env = TestEnvironment(loggedIn) { respondJson("""{"code":"X","message":"bad"}""", status) }
            val result = TodoRepositoryImpl(TodoApi(env.client)).getTodo("t1")
            assertEquals(expected, assertIs<DomainResult.Failure>(result).error, "for $status")
        }
    }

    @Test
    fun networkFailuresAreMappedToNetworkError() = runTest {
        val env = TestEnvironment(loggedIn) { throw IOException("offline") }
        val result = TodoRepositoryImpl(TodoApi(env.client)).getTodo("t1")
        assertEquals(DomainError.Network, assertIs<DomainResult.Failure>(result).error)
    }

    @Test
    fun expiredAccessTokenIsRefreshedAndTheRequestRetried() = runTest {
        val env = TestEnvironment(loggedIn) { request ->
            when {
                request.url.encodedPath == ApiRoutes.AUTH_REFRESH -> respondJson(TOKENS_JSON)
                request.headers[HttpHeaders.Authorization] == "Bearer old-access" -> respondUnauthorized()
                else -> respondJson("[]")
            }
        }

        val result = TodoRepositoryImpl(TodoApi(env.client)).getTodos(range)

        assertIs<DomainResult.Success<List<Todo>>>(result)
        assertEquals(StoredTokens("new-access", "new-refresh"), env.storage.tokens)
        assertEquals("Bearer new-access", env.requests.last().headers[HttpHeaders.Authorization])
        assertEquals(SessionState.LOGGED_IN, env.session.session.value)
    }

    @Test
    fun failedRefreshEndsTheSession() = runTest {
        val env = TestEnvironment(loggedIn) { respondUnauthorized() }

        val result = TodoRepositoryImpl(TodoApi(env.client)).getTodos(range)

        assertEquals(DomainError.Unauthorized, assertIs<DomainResult.Failure>(result).error)
        assertNull(env.storage.tokens)
        assertEquals(SessionState.LOGGED_OUT, env.session.session.value)
    }

    @Test
    fun loginStoresTokensWithoutSendingABearerHeader() = runTest {
        val env = TestEnvironment { respondJson(TOKENS_JSON) }
        val repository = AuthRepositoryImpl(AuthApi(env.client), env.session, env.client)
        assertEquals(SessionState.LOGGED_OUT, env.session.session.value)

        val result = repository.login("user@example.com", "password123")

        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(StoredTokens("new-access", "new-refresh"), env.storage.tokens)
        assertEquals(SessionState.LOGGED_IN, env.session.session.value)
        assertNull(env.requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun invalidCredentialsReturnUnauthorizedAndKeepTheSessionLoggedOut() = runTest {
        val env = TestEnvironment { respondUnauthorized() }
        val repository = AuthRepositoryImpl(AuthApi(env.client), env.session, env.client)

        val result = repository.login("user@example.com", "wrong")

        assertEquals(DomainError.Unauthorized, assertIs<DomainResult.Failure>(result).error)
        assertEquals(SessionState.LOGGED_OUT, env.session.session.value)
    }

    @Test
    fun logoutClearsTheSessionEvenWhenTheServerIsUnreachable() = runTest {
        val env = TestEnvironment(loggedIn) { throw IOException("offline") }
        val repository = AuthRepositoryImpl(AuthApi(env.client), env.session, env.client)

        repository.logout()

        assertNull(env.storage.tokens)
        assertEquals(SessionState.LOGGED_OUT, env.session.session.value)
    }
}
