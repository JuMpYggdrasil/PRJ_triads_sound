package com.triadssound.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triadssound.model.ButtonDef
import com.triadssound.model.TriadRow
import com.triadssound.model.allTriadRows
import com.triadssound.sound.PianoEngine

private val rootColor = Color(0xFF42A5F5)
private val thirdBemolColor = Color(0xFFAB47BC)
private val thirdColor = Color(0xFF66BB6A)
private val fifthColor = Color(0xFFFFA726)
private val headerBg = Color(0xFF1A1A2E)
private val rowBg = Color(0xFF16213E)
private val rowBgAlt = Color(0xFF0F3460)

@Composable
fun TriadScreen(engine: PianoEngine) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        stickyHeader {
            HeaderRow()
        }

        itemsIndexed(allTriadRows) { index, triad ->
            TriadRow(
                triad = triad,
                bg = if (index % 2 == 0) rowBg else rowBgAlt,
                engine = engine
            )
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBg)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell("Root", rootColor)
        HeaderCell("3b", thirdBemolColor)
        HeaderCell("3", thirdColor)
        HeaderCell("5th", fifthColor)
    }
}

@Composable
private fun HeaderCell(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun TriadRow(triad: TriadRow, bg: Color, engine: PianoEngine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        triad.buttons.forEach { btn ->
            NoteButton(
                def = btn,
                engine = engine,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
private fun NoteButton(
    def: ButtonDef,
    engine: PianoEngine,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val baseColor = when (def.role) {
        "Root" -> rootColor
        "3b" -> thirdBemolColor
        "3" -> thirdColor
        else -> fifthColor
    }

    val bgColor = if (isPressed) baseColor.mix(Color.White, 0.4f) else baseColor.copy(alpha = 0.85f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, baseColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .pointerInput(def.midiNumber) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    engine.noteOn(def.midiNumber)
                    try {
                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val current = event.changes.find { it.id == pointerId }
                            if (current == null || !current.pressed) break
                        }
                    } finally {
                        isPressed = false
                        engine.noteOff(def.midiNumber)
                    }
                }
            }
    ) {
        Text(
            text = def.display,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun Color.mix(other: Color, fraction: Float): Color {
    val r = red * (1 - fraction) + other.red * fraction
    val g = green * (1 - fraction) + other.green * fraction
    val b = blue * (1 - fraction) + other.blue * fraction
    return Color(r, g, b, alpha)
}
