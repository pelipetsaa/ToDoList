package com.pelypets.todolist.core.api

object ApiRoutes {
    const val BASE = "/api/v1"

    const val AUTH_REGISTER = "$BASE/auth/register"
    const val AUTH_LOGIN = "$BASE/auth/login"
    const val AUTH_GOOGLE = "$BASE/auth/google"
    const val AUTH_REFRESH = "$BASE/auth/refresh"
    const val AUTH_LOGOUT = "$BASE/auth/logout"

    const val ME = "$BASE/me"

    const val TODOS = "$BASE/todos"
    const val TODO_ID_PARAM = "id"
    const val TODO_BY_ID = "$TODOS/{$TODO_ID_PARAM}"
    const val QUERY_FROM = "from"
    const val QUERY_TO = "to"

    const val HEALTH = "/health"

    fun todo(id: String): String = "$TODOS/$id"
}
