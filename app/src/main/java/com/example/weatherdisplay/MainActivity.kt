package com.example.weatherdisplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherSlideshow()
                }
            }
        }
    }
}

@Composable
fun WeatherSlideshow() {
    val viewModel: WeatherViewModel = viewModel()
    
    LaunchedEffect(Unit) {
        viewModel.startAutoRefresh(Config.DEFAULT_LOCATION)
    }

    val screens = listOf<@Composable () -> Unit>(
        { ScreenOne(viewModel) },
        { RadarScreen(viewModel) }
    )
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15000)
            currentIndex = (currentIndex + 1) % screens.size
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        screens[currentIndex]()
    }
}

@Composable
fun ScreenOne(viewModel: WeatherViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (viewModel.errorMessage != null) {
            Text(
                text = viewModel.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (viewModel.forecast.isNotEmpty()) {
            ForecastScreen(viewModel.forecast)
        }
    }
}

@Composable
fun ForecastScreen(forecast: List<ForecastDay>) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        forecast.forEach { day ->
            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(
                        width = 1.dp,
                        color = Color.LightGray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = formatDay(day.date),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = day.icon,
                        fontSize = 80.sp,
                        lineHeight = 90.sp
                    )
                    Text(text = day.description, style = MaterialTheme.typography.bodyLarge)
                    Text(text = day.temp, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

@Composable
fun RadarScreen(viewModel: WeatherViewModel) {
    val lat = viewModel.latitude
    val lon = viewModel.longitude
    val timestamps = viewModel.radarTimestamps
    
    if (lat == 0.0 || timestamps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var frameIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(timestamps) {
        while(true) {
            delay(800)
            frameIndex = (frameIndex + 1) % timestamps.size
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Base Map: Using a local resource for stability.
        // You must add 'base_map.png' to your app/src/main/res/drawable folder.
        Image(
            painter = painterResource(id = R.drawable.base_map),
            contentDescription = "Base Map",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Radar Overlay from RainViewer
        // Fetched dynamically and layered on top of your local base map.
        val timestamp = timestamps[frameIndex]
        val radarUrl = "https://tilecache.rainviewer.com/v2/radar/$timestamp/1200/8/$lat/$lon/2/1_1.png"
        
        AsyncImage(
            model = radarUrl,
            contentDescription = "Radar Overlay",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Info Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(32.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Text("Live Weather Radar", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("Location: ${Config.DEFAULT_LOCATION}", color = Color.LightGray, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

fun formatDay(dateStr: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("EEE\nMMM d"))
    } catch (e: Exception) {
        dateStr
    }
}
