package com.example.mycomposeapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun BuilderScreen() {
    var appName by remember { mutableStateOf(TextFieldValue("")) }
    var spec by remember { mutableStateOf(TextFieldValue("")) }

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

        Button(
            onClick = { /* подключим на следующем шаге */ },
            enabled = appName.text.trim().isNotEmpty() && spec.text.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate plan (скоро)")
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Дальше: модель сделает PLAN → создаст файлы → push в GitHub → сборка APK.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
