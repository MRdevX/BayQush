package me.mrashidi.bayqush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import me.mrashidi.bayqush.ui.SetupScreen
import me.mrashidi.bayqush.ui.theme.BayQushTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BayQushTheme {
                SetupScreen()
            }
        }
    }
}
