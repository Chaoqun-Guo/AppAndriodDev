package com.gogogo.appgo.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

fun formatDistance(distanceMeters: Double): String =
    if (distanceMeters >= 1000) "${"%.2f".format(distanceMeters / 1000.0)} km" else "${distanceMeters.roundToInt()} m"

fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

fun formatPace(secondsPerKm: Int): String {
    if (secondsPerKm <= 0) return "--"
    val min = secondsPerKm / 60
    val sec = secondsPerKm % 60
    return "%d'%02d\"/km".format(min, sec)
}

fun formatDateTime(millis: Long): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime())

fun weekDisplay(anchor: LocalDate): String {
    val monday = anchor.minusDays(anchor.dayOfWeek.value.toLong() - 1)
    val sunday = monday.plusDays(6)
    val fmt = DateTimeFormatter.ofPattern("MM-dd")
    return "${monday.format(fmt)} ~ ${sunday.format(fmt)}"
}

fun azimuthDirection(azimuth: Float): String {
    val normalized = ((azimuth % 360) + 360) % 360
    return when (normalized) {
        in 337.5f..360f, in 0f..22.5f -> "北"
        in 22.5f..67.5f -> "东北"
        in 67.5f..112.5f -> "东"
        in 112.5f..157.5f -> "东南"
        in 157.5f..202.5f -> "南"
        in 202.5f..247.5f -> "西南"
        in 247.5f..292.5f -> "西"
        else -> "西北"
    }
}

fun formatMps(speed: Double): String = String.format(Locale.getDefault(), "%.2f m/s", speed)
