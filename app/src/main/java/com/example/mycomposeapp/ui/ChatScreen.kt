package com.example.mycomposeapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class ChatMsg(val role: Role, val text: String)
enum class Role { USER, MODEL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMsg>,
    isTyping: Boolean,
    onCopy: (String) -> Unit,
    onClear: () -> Unit,
    onSend: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Local Chat") },
            actions = {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            state = listState
        ) {
            items(messages) { msg ->
                Bubble(
                    role = msg.role,
                    text = msg.text,
                    onCopy = if (msg.role == Role.MODEL) ({ onCopy(msg.text) }) else null
                )
                Spacer(Modifier.height(8.dp))
            }

            if (isTyping) {
                item { Bubble(role = Role.MODEL, text = "…", onCopy = null) }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Type a message…") },
                singleLine = true
            )
            Spacer(Modifier.width(10.dp))
            Button(
                enabled = input.isNotBlank(),
                onClick = {
                    val t = input.trim()
                    input = ""
                    onSend(t)
                }
            ) { Text("Send") }
        }
    }
}

@Composable
private fun Bubble(role: Role, text: String, onCopy: (() -> Unit)?) {
    val isUser = role == Role.USER
    val shape = RoundedCornerShape(18.dp)
    val bg = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(shape = shape, color = bg) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(text = text, color = fg)
                }
            }
            if (!isUser && onCopy != null) {
                TextButton(contentPadding = PaddingValues(0.dp), onClick = onCopy) { Text("Copy") }
            }
        }
    }
}
