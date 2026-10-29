package com.pelypets.todolist.server

import com.pelypets.todolist.core.api.ApiRoutes
import com.pelypets.todolist.core.dto.ErrorCodes
import com.pelypets.todolist.core.dto.TodoDto
import com.pelypets.todolist.core.dto.TodoInputDto
import com.pelypets.todolist.core.dto.TodoStatusDto
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.HttpStatusCode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class TodoApiTest {

    @BeforeTest
    fun cleanDatabase() = TestDatabase.truncateAll()

    @Test
    fun `created todo can be fetched by id`() = apiTest {
        val token = register().accessToken
        val created = createTodo(token, "  Buy milk  ", "2026-10-29T10:00:00Z", TodoStatusDto.IN_PROGRESS)

        assertEquals("Buy milk", created.title)
        assertEquals(TodoStatusDto.IN_PROGRESS, created.status)

        val fetched = client.get(ApiRoutes.todo(created.id)) { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, fetched.status)
        assertEquals(created, fetched.body<TodoDto>())
    }

    @Test
    fun `list returns todos in the half open range sorted by due date`() = apiTest {
        val token = register().accessToken
        createTodo(token, "later", "2026-10-29T18:00:00Z")
        createTodo(token, "at from", "2026-10-29T00:00:00Z")
        createTodo(token, "at to (excluded)", "2026-10-30T00:00:00Z")
        createTodo(token, "yesterday", "2026-10-28T23:59:59Z")
        createTodo(token, "morning", "2026-10-29T09:30:00Z")

        val response = client.get(ApiRoutes.TODOS) {
            bearerAuth(token)
            parameter(ApiRoutes.QUERY_FROM, "2026-10-29T00:00:00Z")
            parameter(ApiRoutes.QUERY_TO, "2026-10-30T00:00:00Z")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf("at from", "morning", "later"), response.body<List<TodoDto>>().map { it.title })
    }

    @Test
    fun `list validates the range`() = apiTest {
        val token = register().accessToken

        client.get(ApiRoutes.TODOS) { bearerAuth(token) }
            .assertError(HttpStatusCode.BadRequest, ErrorCodes.VALIDATION_ERROR)
        client.get(ApiRoutes.TODOS) {
            bearerAuth(token)
            parameter(ApiRoutes.QUERY_FROM, "yesterday")
            parameter(ApiRoutes.QUERY_TO, "2026-10-30T00:00:00Z")
        }.assertError(HttpStatusCode.BadRequest, ErrorCodes.VALIDATION_ERROR)
        client.get(ApiRoutes.TODOS) {
            bearerAuth(token)
            parameter(ApiRoutes.QUERY_FROM, "2026-10-30T00:00:00Z")
            parameter(ApiRoutes.QUERY_TO, "2026-10-29T00:00:00Z")
        }.assertError(HttpStatusCode.BadRequest, ErrorCodes.VALIDATION_ERROR)
    }

    @Test
    fun `update changes all editable fields`() = apiTest {
        val token = register().accessToken
        val created = createTodo(token, "Draft", "2026-10-29T10:00:00Z")

        val response = client.put(ApiRoutes.todo(created.id)) {
            bearerAuth(token)
            json(
                TodoInputDto(
                    title = "Final",
                    description = "Details",
                    dueAt = Instant.parse("2026-11-01T08:15:00Z"),
                    status = TodoStatusDto.DONE,
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = response.body<TodoDto>()
        assertEquals("Final", updated.title)
        assertEquals("Details", updated.description)
        assertEquals(Instant.parse("2026-11-01T08:15:00Z"), updated.dueAt)
        assertEquals(TodoStatusDto.DONE, updated.status)
        assertEquals(created.createdAt, updated.createdAt)
        assertTrue(updated.updatedAt >= created.updatedAt)
    }

    @Test
    fun `delete removes the todo`() = apiTest {
        val token = register().accessToken
        val created = createTodo(token, "Temp", "2026-10-29T10:00:00Z")

        val deleted = client.delete(ApiRoutes.todo(created.id)) { bearerAuth(token) }
        assertEquals(HttpStatusCode.NoContent, deleted.status)

        client.get(ApiRoutes.todo(created.id)) { bearerAuth(token) }
            .assertError(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND)
        client.delete(ApiRoutes.todo(created.id)) { bearerAuth(token) }
            .assertError(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND)
    }

    @Test
    fun `users cannot see or modify each other's todos`() = apiTest {
        val owner = register(email = "owner@example.com").accessToken
        val intruder = register(email = "intruder@example.com").accessToken
        val todo = createTodo(owner, "Private", "2026-10-29T10:00:00Z")

        client.get(ApiRoutes.todo(todo.id)) { bearerAuth(intruder) }
            .assertError(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND)
        client.put(ApiRoutes.todo(todo.id)) {
            bearerAuth(intruder)
            json(TodoInputDto(title = "Hacked", dueAt = Instant.parse("2026-10-29T10:00:00Z")))
        }.assertError(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND)
        client.delete(ApiRoutes.todo(todo.id)) { bearerAuth(intruder) }
            .assertError(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND)

        val intruderList = client.get(ApiRoutes.TODOS) {
            bearerAuth(intruder)
            parameter(ApiRoutes.QUERY_FROM, "2026-10-01T00:00:00Z")
            parameter(ApiRoutes.QUERY_TO, "2026-11-01T00:00:00Z")
        }.body<List<TodoDto>>()
        assertTrue(intruderList.isEmpty())

        val ownerView = client.get(ApiRoutes.todo(todo.id)) { bearerAuth(owner) }.body<TodoDto>()
        assertEquals("Private", ownerView.title)
    }

    @Test
    fun `todo input is validated`() = apiTest {
        val token = register().accessToken

        client.post(ApiRoutes.TODOS) {
            bearerAuth(token)
            json(TodoInputDto(title = "   ", dueAt = Instant.parse("2026-10-29T10:00:00Z")))
        }.assertError(HttpStatusCode.BadRequest, ErrorCodes.VALIDATION_ERROR)

        client.post(ApiRoutes.TODOS) {
            bearerAuth(token)
            json(TodoInputDto(title = "x".repeat(201), dueAt = Instant.parse("2026-10-29T10:00:00Z")))
        }.assertError(HttpStatusCode.BadRequest, ErrorCodes.VALIDATION_ERROR)

        client.get(ApiRoutes.todo("not-a-uuid")) { bearerAuth(token) }
            .assertError(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND)
    }
}
