package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.GameViewModel
import com.example.ui.MainHorrorGameView
import com.example.ui.theme.HorrorBlack
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val gameViewModel: GameViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = HorrorBlack
        ) {
          MainHorrorGameView(viewModel = gameViewModel)
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()
    gameViewModel.audioEngine.stop()
    gameViewModel.headTracker.stop()
  }

  override fun onResume() {
    super.onResume()
    if (gameViewModel.isVRMode.value) {
      gameViewModel.headTracker.start()
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Horror 3D") }
}
