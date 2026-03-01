package com.example.mycomposeapp

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

@Composable
fun BuilderScreen(context: Context) {
    var desc by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("") }
    var apkUri by remember { mutableStateOf<Uri?>(null) }

    val main = remember { Handler(Looper.getMainLooper()) }

    fun append(s: String) {
        log += (if (log.isBlank()) "" else "\n") + s
    }

    Column(
        Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Builder", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Описание приложения / фичи") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 6
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it.trim() },
            label = { Text("GitHub Token (PAT)") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(Modifier.height(10.dp))

        Button(
            enabled = !busy && desc.isNotBlank() && token.isNotBlank(),
            onClick = {
                busy = true
                log = ""
                apkUri = null

                thread {
                    try {
                        main.post { append("1) Запрашиваю у локальной модели JSON с файлами...") }
                        val spec = LocalModelApi.requestFilesJson(desc)

                        val commitMsg = spec.optString("commit_message", "AI update")
                        val files = spec.optJSONArray("files") ?: JSONArray()

                        if (files.length() == 0) throw RuntimeException("JSON files[] is empty")

                        val gh = GithubApi(
                            owner = "bobbibob",
                            repo = "MyComposeApp",
                            token = token
                        )

                        main.post { append("2) Загружаю файлы в GitHub (commit)...") }

                        var lastCommitSha = ""
                        for (i in 0 until files.length()) {
                            val f = files.getJSONObject(i)
                            val path = f.getString("path")
                            val content = f.getString("content")

                            val sha = gh.getFileSha(path)
                            lastCommitSha = gh.putFile(
                                path = path,
                                contentUtf8 = content,
                                message = commitMsg,
                                shaIfExists = sha
                            )
                            main.post { append("   - OK: $path") }
                        }

                        main.post { append("3) Жду GitHub Actions: Android Debug APK...") }
                        val runId = waitRunForSha(gh, workflowName = "Android Debug APK", headSha = lastCommitSha)

                        main.post { append("   Run: $runId. Жду завершение...") }
                        waitRunSuccess(gh, runId)

                        main.post { append("4) Скачиваю artifact: app-debug-apk...") }
                        val artifactId = findArtifactId(gh, runId, "app-debug-apk")
                        val zipStream = gh.downloadArtifactZip(artifactId)

                        main.post { append("5) Извлекаю APK и сохраняю в Downloads...") }
                        val uri = GithubApi.extractApkToDownloads(context, zipStream, "LocalChat-latest.apk")

                        main.post {
                            apkUri = uri
                            append("✅ Готово! APK сохранён в Downloads как LocalChat-latest.apk")
                            busy = false
                        }
                    } catch (e: Exception) {
                        main.post {
                            append("❌ Ошибка: ${e.message}")
                            busy = false
                        }
                    }
                }
            }
        ) {
            Text(if (busy) "Работаю..." else "Generate → Build → Download APK")
        }

        Spacer(Modifier.height(12.dp))

        if (apkUri != null) {
            Text("APK готов: $apkUri", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Text("Лог:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(log.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall)
    }
}

private fun waitRunForSha(gh: GithubApi, workflowName: String, headSha: String): Long {
    val deadline = System.currentTimeMillis() + 10 * 60_000L
    while (System.currentTimeMillis() < deadline) {
        val runs = gh.listRuns()
        for (i in 0 until runs.length()) {
            val r = runs.getJSONObject(i)
            if (r.optString("name") == workflowName && r.optString("head_sha") == headSha) {
                return r.getLong("id")
            }
        }
        Thread.sleep(4000)
    }
    throw RuntimeException("Timeout: не нашёл workflow run для commit $headSha")
}

private fun waitRunSuccess(gh: GithubApi, runId: Long) {
    val deadline = System.currentTimeMillis() + 15 * 60_000L
    while (System.currentTimeMillis() < deadline) {
        val r = gh.getRun(runId)
        val status = r.optString("status")
        val conclusion = r.optString("conclusion", "null")
        if (status == "completed") {
            if (conclusion == "success") return
            throw RuntimeException("Actions failed: $conclusion")
        }
        Thread.sleep(5000)
    }
    throw RuntimeException("Timeout: Actions не завершился")
}

private fun findArtifactId(gh: GithubApi, runId: Long, artifactName: String): Long {
    val artifacts = gh.listArtifacts(runId)
    for (i in 0 until artifacts.length()) {
        val a = artifacts.getJSONObject(i)
        if (a.optString("name") == artifactName) return a.getLong("id")
    }
    throw RuntimeException("Artifact not found: $artifactName")
}
