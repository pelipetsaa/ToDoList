package com.pelypets.todolist.feature.tododetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pelypets.todolist.core.ui.components.ProgressButton
import com.pelypets.todolist.core.ui.formatted
import com.pelypets.todolist.core.ui.text
import com.pelypets.todolist.domain.model.TodoStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import todolist.app.shared.generated.resources.Res
import todolist.app.shared.generated.resources.back
import todolist.app.shared.generated.resources.cancel
import todolist.app.shared.generated.resources.delete
import todolist.app.shared.generated.resources.delete_confirm_message
import todolist.app.shared.generated.resources.delete_confirm_title
import todolist.app.shared.generated.resources.edit_todo_title
import todolist.app.shared.generated.resources.field_date
import todolist.app.shared.generated.resources.field_description
import todolist.app.shared.generated.resources.field_status
import todolist.app.shared.generated.resources.field_time
import todolist.app.shared.generated.resources.field_title
import todolist.app.shared.generated.resources.ic_arrow_back
import todolist.app.shared.generated.resources.ic_delete
import todolist.app.shared.generated.resources.new_todo_title
import todolist.app.shared.generated.resources.ok
import todolist.app.shared.generated.resources.retry
import todolist.app.shared.generated.resources.save
import kotlin.time.Instant

@Composable
fun TodoDetailRoute(
    todoId: String?,
    onClose: () -> Unit,
    viewModel: TodoDetailViewModel = koinViewModel(parameters = { parametersOf(todoId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TodoDetailEffect.Close -> onClose()
                is TodoDetailEffect.ShowError -> snackbarHostState.showSnackbar(getString(effect.error.text))
            }
        }
    }

    TodoDetailScreen(state = state, snackbarHostState = snackbarHostState, onIntent = viewModel::onIntent)
}

@Composable
fun TodoDetailScreen(
    state: TodoDetailState,
    snackbarHostState: SnackbarHostState,
    onIntent: (TodoDetailIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) Res.string.new_todo_title else Res.string.edit_todo_title)) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(TodoDetailIntent.BackClicked) }) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { onIntent(TodoDetailIntent.DeleteClicked) }, enabled = state.canEdit) {
                            Icon(painterResource(Res.drawable.ic_delete), contentDescription = stringResource(Res.string.delete))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.loadError != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(state.loadError.text), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { onIntent(TodoDetailIntent.Retry) }) { Text(stringResource(Res.string.retry)) }
                }
                else -> TodoForm(state, onIntent)
            }
        }
    }

    when (state.dialog) {
        TodoDetailDialog.DATE_PICKER -> DueDatePickerDialog(state.date, onIntent)
        TodoDetailDialog.TIME_PICKER -> DueTimePickerDialog(state.time, onIntent)
        TodoDetailDialog.DELETE_CONFIRMATION -> DeleteConfirmationDialog(onIntent)
        null -> Unit
    }
}

@Composable
private fun TodoForm(state: TodoDetailState, onIntent: (TodoDetailIntent) -> Unit) {
    val enabled = state.canEdit
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = { onIntent(TodoDetailIntent.TitleChanged(it)) },
            label = { Text(stringResource(Res.string.field_title)) },
            isError = state.titleError != null,
            supportingText = state.titleError?.let { { Text(stringResource(it.text)) } },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = { onIntent(TodoDetailIntent.DescriptionChanged(it)) },
            label = { Text(stringResource(Res.string.field_description)) },
            isError = state.descriptionError != null,
            supportingText = state.descriptionError?.let { { Text(stringResource(it.text)) } },
            enabled = enabled,
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PickerField(
                label = stringResource(Res.string.field_date),
                value = state.date.formatted(),
                enabled = enabled,
                onClick = { onIntent(TodoDetailIntent.DateClicked) },
                modifier = Modifier.weight(3f),
            )
            PickerField(
                label = stringResource(Res.string.field_time),
                value = state.time.formatted(),
                enabled = enabled,
                onClick = { onIntent(TodoDetailIntent.TimeClicked) },
                modifier = Modifier.weight(2f),
            )
        }
        Text(stringResource(Res.string.field_status), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            TodoStatus.entries.forEachIndexed { index, status ->
                SegmentedButton(
                    selected = status == state.status,
                    onClick = { onIntent(TodoDetailIntent.StatusSelected(status)) },
                    shape = SegmentedButtonDefaults.itemShape(index, TodoStatus.entries.size),
                    enabled = enabled,
                ) {
                    Text(stringResource(status.text), maxLines = 1)
                }
            }
        }
        ProgressButton(
            text = stringResource(Res.string.save),
            inProgress = state.isSaving,
            enabled = enabled,
            onClick = { onIntent(TodoDetailIntent.SaveClicked) },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** A read-only text field that opens a picker; the overlay keeps the field from taking focus. */
@Composable
private fun PickerField(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable(enabled = enabled, onClick = onClick))
    }
}

@Composable
private fun DueDatePickerDialog(date: LocalDate, onIntent: (TodoDetailIntent) -> Unit) {
    // The Material date picker works with UTC midnight timestamps.
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
    )
    DatePickerDialog(
        onDismissRequest = { onIntent(TodoDetailIntent.DialogDismissed) },
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis ?: return@TextButton
                    val selected = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                    onIntent(TodoDetailIntent.DateSelected(selected))
                },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text(stringResource(Res.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(TodoDetailIntent.DialogDismissed) }) { Text(stringResource(Res.string.cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun DueTimePickerDialog(time: LocalTime, onIntent: (TodoDetailIntent) -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = { onIntent(TodoDetailIntent.DialogDismissed) },
        confirmButton = {
            TextButton(onClick = { onIntent(TodoDetailIntent.TimeSelected(LocalTime(pickerState.hour, pickerState.minute))) }) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(TodoDetailIntent.DialogDismissed) }) { Text(stringResource(Res.string.cancel)) }
        },
        text = { TimePicker(state = pickerState) },
    )
}

@Composable
private fun DeleteConfirmationDialog(onIntent: (TodoDetailIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(TodoDetailIntent.DialogDismissed) },
        title = { Text(stringResource(Res.string.delete_confirm_title)) },
        text = { Text(stringResource(Res.string.delete_confirm_message)) },
        confirmButton = {
            TextButton(onClick = { onIntent(TodoDetailIntent.DeleteConfirmed) }) {
                Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(TodoDetailIntent.DialogDismissed) }) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
