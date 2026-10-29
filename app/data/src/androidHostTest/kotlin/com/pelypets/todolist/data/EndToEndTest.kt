package com.pelypets.todolist.data

import com.pelypets.todolist.data.remote.ApiConfig
import com.pelypets.todolist.data.remote.AuthApi
import com.pelypets.todolist.data.remote.HttpClientFactory
import com.pelypets.todolist.data.remote.TodoApi
import com.pelypets.todolist.data.repository.AuthRepositoryImpl
import com.pelypets.todolist.data.repository.TodoRepositoryImpl
import com.pelypets.todolist.data.session.SessionManager
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.domain.model.TimeRange
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.model.TodoStatus
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * The client's real HTTP stack (OkHttp, auth plugin, repositories) against a running server, catching contract
 * drift that the MockEngine tests cannot. Skipped unless E2E_BASE_URL points at a server, e.g.
 * `E2E_BASE_URL=http://localhost:8080 ./gradlew :app:data:testAndroidHostTest --tests '*EndToEndTest*'`.
 */
class EndToEndTest {

    private val baseUrl: String? = System.getenv("E2E_BASE_URL")

    private val storage = InMemoryTokenStorage()
    private val session = SessionManager(storage)
    private val client by lazy { HttpClientFactory.create(OkHttp.create(), ApiConfig(baseUrl!!), session) }
    private val auth by lazy { AuthRepositoryImpl(AuthApi(client), session, client) }
    private val todos by lazy { TodoRepositoryImpl(TodoApi(client)) }

    // A fresh account per run keeps the test independent of whatever the database already holds.
    private val email = "e2e-${Clock.System.now().toEpochMilliseconds()}@example.com"
    private val password = "password123"

    @BeforeTest
    fun requireServer() {
        Assume.assumeTrue("Set E2E_BASE_URL to run against a server", !baseUrl.isNullOrBlank())
    }

    @Test
    fun signUpManageTodosAndSignOut() = runBlocking<Unit> {
        assertIs<DomainResult.Success<Unit>>(auth.register(email, password))
        assertEquals(SessionState.LOGGED_IN, session.session.value)
        assertEquals(DomainError.Conflict, failureOf(auth.register(email.uppercase(), password)))

        val now = Clock.System.now()
        val draft = TodoDraft("E2E todo", "created by EndToEndTest", now + 1.hours, TodoStatus.IN_PROGRESS)
        val created = success(todos.create(draft))
        assertEquals("E2E todo", created.title)
        assertEquals(TodoStatus.IN_PROGRESS, created.status)
        assertEquals(draft.dueAt.wholeSeconds(), created.dueAt.wholeSeconds())

        val listed = success(todos.getTodos(TimeRange(now - 1.days, now + 1.days)))
        assertTrue(listed.any { it.id == created.id }, "the new todo is listed")

        val updated = success(todos.update(created.id, draft.copy(title = "E2E done", status = TodoStatus.DONE)))
        assertEquals(TodoStatus.DONE, updated.status)
        assertEquals("E2E done", success(todos.getTodo(created.id)).title)

        assertIs<DomainResult.Success<Unit>>(todos.delete(created.id))
        assertEquals(DomainError.NotFound, failureOf(todos.getTodo(created.id)))

        auth.logout()
        assertEquals(SessionState.LOGGED_OUT, session.session.value)
        assertNull(storage.tokens)
        assertEquals(DomainError.Unauthorized, failureOf(todos.getTodos(TimeRange(now - 1.days, now + 1.days))))

        assertEquals(DomainError.Unauthorized, failureOf(auth.login(email, "wrong-password")))
        assertIs<DomainResult.Success<Unit>>(auth.login(email, password))
        assertNotNull(storage.tokens)
    }

    @Test
    fun serverValidationErrorsReachTheClient() = runBlocking<Unit> {
        val error = failureOf(auth.register(email, "short"))
        assertIs<DomainError.Validation>(error)
        assertTrue(error.serverMessage.orEmpty().contains("8"), "the server explains the password rule")
    }

    private fun <T> success(result: DomainResult<T>): T = assertIs<DomainResult.Success<T>>(result).value

    private fun failureOf(result: DomainResult<*>): DomainError = assertIs<DomainResult.Failure>(result).error

    private fun Instant.wholeSeconds() = epochSeconds
}
