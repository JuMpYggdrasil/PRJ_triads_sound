package com.triadssound

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.triadssound.sound.PianoEngine
import com.triadssound.ui.TriadScreen

class MainActivity : ComponentActivity() {

    private var engine: PianoEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pianoEngine = PianoEngine()
        pianoEngine.start()
        engine = pianoEngine

        setContent {
            DisposableEffect(Unit) {
                onDispose {
                    pianoEngine.stop()
                }
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                TriadScreen(engine = pianoEngine)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        engine?.releaseAll()
    }
}
