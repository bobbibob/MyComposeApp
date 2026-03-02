package com.example.mycomposeapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LocalModelApi {
    private const val ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"

    /**
     * Streaming chat: вызывает onDelta() каждый раз, когда приходит новый кусок текста.
     * Возвращает полный ответ в конце.
     */
    suspend fun chatStream(
        userText: String,
        onDelta: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {

        val payload = JSONObject().apply {
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", userText)
            }))
            put("stream", true)
            put("temperature", 0.2)
            put("max_tokens", 512)
        }.toString()

        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 0 // streaming: без таймаута чтения
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "text/event-stream")
        }

        conn.outputStream.use { os ->
            os.write(payload.toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val reader = BufferedReader(InputStreamReader(stream))

        if (code !in 200..299) {
            val err = reader.readText()
            return@withContext "HTTP $code: $err"
        }

        val full = StringBuilder()

        // OpenAI-style SSE:
        // data: {...}
        // data: [DONE]
        while (true) {
            val line = reader.readLine() ?: break
            if (!line.startsWith("data:")) continue

            val data = line.removePrefix("data:").trim()
            if (data == "[DONE]") break
            if (data.isEmpty()) continue

            // chunk json
            val chunk = runCatching { JSONObject(data) }.getOrNull() ?: continue

            val choice0 = runCatching {
                chunk.getJSONArray("choices").getJSONObject(0)
            }.getOrNull() ?: continue

            // streaming обычно кладёт текст в choices[0].delta.content
            val delta = choice0.optJSONObject("delta")?.optString("content", "") ?: ""

            // иногда (в некоторых реализациях) может прилетать message.content — на всякий
            val fallback = choice0.optJSONObject("message")?.optString("content", "") ?: ""

            val piece = if (delta.isNotEmpty()) delta else fallback
            if (piece.isNotEmpty()) {
                full.append(piece)
                onDelta(piece)
            }
        }

        full.toString()
    }
}
