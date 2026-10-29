package com.pelypets.todolist.testing

import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.TimeRange
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.model.TodoStatus
import com.pelypets.todolist.domain.repository.AuthRepository
import com.pelypets.todolist.domain.repository.TodoRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.Clock
import kotlin.time.Instant

/** Runs `viewModelScope` coroutines eagerly on the test thread. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {
    @BeforeTest
    fun setUpMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDownMainDispatcher() = Dispatchers.resetMain()
}

class FixedClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

/** Lets a test hold a call open to observe in-progress state, then release it with a result. */
class Gate<T> {
    private var deferred: CompletableDeferred<T>? = null

    fun close() {
        deferred = CompletableDeferred()
    }

    fun open(value: T) {
        deferred?.complete(value)
        deferred = null
    }

    suspend fun pass(value: () -> T): T = deferred?.await() ?: value()
}

class FakeAuthRepository : AuthRepository {
    val loginCalls = mutableListOf<Pair<String, String>>()
    val registerCalls = mutableListOf<Pair<String, String>>()
    val googleTokens = mutableListOf<String>()
    var logoutCalls = 0
    var result: DomainResult<Unit> = DomainResult.Success(Unit)
    val gate = Gate<DomainResult<Unit>>()

    override suspend fun login(email: String, password: String): DomainResult<Unit> {
        loginCalls += email to password
        return gate.pass { result }
    }

    override suspend fun register(email: String, password: String): DomainResult<Unit> {
        registerCalls += email to password
        return gate.pass { result }
    }

    override suspend fun loginWithGoogle(idToken: String): DomainResult<Unit> {
        googleTokens += idToken
        return gate.pass { result }
    }

    override suspend fun logout(): DomainResult<Unit> {
        logoutCalls++
        return DomainResult.Success(Unit)
    }
}

class FakeTodoRepository : TodoRepository {
    val todos = mutableListOf<Todo>()
    val requestedRanges = mutableListOf<TimeRange>()
    val created = mutableListOf<TodoDraft>()
    val updated = mutableListOf<Pair<String, TodoDraft>>()
    val deleted = mutableListOf<String>()
    var failure: DomainResult.Failure? = null
    val listGate = Gate<DomainResult<List<Todo>>>()

    override suspend fun getTodos(range: TimeRange): DomainResult<List<Todo>> {
        requestedRanges += range
        return listGate.pass { failure ?: DomainResult.Success(todos.filter { it.dueAt in range }) }
    }

    override suspend fun getTodo(id: String): DomainResult<Todo> =
        failure ?: todos.firstOrNull { it.id == id }?.let { DomainResult.Success(it) }
            ?: DomainResult.Failure(DomainError.NotFound)

    override suspend fun create(draft: TodoDraft): DomainResult<Todo> {
        failure?.let { return it }
        created += draft
        return DomainResult.Success(draft.toTodo("new-${created.size}"))
    }

    override suspend fun update(id: String, draft: TodoDraft): DomainResult<Todo> {
        failure?.let { return it }
        updated += id to draft
        return DomainResult.Success(draft.toTodo(id))
    }

    override suspend fun delete(id: String): DomainResult<Unit> {
        failure?.let { return it }
        deleted += id
        return DomainResult.Success(Unit)
    }

    private fun TodoDraft.toTodo(id: String) = Todo(id, title, description, dueAt, status)
}

fun todo(
    id: String,
    dueAt: String,
    title: String = "Todo $id",
    status: TodoStatus = TodoStatus.TODO,
) = Todo(id, title, description = "", dueAt = Instant.parse(dueAt), status = status)
