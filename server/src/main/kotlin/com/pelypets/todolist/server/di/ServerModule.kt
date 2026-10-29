package com.pelypets.todolist.server.di

import com.pelypets.todolist.server.application.AuthService
import com.pelypets.todolist.server.application.TodoService
import com.pelypets.todolist.server.config.AppConfig
import com.pelypets.todolist.server.domain.port.AccessTokenIssuer
import com.pelypets.todolist.server.domain.port.GoogleTokenVerifier
import com.pelypets.todolist.server.domain.port.PasswordHasher
import com.pelypets.todolist.server.domain.port.RefreshTokenGenerator
import com.pelypets.todolist.server.domain.port.RefreshTokenRepository
import com.pelypets.todolist.server.domain.port.TodoRepository
import com.pelypets.todolist.server.domain.port.UserRepository
import com.pelypets.todolist.server.infrastructure.db.AppDatabase
import com.pelypets.todolist.server.infrastructure.db.ExposedRefreshTokenRepository
import com.pelypets.todolist.server.infrastructure.db.ExposedTodoRepository
import com.pelypets.todolist.server.infrastructure.db.ExposedUserRepository
import com.pelypets.todolist.server.infrastructure.security.BcryptPasswordHasher
import com.pelypets.todolist.server.infrastructure.security.GoogleApiTokenVerifier
import com.pelypets.todolist.server.infrastructure.security.JwtAccessTokenIssuer
import com.pelypets.todolist.server.infrastructure.security.SecureRefreshTokenGenerator
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

fun serverModule(config: AppConfig, database: AppDatabase) = module {
    single { config }
    single { database }
    single<Clock> { Clock.System }

    single<UserRepository> { ExposedUserRepository(get()) }
    single<TodoRepository> { ExposedTodoRepository(get()) }
    single<RefreshTokenRepository> { ExposedRefreshTokenRepository(get()) }

    single<PasswordHasher> { BcryptPasswordHasher() }
    single { JwtAccessTokenIssuer(config.jwt, get()) } bind AccessTokenIssuer::class
    single<RefreshTokenGenerator> { SecureRefreshTokenGenerator() }
    single<GoogleTokenVerifier> { GoogleApiTokenVerifier(config.google.clientIds) }

    single {
        AuthService(
            users = get(),
            refreshTokens = get(),
            passwordHasher = get(),
            accessTokenIssuer = get(),
            refreshTokenGenerator = get(),
            googleTokenVerifier = get(),
            refreshTokenTtl = config.jwt.refreshTokenTtlDays.days,
            clock = get(),
        )
    }
    single { TodoService(get(), get()) }
}
