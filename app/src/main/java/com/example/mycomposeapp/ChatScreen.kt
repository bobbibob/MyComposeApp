package com.example.mycomposeapp

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class Msg(val role: String, val text: String)

@Composable
fun ChatScreen() {
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<Msg>() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Local Model Chat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { m ->
                val label = if (m.role == "user") "You" else "Model"
                Text("$label: ${m.text}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(10.dp))
            }
        }

        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = input.isNotBlank() && !busy,
                onClick = {
                    val text = input.trim()
                    input = ""
                    messages.add(Msg("user", text))
                    busy = true

                    thread {
                        val reply = postToLocalModel(text)
                        mainHandler.post {
                            messages.add(Msg("assistant", reply.ifBlank { "(empty reply)" }))
                            busy = false
                        }
                    }
                }
            ) { Text(if (busy) "..." else "Send") }
        }

        Spacer(Modifier.height(6.dp))
        Text("API: http://127.0.0.1:8080/chat", style = MaterialTheme.typography.bodySmall)
    }
}

private fun postToLocalModel(message: String): String {
    return try {
        val url = URL("http://127.0.0.1:8080/chat")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            connectTimeout = 8000
            readTimeout = 120000
        }

        val payload = JSONObject().put("message", message).toString()
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = BufferedReader(InputStreamReader(stream)).readText()

        JSONObject(body).optString("reply", "Error: bad JSON")
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
