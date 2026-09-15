package com.example.slideshowdisplay.data

import com.google.gson.annotations.SerializedName

// --- Open-Meteo API Models ---
data class WeatherResponse(
    val hourly: HourlyData,
    val daily: DailyData,
    val utc_offset_seconds: Int
)

data class HourlyData(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val precipitation: List<Double>,
    val uv_index: List<Double>,
    val precipitation_probability: List<Int>?,
    val weathercode: List<Int>?,
    val apparent_temperature: List<Double>?,
    val relative_humidity_2m: List<Int>?,
    val wind_speed_10m: List<Double>?,
    val wind_direction_10m: List<Int>?
)

data class DailyData(
    val time: List<String>,
    val sunrise: List<String>,
    val sunset: List<String>,
    val uv_index_max: List<Double>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val weathercode: List<Int>?,
    val daylight_duration: List<Double>?
)

// --- NewsData API Models ---
data class NewsResponse(
    val status: String,
    val results: List<NewsArticle>
)

data class NewsArticle(
    val title: String?,
    val description: String?,
    val pubDate: String?,
    val source_name: String?
)

// --- Geocoding API Models ---
data class ZipCodeResponse(
    @SerializedName("post code") val postCode: String,
    val country: String,
    val places: List<Place>
)

data class Place(
    @SerializedName("place name") val placeName: String,
    val longitude: String,
    val latitude: String,
    val state: String
)

data class LocationData(
    val cityName: String,
    val lat: Double,
    val lon: Double
)
