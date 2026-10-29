package com.pelypets.todolist.feature.tododetail

import app.cash.turbine.test
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.error.ValidationError
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.model.TodoStatus
import com.pelypets.todolist.domain.usecase.DefaultDueDateUseCase
import com.pelypets.todolist.domain.usecase.DeleteTodoUseCase
import com.pelypets.todolist.domain.usecase.GetTodoUseCase
import com.pelypets.todolist.domain.usecase.SaveTodoUseCase
import com.pelypets.todolist.testing.FakeTodoRepository
import com.pelypets.todolist.testing.FixedClock
import com.pelypets.todolist.testing.ViewModelTest
import com.pelypets.todolist.testing.todo
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class TodoDetailViewModelTest : ViewModelTest() {

    private val repository = FakeTodoRepository()
    private val timeZone = TimeZone.of("Europe/Kyiv")
    private val clock = FixedClock(Instant.parse("2026-10-29T10:20:00Z")) // 12:20 in Kyiv

    private fun viewModel(id: String?) = TodoDetailViewModel(
        todoId = id,
        getTodo = GetTodoUseCase(repository),
        saveTodo = SaveTodoUseCase(repository),
        deleteTodo = DeleteTodoUseCase(repository),
        defaultDueDate = DefaultDueDateUseCase(clock, timeZone),
        timeZone = timeZone,
    )

    @Test
    fun newTodoDefaultsToTheNextFullHour() {
        val state = viewModel(null).state.value
        assertTrue(state.isNew)
        assertFalse(state.isLoading)
        assertEquals(LocalDate(2026, 10, 29), state.date)
        assertEquals(LocalTime(13, 0), state.time)
        assertEquals(TodoStatus.TODO, state.status)
    }

    @Test
    fun savingANewTodoCombinesDateAndTimeInTheUserTimeZone() = runTest {
        val vm = viewModel(null)
        vm.effects.test {
            vm.onIntent(TodoDetailIntent.TitleChanged("  Dentist  "))
            vm.onIntent(TodoDetailIntent.DateSelected(LocalDate(2026, 11, 3)))
            vm.onIntent(TodoDetailIntent.TimeSelected(LocalTime(9, 30)))
            vm.onIntent(TodoDetailIntent.StatusSelected(TodoStatus.IN_PROGRESS))
            vm.onIntent(TodoDetailIntent.SaveClicked)

            assertEquals(TodoDetailEffect.Close, awaitItem())
        }
        // Kyiv is UTC+2 in November.
        assertEquals(
            listOf(TodoDraft("Dentist", "", Instant.parse("2026-11-03T07:30:00Z"), TodoStatus.IN_PROGRESS)),
            repository.created,
        )
    }

    @Test
    fun blankTitleIsReportedInlineWithoutSaving() = runTest {
        val vm = viewModel(null)
        vm.effects.test {
            vm.onIntent(TodoDetailIntent.SaveClicked)
            expectNoEvents()
        }
        assertEquals(ValidationError.TITLE_BLANK, vm.state.value.titleError)
        assertFalse(vm.state.value.isSaving)
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun existingTodoIsLoadedAndUpdated() = runTest {
        repository.todos += todo("7", "2026-10-30T15:45:00Z", title = "Call mom", status = TodoStatus.DONE)
        val vm = viewModel("7")

        val loaded = vm.state.value
        assertEquals("Call mom", loaded.title)
        assertEquals(LocalDate(2026, 10, 30), loaded.date)
        assertEquals(LocalTime(17, 45), loaded.time)
        assertEquals(TodoStatus.DONE, loaded.status)

        vm.effects.test {
            vm.onIntent(TodoDetailIntent.TitleChanged("Call dad"))
            vm.onIntent(TodoDetailIntent.SaveClicked)
            assertEquals(TodoDetailEffect.Close, awaitItem())
        }
        assertEquals("7", repository.updated.single().first)
        assertEquals("Call dad", repository.updated.single().second.title)
        assertEquals(Instant.parse("2026-10-30T15:45:00Z"), repository.updated.single().second.dueAt)
    }

    @Test
    fun missingTodoShowsALoadError() {
        val vm = viewModel("missing")
        assertEquals(DomainError.NotFound, vm.state.value.loadError)
        assertFalse(vm.state.value.canEdit)
    }

    @Test
    fun deleteAsksForConfirmationFirst() = runTest {
        repository.todos += todo("7", "2026-10-30T15:45:00Z")
        val vm = viewModel("7")

        vm.effects.test {
            vm.onIntent(TodoDetailIntent.DeleteClicked)
            assertEquals(TodoDetailDialog.DELETE_CONFIRMATION, vm.state.value.dialog)
            assertTrue(repository.deleted.isEmpty())

            vm.onIntent(TodoDetailIntent.DeleteConfirmed)
            assertEquals(TodoDetailEffect.Close, awaitItem())
        }
        assertEquals(listOf("7"), repository.deleted)
        assertNull(vm.state.value.dialog)
    }

    @Test
    fun saveFailureKeepsTheFormAndShowsAnError() = runTest {
        val vm = viewModel(null)
        vm.onIntent(TodoDetailIntent.TitleChanged("Dentist"))
        repository.failure = DomainResult.Failure(DomainError.Network)

        vm.effects.test {
            vm.onIntent(TodoDetailIntent.SaveClicked)
            assertEquals(TodoDetailEffect.ShowError(DomainError.Network), awaitItem())
        }
        assertFalse(vm.state.value.isSaving)
        assertEquals("Dentist", vm.state.value.title)
    }
}
