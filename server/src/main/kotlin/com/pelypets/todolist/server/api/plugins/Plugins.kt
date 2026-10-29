package com.pelypets.todolist.server.api.plugins

import com.auth0.jwt.JWTVerifier
import com.pelypets.todolist.core.dto.ErrorCodes
import com.pelypets.todolist.core.dto.ErrorResponse
import com.pelypets.todolist.core.dto.GoogleLoginDto
import com.pelypets.todolist.core.dto.RefreshTokenDto
import com.pelypets.todolist.server.config.AppEnvironment
import com.pelypets.todolist.server.config.JwtConfig
import com.pelypets.todolist.server.config.RateLimitConfig
import com.pelypets.todolist.server.domain.EmailTakenException
import com.pelypets.todolist.server.domain.NotFoundException
import com.pelypets.todolist.server.domain.UnauthorizedException
import com.pelypets.todolist.server.domain.ValidationException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.security.MessageDigest
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

const val JWT_AUTH = "auth-jwt"
val AUTH_RATE_LIMIT = RateLimitName("auth")
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
        // ANSI colour codes would end up inside JSON log messages.
        disableDefaultColors()
        callIdMdc("call-id")
        filter { call -> call.request.path().startsWith("/api") }
    }
}

/**
 * Limits sign-in, sign-up and token endpoints per client IP to slow down password guessing.
 * Behind a proxy ([trustProxy]) the client IP is the last X-Forwarded-For entry, the one the proxy appended itself.
 */
fun Application.configureRateLimits(config: RateLimitConfig, trustProxy: Boolean) {
    if (trustProxy) {
        install(XForwardedHeaders) { useLastProxy() }
    }
    install(RateLimit) {
        register(AUTH_RATE_LIMIT) {
            rateLimiter(limit = config.authRequestsPerMinute, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteAddress }
        }
    }
}

const val METRICS_PATH = "/metrics"

/**
 * Prometheus metrics (request timings, JVM) at [METRICS_PATH]. With a [token] the scraper must send it as a bearer
 * token; in production the endpoint exists only when a token is configured.
 */
fun Application.configureMetrics(token: String?, environment: AppEnvironment) {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) { this.registry = registry }
    if (token == null && environment == AppEnvironment.PRODUCTION) return

    val expected = token?.let { "Bearer $it".encodeToByteArray() }
    routing {
        get(METRICS_PATH) {
            val sent = call.request.headers[HttpHeaders.Authorization]?.encodeToByteArray()
            if (expected != null && (sent == null || !MessageDigest.isEqual(expected, sent))) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(ErrorCodes.UNAUTHORIZED, "Metrics token is missing or invalid"))
                return@get
            }
            call.respondText(registry.scrape(), ContentType.parse("text/plain; version=0.0.4; charset=utf-8"))
        }
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
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status, ErrorResponse(ErrorCodes.TOO_MANY_REQUESTS, "Too many requests, try again later"))
        }
        exception<Throwable> { call, e ->
            logger.error("Unhandled error", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(ErrorCodes.INTERNAL_ERROR, "Internal server error"))
        }
    }
}
