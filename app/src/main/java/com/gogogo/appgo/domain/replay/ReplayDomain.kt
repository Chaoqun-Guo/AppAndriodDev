package com.gogogo.appgo.domain.replay

import com.gogogo.appgo.model.DetailReplayDirection
import com.gogogo.appgo.model.DetailReplayPoint
import com.gogogo.appgo.model.TrackPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ReplayDomain {
    fun buildReplayPoints(track: List<TrackPoint>): List<DetailReplayPoint> {
        if (track.isEmpty()) return emptyList()
        val replay = ArrayList<DetailReplayPoint>(track.size)
        var cumulative = 0.0
        track.forEachIndexed { idx, point ->
            if (idx > 0) {
                val prev = track[idx - 1]
                cumulative += haversineMeters(prev.latitude, prev.longitude, point.latitude, point.longitude)
            }
            replay += DetailReplayPoint(
                index = idx,
                latitude = point.latitude,
                longitude = point.longitude,
                altitude = point.altitude,
                cumulativeDistanceMeters = cumulative,
                speedMps = point.speedMps,
                timestampMillis = point.timestampMillis,
                heartRate = point.heartRate,
            )
        }
        return replay
    }

    fun nextIndex(
        current: Int,
        lastIndex: Int,
        direction: DetailReplayDirection,
        isLongTrack: Boolean,
    ): Int {
        val step = if (isLongTrack) 2 else 1
        return when (direction) {
            DetailReplayDirection.FORWARD -> (current + step).coerceAtMost(lastIndex)
            DetailReplayDirection.REVERSE -> (current - step).coerceAtLeast(0)
        }
    }

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

