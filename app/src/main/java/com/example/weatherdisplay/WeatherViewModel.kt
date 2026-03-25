package com.example.weatherdisplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
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
    }

    var forecast by mutableStateOf<List<ForecastDay>>(emptyList())
        private set

    var latitude by mutableStateOf(0.0)
        private set
        
    var longitude by mutableStateOf(0.0)
        private set

    var radarTimestamps by mutableStateOf<List<Long>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Starts a background loop to fetch weather data immediately and then every hour.
     */
    fun startAutoRefresh(query: String) {
        viewModelScope.launch {
            while (true) {
                fetchWeatherInternal(query)
                // Refresh radar timestamps more frequently (every 10 mins)
                fetchRadarTimestamps()
                delay(3600_000) // Main refresh every hour
            }
        }
        
        // Also start a separate loop just for radar timestamps
        viewModelScope.launch {
            while(true) {
                delay(600_000) // 10 minutes
                fetchRadarTimestamps()
            }
        }
    }

    private suspend fun fetchRadarTimestamps() {
        try {
            val response: RainViewerResponse = client.get("https://api.rainviewer.com/public/weather-maps.json").body()
            // Take the last 10 past frames for a good loop
            radarTimestamps = response.radar.past.takeLast(10).map { it.time }
        } catch (e: Exception) {
            // Silently fail for background updates if we already have data
        }
    }

    private suspend fun fetchWeatherInternal(query: String) {
        if (forecast.isEmpty()) {
            isLoading = true
        }
        errorMessage = null
        
        try {
            // 1. Geocoding
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$query&count=1&language=en&format=json"
            val geoResponse: GeocodingResponse = client.get(geoUrl).body()
            
            val location = geoResponse.results?.firstOrNull()
            if (location == null) {
                errorMessage = "Location not found"
                isLoading = false
                return
            }

            latitude = location.latitude
            longitude = location.longitude

            // 2. Weather: Using weather_code in the request
            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&daily=weather_code,temperature_2m_max&temperature_unit=fahrenheit&timezone=auto"
            val weatherResponse: WeatherResponse = client.get(weatherUrl).body()

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
                errorMessage = "Error: ${e.localizedMessage}"
            }
        } finally {
            isLoading = false
        }
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
