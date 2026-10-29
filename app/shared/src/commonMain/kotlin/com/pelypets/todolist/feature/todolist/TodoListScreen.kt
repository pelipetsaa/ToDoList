package com.pelypets.todolist.feature.todolist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pelypets.todolist.core.ui.components.StatusBadge
import com.pelypets.todolist.core.ui.formatted
import com.pelypets.todolist.core.ui.text
import com.pelypets.todolist.domain.model.Period
import com.pelypets.todolist.domain.model.Todo
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import todolist.app.shared.generated.resources.Res
import todolist.app.shared.generated.resources.add_todo
import todolist.app.shared.generated.resources.empty_hint
import todolist.app.shared.generated.resources.empty_month
import todolist.app.shared.generated.resources.empty_today
import todolist.app.shared.generated.resources.empty_week
import todolist.app.shared.generated.resources.ic_add
import todolist.app.shared.generated.resources.ic_check_list
import todolist.app.shared.generated.resources.ic_logout
import todolist.app.shared.generated.resources.logout
import todolist.app.shared.generated.resources.retry
import todolist.app.shared.generated.resources.todos_title

@Composable
fun TodoListRoute(
    onOpenTodo: (id: String?) -> Unit,
    viewModel: TodoListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onIntent(TodoListIntent.ScreenShown) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TodoListEffect.OpenTodo -> onOpenTodo(effect.id)
                is TodoListEffect.ShowError -> snackbarHostState.showSnackbar(getString(effect.error.text))
            }
        }
    }

    TodoListScreen(state = state, snackbarHostState = snackbarHostState, onIntent = viewModel::onIntent)
}

@Composable
fun TodoListScreen(
    state: TodoListState,
    snackbarHostState: SnackbarHostState,
    onIntent: (TodoListIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.todos_title)) },
                actions = {
                    IconButton(onClick = { onIntent(TodoListIntent.LogoutClicked) }) {
                        Icon(painterResource(Res.drawable.ic_logout), contentDescription = stringResource(Res.string.logout))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(TodoListIntent.AddClicked) }) {
                Icon(painterResource(Res.drawable.ic_add), contentDescription = stringResource(Res.string.add_todo))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = state.period.ordinal) {
                Period.entries.forEach { period ->
                    Tab(
                        selected = period == state.period,
                        onClick = { onIntent(TodoListIntent.PeriodSelected(period)) },
                        text = { Text(stringResource(period.text)) },
                    )
                }
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(TodoListIntent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.error != null -> Placeholder(
                        title = stringResource(state.error.text),
                        action = stringResource(Res.string.retry) to { onIntent(TodoListIntent.Retry) },
                    )
                    state.hasLoaded && state.todos.isEmpty() -> Placeholder(
                        title = stringResource(state.period.emptyText),
                        subtitle = stringResource(Res.string.empty_hint),
                    )
                    else -> TodoList(state.todos, onClick = { onIntent(TodoListIntent.TodoClicked(it)) })
                }
            }
        }
    }
}

@Composable
private fun TodoList(todos: List<Todo>, onClick: (String) -> Unit) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Keeps the last row clear of the FAB.
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        items(todos, key = { it.id }) { todo ->
            TodoRow(todo, timeZone, onClick = { onClick(todo.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun TodoRow(todo: Todo, timeZone: TimeZone, onClick: () -> Unit) {
    val dueAt = todo.dueAt.toLocalDateTime(timeZone)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Text(
                text = dueAt.time.formatted(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(todo.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(dueAt.date.formatted()) },
        trailingContent = { StatusBadge(todo.status) },
    )
}

/** Scrollable so that pull-to-refresh also works on empty and error states. */
@Composable
private fun Placeholder(
    title: String,
    subtitle: String? = null,
    action: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(
            painterResource(Res.drawable.ic_check_list),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 64.dp),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
        )
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.let { (label, onClick) -> Button(onClick = onClick) { Text(label) } }
    }
}

private val Period.emptyText
    get() = when (this) {
        Period.TODAY -> Res.string.empty_today
        Period.WEEK -> Res.string.empty_week
        Period.MONTH -> Res.string.empty_month
    }
