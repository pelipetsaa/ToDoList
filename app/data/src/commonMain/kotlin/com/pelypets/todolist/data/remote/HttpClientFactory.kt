package com.pelypets.todolist.data.remote

import com.pelypets.todolist.core.api.ApiRoutes
import com.pelypets.todolist.core.dto.RefreshTokenDto
import com.pelypets.todolist.core.dto.TokenPairDto
import com.pelypets.todolist.data.mapper.toStoredTokens
import com.pelypets.todolist.data.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

data class ApiConfig(val baseUrl: String, val enableLogging: Boolean = false)

internal val DataJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

object HttpClientFactory {

    /**
     * The free Render instance sleeps after 15 minutes without traffic and holds the first request while it starts,
     * which can take close to a minute; a shorter timeout would turn every first sign-in into a network error.
     */
    internal const val REQUEST_TIMEOUT_MILLIS = 75_000L

    /** Statuses a proxy returns while the server restarts or redeploys; worth another try for reads. */
    private val transientStatuses = setOf(HttpStatusCode.BadGateway, HttpStatusCode.ServiceUnavailable, HttpStatusCode.GatewayTimeout)

    fun create(engine: HttpClientEngine, config: ApiConfig, session: SessionManager): HttpClient = HttpClient(engine) {
        expectSuccess = true
        defaultRequest { url(config.baseUrl) }
        install(ContentNegotiation) { json(DataJson) }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
        // Only GETs are retried: repeating a POST could create the same todo twice.
        install(HttpRequestRetry) {
            maxRetries = 2
            retryIf { request, response -> request.method == HttpMethod.Get && response.status in transientStatuses }
            exponentialDelay()
        }
        install(Auth) {
            bearer {
                loadTokens {
                    session.currentTokens()?.let { BearerTokens(it.accessToken, it.refreshToken) }
                }
                sendWithoutRequest { request ->
                    !request.url.encodedPath.startsWith("${ApiRoutes.BASE}/auth")
                }
                refreshTokens {
                    val refreshToken = oldTokens?.refreshToken ?: session.currentTokens()?.refreshToken
                    if (refreshToken == null) {
                        session.end()
                        return@refreshTokens null
                    }
                    try {
                        val pair = client.post(ApiRoutes.AUTH_REFRESH) {
                            markAsRefreshTokenRequest()
                            jsonBody(RefreshTokenDto(refreshToken))
                        }.body<TokenPairDto>()
                        session.start(pair.toStoredTokens())
                        BearerTokens(pair.accessToken, pair.refreshToken)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: ResponseException) {
                        session.end()
                        null
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
        if (config.enableLogging) {
            install(Logging) { level = LogLevel.INFO }
        }
    }
}

/** The Auth plugin caches tokens in memory; call this whenever the stored tokens change outside a refresh. */
fun HttpClient.clearCachedTokens() {
    authProvider<BearerAuthProvider>()?.clearToken()
}
