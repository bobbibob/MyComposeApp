package com.example.mycomposeapp

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LocalModelApi {
    private const val ENDPOINT = "http://127.0.0.1:8080/chat"

    fun requestFilesJson(appDescription: String): JSONObject {
        val prompt = """
Ты — senior Android developer.
Проект: Kotlin + Jetpack Compose, package com.example.mycomposeapp.
Нужно реализовать приложение/изменения по описанию ниже.

Жёсткие правила:
- Верни ТОЛЬКО валидный JSON, без пояснений и без markdown.
- Формат JSON:
{
  "commit_message": "коротко",
  "files": [
    {"path":"app/src/main/java/.../File.kt","content":"..."},
    {"path":"app/src/main/res/...","content":"..."}
  ]
}
- Разрешённые пути: app/src/main/java/, app/src/main/res/, app/src/main/AndroidManifest.xml
- НЕ трогай версии Gradle/Kotlin/AGP.
- Если нужен новый файл — добавь его в files.
- Контент файлов должен быть полным.

ОПИСАНИЕ:
$appDescription
        """.trimIndent()

        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            connectTimeout = 8000
            readTimeout = 180000
        }

        val payload = JSONObject().put("message", prompt).toString()
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = BufferedReader(InputStreamReader(stream)).readText()

        val reply = JSONObject(body).optString("reply", "")
        if (reply.isBlank()) throw RuntimeException("Model returned empty reply")
        return JSONObject(reply) // обязано быть чистым JSON
    }
}
