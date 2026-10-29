package com.pelypets.todolist.data.remote

import com.pelypets.todolist.core.api.ApiRoutes
import com.pelypets.todolist.core.dto.EmailCredentialsDto
import com.pelypets.todolist.core.dto.GoogleLoginDto
import com.pelypets.todolist.core.dto.RefreshTokenDto
import com.pelypets.todolist.core.dto.TodoDto
import com.pelypets.todolist.core.dto.TodoInputDto
import com.pelypets.todolist.core.dto.TokenPairDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.time.Instant

class AuthApi(private val client: HttpClient) {

    suspend fun login(credentials: EmailCredentialsDto): TokenPairDto =
        client.post(ApiRoutes.AUTH_LOGIN) { jsonBody(credentials) }.body()

    suspend fun register(credentials: EmailCredentialsDto): TokenPairDto =
        client.post(ApiRoutes.AUTH_REGISTER) { jsonBody(credentials) }.body()

    suspend fun loginWithGoogle(idToken: String): TokenPairDto =
        client.post(ApiRoutes.AUTH_GOOGLE) { jsonBody(GoogleLoginDto(idToken)) }.body()

    suspend fun logout(refreshToken: String) {
        client.post(ApiRoutes.AUTH_LOGOUT) { jsonBody(RefreshTokenDto(refreshToken)) }
    }
}

class TodoApi(private val client: HttpClient) {

    suspend fun getTodos(from: Instant, to: Instant): List<TodoDto> =
        client.get(ApiRoutes.TODOS) {
            parameter(ApiRoutes.QUERY_FROM, from.toString())
            parameter(ApiRoutes.QUERY_TO, to.toString())
        }.body()

    suspend fun getTodo(id: String): TodoDto = client.get(ApiRoutes.todo(id)).body()

    suspend fun create(input: TodoInputDto): TodoDto =
        client.post(ApiRoutes.TODOS) { jsonBody(input) }.body()

    suspend fun update(id: String, input: TodoInputDto): TodoDto =
        client.put(ApiRoutes.todo(id)) { jsonBody(input) }.body()

    suspend fun delete(id: String) {
        client.delete(ApiRoutes.todo(id))
    }
}

internal inline fun <reified T : Any> HttpRequestBuilder.jsonBody(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
