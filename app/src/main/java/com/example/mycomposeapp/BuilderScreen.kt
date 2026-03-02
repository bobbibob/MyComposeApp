package com.example.mycomposeapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BuilderScreen() {

    var appName by remember { mutableStateOf("localai") }
    var description by remember { mutableStateOf("приложение чат с локальной моделью как chatgpt") }
    var status by remember { mutableStateOf("Idle") }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Text("AI Project Builder", style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = appName,
            onValueChange = { appName = it },
            label = { Text("App name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
        )

        Button(
            enabled = !busy,
            onClick = {
                busy = true
                status = "Planning..."
                output = ""

                scope.launch {
                    try {
                        val files = withContext(Dispatchers.IO) {
                            BuilderApi.generatePlan(appName, description)
                        }

                        status = "Generating ${files.size} files..."

                        withContext(Dispatchers.IO) {
                            files.forEach { path ->
                                val content = BuilderApi.generateFile(path, appName, description)
                                val file = File("/storage/emulated/0/AI/generated/$path")
                                file.parentFile?.mkdirs()
                                file.writeText(content)
                            }
                        }

                        status = "Done"
                        output = "Generated ${files.size} files in /AI/generated/"

                    } catch (e: Exception) {
                        status = "Failed"
                        output = "Error: ${e.message}"
                    } finally {
                        busy = false
                    }
                }
            }
        ) {
            Text(if (busy) "Working..." else "Generate Project")
        }

        Text("Status: $status")
        OutlinedTextField(
            value = output,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
