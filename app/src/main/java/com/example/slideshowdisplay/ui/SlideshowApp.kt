package com.example.slideshowdisplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.slideshowdisplay.data.WeatherResponse
import com.example.slideshowdisplay.data.NewsArticle
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun SlideshowAppContainer(viewModel: SlideshowViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    val savedZipCode = viewModel.savedZipCode
    val locationData = viewModel.locationData
    val weatherData = viewModel.weatherData
    val newsArticles = viewModel.newsArticles
    val lastUpdateInstant = viewModel.lastUpdateInstant
    val error = viewModel.error

    if (savedZipCode == null) {
        var inputZip by remember { mutableStateOf("") }
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF1F6FC)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(24.dp).widthIn(max = 400.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Enter Zip Code",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C1E)
                    )
                    Text(
                        text = "Please provide your zip code to display local weather details.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = inputZip,
                        onValueChange = { inputZip = it.take(5).filter { char -> char.isDigit() } },
                        label = { Text("Zip Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (inputZip.length == 5) {
                                viewModel.updateZipCode(context, inputZip)
                            }
                        },
                        enabled = inputZip.length == 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        if (error != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F6FC)), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Button(onClick = {
                        viewModel.resetZipCode(context)
                    }) {
                        Text("Reset Zip Code")
                    }
                }
            }
        } else if (locationData == null || weatherData == null) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F6FC)), contentAlignment = Alignment.Center) {
                Text("Loading data...")
            }
        } else {
            val loc = locationData!!
            val weather = weatherData!!
            SlideshowApp(
                cityName = loc.cityName,
                lat = loc.lat,
                lon = loc.lon,
                weather = weather,
                news = newsArticles,
                lastUpdate = lastUpdateInstant
            )
        }
    }
}

@Composable
fun SlideshowApp(cityName: String, lat: Double, lon: Double, weather: WeatherResponse, news: List<NewsArticle>, lastUpdate: Instant?) {
    val pageCount = 3
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Centralized location-aware clock
    var now by remember(weather.utc_offset_seconds) { 
        mutableStateOf(Instant.now().atOffset(ZoneOffset.ofTotalSeconds(weather.utc_offset_seconds)).toLocalDateTime()) 
    }

    LaunchedEffect(weather.utc_offset_seconds) {
        while (true) {
            now = Instant.now().atOffset(ZoneOffset.ofTotalSeconds(weather.utc_offset_seconds)).toLocalDateTime()
            delay(60000) // Update every minute
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            val nextPage = (pagerState.currentPage + 1) % pageCount
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF1F6FC)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> WeatherForecastPage(cityName, weather, now)
                    1 -> TodayWeatherPage(cityName, lat, lon, weather, now)
                    2 -> NewsPage(news)
                }
            }

            // Subtle "Last Updated" timestamp right-aligned to match the page margins perfectly across all views
            lastUpdate?.let { instant ->
                val localUpdate = instant.atOffset(ZoneOffset.ofTotalSeconds(weather.utc_offset_seconds)).toLocalDateTime()
                val timeText = localUpdate.format(DateTimeFormatter.ofPattern("h:mm a"))
                Text(
                    text = "Last updated: $timeText",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 24.dp) // Aligns with the 24.dp margin of the inner pages
                )
            }
        }
    }
}
