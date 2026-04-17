package com.gogogo.appgo.data

import android.content.ContentValues
import android.content.Context
import com.gogogo.appgo.model.Achievement
import com.gogogo.appgo.model.BacktrackRouteSummary
import com.gogogo.appgo.model.BreadcrumbPoint
import com.gogogo.appgo.model.ExerciseType
import com.gogogo.appgo.model.MarkerPoint
import com.gogogo.appgo.model.StatsSummary
import com.gogogo.appgo.model.TrackPoint
import com.gogogo.appgo.model.WorkoutRecord
import com.gogogo.appgo.model.WorkoutSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WorkoutRepository(context: Context) {
    private val dbHelper = AppDatabaseHelper(context)

    fun saveWorkout(
        record: WorkoutRecord,
        trackPoints: List<TrackPoint>,
        markerPoints: List<MarkerPoint>,
    ): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        return try {
            val recordId = db.insert(
                "workout_records",
                null,
                ContentValues().apply {
                    put("exercise_type", record.type.name)
                    put("start_time_millis", record.startTimeMillis)
                    put("end_time_millis", record.endTimeMillis)
                    put("total_distance_meters", record.totalDistanceMeters)
                    put("total_elevation_gain_meters", record.totalElevationGainMeters)
                    put("average_heart_rate", record.averageHeartRate)
                    put("track_file_path", record.trackFilePath)
                }
            )

            trackPoints.forEach { point ->
                db.insert(
                    "track_points",
                    null,
                    ContentValues().apply {
                        put("workout_id", recordId)
                        put("timestamp_millis", point.timestampMillis)
                        put("latitude", point.latitude)
                        put("longitude", point.longitude)
                        put("altitude", point.altitude)
                        put("heart_rate", point.heartRate)
                        put("speed_mps", point.speedMps)
                    }
                )
            }

            markerPoints.forEach { marker ->
                db.insert(
                    "marker_points",
                    null,
                    ContentValues().apply {
                        put("workout_id", recordId)
                        put("marker_type", marker.type)
                        put("latitude", marker.latitude)
                        put("longitude", marker.longitude)
                        put("photo_path", marker.photoPath)
                        put("note", marker.note)
                    }
                )
            }

            db.setTransactionSuccessful()
            recordId
        } finally {
            db.endTransaction()
        }
    }

    fun loadWorkoutSummaries(): List<WorkoutSummary> {
        val db = dbHelper.readableDatabase
        val result = mutableListOf<WorkoutSummary>()
        val cursor = db.rawQuery(
            """
            SELECT r.id, r.exercise_type, r.start_time_millis, r.end_time_millis,
                   r.total_distance_meters, r.total_elevation_gain_meters,
                   r.average_heart_rate, r.track_file_path,
                   COUNT(t.id) AS track_count
            FROM workout_records r
            LEFT JOIN track_points t ON r.id = t.workout_id
            GROUP BY r.id
            ORDER BY r.start_time_millis DESC
            """.trimIndent(),
            null,
        )

        cursor.use {
            while (it.moveToNext()) {
                val record = WorkoutRecord(
                    id = it.getLong(0),
                    type = ExerciseType.valueOf(it.getString(1)),
                    startTimeMillis = it.getLong(2),
                    endTimeMillis = it.getLong(3),
                    totalDistanceMeters = it.getDouble(4),
                    totalElevationGainMeters = it.getDouble(5),
                    averageHeartRate = if (it.isNull(6)) null else it.getInt(6),
                    trackFilePath = it.getString(7),
                )
                result += WorkoutSummary(record = record, trackPointCount = it.getInt(8))
            }
        }

        return result
    }

    fun loadTrackPoints(workoutId: Long): List<TrackPoint> {
        val db = dbHelper.readableDatabase
        val points = mutableListOf<TrackPoint>()
        val cursor = db.rawQuery(
            """
            SELECT workout_id, timestamp_millis, latitude, longitude, altitude, heart_rate, speed_mps
            FROM track_points
            WHERE workout_id = ?
            ORDER BY timestamp_millis ASC
            """.trimIndent(),
            arrayOf(workoutId.toString()),
        )

        cursor.use {
            while (it.moveToNext()) {
                points += TrackPoint(
                    workoutId = it.getLong(0),
                    timestampMillis = it.getLong(1),
                    latitude = it.getDouble(2),
                    longitude = it.getDouble(3),
                    altitude = it.getDouble(4),
                    heartRate = if (it.isNull(5)) null else it.getInt(5),
                    speedMps = it.getDouble(6),
                )
            }
        }
        return points
    }

    fun saveBacktrackRoute(
        routeName: String,
        sourceWorkoutId: Long?,
        nodes: List<BreadcrumbPoint>,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): Long {
        if (nodes.size < 2) return -1L
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        return try {
            val routeId = db.insert(
                "backtrack_routes",
                null,
                ContentValues().apply {
                    put("route_name", routeName)
                    put("created_at_millis", createdAtMillis)
                    if (sourceWorkoutId != null) {
                        put("source_workout_id", sourceWorkoutId)
                    } else {
                        putNull("source_workout_id")
                    }
                }
            )
            nodes.forEachIndexed { index, node ->
                db.insert(
                    "backtrack_nodes",
                    null,
                    ContentValues().apply {
                        put("route_id", routeId)
                        put("seq_index", index)
                        put("timestamp_millis", node.timestampMillis)
                        put("latitude", node.latitude)
                        put("longitude", node.longitude)
                    }
                )
            }
            db.setTransactionSuccessful()
            routeId
        } finally {
            db.endTransaction()
        }
    }

    fun loadBacktrackRoutes(limit: Int = 30): List<BacktrackRouteSummary> {
        val db = dbHelper.readableDatabase
        val rows = mutableListOf<BacktrackRouteSummary>()
        val cursor = db.rawQuery(
            """
            SELECT r.id, r.route_name, r.created_at_millis, r.source_workout_id, COUNT(n.id) AS node_count
            FROM backtrack_routes r
            LEFT JOIN backtrack_nodes n ON r.id = n.route_id
            GROUP BY r.id
            ORDER BY r.created_at_millis DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(limit.toString()),
        )
        cursor.use {
            while (it.moveToNext()) {
                rows += BacktrackRouteSummary(
                    id = it.getLong(0),
                    name = it.getString(1),
                    createdAtMillis = it.getLong(2),
                    sourceWorkoutId = if (it.isNull(3)) null else it.getLong(3),
                    nodeCount = it.getInt(4),
                )
            }
        }
        return rows
    }

    fun loadBacktrackNodes(routeId: Long): List<BreadcrumbPoint> {
        val db = dbHelper.readableDatabase
        val rows = mutableListOf<BreadcrumbPoint>()
        val cursor = db.rawQuery(
            """
            SELECT timestamp_millis, latitude, longitude
            FROM backtrack_nodes
            WHERE route_id = ?
            ORDER BY seq_index ASC
            """.trimIndent(),
            arrayOf(routeId.toString()),
        )
        cursor.use {
            while (it.moveToNext()) {
                rows += BreadcrumbPoint(
                    timestampMillis = it.getLong(0),
                    latitude = it.getDouble(1),
                    longitude = it.getDouble(2),
                )
            }
        }
        return rows
    }

    fun statsForDays(days: Long, nowMillis: Long = System.currentTimeMillis()): StatsSummary {
        val zone = ZoneId.systemDefault()
        val endDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val startDate = endDate.minusDays(days - 1)
        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        return statsBetween(startMillis, endMillis)
    }

    fun statsForYear(nowMillis: Long = System.currentTimeMillis()): StatsSummary {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val start = LocalDate.of(date.year, 1, 1)
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = start.plusYears(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return statsBetween(startMillis, endMillis)
    }

    fun loadAchievements(): Set<Achievement> {
        val all = loadWorkoutSummaries()
        val totalDistance = all.sumOf { it.record.totalDistanceMeters }
        val totalAscent = all.sumOf { it.record.totalElevationGainMeters }
        val maxSingle = all.maxOfOrNull { it.record.totalDistanceMeters } ?: 0.0

        return buildSet {
            if (maxSingle >= 10_000) add(Achievement.FIRST_10K)
            if (totalAscent >= 1_000) add(Achievement.TOTAL_ASCENT_1000)
            if (totalDistance >= 100_000) add(Achievement.TOTAL_DISTANCE_100K)
            if (all.size >= 10) add(Achievement.TOTAL_WORKOUTS_10)
        }
    }

    private fun statsBetween(startMillis: Long, endMillis: Long): StatsSummary {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT
                COALESCE(SUM(total_distance_meters), 0),
                COALESCE(SUM(end_time_millis - start_time_millis), 0),
                COALESCE(SUM(total_elevation_gain_meters), 0)
            FROM workout_records
            WHERE start_time_millis BETWEEN ? AND ?
            """.trimIndent(),
            arrayOf(startMillis.toString(), endMillis.toString()),
        )

        cursor.use {
            if (!it.moveToFirst()) {
                return StatsSummary(0.0, 0, 0.0, 0)
            }

            val distance = it.getDouble(0)
            val durationMillis = it.getLong(1)
            val elevation = it.getDouble(2)
            val calories = ((distance / 1000.0) * 55).toInt()
            return StatsSummary(
                totalDistanceMeters = distance,
                totalDurationSeconds = durationMillis / 1000,
                totalElevationGainMeters = elevation,
                estimatedCalories = calories,
            )
        }
    }
}
