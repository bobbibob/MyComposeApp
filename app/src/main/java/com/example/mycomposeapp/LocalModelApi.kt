package com.example.mycomposeapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LocalModelApi {
    // llama-server (OpenAI compatible)
    private const val ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"

    suspend fun chat(userText: String): String = withContext(Dispatchers.IO) {
        val url = URL(ENDPOINT)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        val body = JSONObject().apply {
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userText)
                })
            })
            // необязательно, но полезно:
            put("temperature", 0.2)
            put("max_tokens", 512)
        }.toString()

        conn.outputStream.use { os ->
            os.write(body.toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val respText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

        if (code !in 200..299) {
            return@withContext "HTTP $code: $respText"
        }

        // Parse OpenAI response: choices[0].message.content
        val root = JSONObject(respText)
        val content = root.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        content
    }
}
