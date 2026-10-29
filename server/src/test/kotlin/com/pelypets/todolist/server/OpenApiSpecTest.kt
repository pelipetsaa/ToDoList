package com.pelypets.todolist.server

import com.pelypets.todolist.core.api.ApiRoutes
import com.pelypets.todolist.server.api.routes.HEALTH_READY
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenApiSpecTest {

    private val servedPaths = setOf(
        ApiRoutes.AUTH_REGISTER,
        ApiRoutes.AUTH_LOGIN,
        ApiRoutes.AUTH_GOOGLE,
        ApiRoutes.AUTH_REFRESH,
        ApiRoutes.AUTH_LOGOUT,
        ApiRoutes.ME,
        ApiRoutes.TODOS,
        ApiRoutes.TODO_BY_ID,
        ApiRoutes.HEALTH,
        HEALTH_READY,
    )

    private val spec = checkNotNull(javaClass.classLoader.getResource(OPENAPI_SPEC)).readText()

    /** Top-level keys under `paths:` are indented by exactly two spaces. */
    private val documentedPaths = Regex("^ {2}(/\\S*):\\s*$", RegexOption.MULTILINE)
        .findAll(spec)
        .map { it.groupValues[1] }
        .toSet()

    @Test
    fun `the spec documents exactly the served paths`() {
        assertEquals(servedPaths, documentedPaths)
    }

    @Test
    fun `every error code clients can receive is documented`() {
        listOf("VALIDATION_ERROR", "UNAUTHORIZED", "NOT_FOUND", "EMAIL_TAKEN", "TOO_MANY_REQUESTS", "INTERNAL_ERROR")
            .forEach { code -> assertTrue(code in spec, "$code is missing from the spec") }
    }

    @Test
    fun `swagger ui is served`() = apiTest {
        val page = client.get("/$API_DOCS_PATH")
        assertEquals(HttpStatusCode.OK, page.status)
        assertTrue("swagger" in page.bodyAsText().lowercase())

        val specResponse = client.get("/$API_DOCS_PATH/documentation.yaml")
        assertEquals(HttpStatusCode.OK, specResponse.status)
        assertTrue("/api/v1/todos" in specResponse.bodyAsText())
    }
}
