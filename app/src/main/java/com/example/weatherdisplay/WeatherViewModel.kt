package com.example.weatherdisplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class WeatherViewModel : ViewModel() {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(DefaultRequest) {
            header("User-Agent", "WeatherDisplayApp/1.0")
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
        }
    }

    var forecast by mutableStateOf<List<ForecastDay>>(emptyList())
        private set

    var latitude by mutableStateOf(0.0)
        private set
        
    var longitude by mutableStateOf(0.0)
        private set

    // var radarTimestamps by mutableStateOf<List<Long>>(emptyList())
    //    private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun startAutoRefresh(query: String) {
        viewModelScope.launch {
            while (true) {
                fetchWeatherInternal(query)
                // fetchRadarTimestamps()
                delay(3600_000) // 1 hour
            }
        }
        
        /*
        viewModelScope.launch {
            while(true) {
                delay(600_000) // 10 minutes
                fetchRadarTimestamps()
            }
        }
        */
    }

    /*
    private suspend fun fetchRadarTimestamps() {
        try {
            val response: RainViewerResponse = client.get("https://api.rainviewer.com/public/weather-maps.json").body()
            radarTimestamps = response.radar.past.takeLast(10).map { it.time }
        } catch (e: Exception) {
            // Background update failed
        }
    }
    */

    private suspend fun fetchWeatherInternal(query: String) {
        if (forecast.isEmpty()) {
            isLoading = true
        }
        errorMessage = null
        
        try {
            val location = geocodeLocation(query)
            
            if (location == null) {
                errorMessage = "Location '$query' not found."
                isLoading = false
                return
            }

            latitude = location.first
            longitude = location.second

            val weatherUrl = "https://api.open-meteo.com/v1/forecast"
            val weatherResponse: WeatherResponse = client.get(weatherUrl) {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("daily", "weather_code,temperature_2m_max")
                parameter("temperature_unit", "fahrenheit")
                parameter("timezone", "auto")
            }.body()

            forecast = weatherResponse.daily.time.take(5).mapIndexed { index, date ->
                ForecastDay(
                    date = date,
                    icon = getWeatherIcon(weatherResponse.daily.weatherCode[index]),
                    description = getWeatherDescription(weatherResponse.daily.weatherCode[index]),
                    temp = "${weatherResponse.daily.maxTemp[index]}°F"
                )
            }
        } catch (e: Exception) {
            if (forecast.isEmpty()) {
                errorMessage = "Network Error: Please check your internet connection."
            }
        } finally {
            isLoading = false
        }
    }

    private suspend fun geocodeLocation(query: String): Pair<Double, Double>? {
        try {
            val response: GeocodingResponse = client.get("https://geocoding-api.open-meteo.com/v1/search") {
                parameter("name", query)
                parameter("count", 1)
                parameter("language", "en")
                parameter("format", "json")
            }.body()
            
            response.results?.firstOrNull()?.let {
                return it.latitude to it.longitude
            }
        } catch (e: Exception) {}

        try {
            val response: List<Map<String, String>> = client.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", query)
                parameter("format", "json")
                parameter("limit", 1)
            }.body()
            
            response.firstOrNull()?.let {
                return it["lat"]!!.toDouble() to it["lon"]!!.toDouble()
            }
        } catch (e: Exception) {}
        
        return null
    }

    private fun getWeatherIcon(code: Int): String {
        return when (code) {
            0 -> "☀️"
            1, 2, 3 -> "🌤️"
            45, 48 -> "🌫️"
            51, 53, 55 -> "🌦️"
            61, 63, 65 -> "🌧️"
            71, 73, 75 -> "❄️"
            95, 96, 99 -> "⛈️"
            else -> "❓"
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            95 -> "Thunderstorm"
            else -> "Unknown"
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
