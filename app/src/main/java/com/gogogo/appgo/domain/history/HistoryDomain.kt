package com.gogogo.appgo.domain.history

import com.gogogo.appgo.model.ExerciseType
import com.gogogo.appgo.model.StatsSummary
import com.gogogo.appgo.model.WorkoutSummary
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class HistoryDerivedResult(
    val dailyStats: StatsSummary,
    val weeklyStats: StatsSummary,
    val monthlyStats: StatsSummary,
    val yearlyStats: StatsSummary,
    val customRangeStats: StatsSummary,
    val filteredHistory: List<WorkoutSummary>,
)

object HistoryDomain {
    fun derive(
        history: List<WorkoutSummary>,
        historyFilterDate: LocalDate,
        weekAnchorDate: LocalDate,
        monthAnchor: YearMonth,
        yearAnchor: Int,
        customRangeStartDate: LocalDate,
        customRangeEndDate: LocalDate,
        historyTypeFilter: ExerciseType?,
    ): HistoryDerivedResult {
        val dayStart = historyFilterDate
        val dayEnd = historyFilterDate
        val weekStart = weekAnchorDate.minusDays(weekAnchorDate.dayOfWeek.value.toLong() - 1)
        val weekEnd = weekStart.plusDays(6)
        val monthStart = monthAnchor.atDay(1)
        val monthEnd = monthAnchor.atEndOfMonth()
        val yearStart = LocalDate.of(yearAnchor, 1, 1)
        val yearEnd = LocalDate.of(yearAnchor, 12, 31)
        val customStart = minOf(customRangeStartDate, customRangeEndDate)
        val customEnd = maxOf(customRangeStartDate, customRangeEndDate)

        return HistoryDerivedResult(
            dailyStats = statsInRange(history, dayStart, dayEnd),
            weeklyStats = statsInRange(history, weekStart, weekEnd),
            monthlyStats = statsInRange(history, monthStart, monthEnd),
            yearlyStats = statsInRange(history, yearStart, yearEnd),
            customRangeStats = statsInRange(history, customStart, customEnd),
            filteredHistory = history.filter { summary ->
                toLocalDate(summary.record.startTimeMillis) == historyFilterDate &&
                    (historyTypeFilter == null || summary.record.type == historyTypeFilter)
            },
        )
    }

    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun statsInRange(history: List<WorkoutSummary>, start: LocalDate, end: LocalDate): StatsSummary {
        val filtered = history.filter {
            val d = toLocalDate(it.record.startTimeMillis)
            !d.isBefore(start) && !d.isAfter(end)
        }
        val distance = filtered.sumOf { it.record.totalDistanceMeters }
        val durationSeconds = filtered.sumOf { it.record.duration.seconds }
        val elevation = filtered.sumOf { it.record.totalElevationGainMeters }
        return StatsSummary(
            totalDistanceMeters = distance,
            totalDurationSeconds = durationSeconds,
            totalElevationGainMeters = elevation,
            estimatedCalories = ((distance / 1000.0) * 55).toInt(),
        )
    }
}

