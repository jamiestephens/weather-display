package com.example.weatherdisplay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
)

@Serializable
data class WeatherResponse(
    val daily: DailyData
)

@Serializable
data class DailyData(
    val time: List<String>,
    @SerialName("weather_code")
    val weatherCode: List<Int>,
    @SerialName("temperature_2m_max")
    val maxTemp: List<Double>
)

@Serializable
data class RainViewerResponse(
    val version: String,
    val generated: Long,
    val host: String,
    val radar: RadarData
)

@Serializable
data class RadarData(
    val past: List<RadarFrame>,
    val nowcast: List<RadarFrame>
)

@Serializable
data class RadarFrame(
    val time: Long,
    val path: String
)

data class ForecastDay(
    val date: String,
    val icon: String,
    val description: String,
    val temp: String
)
