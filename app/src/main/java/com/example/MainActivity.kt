package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.presentation.navigation.AppNavHost
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DuoPlanTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      DuoPlanTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = DarkCanvas
        ) {
          AppNavHost()
        }
      }
    }
  }
}

