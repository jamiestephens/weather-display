import java.net.URL
import java.net.URLEncoder
import java.net.HttpURLConnection
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
        
        val loc = project.findProperty("defaultLocation")?.toString() ?: "27605"
        val lat = project.findProperty("defaultLat")?.toString() ?: "0.0"
        val lon = project.findProperty("defaultLon")?.toString() ?: "0.0"
        
        buildConfigField("String", "DEFAULT_LOCATION", "\"$loc\"")
        buildConfigField("Double", "DEFAULT_LAT", lat)
        buildConfigField("Double", "DEFAULT_LON", lon)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

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

tasks.register("setupMap") {
    group = "setup"
    description = "Downloads a static map image for the configured location."

    doLast {
        val location = project.findProperty("defaultLocation")?.toString() ?: "27605"
        var lat = project.findProperty("defaultLat")?.toString()?.toDoubleOrNull()
        var lon = project.findProperty("defaultLon")?.toString()?.toDoubleOrNull()

        if (lat == null || lon == null) {
            println("Geocoding location: $location...")
            val encodedLocation = URLEncoder.encode(location, "UTF-8")
            
            // Try Open-Meteo first, then Nominatim as fallback
            val providers = listOf(
                "https://geocoding-api.open-meteo.com/v1/search?name=$encodedLocation&count=1&format=json",
                "https://nominatim.openstreetmap.org/search?q=$encodedLocation&format=json&limit=1"
            )

            for (url in providers) {
                try {
                    println("Attempting geocoding with: $url")
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (WeatherDisplayApp)")
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000

                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                    val data = JsonSlurper().parseText(json)
                    
                    if (url.contains("open-meteo")) {
                        val res = (data as Map<*, *>)["results"] as? List<*>
                        if (res != null && res.isNotEmpty()) {
                            val first = res[0] as Map<*, *>
                            lat = (first["latitude"] as Number).toDouble()
                            lon = (first["longitude"] as Number).toDouble()
                            break
                        }
                    } else {
                        val res = data as List<*>
                        if (res.isNotEmpty()) {
                            val first = res[0] as Map<*, *>
                            lat = (first["lat"] as String).toDouble()
                            lon = (first["lon"] as String).toDouble()
                            break
                        }
                    }
                } catch (e: Exception) {
                    println("Provider failed: ${e.message}")
                }
            }
        }

        if (lat == null || lon == null) {
            throw GradleException("Could not find coordinates. Please set 'defaultLat' and 'defaultLon' manually in gradle.properties.")
        }

        println("Coordinates: $lat, $lon")
        val mapUrl = "https://static-maps.yandex.ru/1.x/?ll=$lon,$lat&z=10&l=map&size=600,450&scale=2"
        val outputFile = file("src/main/res/drawable/base_map.png")
        
        try {
            val conn = URL(mapUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.inputStream.use { input ->
                outputFile.parentFile.mkdirs()
                outputFile.outputStream().use { output -> input.copyTo(output) }
            }
            file("src/main/res/drawable/base_map.xml").delete()
            println("Success! Map saved to ${outputFile.absolutePath}")
        } catch (e: Exception) {
            println("Automated download failed. Please download the map manually:")
            println("URL: $mapUrl")
            println("Save as: src/main/res/drawable/base_map.png")
        }
    }
}
