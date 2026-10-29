package com.pelypets.todolist.server.api.routes

import com.pelypets.todolist.core.api.ApiRoutes
import com.pelypets.todolist.core.dto.EmailCredentialsDto
import com.pelypets.todolist.core.dto.GoogleLoginDto
import com.pelypets.todolist.core.dto.HealthDto
import com.pelypets.todolist.core.dto.RefreshTokenDto
import com.pelypets.todolist.core.dto.TodoInputDto
import com.pelypets.todolist.server.api.toDraft
import com.pelypets.todolist.server.api.toDto
import com.pelypets.todolist.server.application.AuthService
import com.pelypets.todolist.server.application.TodoService
import com.pelypets.todolist.server.domain.NotFoundException
import com.pelypets.todolist.server.domain.UnauthorizedException
import com.pelypets.todolist.server.domain.ValidationException
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import java.util.UUID
import kotlin.time.Instant

fun Route.healthRoutes() {
    get(ApiRoutes.HEALTH) { call.respond(HealthDto(status = "UP")) }
}

fun Route.authRoutes(authService: AuthService) {
    post(ApiRoutes.AUTH_REGISTER) {
        val body = call.receive<EmailCredentialsDto>()
        call.respond(HttpStatusCode.Created, authService.register(body.email, body.password).toDto())
    }
    post(ApiRoutes.AUTH_LOGIN) {
        val body = call.receive<EmailCredentialsDto>()
        call.respond(authService.login(body.email, body.password).toDto())
    }
    post(ApiRoutes.AUTH_GOOGLE) {
        val body = call.receive<GoogleLoginDto>()
        call.respond(authService.loginWithGoogle(body.idToken).toDto())
    }
    post(ApiRoutes.AUTH_REFRESH) {
        val body = call.receive<RefreshTokenDto>()
        call.respond(authService.refresh(body.refreshToken).toDto())
    }
    post(ApiRoutes.AUTH_LOGOUT) {
        val body = call.receive<RefreshTokenDto>()
        authService.logout(body.refreshToken)
        call.respond(HttpStatusCode.NoContent)
    }
}

/** Must be registered inside `authenticate(JWT_AUTH)`. */
fun Route.meRoutes(authService: AuthService) {
    get(ApiRoutes.ME) {
        call.respond(authService.currentUser(call.userId()).toDto())
    }
}

/** Must be registered inside `authenticate(JWT_AUTH)`. */
fun Route.todoRoutes(todoService: TodoService) {
    get(ApiRoutes.TODOS) {
        val from = call.instantQuery(ApiRoutes.QUERY_FROM)
        val to = call.instantQuery(ApiRoutes.QUERY_TO)
        call.respond(todoService.list(call.userId(), from, to).map { it.toDto() })
    }
    get(ApiRoutes.TODO_BY_ID) {
        call.respond(todoService.get(call.userId(), call.todoId()).toDto())
    }
    post(ApiRoutes.TODOS) {
        val body = call.receive<TodoInputDto>()
        call.respond(HttpStatusCode.Created, todoService.create(call.userId(), body.toDraft()).toDto())
    }
    put(ApiRoutes.TODO_BY_ID) {
        val id = call.todoId()
        val body = call.receive<TodoInputDto>()
        call.respond(todoService.update(call.userId(), id, body.toDraft()).toDto())
    }
    delete(ApiRoutes.TODO_BY_ID) {
        todoService.delete(call.userId(), call.todoId())
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun RoutingCall.userId(): UUID = principal<JWTPrincipal>()?.subject?.let(UUID::fromString) ?: throw UnauthorizedException()

private fun RoutingCall.todoId(): UUID = parameters[ApiRoutes.TODO_ID_PARAM]
    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    ?: throw NotFoundException("Todo not found")

private fun RoutingCall.instantQuery(name: String): Instant {
    val raw = request.queryParameters[name] ?: throw ValidationException("Query parameter '$name' is required")
    return runCatching { Instant.parse(raw) }.getOrElse {
        throw ValidationException("Query parameter '$name' must be an ISO-8601 instant")
    }
}
