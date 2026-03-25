package com.example.weatherdisplay

object Config {
    /**
     * The geographic area (Zip Code or City Name).
     * This value is now pulled directly from gradle.properties at build time.
     */
    const val DEFAULT_LOCATION = BuildConfig.DEFAULT_LOCATION
}
