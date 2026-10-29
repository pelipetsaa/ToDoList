package com.pelypets.todolist.server

import com.pelypets.todolist.server.api.plugins.AUTH_RATE_LIMIT
import com.pelypets.todolist.server.api.plugins.JWT_AUTH
import com.pelypets.todolist.server.api.plugins.configureMetrics
import com.pelypets.todolist.server.api.plugins.configureMonitoring
import com.pelypets.todolist.server.api.plugins.configureRateLimits
import com.pelypets.todolist.server.api.plugins.configureSecurity
import com.pelypets.todolist.server.api.plugins.configureSerialization
import com.pelypets.todolist.server.api.plugins.configureStatusPages
import com.pelypets.todolist.server.api.plugins.configureValidation
import com.pelypets.todolist.server.api.routes.authRoutes
import com.pelypets.todolist.server.api.routes.healthRoutes
import com.pelypets.todolist.server.api.routes.meRoutes
import com.pelypets.todolist.server.api.routes.todoRoutes
import com.pelypets.todolist.server.application.AuthService
import com.pelypets.todolist.server.application.TodoService
import com.pelypets.todolist.server.config.AppConfig
import com.pelypets.todolist.server.di.serverModule
import com.pelypets.todolist.server.infrastructure.db.AppDatabase
import com.pelypets.todolist.server.infrastructure.security.JwtAccessTokenIssuer
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import org.koin.core.module.Module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.KoinIsolated

/** Swagger UI for [OPENAPI_SPEC], a hand-written spec kept in sync with the routes by OpenApiSpecTest. */
const val API_DOCS_PATH = "docs"
const val OPENAPI_SPEC = "openapi/documentation.yaml"

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() = configureApp(AppConfig.from(environment.config))

fun Application.configureApp(config: AppConfig, overrides: List<Module> = emptyList()) {
    val database = AppDatabase.connect(config.database)
    monitor.subscribe(ApplicationStopped) { database.close() }

    install(KoinIsolated) {
        allowOverride(true)
        modules(listOf(serverModule(config, database)) + overrides)
    }

    val authService = get<AuthService>()
    val todoService = get<TodoService>()

    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    configureValidation()
    configureRateLimits(config.rateLimit, config.trustProxy)
    configureSecurity(config.jwt, get<JwtAccessTokenIssuer>().verifier)
    configureMetrics(config.metricsToken, config.environment)

    routing {
        swaggerUI(path = API_DOCS_PATH, swaggerFile = OPENAPI_SPEC)
        healthRoutes(database)
        rateLimit(AUTH_RATE_LIMIT) {
            authRoutes(authService)
        }
        authenticate(JWT_AUTH) {
            meRoutes(authService)
            todoRoutes(todoService)
        }
    }
}
