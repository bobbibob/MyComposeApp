package com.example.mycomposeapp

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LocalModelApi {

    private const val ENDPOINT = "http://127.0.0.1:8080/chat"

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) {
            throw RuntimeException("Model did not return JSON")
        }
        return text.substring(start, end + 1)
    }

    fun requestFilesJson(appDescription: String): JSONObject {

        val prompt = """
Ты — senior Android developer.

Верни ТОЛЬКО JSON без пояснений.

Формат:
{
  "commit_message":"...",
  "files":[
    {"path":"...","content":"..."}
  ]
}

Описание:
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

        val stream =
            if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream

        val body = BufferedReader(InputStreamReader(stream)).readText()
        val reply = JSONObject(body).optString("reply", "")

        if (reply.isBlank())
            throw RuntimeException("Empty model reply")

        // ⭐ главное исправление
        val jsonOnly = extractJson(reply)

        return JSONObject(jsonOnly)
    }
}
