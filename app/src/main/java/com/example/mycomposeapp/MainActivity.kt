package com.example.mycomposeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    AppTabs()
                }
            }
        }
    }
}

@Composable
private fun AppTabs() {
    var tab by remember { mutableStateOf(0) }
    val titles = listOf("Chat", "Builder")

    Column {
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(title) }
                )
            }
        }

        when (tab) {
            0 -> ChatScreen()
            1 -> BuilderScreen()
        }
    }
}
