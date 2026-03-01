package com.example.mycomposeapp

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LocalModelApi {

    private const val ENDPOINT = "http://127.0.0.1:8080/chat"

    data class PlanResult(val plan: JSONObject, val raw: String)

    private fun post(message: String, timeoutMs: Int = 240_000): String {
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            connectTimeout = 8000
            readTimeout = timeoutMs
        }

        val payload = JSONObject().put("message", message).toString()
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val body = BufferedReader(InputStreamReader(stream)).readText()
        val reply = JSONObject(body).optString("reply", "")
        if (reply.isBlank()) throw RuntimeException("Empty model reply")
        return reply
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) throw RuntimeException("Model did not return JSON. Raw: ${text.take(200)}")
        return text.substring(start, end + 1)
    }

    /** Этап 1: план с путями */
    fun requestPlan(appDescription: String, retries: Int = 3): PlanResult {
        val basePrompt = """
Ты — senior Android developer.

Верни ТОЛЬКО валидный JSON и затем напиши маркер <END_JSON>.
Никаких пояснений, никакого markdown.

Формат:
{
  "commit_message": "коротко",
  "paths": [
    "app/src/main/java/.../File.kt",
    "app/src/main/res/.../file.xml",
    "app/src/main/AndroidManifest.xml"
  ]
}
<END_JSON>

Правила:
- НЕ меняй версии Gradle/Kotlin/AGP.
- Пути только внутри app/src/main/java/, app/src/main/res/, app/src/main/AndroidManifest.xml
- Минимизируй количество файлов.

ОПИСАНИЕ:
$appDescription
        """.trimIndent()

        var lastRaw = ""
        var lastErr: Exception? = null

        repeat(retries) { attempt ->
            try {
                val raw = post(basePrompt, timeoutMs = 240_000)
                lastRaw = raw
                val json = JSONObject(extractJson(raw))
                return PlanResult(json, raw)
            } catch (e: Exception) {
                lastErr = e
                // чуть меняем промпт, чтобы "встряхнуть" модель
                Thread.sleep(500L + attempt * 400L)
            }
        }
        throw RuntimeException("Plan failed after $retries tries. Last raw: ${lastRaw.take(400)}. Err: ${lastErr?.message}")
    }

    /** Этап 2: контент конкретного файла */
    fun requestFileContent(appDescription: String, path: String): String {
        val prompt = """
Верни ТОЛЬКО полный контент файла для пути ниже. Никаких пояснений.
Путь: $path
Описание: $appDescription
        """.trimIndent()
        return post(prompt, timeoutMs = 300_000).trim()
    }

    fun planToPaths(plan: JSONObject): List<String> {
        val arr = plan.optJSONArray("paths") ?: JSONArray()
        return (0 until arr.length()).map { arr.getString(it) }
    }
}
