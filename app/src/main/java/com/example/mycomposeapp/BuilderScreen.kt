package com.example.mycomposeapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BuilderScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var appName by remember { mutableStateOf(TextFieldValue("")) }
    var spec by remember { mutableStateOf(TextFieldValue("")) }

    var isWorking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Idle") }
    var planText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Builder", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = appName,
            onValueChange = { appName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("App name (обязательно)") },
            placeholder = { Text("Например: LocalAI") },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = spec,
            onValueChange = { spec = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("Описание приложения") },
            placeholder = { Text("Опиши, что должно уметь приложение...") }
        )

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val name = appName.text.trim()
                    val s = spec.text.trim()
                    if (name.isEmpty() || s.isEmpty() || isWorking) return@Button

                    isWorking = true
                    status = "Connecting to 127.0.0.1:8080…"
                    planText = ""

                    scope.launch(Dispatchers.IO) {
                        val out = try {
                            status = "Requesting model…"
                            BuilderApi.generatePlan(name, s)
                        } catch (e: Exception) {
                            "Error: ${e.message ?: e::class.java.simpleName}"
                        }

                        withContext(Dispatchers.Main) {
                            planText = out
                            status = if (out.startsWith("HTTP") || out.startsWith("Error")) "Failed" else "Done"
                            isWorking = false
                        }
                    }
                },
                enabled = appName.text.trim().isNotEmpty() && spec.text.trim().isNotEmpty() && !isWorking,
                modifier = Modifier.weight(1f)
            ) { Text(if (isWorking) "Generating…" else "Generate plan") }

            OutlinedButton(
                onClick = {
                    planText = ""
                    status = "Idle"
                },
                enabled = !isWorking,
                modifier = Modifier.weight(1f)
            ) { Text("Clear") }
        }

        Spacer(Modifier.height(10.dp))
        Text("Status: $status", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Plan output", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("plan", planText))
            }, enabled = planText.isNotEmpty()) { Text("Copy") }
        }

        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 280.dp)
        ) {
            Text(
                if (planText.isBlank()) "(пока пусто)" else planText,
                modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Если висит на Connecting — проверь, что llama-server запущен и слушает 127.0.0.1:8080.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
