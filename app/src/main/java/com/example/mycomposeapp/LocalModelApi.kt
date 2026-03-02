package com.example.mycomposeapp

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object LocalModelApi {
    // llama-server (OpenAI compatible)
    private const val ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder().build()

    @Throws(IOException::class)
    fun chat(message: String): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "user").put("content", message))

        val payload = JSONObject()
            .put("messages", messages)
            // можно добавить параметры, если нужно:
            // .put("temperature", 0.2)
            // .put("max_tokens", 256)

        val body = payload.toString().toRequestBody(JSON)

        val req = Request.Builder()
            .url(ENDPOINT)
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: $text")
            }

            // OpenAI-style: choices[0].message.content
            val json = JSONObject(text)
            val content = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            return content.trim()
        }
    }
}
