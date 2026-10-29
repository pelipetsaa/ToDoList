package com.pelypets.todolist.domain.usecase

import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.period.PeriodRangeCalculator
import com.pelypets.todolist.domain.repository.TodoRepository
import com.pelypets.todolist.domain.validation.TodoValidator
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class GetTodosForPeriodUseCase(
    private val todoRepository: TodoRepository,
    private val rangeCalculator: PeriodRangeCalculator,
    private val clock: Clock,
) {
    suspend operator fun invoke(period: Period): DomainResult<List<Todo>> =
        todoRepository.getTodos(rangeCalculator.rangeFor(period, clock.now()))
}

class GetTodoUseCase(private val todoRepository: TodoRepository) {
    suspend operator fun invoke(id: String): DomainResult<Todo> = todoRepository.getTodo(id)
}

/** Creates a todo when [id] is null, otherwise updates the existing one. */
class SaveTodoUseCase(private val todoRepository: TodoRepository) {
    suspend operator fun invoke(id: String?, draft: TodoDraft): DomainResult<Todo> {
        val errors = TodoValidator.validate(draft)
        if (errors.isNotEmpty()) return DomainResult.Failure(DomainError.Validation(errors))
        val normalized = draft.copy(title = draft.title.trim())
        return if (id == null) todoRepository.create(normalized) else todoRepository.update(id, normalized)
    }
}

class DeleteTodoUseCase(private val todoRepository: TodoRepository) {
    suspend operator fun invoke(id: String): DomainResult<Unit> = todoRepository.delete(id)
}

/** Default due date for a new todo: the next full hour in the user's time zone. */
class DefaultDueDateUseCase(private val clock: Clock, private val timeZone: TimeZone) {
    operator fun invoke(): Instant {
        val now = clock.now().toLocalDateTime(timeZone)
        val startOfHour = LocalDateTime(now.date, LocalTime(now.hour, 0))
        return startOfHour.toInstant(timeZone) + 1.hours
    }
}
