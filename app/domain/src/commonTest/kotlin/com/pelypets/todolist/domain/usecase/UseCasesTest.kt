package com.pelypets.todolist.domain.usecase

import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.TimeRange
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.model.TodoStatus
import com.pelypets.todolist.domain.period.PeriodRangeCalculator
import com.pelypets.todolist.domain.repository.AuthRepository
import com.pelypets.todolist.domain.repository.TodoRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class UseCasesTest {

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private class FakeAuthRepository : AuthRepository {
        val calls = mutableListOf<String>()
        override suspend fun login(email: String, password: String) = record("login:$email")
        override suspend fun register(email: String, password: String) = record("register:$email")
        override suspend fun loginWithGoogle(idToken: String) = record("google:$idToken")
        override suspend fun logout() = record("logout")
        private fun record(call: String): DomainResult<Unit> {
            calls += call
            return DomainResult.Success(Unit)
        }
    }

    private class FakeTodoRepository : TodoRepository {
        var requestedRange: TimeRange? = null
        var created: TodoDraft? = null
        var updated: Pair<String, TodoDraft>? = null

        override suspend fun getTodos(range: TimeRange): DomainResult<List<Todo>> {
            requestedRange = range
            return DomainResult.Success(emptyList())
        }
        override suspend fun getTodo(id: String) = DomainResult.Failure(DomainError.NotFound)
        override suspend fun create(draft: TodoDraft): DomainResult<Todo> {
            created = draft
            return DomainResult.Success(draft.toTodo("new"))
        }
        override suspend fun update(id: String, draft: TodoDraft): DomainResult<Todo> {
            updated = id to draft
            return DomainResult.Success(draft.toTodo(id))
        }
        override suspend fun delete(id: String) = DomainResult.Success(Unit)

        private fun TodoDraft.toTodo(id: String) = Todo(id, title, description, dueAt, status)
    }

    private val draft = TodoDraft("  Buy milk ", "", Instant.parse("2026-10-29T10:00:00Z"), TodoStatus.TODO)

    @Test
    fun loginValidatesBeforeCallingTheRepository() = runTest {
        val repository = FakeAuthRepository()
        val result = LoginWithEmailUseCase(repository)("bad", "")

        val failure = assertIs<DomainResult.Failure>(result)
        assertEquals(
            DomainError.Validation(listOf(ValidationError.EMAIL_INVALID, ValidationError.PASSWORD_BLANK)),
            failure.error,
        )
        assertEquals(emptyList(), repository.calls)
    }

    @Test
    fun loginTrimsTheEmail() = runTest {
        val repository = FakeAuthRepository()
        LoginWithEmailUseCase(repository)("  user@example.com ", "secret")
        assertEquals(listOf("login:user@example.com"), repository.calls)
    }

    @Test
    fun registerRequiresMatchingPasswords() = runTest {
        val repository = FakeAuthRepository()
        val result = RegisterUseCase(repository)("user@example.com", "password123", "password124")
        assertEquals(
            DomainError.Validation(listOf(ValidationError.PASSWORD_MISMATCH)),
            assertIs<DomainResult.Failure>(result).error,
        )
    }

    @Test
    fun getTodosForPeriodRequestsTheComputedRange() = runTest {
        val repository = FakeTodoRepository()
        val useCase = GetTodosForPeriodUseCase(
            repository,
            PeriodRangeCalculator(TimeZone.UTC),
            FixedClock(Instant.parse("2026-10-29T13:00:00Z")),
        )

        useCase(Period.MONTH)

        assertEquals(
            TimeRange(Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z")),
            repository.requestedRange,
        )
    }

    @Test
    fun saveTodoCreatesWhenIdIsNullAndTrimsTheTitle() = runTest {
        val repository = FakeTodoRepository()
        SaveTodoUseCase(repository)(null, draft)
        assertEquals("Buy milk", repository.created?.title)
        assertNull(repository.updated)
    }

    @Test
    fun saveTodoUpdatesWhenIdIsPresent() = runTest {
        val repository = FakeTodoRepository()
        SaveTodoUseCase(repository)("42", draft)
        assertEquals("42", repository.updated?.first)
        assertNull(repository.created)
    }

    @Test
    fun saveTodoRejectsInvalidDrafts() = runTest {
        val repository = FakeTodoRepository()
        val result = SaveTodoUseCase(repository)(null, draft.copy(title = " "))
        assertEquals(
            DomainError.Validation(listOf(ValidationError.TITLE_BLANK)),
            assertIs<DomainResult.Failure>(result).error,
        )
        assertNull(repository.created)
    }

    @Test
    fun defaultDueDateIsTheNextFullHour() {
        val useCase = DefaultDueDateUseCase(FixedClock(Instant.parse("2026-10-29T13:45:12Z")), TimeZone.UTC)
        assertEquals(Instant.parse("2026-10-29T14:00:00Z"), useCase())
    }
}
