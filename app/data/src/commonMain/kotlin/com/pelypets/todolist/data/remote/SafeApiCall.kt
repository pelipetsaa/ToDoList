package com.pelypets.todolist.data.remote

import com.pelypets.todolist.core.dto.ErrorResponse
import com.pelypets.todolist.domain.DomainResult
import com.pelypets.todolist.domain.error.DomainError
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

internal suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): DomainResult<T> =
    try {
        DomainResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        DomainResult.Failure(e.toDomainError())
    } catch (e: IOException) {
        DomainResult.Failure(DomainError.Network)
    } catch (e: Exception) {
        DomainResult.Failure(DomainError.Unknown(e.message))
    }

internal suspend fun ResponseException.toDomainError(): DomainError {
    val message = runCatching { response.body<ErrorResponse>().message }.getOrNull()
    return when (response.status) {
        HttpStatusCode.BadRequest -> DomainError.Validation(serverMessage = message)
        HttpStatusCode.Unauthorized -> DomainError.Unauthorized
        HttpStatusCode.NotFound -> DomainError.NotFound
        HttpStatusCode.Conflict -> DomainError.Conflict
        HttpStatusCode.TooManyRequests -> DomainError.TooManyRequests
        else -> DomainError.Unknown(message ?: "HTTP ${response.status.value}")
    }
}
