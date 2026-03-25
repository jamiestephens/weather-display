import java.net.URL
import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.example.weatherdisplay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.weatherdisplay"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Pass the location to the app code via BuildConfig
        val loc = project.findProperty("defaultLocation") ?: "90210"
        buildConfigField("String", "DEFAULT_LOCATION", "\"$loc\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // ... (rest of your android block remains the same)
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.coil-kt:coil-compose:2.6.0")
    testImplementation(libs.junit)
}

// SETUP COMMAND: Run this task to download the map automatically
tasks.register("setupMap") {
    group = "setup"
    description = "Downloads a static map image for the configured location."

    doLast {
        val location = project.findProperty("defaultLocation") ?: "90210"
        println("Setting up map for: $location")

        // 1. Geocode the location using Open-Meteo
        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$location&count=1&language=en&format=json"
        val geoJson = URL(geoUrl).readText()
        val geoData = JsonSlurper().parseText(geoJson) as Map<*, *>
        val results = geoData["results"] as? List<*>
        
        if (results == null || results.isEmpty()) {
            throw GradleException("Could not find coordinates for $location")
        }

        val firstResult = results[0] as Map<*, *>
        val lat = firstResult["latitude"]
        val lon = firstResult["longitude"]
        println("Found coordinates: $lat, $lon")

        // 2. Download a high-res static map from CartoDB (Zoom level 10)
        // We'll use a tile-based approach to get a single image centered on the point
        // For a simple setup, we'll download a 1200x800 static image from a free provider
        val mapUrl = "https://static-maps.yandex.ru/1.x/?ll=$lon,$lat&z=10&l=map&size=600,450&scale=2"
        val outputFile = file("src/main/res/drawable/base_map.png")
        
        println("Downloading map from $mapUrl...")
        outputFile.parentFile.mkdirs()
        URL(mapUrl).openStream().use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        // Remove the old XML placeholder if it exists
        val oldXml = file("src/main/res/drawable/base_map.xml")
        if (oldXml.exists()) oldXml.delete()

        println("Success! Map saved to ${outputFile.absolutePath}")
    }
}
