package com.example.mycomposeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AppRoot(this)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(activity: ComponentActivity) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Chat", "Builder")

    Column {
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text(title) }
                )
            }
        }
        when (tab) {
            0 -> ChatScreen()
            1 -> BuilderScreen(activity)
        }
    }
}
