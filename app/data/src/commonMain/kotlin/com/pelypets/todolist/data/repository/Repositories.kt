package com.pelypets.todolist.data.repository

import com.pelypets.todolist.core.dto.EmailCredentialsDto
import com.pelypets.todolist.core.dto.TokenPairDto
import com.pelypets.todolist.data.mapper.toDomain
import com.pelypets.todolist.data.mapper.toDto
import com.pelypets.todolist.data.mapper.toStoredTokens
import com.pelypets.todolist.data.remote.AuthApi
import com.pelypets.todolist.data.remote.TodoApi
import com.pelypets.todolist.data.remote.clearCachedTokens
import com.pelypets.todolist.data.remote.safeApiCall
import com.pelypets.todolist.data.session.SessionManager
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.map
import com.pelypets.todolist.domain.model.TimeRange
import com.pelypets.todolist.domain.model.Todo
import com.pelypets.todolist.domain.model.TodoDraft
import com.pelypets.todolist.domain.repository.AuthRepository
import com.pelypets.todolist.domain.repository.TodoRepository
import io.ktor.client.HttpClient

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val session: SessionManager,
    private val httpClient: HttpClient,
) : AuthRepository {

    override suspend fun login(email: String, password: String): DomainResult<Unit> =
        authenticate { api.login(EmailCredentialsDto(email, password)) }

    override suspend fun register(email: String, password: String): DomainResult<Unit> =
        authenticate { api.register(EmailCredentialsDto(email, password)) }

    override suspend fun loginWithGoogle(idToken: String): DomainResult<Unit> =
        authenticate { api.loginWithGoogle(idToken) }

    override suspend fun logout(): DomainResult<Unit> {
        session.currentTokens()?.refreshToken?.let { refreshToken ->
            // Best effort: the local session is cleared even if the server cannot be reached.
            safeApiCall { api.logout(refreshToken) }
        }
        session.end()
        httpClient.clearCachedTokens()
        return DomainResult.Success(Unit)
    }

    private suspend inline fun authenticate(crossinline call: suspend () -> TokenPairDto): DomainResult<Unit> =
        safeApiCall { call() }.map { tokens ->
            session.start(tokens.toStoredTokens())
            httpClient.clearCachedTokens()
        }
}

class TodoRepositoryImpl(private val api: TodoApi) : TodoRepository {

    override suspend fun getTodos(range: TimeRange): DomainResult<List<Todo>> =
        safeApiCall { api.getTodos(range.from, range.to).map { it.toDomain() } }

    override suspend fun getTodo(id: String): DomainResult<Todo> =
        safeApiCall { api.getTodo(id).toDomain() }

    override suspend fun create(draft: TodoDraft): DomainResult<Todo> =
        safeApiCall { api.create(draft.toDto()).toDomain() }

    override suspend fun update(id: String, draft: TodoDraft): DomainResult<Todo> =
        safeApiCall { api.update(id, draft.toDto()).toDomain() }

    override suspend fun delete(id: String): DomainResult<Unit> =
        safeApiCall { api.delete(id) }
}
