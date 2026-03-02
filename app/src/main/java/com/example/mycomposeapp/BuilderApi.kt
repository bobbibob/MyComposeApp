package com.example.mycomposeapp

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object BuilderApi {
    // llama-server OpenAI-compatible endpoint
    private const val ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    /**
     * Returns raw model content (choices[0].message.content) or throws with readable error.
     */
    fun generatePlan(appName: String, description: String): String {
        val sys = "You are a software planner. Output ONLY valid JSON. No markdown. No explanations."
        val user = buildString {
            append("Create a minimal implementation plan for an Android app.\n")
            append("App name: ").append(appName.trim()).append("\n")
            append("Description: ").append(description.trim()).append("\n\n")
            append("Return ONLY JSON with keys:\n")
            append("{\"summary\":string,\"files\":[{\"path\":string,\"action\":\"create|update\",\"reason\":string}],\"steps\":[string]}\n")
            append("Keep it short.\n")
        }

        val bodyJson = JSONObject().apply {
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", sys))
                put(JSONObject().put("role", "user").put("content", user))
            })
            // keep responses short so it doesn't hang
            put("temperature", 0.2)
            put("max_tokens", 500)
        }

        val req = Request.Builder()
            .url(ENDPOINT)
            .post(bodyJson.toString().toRequestBody(JSON))
            .build()

        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                // show server-provided error if any
                throw RuntimeException("HTTP ${resp.code}: ${text.take(4000)}")
            }
            val root = JSONObject(text)
            val choices = root.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                throw RuntimeException("Bad response: no choices. Raw=${text.take(2000)}")
            }
            val msg = choices.getJSONObject(0).optJSONObject("message")
            val content = msg?.optString("content") ?: ""
            if (content.isBlank()) {
                throw RuntimeException("Empty content. Raw=${text.take(2000)}")
            }
            return content
        }
    }
}
