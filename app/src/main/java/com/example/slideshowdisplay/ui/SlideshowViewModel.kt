package com.example.slideshowdisplay.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.slideshowdisplay.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

class SlideshowViewModel : ViewModel() {
    var savedZipCode by mutableStateOf<String?>(null)
        private set

    var locationData by mutableStateOf<LocationData?>(null)
        private set

    var weatherData by mutableStateOf<WeatherResponse?>(null)
        private set

    var newsArticles by mutableStateOf<List<NewsArticle>>(emptyList())
        private set

    var lastUpdateInstant by mutableStateOf<Instant?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var isLoopRunning = false

    fun init(context: Context) {
        if (savedZipCode == null) {
            val zip = PreferenceManager.getZip(context)
            if (!zip.isNullOrEmpty()) {
                savedZipCode = zip
                loadLocation(zip)
            }
        }
    }

    fun updateZipCode(context: Context, zip: String) {
        PreferenceManager.saveConfig(context, zip)
        savedZipCode = zip
        error = null
        loadLocation(zip)
    }

    fun resetZipCode(context: Context) {
        PreferenceManager.saveConfig(context, "")
        savedZipCode = null
        locationData = null
        weatherData = null
        newsArticles = emptyList()
        error = null
        isLoopRunning = false
    }

    private fun loadLocation(zip: String) {
        viewModelScope.launch {
            try {
                val geoResponse = ApiClient.geoApi.getLatLon(zip)
                val firstPlace = geoResponse.places.firstOrNull()
                if (firstPlace != null) {
                    locationData = LocationData(
                        cityName = firstPlace.placeName,
                        lat = firstPlace.latitude.toDoubleOrNull() ?: 0.0,
                        lon = firstPlace.longitude.toDoubleOrNull() ?: 0.0
                    )
                    startDataLoop()
                } else {
                    error = "Could not resolve zip code: $zip"
                }
            } catch (e: Exception) {
                Log.e("GeoError", "Geocoding failed", e)
                error = "Failed to resolve location."
            }
        }
    }

    private fun startDataLoop() {
        if (isLoopRunning) return
        isLoopRunning = true
        viewModelScope.launch {
            while (isLoopRunning) {
                val loc = locationData ?: break
                try {
                    weatherData = ApiClient.weatherApi.getForecast(loc.lat, loc.lon)
                    val responseBody = ApiClient.newsRss.getLatestNews()
                    val xmlString = responseBody.string()
                    val parsedArticles = parseNytRss(xmlString)
                    newsArticles = parsedArticles.take(4)
                    lastUpdateInstant = Instant.now()
                    error = null
                } catch (e: Exception) {
                    Log.e("FetchError", "Failed to load data", e)
                    if (weatherData == null) {
                        error = "Failed to load dashboard data."
                    }
                }
                delay(2 * 60 * 60 * 1000)
            }
        }
    }
}
