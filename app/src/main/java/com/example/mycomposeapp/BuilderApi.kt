package com.example.mycomposeapp

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object BuilderApi {
    private const val ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"

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
Make changes for a Kotlin + Jetpack Compose app.
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

        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val respText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

        if (code !in 200..299) {
            return "HTTP $code: $respText"
        }

        val root = JSONObject(respText)
        return root.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}
