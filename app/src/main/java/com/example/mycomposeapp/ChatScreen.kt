package com.example.mycomposeapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Role { USER, MODEL }
private data class ChatMsg(val role: Role, val text: String)

@Composable
fun ChatScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf(TextFieldValue("")) }
    var isSending by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMsg>() }

    fun scrollToBottom(animated: Boolean) {
        scope.launch {
            if (messages.isEmpty()) return@launch
            val idx = messages.size - 1
            if (animated) listState.animateScrollToItem(idx) else listState.scrollToItem(idx)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Local Model Chat",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            "API: http://127.0.0.1:8080/v1/chat/completions (stream)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Divider()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { m -> BubbleRow(ctx = ctx, msg = m) }
        }

        Divider()

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message…") },
                singleLine = true
            )

            Spacer(Modifier.width(8.dp))

            Button(
                enabled = !isSending,
                onClick = {
                    val text = input.text.trim()
                    if (text.isEmpty()) return@Button

                    input = TextFieldValue("")
                    messages.add(ChatMsg(Role.USER, text))

                    // сразу скроллим к сообщению юзера
                    scrollToBottom(animated = true)

                    val modelIndex = messages.size
                    messages.add(ChatMsg(Role.MODEL, "")) // пустой пузырёк
                    isSending = true

                    // скроллим к пустому пузырьку модели
                    scrollToBottom(animated = false)

                    scope.launch(Dispatchers.IO) {
                        try {
                            LocalModelApi.chatStream(text) { piece ->
                                // обновляем UI и скроллим после каждого кусочка
                                scope.launch(Dispatchers.Main) {
                                    val cur = messages[modelIndex].text
                                    messages[modelIndex] = messages[modelIndex].copy(text = cur + piece)
                                    // держим низ во время стрима
                                    scrollToBottom(animated = false)
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                messages[modelIndex] = messages[modelIndex].copy(
                                    text = "Error: ${e.message ?: e::class.java.simpleName}"
                                )
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isSending = false
                                // финальный скролл в самый низ
                                scrollToBottom(animated = true)
                            }
                        }
                    }
                }
            ) { Text(if (isSending) "…" else "Send") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                messages.clear()
                isSending = false
                input = TextFieldValue("")
            }) { Text("Clear chat") }
        }
    }
}

@Composable
private fun BubbleRow(ctx: Context, msg: ChatMsg) {
    val isUser = msg.role == Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            val bg = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            val fg = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(shape = RoundedCornerShape(18.dp), color = bg) {
                Text(
                    text = if (msg.text.isEmpty() && !isUser) "…" else msg.text,
                    color = fg,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            if (!isUser && msg.text.isNotEmpty()) {
                TextButton(
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("reply", msg.text))
                    }
                ) { Text("Copy") }
            }
        }
    }
}
