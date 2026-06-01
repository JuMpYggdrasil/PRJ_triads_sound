package com.triadssound.model

data class ButtonDef(
    val display: String,
    val midiNumber: Int,
    val role: String
)

data class TriadRow(
    val buttons: List<ButtonDef>
)

val allTriadRows: List<TriadRow> = listOf(
    row("C",48, "Eb",51, "E",52, "G",55),
    row("C#",49, "E",52, "F",53, "G#",56),
    row("D",50, "F",53, "F#",54, "A",57),
    row("Eb",51, "Gb",54, "G",55, "Bb",58),
    row("E",52, "G",55, "G#",56, "B",59),
    row("F",53, "Ab",56, "A",57, "C",60),
    row("F#",54, "A",57, "A#",58, "C#",61),
    row("G",55, "Bb",58, "B",59, "D",62),
    row("Ab",56, "B",59, "C",60, "Eb",63),
    row("A",57, "C",60, "C#",61, "E",64),
    row("Bb",58, "Db",61, "D",62, "F",65),
    row("B",59, "D",62, "D#",63, "F#",66),
)

private fun row(
    rD: String, rM: Int,
    bD: String, bM: Int,
    tD: String, tM: Int,
    fD: String, fM: Int
) = TriadRow(listOf(
    ButtonDef(rD, rM, "Root"),
    ButtonDef(bD, bM, "3b"),
    ButtonDef(tD, tM, "3"),
    ButtonDef(fD, fM, "5th")
))
