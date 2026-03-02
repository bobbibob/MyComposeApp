package com.example.mycomposeapp

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object BuilderApi {

    private const val ENDPOINT = "http://127.0.0.1:8080/v1/chat/completions"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun call(messages: JSONArray, maxTokens: Int = 800): String {
        val body = JSONObject().apply {
            put("messages", messages)
            put("temperature", 0.2)
            put("max_tokens", maxTokens)
        }

        val req = Request.Builder()
            .url(ENDPOINT)
            .post(body.toString().toRequestBody(JSON))
            .build()

        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}: $text")

            val root = JSONObject(text)
            return root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    fun generatePlan(appName: String, desc: String): List<String> {
        val sys = "Return ONLY JSON array of file paths. No markdown."
        val user = """
Create minimal Android Jetpack Compose app.
App name: $appName
Description: $desc

Return JSON array like:
["path1","path2"]
""".trimIndent()

        val messages = JSONArray()
            .put(JSONObject().put("role","system").put("content",sys))
            .put(JSONObject().put("role","user").put("content",user))

        val raw = call(messages, 400)
        val clean = raw.substringAfter("[").substringBeforeLast("]")
        val arr = JSONArray("[$clean]")

        return List(arr.length()) { arr.getString(it) }
    }

    fun generateFile(path: String, appName: String, desc: String): String {
        val sys = "Return ONLY file content. No markdown. No explanation."
        val user = """
Write file for Android project.
App: $appName
Description: $desc
Path: $path
""".trimIndent()

        val messages = JSONArray()
            .put(JSONObject().put("role","system").put("content",sys))
            .put(JSONObject().put("role","user").put("content",user))

        return call(messages, 1500)
    }
}
