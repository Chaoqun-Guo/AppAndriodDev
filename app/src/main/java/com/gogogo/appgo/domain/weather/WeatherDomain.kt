package com.gogogo.appgo.domain.weather

import com.gogogo.appgo.model.WeatherDailyForecast
import com.gogogo.appgo.model.WeatherSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

object WeatherDomain {
    fun buildLocationDrivenWeather(
        now: Long,
        latitude: Double?,
        longitude: Double?,
        altitude: Double,
        currentSpeedMps: Double,
        pressureHpa: Float?,
        windDirectionDegree: Int?,
        humidity: Int,
    ): WeatherSnapshot {
        val lat = (latitude ?: 31.23).toFloat()
        val lon = (longitude ?: 121.47).toFloat()
        val tempBase = (20f + (sin(lat / 18f) * 6f) + (cos(lon / 25f) * 4f)).coerceIn(-20f, 42f)
        val windBase = (currentSpeedMps.toFloat() * 0.7f + 1.2f).coerceAtMost(18f)
        val pressure = pressureHpa ?: (1013f - altitude.toFloat() * 0.11f)
        val precip = (humidity - 65).coerceAtLeast(0) / 12f
        val feelsLike = tempBase + (humidity - 55) * 0.03f
        val visibility = (18f - precip * 1.5f).coerceIn(1f, 25f)
        val cloudCover = (humidity + 8).coerceIn(5, 100)
        val uv = estimateUvIndex(now)
        return WeatherSnapshot(
            source = "手机天气服务(定位)",
            temperatureC = tempBase,
            feelsLikeC = feelsLike,
            precipitationMm = precip,
            humidityPercent = humidity,
            pressureHpa = pressure,
            windSpeedMs = windBase,
            windGustMs = (windBase * 1.45f).coerceAtMost(28f),
            windDirectionDegree = windDirectionDegree,
            uvIndex = uv,
            visibilityKm = visibility,
            cloudCoverPercent = cloudCover,
            dewPointC = (tempBase - ((100 - humidity) / 5.0f)),
            sunriseTimeText = estimateSunriseText(now),
            sunsetTimeText = estimateSunsetText(now),
            updateTimeMillis = now,
        )
    }

    fun buildThreeDayForecast(weather: WeatherSnapshot, now: Long): List<WeatherDailyForecast> {
        val baseTemp = weather.temperatureC ?: 20f
        val basePrecip = weather.precipitationMm ?: 0f
        val wind = weather.windSpeedMs ?: 1.5f
        val gust = weather.windGustMs ?: (wind * 1.4f)
        val visibility = weather.visibilityKm ?: 10f
        val baseDirection = weather.windDirectionDegree ?: 0
        return (1..3).map { day ->
            val date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().plusDays(day.toLong())
            val delta = when (day) {
                1 -> 1.5f
                2 -> -0.8f
                else -> 0.4f
            }
            val min = (baseTemp - 4f + delta).coerceIn(-30f, 40f)
            val max = (baseTemp + 4f + delta).coerceIn(-20f, 48f)
            val rain = (basePrecip + day * 0.6f).coerceAtLeast(0f)
            val label = when {
                rain >= 5f -> "中雨"
                rain >= 2f -> "小雨"
                (weather.cloudCoverPercent ?: 0) >= 65 -> "多云"
                else -> "晴"
            }
            WeatherDailyForecast(
                dateText = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                weatherLabel = label,
                minTempC = min,
                maxTempC = max,
                precipitationMm = rain,
                windSpeedMs = (wind + day * 0.4f).coerceAtMost(20f),
                windDirectionDegree = (baseDirection + day * 18) % 360,
                windGustMs = (gust + day * 0.6f).coerceAtMost(30f),
                visibilityKm = (visibility - day * 0.3f).coerceIn(1f, 30f),
            )
        }
    }

    private fun estimateUvIndex(now: Long): Float {
        val local = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime()
        val minutes = local.hour * 60 + local.minute
        val peak = 12 * 60
        val span = 6 * 60f
        val ratio = 1f - (kotlin.math.abs(minutes - peak) / span).coerceAtMost(1f)
        return (ratio * 9.5f).coerceAtLeast(0f)
    }

    private fun estimateSunriseText(now: Long): String {
        val month = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().monthValue
        val hour = when (month) {
            in 5..8 -> 5
            in 3..4, in 9..10 -> 6
            else -> 7
        }
        return "%02d:%02d".format(hour, 30)
    }

    private fun estimateSunsetText(now: Long): String {
        val month = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().monthValue
        val hour = when (month) {
            in 5..8 -> 19
            in 3..4, in 9..10 -> 18
            else -> 17
        }
        return "%02d:%02d".format(hour, 30)
    }
}
