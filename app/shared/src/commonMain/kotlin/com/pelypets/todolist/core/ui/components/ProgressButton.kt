package com.pelypets.todolist.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressButton(
    text: String,
    inProgress: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
) {
    val buttonModifier = modifier.fillMaxWidth().height(52.dp)
    val content: @Composable () -> Unit = {
        if (inProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            Text(text)
        }
    }
    if (outlined) {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = buttonModifier) { content() }
    } else {
        Button(onClick = onClick, enabled = enabled, modifier = buttonModifier) { content() }
    }
}
