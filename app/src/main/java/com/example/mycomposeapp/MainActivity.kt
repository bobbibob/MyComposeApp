package com.example.mycomposeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.mycomposeapp.ui.*
import com.example.mycomposeapp.ui.theme.LocalChatTheme
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LocalChatTheme {

                val clipboard = LocalClipboardManager.current
                var messages by remember { mutableStateOf(listOf<ChatMsg>()) }
                var typing by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                ChatScreen(
                    messages = messages,
                    isTyping = typing,
                    onCopy = {
                        clipboard.setText(AnnotatedString(it))
                    },
                    onClear = {
                        messages = emptyList()
                    },
                    onSend = { text ->

                        messages = messages + ChatMsg(Role.USER, text)
                        typing = true

                        scope.launch(Dispatchers.IO) {

                            val body = JSONObject()
                            body.put("message", text)

                            val request = Request.Builder()
                                .url("http://127.0.0.1:8080/chat")
                                .post(
                                    RequestBody.create(
                                        MediaType.parse("application/json"),
                                        body.toString()
                                    )
                                )
                                .build()

                            try {
                                client.newCall(request).execute().use { resp ->
                                    val json = JSONObject(resp.body!!.string())
                                    val reply = json.getString("reply")

                                    withContext(Dispatchers.Main) {
                                        typing = false
                                        messages =
                                            messages + ChatMsg(Role.MODEL, reply)
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    typing = false
                                    messages =
                                        messages + ChatMsg(
                                            Role.MODEL,
                                            "ERROR: " + e.message
                                        )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
