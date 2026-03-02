package com.example.mycomposeapp

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object BuilderApi {
    private const val ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"

    private val JSON: MediaType = MediaType.get("application/json; charset=utf-8")

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(50, TimeUnit.SECONDS)
        .build()

    fun generatePlan(appName: String, spec: String): String {
        val system = """
You are an expert Android developer.
Return ONLY valid JSON. No markdown. No extra text.
Schema:
{
  "app_name": "...",
  "package": "com.example.<something>",
  "min_sdk": 26,
  "compile_sdk": 34,
  "changes": [
    {"path":"...", "action":"create|modify|delete", "summary":"..."}
  ]
}
""".trimIndent()

        val user = """
APP_NAME: $appName

SPEC:
$spec
""".trimIndent()

        val payload = JSONObject().apply {
            put("messages", JSONArray().apply {
                put(JSONObject().put("role","system").put("content", system))
                put(JSONObject().put("role","user").put("content", user))
            })
            put("temperature", 0.2)
            put("max_tokens", 900)
        }.toString()

        val req = Request.Builder()
            .url(ENDPOINT)
            .post(RequestBody.create(JSON, payload))
            .build()

        client.newCall(req).execute().use { resp ->
            val body = resp.body()?.string() ?: ""
            if (!resp.isSuccessful) return "HTTP ${resp.code()}: $body"
            return try {
                val root = JSONObject(body)
                root.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            } catch (e: Exception) {
                "Error: bad JSON from server\n$body"
            }
        }
    }
}
