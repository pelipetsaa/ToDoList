package com.pelypets.todolist.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Login : Screen

    @Serializable data object Register : Screen

    @Serializable data object TodoList : Screen

    /** A null [id] opens the editor for a new todo. */
    @Serializable data class TodoDetail(val id: String? = null) : Screen
}
