package com.gogogo.appgo.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DB_NAME,
    null,
    DB_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE workout_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_type TEXT NOT NULL,
                start_time_millis INTEGER NOT NULL,
                end_time_millis INTEGER NOT NULL,
                total_distance_meters REAL NOT NULL,
                total_elevation_gain_meters REAL NOT NULL,
                average_heart_rate INTEGER,
                track_file_path TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE track_points (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                workout_id INTEGER NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                altitude REAL NOT NULL,
                heart_rate INTEGER,
                speed_mps REAL NOT NULL,
                FOREIGN KEY(workout_id) REFERENCES workout_records(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE marker_points (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                workout_id INTEGER NOT NULL,
                marker_type TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                photo_path TEXT,
                note TEXT NOT NULL,
                FOREIGN KEY(workout_id) REFERENCES workout_records(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE backtrack_routes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                route_name TEXT NOT NULL,
                created_at_millis INTEGER NOT NULL,
                source_workout_id INTEGER,
                FOREIGN KEY(source_workout_id) REFERENCES workout_records(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE backtrack_nodes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                route_id INTEGER NOT NULL,
                seq_index INTEGER NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                FOREIGN KEY(route_id) REFERENCES backtrack_routes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS backtrack_routes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    route_name TEXT NOT NULL,
                    created_at_millis INTEGER NOT NULL,
                    source_workout_id INTEGER,
                    FOREIGN KEY(source_workout_id) REFERENCES workout_records(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS backtrack_nodes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    route_id INTEGER NOT NULL,
                    seq_index INTEGER NOT NULL,
                    timestamp_millis INTEGER NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    FOREIGN KEY(route_id) REFERENCES backtrack_routes(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
        }
    }

    companion object {
        private const val DB_NAME = "outdoor_sport.db"
        private const val DB_VERSION = 2
    }
}
