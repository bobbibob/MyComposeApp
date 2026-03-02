package com.example.mycomposeapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BuilderScreen() {
    var appName by remember { mutableStateOf("localai") }
    var description by remember { mutableStateOf("приложение чат с локальной моделью как chatgpt") }

    var status by remember { mutableStateOf("Idle") }
    var planOutput by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Builder", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = appName,
            onValueChange = { appName = it },
            label = { Text("App name (обязательно)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание приложения") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = !isBusy && appName.trim().isNotEmpty() && description.trim().isNotEmpty(),
                onClick = {
                    isBusy = true
                    status = "Generating..."
                    planOutput = ""
                    scope.launch {
                        try {
                            val out = withContext(Dispatchers.IO) {
                                BuilderApi.generatePlan(appName, description)
                            }
                            planOutput = out
                            status = "Success"
                        } catch (e: Exception) {
                            status = "Failed"
                            planOutput = "Error: ${e.message ?: e.javaClass.simpleName}"
                        } finally {
                            isBusy = false
                        }
                    }
                }
            ) {
                Text(if (isBusy) "Generating..." else "Generate plan")
            }

            OutlinedButton(
                enabled = !isBusy,
                onClick = {
                    status = "Idle"
                    planOutput = ""
                }
            ) { Text("Clear") }
        }

        Text("Status: $status", style = MaterialTheme.typography.bodyMedium)

        Text("Plan output", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = planOutput,
            onValueChange = { planOutput = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            placeholder = { Text("Сначала нажми Generate plan. План появится здесь (JSON).") }
        )

        Text(
            "Если висит на Generating — проверь, что llama-server запущен и слушает 127.0.0.1:8080.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
