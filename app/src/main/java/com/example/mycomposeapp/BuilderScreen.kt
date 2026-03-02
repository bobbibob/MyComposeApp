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
    var planText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Builder", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = appName,
            onValueChange = { appName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("App name (обязательно)") },
            placeholder = { Text("Например: LocalChat") },
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
                    planText = ""

                    scope.launch(Dispatchers.IO) {
                        val out = try {
                            BuilderApi.generatePlan(name, s)
                        } catch (e: Exception) {
                            "Error: ${e.message ?: e::class.java.simpleName}"
                        }
                        withContext(Dispatchers.Main) {
                            planText = out
                            isWorking = false
                        }
                    }
                },
                enabled = appName.text.trim().isNotEmpty() && spec.text.trim().isNotEmpty() && !isWorking,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isWorking) "Generating…" else "Generate plan")
            }

            OutlinedButton(
                onClick = {
                    planText = ""
                },
                enabled = !isWorking,
                modifier = Modifier.weight(1f)
            ) { Text("Clear") }
        }

        Spacer(Modifier.height(10.dp))

        if (planText.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Plan output", style = MaterialTheme.typography.titleMedium)

                TextButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("plan", planText))
                }) { Text("Copy") }
            }

            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 260.dp)
            ) {
                Text(
                    planText,
                    modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Text(
                "Сначала нажми Generate plan. План появится здесь (JSON).",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
