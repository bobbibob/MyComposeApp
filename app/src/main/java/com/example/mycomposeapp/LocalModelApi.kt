package com.example.mycomposeapp

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LocalModelApi {

    private const val ENDPOINT = "http://127.0.0.1:8080/chat"

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
        if (start == -1 || end == -1 || end <= start) throw RuntimeException("Model did not return JSON")
        return text.substring(start, end + 1)
    }

    /** Этап 1: получить план — список путей файлов */
    fun requestPlan(appDescription: String): JSONObject {
        val prompt = """
Ты — senior Android developer.

Сначала верни ПЛАН изменений. Верни ТОЛЬКО валидный JSON, без пояснений.

Формат:
{
  "commit_message": "коротко",
  "paths": [
    "app/src/main/java/.../File.kt",
    "app/src/main/res/.../something.xml",
    "app/src/main/AndroidManifest.xml"
  ]
}

Правила:
- НЕ меняй версии Gradle/Kotlin/AGP.
- Пути только внутри app/src/main/java/, app/src/main/res/, app/src/main/AndroidManifest.xml
- Минимизируй количество файлов.

ОПИСАНИЕ:
$appDescription
        """.trimIndent()

        val reply = post(prompt, timeoutMs = 240_000)
        return JSONObject(extractJson(reply))
    }

    /** Этап 2: получить конкретный файл */
    fun requestFileContent(appDescription: String, path: String): String {
        val prompt = """
Ты — senior Android developer.

Верни ТОЛЬКО ПОЛНЫЙ КОНТЕНТ файла для пути ниже.
Никаких объяснений, никакого markdown, просто текст файла.

Путь:
$path

Контекст/описание задачи:
$appDescription
        """.trimIndent()

        // контент может быть длинным, дадим чуть больше времени
        val reply = post(prompt, timeoutMs = 300_000)

        // если модель вдруг вставит мусор — грубо вырежем до "package"/"<" для xml — но только если явно видно
        return reply.trim()
    }

    fun planToPaths(plan: JSONObject): List<String> {
        val arr = plan.optJSONArray("paths") ?: JSONArray()
        return (0 until arr.length()).map { arr.getString(it) }
    }
}
