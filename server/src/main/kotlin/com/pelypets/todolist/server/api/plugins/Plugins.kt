package com.pelypets.todolist.server.api.plugins

import com.pelypets.todolist.core.dto.ErrorCodes
import com.pelypets.todolist.core.dto.ErrorResponse
import com.pelypets.todolist.core.dto.GoogleLoginDto
import com.pelypets.todolist.core.dto.RefreshTokenDto
import com.pelypets.todolist.server.config.JwtConfig
import com.pelypets.todolist.server.domain.EmailTakenException
import com.pelypets.todolist.server.domain.NotFoundException
import com.pelypets.todolist.server.domain.UnauthorizedException
import com.pelypets.todolist.server.domain.ValidationException
import com.auth0.jwt.JWTVerifier
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.UUID

const val JWT_AUTH = "auth-jwt"
private const val REQUEST_ID_HEADER = "X-Request-Id"

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

fun Application.configureSerialization() {
    install(ContentNegotiation) { json(AppJson) }
}

fun Application.configureMonitoring() {
    install(CallId) {
        retrieveFromHeader(REQUEST_ID_HEADER)
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() && it.length <= 64 }
        replyToHeader(REQUEST_ID_HEADER)
    }
    install(CallLogging) {
        level = Level.INFO
        callIdMdc("call-id")
        filter { call -> call.request.path().startsWith("/api") }
    }
}

fun Application.configureSecurity(config: JwtConfig, verifier: JWTVerifier) {
    authentication {
        jwt(JWT_AUTH) {
            realm = config.realm
            verifier(verifier)
            validate { credential ->
                credential.payload.subject
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?.let { JWTPrincipal(credential.payload) }
            }
            challenge { _, _ ->
                call.response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer realm=\"${config.realm}\"")
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(ErrorCodes.UNAUTHORIZED, "Access token is missing, invalid or expired"),
                )
            }
        }
    }
}

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<GoogleLoginDto> {
            if (it.idToken.isBlank()) ValidationResult.Invalid("idToken must not be blank") else ValidationResult.Valid
        }
        validate<RefreshTokenDto> {
            if (it.refreshToken.isBlank()) ValidationResult.Invalid("refreshToken must not be blank") else ValidationResult.Valid
        }
    }
}

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger("StatusPages")
    install(StatusPages) {
        exception<ValidationException> { call, e ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(ErrorCodes.VALIDATION_ERROR, e.message.orEmpty()))
        }
        exception<RequestValidationException> { call, e ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(ErrorCodes.VALIDATION_ERROR, e.reasons.joinToString("; ")))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(ErrorCodes.VALIDATION_ERROR, "Malformed request"))
        }
        exception<UnauthorizedException> { call, e ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(ErrorCodes.UNAUTHORIZED, e.message.orEmpty()))
        }
        exception<NotFoundException> { call, e ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(ErrorCodes.NOT_FOUND, e.message.orEmpty()))
        }
        exception<EmailTakenException> { call, e ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse(ErrorCodes.EMAIL_TAKEN, e.message.orEmpty()))
        }
        exception<Throwable> { call, e ->
            logger.error("Unhandled error", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(ErrorCodes.INTERNAL_ERROR, "Internal server error"))
        }
    }
}
