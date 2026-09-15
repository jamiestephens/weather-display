package com.example.slideshowdisplay.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slideshowdisplay.data.WeatherResponse
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayWeatherPage(cityName: String, lat: Double, lon: Double, weather: WeatherResponse, now: LocalDateTime) {
    val fullDate = remember(now) { now.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())) }
    
    val currentHour = now.hour
    val currentTemp = weather.hourly.temperature_2m.getOrNull(currentHour) ?: 0.0
    val feelsLike = weather.hourly.apparent_temperature?.getOrNull(currentHour) ?: 0.0
    val humidity = weather.hourly.relative_humidity_2m?.getOrNull(currentHour) ?: 0
    val windSpeed = weather.hourly.wind_speed_10m?.getOrNull(currentHour) ?: 0.0
    val windDir = weather.hourly.wind_direction_10m?.getOrNull(currentHour) ?: 0
    val uvIndex = weather.hourly.uv_index.getOrNull(currentHour) ?: 0.0
    val precipProb = weather.hourly.precipitation_probability?.getOrNull(currentHour) ?: 0

    val todayMax = weather.daily.temperature_2m_max.firstOrNull() ?: 0.0
    val todayMin = weather.daily.temperature_2m_min.firstOrNull() ?: 0.0
    val weatherDesc = weather.daily.weathercode?.firstOrNull()?.let { getWeatherDescription(it) } ?: "Clear"

    val sunriseStr = weather.daily.sunrise.firstOrNull() ?: ""
    val sunsetStr = weather.daily.sunset.firstOrNull() ?: ""
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    val sunriseDt = try { LocalDateTime.parse(sunriseStr, isoFormatter) } catch(e: Exception) { null }
    val sunsetDt = try { LocalDateTime.parse(sunsetStr, isoFormatter) } catch(e: Exception) { null }
    val sunriseTime = sunriseDt?.format(timeFormatter) ?: "--:--"
    val sunsetTime = sunsetDt?.format(timeFormatter) ?: "--:--"

    Column(
        modifier = Modifier.fillMaxSize().padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.weight(1.1f).fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp)).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(fullDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(getWeatherIcon(weather.daily.weathercode?.firstOrNull() ?: 0), contentDescription = null, tint = Color(0xFFF18B00), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("${currentTemp.toInt()}°F", fontSize = 42.sp, fontWeight = FontWeight.Bold)
                        Text(weatherDesc, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Feels like ${feelsLike.toInt()}° | ↑ ${todayMax.toInt()}°  ↓ ${todayMin.toInt()}°", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                FaintVerticalDivider()
                HeaderDetailItem(Icons.Default.Umbrella, Color(0xFF2196F3), "PRECIP CHANCE", "$precipProb%")
                FaintVerticalDivider()
                HeaderDetailItem(Icons.Default.Air, Color.Gray, "WIND", "${getWindDirection(windDir)} ${windSpeed.toInt()} mph")
                FaintVerticalDivider()
                HeaderDetailItem(Icons.Default.WaterDrop, Color(0xFF2196F3), "HUMIDITY", "$humidity%")
                FaintVerticalDivider()
                HeaderDetailItem(Icons.Default.WbSunny, Color(0xFFF18B00), "UV INDEX", "${uvIndex.toInt()}", getUVCategory(uvIndex))
                FaintVerticalDivider()
                Column {
                    DetailSunItem(Icons.Default.WbSunny, "SUNRISE", sunriseTime)
                    Spacer(Modifier.height(4.dp))
                    DetailSunItem(Icons.Default.WbTwilight, "SUNSET", sunsetTime)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(3.5f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFE3EFFC), Color(0xFFF1F6FC))))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
        ) {
            HourlyForecastDashboard(weather, now, sunriseDt, sunsetDt)
        }

        WeatherDetailsPane(modifier = Modifier.weight(0.8f), weather = weather)
    }
}

@Composable
fun HourlyForecastDashboard(weather: WeatherResponse, now: LocalDateTime, sunriseDt: LocalDateTime?, sunsetDt: LocalDateTime?) {
    val textMeasurer = rememberTextMeasurer()
    val hourly = weather.hourly
    val currentProgress = (now.hour * 3600 + now.minute * 60 + now.second) / (24f * 3600f)
    val sunriseProgress = sunriseDt?.let { (it.hour * 3600 + it.minute * 60 + it.second) / (24f * 3600f) }
    val sunsetProgress = sunsetDt?.let { (it.hour * 3600 + it.minute * 60 + it.second) / (24f * 3600f) }
    
    val currentTimeLabel = now.format(DateTimeFormatter.ofPattern("h:mm a"))
    
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().padding(start = 140.dp, end = 24.dp).height(50.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                for (i in 0..24 step 3) {
                    val x = (i / 24f) * width
                    val label = if (i == 0 || i == 24) "12 AM" else if (i == 12) "12 PM" else "${i % 12} ${if (i < 12) "AM" else "PM"}"
                    val measured = textMeasurer.measure(label, TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    drawText(textLayoutResult = measured, color = Color.Black, topLeft = Offset(x - measured.size.width / 2, 0f))
                }
                
                sunriseProgress?.let { xP ->
                    val label = sunriseDt?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: ""
                    val measured = textMeasurer.measure(label, TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    drawText(textLayoutResult = measured, color = Color.Red, topLeft = Offset(xP * width - measured.size.width / 2, 22.dp.toPx()))
                }
                sunsetProgress?.let { xP ->
                    val label = sunsetDt?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: ""
                    val measured = textMeasurer.measure(label, TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    drawText(textLayoutResult = measured, color = Color.Red, topLeft = Offset(xP * width - measured.size.width / 2, 22.dp.toPx()))
                }
                
                drawLine(Color.Black.copy(0.15f), Offset(0f, 0f), Offset(0f, height), 2.dp.toPx())
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                DashboardRow(Icons.Default.DeviceThermostat, "Temperature", "°F", hourly.temperature_2m.take(24), Color(0xFFFF9800), Modifier.weight(1f), isLine = true, showGradient = true)
                DashboardRow(Icons.Default.WbSunny, "UV Index", "", hourly.uv_index.take(24), Color(0xFFF18B00), Modifier.weight(1f), isBar = true)
                DashboardRow(Icons.Default.Umbrella, "Precipitation", "Chance", hourly.precipitation_probability?.map { it.toDouble() }?.take(24) ?: List(24){0.0}, Color(0xFF2196F3), Modifier.weight(1f), isLine = true, isPrecip = true, showGradient = true)
                DashboardConditionsRow("Conditions", hourly.weathercode?.take(24) ?: List(24){0}, Modifier.weight(0.8f))
            }

            Canvas(modifier = Modifier.fillMaxSize().padding(start = 140.dp, end = 24.dp)) {
                val width = size.width
                val height = size.height
                val firstThreeRowsHeight = height * (3.0f / 3.8f)
                val lineStart = 20.dp.toPx()
                val lineEnd = firstThreeRowsHeight - 12.dp.toPx()

                sunriseProgress?.let { xP -> 
                    drawLine(
                        color = Color.Red.copy(0.3f), 
                        start = Offset(xP * width, lineStart), 
                        end = Offset(xP * width, lineEnd), 
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    ) 
                }
                sunsetProgress?.let { xP -> 
                    drawLine(
                        color = Color.Red.copy(0.3f), 
                        start = Offset(xP * width, lineStart), 
                        end = Offset(xP * width, lineEnd), 
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    ) 
                }

                val x = currentProgress * width
                drawLine(Color.Blue.copy(0.6f), Offset(x, lineStart), Offset(x, lineEnd), 1.2.dp.toPx())
                
                val bubbleMeasured = textMeasurer.measure(currentTimeLabel, TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                val bWidth = bubbleMeasured.size.width + 12.dp.toPx()
                val bHeight = bubbleMeasured.size.height + 4.dp.toPx()
                
                drawRoundRect(
                    color = Color.Blue.copy(0.85f),
                    topLeft = Offset(x - bWidth / 2, -bHeight / 2 - 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(bWidth, bHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
                drawText(textLayoutResult = bubbleMeasured, color = Color.White, topLeft = Offset(x - bubbleMeasured.size.width / 2, -bHeight / 2 - 4.dp.toPx()))
            }
        }
    }
}

@Composable
fun WeatherForecastPage(cityName: String, weather: WeatherResponse, now: LocalDateTime) {
    val daily = weather.daily
    val hourly = weather.hourly
    
    val forecastStartIndex = if (now.hour >= 23) 1 else 0
    
    val startDate = LocalDate.parse(daily.time.getOrNull(forecastStartIndex) ?: LocalDate.now().toString())
    val endDate = LocalDate.parse(daily.time.getOrNull(forecastStartIndex + 4) ?: daily.time.lastOrNull() ?: LocalDate.now().toString())
    val dateRangeText = "${startDate.format(DateTimeFormatter.ofPattern("MMMM d"))} - ${endDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))}"
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val sunTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    val allTempSamples = (forecastStartIndex until forecastStartIndex + 5).flatMap { i ->
        listOf(
            hourly.temperature_2m.getOrNull(i * 24 + 6) ?: 0.0,
            hourly.temperature_2m.getOrNull(i * 24 + 12) ?: 0.0,
            hourly.temperature_2m.getOrNull(i * 24 + 18) ?: 0.0
        )
    }
    val globalMinTemp = allTempSamples.minOrNull() ?: 0.0
    val globalMaxTemp = allTempSamples.maxOrNull() ?: 100.0

    val allPrecipSamples = (forecastStartIndex until forecastStartIndex + 5).flatMap { i ->
        listOf(
            (hourly.precipitation_probability?.getOrNull(i * 24 + 6) ?: 0).toDouble(),
            (hourly.precipitation_probability?.getOrNull(i * 24 + 12) ?: 0).toDouble(),
            (hourly.precipitation_probability?.getOrNull(i * 24 + 18) ?: 0).toDouble()
        )
    }
    val globalMaxPrecip = allPrecipSamples.maxOrNull()?.coerceAtLeast(20.0) ?: 100.0

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F6FC)).padding(24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text(text = "5 Day Forecast", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1A1C1E)))
                        Text(text = dateRangeText, style = MaterialTheme.typography.headlineSmall, color = Color.Gray)
                    }
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WeatherLabelColumn(modifier = Modifier.weight(0.5f))
                    for (i in forecastStartIndex until forecastStartIndex + 5) {
                        if (i < daily.time.size) {
                            val date = LocalDate.parse(daily.time[i])
                            val dayLabel = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                            val dateLabel = date.format(dateFormatter)
                            
                            val sunriseDt = try { LocalDateTime.parse(daily.sunrise[i], isoFormatter) } catch(e: Exception) { null }
                            val sunsetDt = try { LocalDateTime.parse(daily.sunset[i], isoFormatter) } catch(e: Exception) { null }
                            
                            val uvValues = if (sunriseDt != null && sunsetDt != null) {
                                val startHour = sunriseDt.hour
                                val endHour = sunsetDt.hour
                                val totalDaylightHours = endHour - startHour
                                (0..6).map { s ->
                                    val targetHour = (startHour + (totalDaylightHours * s / 6.0)).toInt().coerceIn(0, 23)
                                    hourly.uv_index.getOrNull(i * 24 + targetHour) ?: 0.0
                                }
                            } else {
                                List(7) { 0.0 }
                            }

                            val t6am = hourly.temperature_2m.getOrNull(i * 24 + 6) ?: 0.0
                            val t12pm = hourly.temperature_2m.getOrNull(i * 24 + 12) ?: 0.0
                            val t6pm = hourly.temperature_2m.getOrNull(i * 24 + 18) ?: 0.0

                            val prob6am = hourly.precipitation_probability?.getOrNull(i * 24 + 6) ?: 0
                            val prob12pm = hourly.precipitation_probability?.getOrNull(i * 24 + 12) ?: 0
                            val prob6pm = hourly.precipitation_probability?.getOrNull(i * 24 + 18) ?: 0
                            
                            val code6am = hourly.weathercode?.getOrNull(i * 24 + 6) ?: 0
                            val code12pm = hourly.weathercode?.getOrNull(i * 24 + 12) ?: 0
                            val code6pm = hourly.weathercode?.getOrNull(i * 24 + 18) ?: 0

                            val dayWeatherCode = daily.weathercode?.getOrNull(i) ?: 0
                            val highTemp = daily.temperature_2m_max.getOrNull(i) ?: 0.0
                            val lowTemp = daily.temperature_2m_min.getOrNull(i) ?: 0.0

                            WeatherColumn(
                                day = dayLabel, 
                                date = dateLabel, 
                                weatherCode = dayWeatherCode,
                                high = highTemp,
                                low = lowTemp,
                                minTemp = globalMinTemp,
                                maxTemp = globalMaxTemp,
                                maxPrecip = globalMaxPrecip,
                                t6a = t6am, 
                                t12p = t12pm, 
                                t6p = t6pm, 
                                uv = uvValues, 
                                p6a = prob6am, 
                                p12p = prob12pm, 
                                p6p = prob6pm, 
                                w6a = code6am,
                                w12p = code12pm,
                                w6p = code6pm,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    for (i in forecastStartIndex until forecastStartIndex + 5) {
                        if (i < daily.time.size) {
                            val sunriseTime = try { LocalDateTime.parse(daily.sunrise[i], isoFormatter).format(sunTimeFormatter) } catch (e: Exception) { "--:--" }
                            val sunsetTime = try { LocalDateTime.parse(daily.sunset[i], isoFormatter).format(sunTimeFormatter) } catch (e: Exception) { "--:--" }
                            DailySunFooter(sunriseTime, sunsetTime, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
