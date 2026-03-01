package com.example.mycomposeapp

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class GithubApi(
    private val owner: String,
    private val repo: String,
    private val token: String
) {
    private fun conn(url: String, method: String): HttpURLConnection {
        val c = (URL(url).openConnection() as HttpURLConnection)
        c.requestMethod = method
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("Authorization", "Bearer $token")
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        c.connectTimeout = 15000
        c.readTimeout = 120000
        return c
    }

    private fun readAll(c: HttpURLConnection): String {
        val stream = try {
            if (c.responseCode in 200..299) c.inputStream else c.errorStream
        } catch (_: Exception) {
            c.errorStream
        }
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream)).readText()
    }

    fun getFileSha(path: String, branch: String = "main"): String? {
        val url = "https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch"
        val c = conn(url, "GET")
        return try {
            val body = readAll(c)
            if (c.responseCode == 200) JSONObject(body).optString("sha", null) else null
        } finally {
            c.disconnect()
        }
    }

    /** PUT contents API. Returns commit sha (from response.commit.sha) */
    fun putFile(path: String, contentUtf8: String, message: String, shaIfExists: String?, branch: String = "main"): String {
        val url = "https://api.github.com/repos/$owner/$repo/contents/$path"
        val c = conn(url, "PUT")
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")

        val b64 = Base64.encodeToString(contentUtf8.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        val payload = JSONObject()
            .put("message", message)
            .put("content", b64)
            .put("branch", branch)

        if (!shaIfExists.isNullOrBlank()) payload.put("sha", shaIfExists)

        c.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

        val body = readAll(c)
        val code = c.responseCode
        c.disconnect()

        if (code !in 200..299) {
            throw RuntimeException("GitHub PUT failed ($code): $body")
        }

        return JSONObject(body).getJSONObject("commit").getString("sha")
    }

    fun listRuns(): JSONArray {
        val url = "https://api.github.com/repos/$owner/$repo/actions/runs?per_page=20"
        val c = conn(url, "GET")
        val body = readAll(c)
        val code = c.responseCode
        c.disconnect()
        if (code !in 200..299) throw RuntimeException("List runs failed ($code): $body")
        return JSONObject(body).getJSONArray("workflow_runs")
    }

    fun getRun(runId: Long): JSONObject {
        val url = "https://api.github.com/repos/$owner/$repo/actions/runs/$runId"
        val c = conn(url, "GET")
        val body = readAll(c)
        val code = c.responseCode
        c.disconnect()
        if (code !in 200..299) throw RuntimeException("Get run failed ($code): $body")
        return JSONObject(body)
    }

    fun listArtifacts(runId: Long): JSONArray {
        val url = "https://api.github.com/repos/$owner/$repo/actions/runs/$runId/artifacts"
        val c = conn(url, "GET")
        val body = readAll(c)
        val code = c.responseCode
        c.disconnect()
        if (code !in 200..299) throw RuntimeException("List artifacts failed ($code): $body")
        return JSONObject(body).getJSONArray("artifacts")
    }

    fun downloadArtifactZip(artifactId: Long): BufferedInputStream {
        val url = "https://api.github.com/repos/$owner/$repo/actions/artifacts/$artifactId/zip"
        val c = conn(url, "GET")
        // Важно: stream нужно прочитать до disconnect; поэтому вернём буферизованный поток и не disconnect здесь
        return BufferedInputStream(c.inputStream)
    }

    companion object {
        /** Save bytes to Downloads using MediaStore, return Uri */
        fun saveToDownloads(context: Context, displayName: String, mime: String, bytes: ByteArray): Uri {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw RuntimeException("Cannot create download uri")
            resolver.openOutputStream(uri, "w")!!.use { it.write(bytes) }
            return uri
        }

        /** Extract first .apk from artifact zip stream, save to Downloads, return Uri */
        fun extractApkToDownloads(context: Context, zipStream: BufferedInputStream, apkName: String): Uri {
            ZipInputStream(zipStream).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                        val bytes = zis.readBytes()
                        return saveToDownloads(
                            context = context,
                            displayName = apkName,
                            mime = "application/vnd.android.package-archive",
                            bytes = bytes
                        )
                    }
                }
            }
            throw RuntimeException("No APK found inside artifact zip")
        }
    }
}
