package com.example.mycomposeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.mycomposeapp.ui.ChatMsg
import com.example.mycomposeapp.ui.ChatScreen
import com.example.mycomposeapp.ui.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface {
                    AppContent()
                }
            }
        }
    }

    @Composable
    private fun AppContent() {
        val clipboard = LocalClipboardManager.current
        var messages by remember { mutableStateOf(listOf<ChatMsg>()) }
        var typing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        ChatScreen(
            messages = messages,
            isTyping = typing,
            onCopy = { clipboard.setText(AnnotatedString(it)) },
            onClear = { messages = emptyList() },
            onSend = { text ->
                messages = messages + ChatMsg(Role.USER, text)
                typing = true

                scope.launch(Dispatchers.IO) {
                    val payload = JSONObject().put("message", text).toString()
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = payload.toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("http://127.0.0.1:8080/v1/chat/completions")
                        .post(body)
                        .build()

                    try {
                        client.newCall(request).execute().use { resp ->
                            val raw = resp.body?.string().orEmpty()
                            val reply = runCatching { JSONObject(raw).optString("reply") }
                                .getOrDefault(raw)
                                .ifBlank { raw }

                            withContext(Dispatchers.Main) {
                                typing = false
                                messages = messages + ChatMsg(Role.MODEL, reply)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            typing = false
                            messages = messages + ChatMsg(Role.MODEL, "ERROR: ${e.message}")
                        }
                    }
                }
            }
        )
    }
}
