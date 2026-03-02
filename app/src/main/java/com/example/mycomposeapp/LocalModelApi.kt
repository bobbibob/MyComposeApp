package com.example.mycomposeapp

import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object LocalModelApi {

    private const val ENDPOINT =
        "http://127.0.0.1:8080/v1/chat/completions"

    private val client = OkHttpClient()

    fun send(message: String): String {

        val json = JSONObject().apply {
            put("messages", JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", message)
            ))
        }

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(ENDPOINT)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->

            val text = response.body?.string()
                ?: return "Empty response"

            val root = JSONObject(text)

            return root
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}
