package com.muses.player.nativem1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.muses.player.nativem1.navigation.MusesApp
import com.muses.player.nativem1.theme.SaltTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaltTheme(useDarkTheme = true) {
                MusesApp()
            }
        }
    }
}
