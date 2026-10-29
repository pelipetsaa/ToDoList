package com.pelypets.todolist.feature.todolist

import app.cash.turbine.test
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.TimeRange
import com.pelypets.todolist.domain.period.PeriodRangeCalculator
import com.pelypets.todolist.domain.usecase.GetTodosForPeriodUseCase
import com.pelypets.todolist.domain.usecase.LogoutUseCase
import com.pelypets.todolist.testing.FakeAuthRepository
import com.pelypets.todolist.testing.FakeTodoRepository
import com.pelypets.todolist.testing.FixedClock
import com.pelypets.todolist.testing.ViewModelTest
import com.pelypets.todolist.testing.todo
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class TodoListViewModelTest : ViewModelTest() {

    private val todos = FakeTodoRepository()
    private val auth = FakeAuthRepository()
    private val clock = FixedClock(Instant.parse("2026-10-29T12:00:00Z")) // Thursday

    private fun viewModel() = TodoListViewModel(
        GetTodosForPeriodUseCase(todos, PeriodRangeCalculator(TimeZone.UTC), clock),
        LogoutUseCase(auth),
    )

    private fun range(from: String, to: String) = TimeRange(Instant.parse(from), Instant.parse(to))

    @Test
    fun showingTheScreenLoadsTodaySortedByDueTime() {
        todos.todos += listOf(
            todo("late", "2026-10-29T18:00:00Z"),
            todo("early", "2026-10-29T08:00:00Z"),
            todo("tomorrow", "2026-10-30T09:00:00Z"),
        )
        val vm = viewModel()
        todos.listGate.close()
        vm.onIntent(TodoListIntent.ScreenShown)
        assertTrue(vm.state.value.isLoading)

        todos.listGate.open(DomainResult.Success(todos.todos.filter { it.id != "tomorrow" }))

        val state = vm.state.value
        assertEquals(listOf("early", "late"), state.todos.map { it.id })
        assertFalse(state.isLoading)
        assertTrue(state.hasLoaded)
        assertEquals(listOf(range("2026-10-29T00:00:00Z", "2026-10-30T00:00:00Z")), todos.requestedRanges)
    }

    @Test
    fun selectingAPeriodRequestsItsRange() {
        val vm = viewModel()
        vm.onIntent(TodoListIntent.PeriodSelected(Period.WEEK))
        vm.onIntent(TodoListIntent.PeriodSelected(Period.MONTH))

        assertEquals(Period.MONTH, vm.state.value.period)
        assertEquals(
            listOf(
                range("2026-10-26T00:00:00Z", "2026-11-02T00:00:00Z"),
                range("2026-10-01T00:00:00Z", "2026-11-01T00:00:00Z"),
            ),
            todos.requestedRanges,
        )
    }

    @Test
    fun reselectingTheSamePeriodDoesNothing() {
        val vm = viewModel()
        vm.onIntent(TodoListIntent.PeriodSelected(Period.TODAY))
        assertTrue(todos.requestedRanges.isEmpty())
    }

    @Test
    fun firstLoadFailureIsShownInline() {
        todos.failure = DomainResult.Failure(DomainError.Network)
        val vm = viewModel()
        vm.onIntent(TodoListIntent.ScreenShown)

        assertEquals(DomainError.Network, vm.state.value.error)

        todos.failure = null
        vm.onIntent(TodoListIntent.Retry)
        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.hasLoaded)
    }

    @Test
    fun refreshFailureKeepsTheListAndShowsASnackbar() = runTest {
        todos.todos += todo("a", "2026-10-29T10:00:00Z")
        val vm = viewModel()
        vm.onIntent(TodoListIntent.ScreenShown)

        vm.effects.test {
            todos.failure = DomainResult.Failure(DomainError.Network)
            vm.onIntent(TodoListIntent.Refresh)

            assertEquals(TodoListEffect.ShowError(DomainError.Network), awaitItem())
            assertEquals(listOf("a"), vm.state.value.todos.map { it.id })
            assertNull(vm.state.value.error)
            assertFalse(vm.state.value.isRefreshing)
        }
    }

    @Test
    fun clicksOpenTheDetailScreen() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onIntent(TodoListIntent.TodoClicked("42"))
            assertEquals(TodoListEffect.OpenTodo("42"), awaitItem())
            vm.onIntent(TodoListIntent.AddClicked)
            assertEquals(TodoListEffect.OpenTodo(null), awaitItem())
        }
    }

    @Test
    fun logoutCallsTheRepository() {
        viewModel().onIntent(TodoListIntent.LogoutClicked)
        assertEquals(1, auth.logoutCalls)
    }
}
