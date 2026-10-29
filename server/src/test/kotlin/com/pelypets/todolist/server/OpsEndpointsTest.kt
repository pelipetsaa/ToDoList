package com.pelypets.todolist.server

import com.pelypets.todolist.core.api.ApiRoutes
import com.pelypets.todolist.core.dto.HealthDto
import com.pelypets.todolist.server.api.plugins.METRICS_PATH
import com.pelypets.todolist.server.api.routes.HEALTH_READY
import com.pelypets.todolist.server.config.AppEnvironment
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpsEndpointsTest {

    @Test
    fun `readiness checks the database`() = apiTest {
        val response = client.get(HEALTH_READY)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("UP", response.body<HealthDto>().status)
    }

    @Test
    fun `metrics are open in development and count requests`() = apiTest {
        client.get(ApiRoutes.HEALTH)

        val response = client.get(METRICS_PATH)

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue("ktor_http_server_requests" in response.bodyAsText())
    }

    @Test
    fun `metrics require the token when one is configured`() = apiTest(
        config = testAppConfig().copy(metricsToken = "scrape-token"),
    ) {
        assertEquals(HttpStatusCode.Unauthorized, client.get(METRICS_PATH).status)
        assertEquals(HttpStatusCode.Unauthorized, client.get(METRICS_PATH) { bearerAuth("wrong") }.status)
        assertEquals(HttpStatusCode.OK, client.get(METRICS_PATH) { bearerAuth("scrape-token") }.status)
    }

    @Test
    fun `production without a token does not expose metrics`() = apiTest(
        config = testAppConfig().copy(environment = AppEnvironment.PRODUCTION),
    ) {
        assertEquals(HttpStatusCode.NotFound, client.get(METRICS_PATH).status)
    }
}
