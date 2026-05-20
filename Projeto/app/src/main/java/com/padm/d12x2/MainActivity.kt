package com.padm.d12x2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.padm.d12x2.ui.theme.D12DiceRollerTheme

// Define o conteúdo com o tema e o ecrã principal
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            D12DiceRollerTheme {
                DiceRollerScreen()
            }
        }
    }
}