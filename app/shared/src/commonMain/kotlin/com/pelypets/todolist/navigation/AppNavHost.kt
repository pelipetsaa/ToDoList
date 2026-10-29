package com.pelypets.todolist.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pelypets.todolist.domain.model.SessionState
import com.pelypets.todolist.feature.auth.login.LoginRoute
import com.pelypets.todolist.feature.auth.register.RegisterRoute
import com.pelypets.todolist.feature.authgate.AuthGateViewModel
import com.pelypets.todolist.feature.tododetail.TodoDetailRoute
import com.pelypets.todolist.feature.todolist.TodoListRoute
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    authGate: AuthGateViewModel = koinViewModel(),
) {
    val session by authGate.session.collectAsStateWithLifecycle()
    val startDestination = remember { session.home }

    LaunchedEffect(session) {
        val destination = navController.currentBackStackEntry?.destination ?: return@LaunchedEffect
        val inAuthFlow = destination.hasRoute<Screen.Login>() || destination.hasRoute<Screen.Register>()
        if (inAuthFlow == (session == SessionState.LOGGED_OUT)) return@LaunchedEffect
        navController.navigate(session.home) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Screen.Login> {
            LoginRoute(onNavigateToRegister = { navController.navigate(Screen.Register) })
        }
        composable<Screen.Register> {
            RegisterRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable<Screen.TodoList> {
            TodoListRoute(onOpenTodo = { id -> navController.navigate(Screen.TodoDetail(id)) })
        }
        composable<Screen.TodoDetail> { entry ->
            TodoDetailRoute(
                todoId = entry.toRoute<Screen.TodoDetail>().id,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

private val SessionState.home: Screen
    get() = if (this == SessionState.LOGGED_IN) Screen.TodoList else Screen.Login
