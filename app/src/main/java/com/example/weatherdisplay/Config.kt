package com.example.weatherdisplay

object Config {
    /**
     * The geographic area (Zip Code or City Name).
     * These values are pulled directly from gradle.properties at build time via BuildConfig.
     */
    const val DEFAULT_LOCATION = BuildConfig.DEFAULT_LOCATION
    const val DEFAULT_LAT = BuildConfig.DEFAULT_LAT
    const val DEFAULT_LON = BuildConfig.DEFAULT_LON
}
