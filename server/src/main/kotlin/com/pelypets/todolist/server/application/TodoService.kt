package com.pelypets.todolist.server.application

import com.pelypets.todolist.server.domain.NotFoundException
import com.pelypets.todolist.server.domain.ValidationException
import com.pelypets.todolist.server.domain.Validation
import com.pelypets.todolist.server.domain.model.Todo
import com.pelypets.todolist.server.domain.model.TodoDraft
import com.pelypets.todolist.server.domain.port.TodoRepository
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

class TodoService(
    private val todos: TodoRepository,
    private val clock: Clock,
) {

    suspend fun list(userId: UUID, from: Instant, to: Instant): List<Todo> {
        if (from >= to) throw ValidationException("'from' must be before 'to'")
        return todos.findInRange(userId, from, to)
    }

    suspend fun get(userId: UUID, id: UUID): Todo =
        todos.findById(userId, id) ?: throw notFound()

    suspend fun create(userId: UUID, draft: TodoDraft): Todo {
        Validation.require(Validation.todoError(draft.title, draft.description))
        val now = clock.now()
        return todos.insert(
            Todo(
                id = UUID.randomUUID(),
                userId = userId,
                title = draft.title.trim(),
                description = draft.description,
                dueAt = draft.dueAt,
                status = draft.status,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(userId: UUID, id: UUID, draft: TodoDraft): Todo {
        Validation.require(Validation.todoError(draft.title, draft.description))
        val existing = get(userId, id)
        val updated = existing.copy(
            title = draft.title.trim(),
            description = draft.description,
            dueAt = draft.dueAt,
            status = draft.status,
            updatedAt = clock.now(),
        )
        return todos.update(updated) ?: throw notFound()
    }

    suspend fun delete(userId: UUID, id: UUID) {
        if (!todos.delete(userId, id)) throw notFound()
    }

    private fun notFound() = NotFoundException("Todo not found")
}
