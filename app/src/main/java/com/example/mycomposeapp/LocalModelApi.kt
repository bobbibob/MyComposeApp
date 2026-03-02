package com.example.mycomposeapp

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object LocalModelApi {

    private const val ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"
    private val client = OkHttpClient()

    fun send(message: String): String {
        val json = JSONObject().apply {
            put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", message)
                )
            )
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(ENDPOINT)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: return "Empty response"
            if (!response.isSuccessful) return "HTTP ${response.code}: $text"

            val root = JSONObject(text)
            return root
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}
