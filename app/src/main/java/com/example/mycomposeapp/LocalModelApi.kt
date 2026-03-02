package com.example.mycomposeapp

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LocalModelApi {
    private const val ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"

    // НЕ suspend: вызываем из Dispatchers.IO
    fun chatStream(userText: String, onDelta: (String) -> Unit): String {
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
            readTimeout = 0
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
            return "HTTP $code: " + reader.readText()
        }

        val full = StringBuilder()

        while (true) {
            val line = reader.readLine() ?: break
            if (!line.startsWith("data:")) continue
            val data = line.removePrefix("data:").trim()
            if (data == "[DONE]") break
            if (data.isEmpty()) continue

            val chunk = runCatching { JSONObject(data) }.getOrNull() ?: continue
            val choice0 = runCatching {
                chunk.getJSONArray("choices").getJSONObject(0)
            }.getOrNull() ?: continue

            val delta = choice0.optJSONObject("delta")?.optString("content", "") ?: ""
            val fallback = choice0.optJSONObject("message")?.optString("content", "") ?: ""
            val piece = if (delta.isNotEmpty()) delta else fallback

            if (piece.isNotEmpty()) {
                full.append(piece)
                onDelta(piece)
            }
        }

        return full.toString()
    }
}
