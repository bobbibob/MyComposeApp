package com.example.mycomposeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mycomposeapp.LocalModelApi.ApiResponse
import com.example.mycomposeapp.LocalModelApi.ChatMessage
import com.example.mycomposeapp.LocalModelApi.ChatResponse
import com.example.mycomposeapp.LocalModelApi.generateResponse
import com.example.mycomposeapp.LocalModelApi.sendMessage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val isTyping = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = messages.lastOrNull()?.text ?: "",
                onValueChange = { text ->
                    messages.lastOrNull()?.text = text
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.weight(1f),
                enabled = !isTyping,
                onImeAction = { coroutineScope.launch { sendMessage(messages.lastOrNull()?.text ?: "") } }
            )

            Button(
                onClick = { coroutineScope.launch { sendMessage(messages.lastOrNull()?.text ?: "") } },
                enabled = !isTyping
            ) {
                Text("Send")
            }

            Button(
                onClick = { messages.clear() }
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val imageRequest = ImageRequest.Builder(context = LocalContext.current)
        .data(message.imageUrl)
        .build()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .align(if (isUser) Alignment.End else Alignment.Start),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (message.imageUrl != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "User avatar",
                modifier = Modifier.size(48.dp)
            )
        }

        Column