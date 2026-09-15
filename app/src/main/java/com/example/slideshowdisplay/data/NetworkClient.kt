package com.example.slideshowdisplay.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

interface OpenMeteoApiService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("daily") daily: String = "sunrise,sunset,uv_index_max,temperature_2m_max,temperature_2m_min,weathercode,daylight_duration",
        @Query("hourly") hourly: String = "temperature_2m,precipitation,uv_index,precipitation_probability,weathercode,apparent_temperature,relative_humidity_2m,wind_speed_10m,wind_direction_10m",
        @Query("timezone") timezone: String = "auto",
        @Query("temperature_unit") tempUnit: String = "fahrenheit",
        @Query("wind_speed_unit") windUnit: String = "mph",
        @Query("precipitation_unit") precipUnit: String = "inch"
    ): WeatherResponse
}

interface NewsRssService {
    @GET("services/xml/rss/nyt/HomePage.xml")
    suspend fun getLatestNews(): okhttp3.ResponseBody
}

interface GeocodingApiService {
    @GET("us/{zip}")
    suspend fun getLatLon(@Path("zip") zip: String): ZipCodeResponse
}

object ApiClient {
    private val retrofitWeather = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val weatherApi: OpenMeteoApiService = retrofitWeather.create(OpenMeteoApiService::class.java)

    private val retrofitNews = Retrofit.Builder()
        .baseUrl("https://rss.nytimes.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val newsRss: NewsRssService = retrofitNews.create(NewsRssService::class.java)

    private val retrofitGeo = Retrofit.Builder()
        .baseUrl("https://api.zippopotam.us/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val geoApi: GeocodingApiService = retrofitGeo.create(GeocodingApiService::class.java)
}
