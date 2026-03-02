package com.example.mycomposeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mycomposeapp.ui.theme.MyComposeAppTheme
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

class ChatScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyComposeAppTheme {
                ChatScreenContent()
            }
        }
    }
}

@Composable
fun ChatScreenContent() {
    val messages = remember { mutableStateListOf<Message>() }
    val input = remember { mutableStateOf(TextFieldValue("")) }
    val isLoading = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = input.value,
                onValueChange = { input.value = it },
                placeholder = { Text("Type your message...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )

            Button(
                onClick = { sendMessage() },
                enabled = input.value.text.isNotEmpty() && !isLoading
            ) {
                Text("Send")
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.role == "user"
    val imageRequest = ImageRequest.Builder(context = LocalContext.current)
        .data(message.avatarUrl)
        .build()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .align(if (isUser) Alignment.End else Alignment.Start)
    ) {
        if (!isUser) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        Column(
            modifier = Modifier
                .padding(start = if (isUser) 16.dp else 0.dp)
                .align(if (isUser) Alignment.End else Alignment.Start)
        ) {