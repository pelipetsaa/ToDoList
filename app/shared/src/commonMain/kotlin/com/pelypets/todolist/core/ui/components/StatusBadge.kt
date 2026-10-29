package com.pelypets.todolist.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pelypets.todolist.core.ui.text
import com.pelypets.todolist.domain.model.TodoStatus
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatusBadge(status: TodoStatus, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (status) {
        TodoStatus.TODO -> colors.surfaceVariant to colors.onSurfaceVariant
        TodoStatus.IN_PROGRESS -> colors.primaryContainer to colors.onPrimaryContainer
        TodoStatus.DONE -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text = stringResource(status.text),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
