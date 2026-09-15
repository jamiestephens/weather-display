package com.example.slideshowdisplay.ui

import android.util.Log
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slideshowdisplay.data.NewsArticle
import com.example.slideshowdisplay.data.WeatherResponse
import java.util.Locale

fun parseNytRss(xmlInput: String): List<NewsArticle> {
    val articles = mutableListOf<NewsArticle>()
    try {
        val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(java.io.StringReader(xmlInput))

        var eventType = parser.eventType
        var insideItem = false
        var currentTag: String? = null
        var currentTitle = ""
        var currentDescription = ""
        var currentPubDate = ""

        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    if (tagName.equals("item", ignoreCase = true)) {
                        insideItem = true
                        currentTitle = ""
                        currentDescription = ""
                        currentPubDate = ""
                    } else if (insideItem) {
                        currentTag = tagName
                    }
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> {
                    if (insideItem && currentTag != null) {
                        val text = parser.text
                        if (text != null) {
                            when (currentTag) {
                                "title" -> currentTitle += text
                                "description" -> currentDescription += text
                                "pubDate" -> currentPubDate += text
                            }
                        }
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                    val tagName = parser.name
                    if (tagName.equals("item", ignoreCase = true)) {
                        insideItem = false
                        articles.add(
                            NewsArticle(
                                title = currentTitle.trim(),
                                description = currentDescription.trim(),
                                pubDate = currentPubDate.trim(),
                                source_name = "New York Times"
                            )
                        )
                    } else if (insideItem && tagName == currentTag) {
                        currentTag = null
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (e: Exception) {
        Log.e("RssParser", "Error parsing RSS XML", e)
    }
    return articles
}

fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "Clear"
        1 -> "Mainly Clear"
        2 -> "Partially Cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Showers"
        95, 96, 99 -> "T-Storm"
        else -> "Cloudy"
    }
}

fun getWeatherIcon(code: Int, isNight: Boolean = false): ImageVector {
    return when (code) {
        0 -> if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny
        1 -> if (isNight) Icons.Default.NightsStay else Icons.Default.CloudQueue
        2 -> if (isNight) Icons.Default.NightsStay else Icons.Default.WbCloudy
        3 -> Icons.Default.Cloud
        61, 63, 65, 80, 81, 82 -> Icons.Default.Umbrella
        else -> Icons.Default.Cloud
    }
}

fun getWindDirection(index: Int): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return directions[index % 8]
}

fun getUVColor(uv: Double): Color {
    if (uv <= 0.0) return Color(0xFFBDC6D1)
    
    val snappedUV = (Math.round(uv * 4) / 4.0)
    
    val anchors = listOf(
        0.0 to Color(0xFFBDC6D1),
        1.0 to Color(0xFF95C158),
        3.0 to Color(0xFFBDD158),
        5.0 to Color(0xFFF1D833),
        6.0 to Color(0xFFF1B433),
        7.0 to Color(0xFFF28F33),
        8.0 to Color(0xFFF26C33),
        11.0 to Color(0xFFE53210),
        13.0 to Color(0xFFB577BD)
    )

    for (i in 0 until anchors.size - 1) {
        val (v1, c1) = anchors[i]
        val (v2, c2) = anchors[i + 1]
        if (snappedUV <= v2) {
            val fraction = ((snappedUV - v1) / (v2 - v1)).toFloat()
            return Color(
                red = c1.red + (c2.red - c1.red) * fraction,
                green = c1.green + (c2.green - c1.green) * fraction,
                blue = c1.blue + (c2.blue - c1.blue) * fraction,
                alpha = 1.0f
            )
        }
    }
    return anchors.last().second
}

fun getUVCategory(uv: Double): String {
    return when {
        uv < 3 -> "Low"
        uv < 6 -> "Moderate"
        uv < 8 -> "High"
        uv < 11 -> "Very High"
        else -> "Extreme"
    }
}

@Composable
fun DashboardRow(icon: ImageVector, label: String, unit: String, data: List<Double>, color: Color, modifier: Modifier = Modifier, isLine: Boolean = false, isBar: Boolean = false, isPrecip: Boolean = false, showGradient: Boolean = false) {
    val textMeasurer = rememberTextMeasurer()
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.width(140.dp).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Gray.copy(0.7f), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (unit.isNotEmpty()) Text(unit, fontSize = 8.sp, color = Color.Gray)
            }
        }
        Box(modifier = Modifier.fillMaxSize().padding(end = 24.dp, top = 20.dp, bottom = 12.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val actualMax = data.maxOrNull() ?: 1.0
                val actualMin = data.minOrNull() ?: 0.0
                val rangeVal = (actualMax - actualMin).coerceAtLeast(1.0)
                
                val minVal = if (isLine && actualMin > 0) actualMin - (rangeVal * 0.3) else if (isLine) actualMin - (rangeVal * 0.15) else actualMin
                val maxVal = if (isLine) actualMax + (rangeVal * 0.2) else actualMax
                val range = (maxVal - minVal).coerceAtLeast(0.1)

                if (isLine) {
                    val stepX = width / 23f
                    val path = Path()
                    
                    data.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = height - ((v - minVal) / range).toFloat() * height
                        if (i == 0) path.moveTo(x, y) else {
                            val prevX = (i - 1) * stepX
                            val prevY = height - ((data[i - 1] - minVal) / range).toFloat() * height
                            path.cubicTo(prevX + (x - prevX) / 2, prevY, prevX + (x - prevX) / 2, y, x, y)
                        }
                    }

                    if (showGradient) {
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }
                        drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(0.3f), Color.Transparent)))
                    }
                    drawPath(path, color, style = Stroke(2.5.dp.toPx()))
                    
                    drawLine(Color.Black.copy(0.08f), Offset(0f, height), Offset(width, height), 1.dp.toPx())
                    drawLine(Color.Black.copy(0.08f), Offset(0f, 0f), Offset(0f, height), 1.2.dp.toPx())

                    data.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = height - ((v - minVal) / range).toFloat() * height
                        if (i % 3 == 0) {
                            drawCircle(color, 2.8.dp.toPx(), Offset(x, y))
                            val labelText = if (isPrecip) "${v.toInt()}%" else "${v.toInt()}°"
                            val measured = textMeasurer.measure(labelText, TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium))
                            drawText(textLayoutResult = measured, color = Color.DarkGray, topLeft = Offset(x - measured.size.width / 2, y - 22.dp.toPx()))
                        }
                    }
                } else if (isBar) {
                    val barWidth = 30.dp.toPx()
                    val innerGap = 3.5.dp.toPx()
                    drawLine(Color.Black.copy(0.08f), Offset(0f, 0f), Offset(0f, height), 1.2.dp.toPx())
                    
                    data.forEachIndexed { i, v ->
                        val hCenter: Int
                        val pOffset: Int
                        
                        if (i <= 1) {
                            hCenter = 0
                            pOffset = i
                        } else if (i == 23) {
                            hCenter = 24
                            pOffset = -1
                        } else {
                            hCenter = 3 * ((i + 1) / 3)
                            pOffset = i - hCenter
                        }
                        
                        val xCenter = (hCenter / 24f) * width
                        val x = xCenter + pOffset * (barWidth + innerGap)
                        
                        val minBarHeight = 8.dp.toPx()
                        val barHeight = minBarHeight + (v / 12.0 * (height * 2.2f - minBarHeight)).toFloat()
                        drawRoundRect(
                            color = getUVColor(v),
                            topLeft = Offset(x - barWidth / 2, height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                        
                        if (pOffset == 0 || (hCenter == 0 && i == 0) || (hCenter == 24 && i == 23)) {
                            val labelText = v.toInt().toString()
                            val measured = textMeasurer.measure(labelText, TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium))
                            drawText(textLayoutResult = measured, color = Color.DarkGray, topLeft = Offset(x - measured.size.width / 2, height - barHeight - 16.dp.toPx()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardConditionsRow(label: String, codes: List<Int>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.width(140.dp).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, null, tint = Color.Gray.copy(0.7f), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxSize().padding(end = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.width(1.dp).fillMaxHeight()) {
                drawLine(Color.Black.copy(0.08f), Offset(0f, 0f), Offset(0f, size.height), 1.2.dp.toPx())
            }
            for (i in codes.indices step 3) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(getWeatherIcon(codes[i]), null, tint = Color(0xFFF18B00), modifier = Modifier.size(26.dp))
                    Text(getWeatherDescription(codes[i]), fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun HeaderDetailItem(icon: ImageVector, color: Color, label: String, value: String, subValue: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (subValue != null) Text(subValue, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailSunItem(icon: ImageVector, label: String, time: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFFF18B00), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(time, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WeatherDetailsPane(modifier: Modifier = Modifier, weather: WeatherResponse) {
    val daylightSec = weather.daily.daylight_duration?.firstOrNull() ?: 0.0
    val hours = (daylightSec / 3600).toInt()
    val minutes = ((daylightSec % 3600) / 60).toInt()
    val daylightText = "${hours}h ${minutes}m"

    Box(modifier = modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            WeatherDetailItem(Icons.Default.Widgets, Color(0xFF3EA72D), "AIR QUALITY", "Good", "32 AQI", modifier = Modifier.weight(1f))
            FaintVerticalDivider()
            WeatherDetailItem(Icons.Default.Eco, Color(0xFF3EA72D), "POLLEN", "Low", modifier = Modifier.weight(1f))
            FaintVerticalDivider()
            WeatherDetailItem(Icons.Default.Nightlight, Color(0xFF607D8B), "MOON PHASE", "Waning Gibbous", "76%", modifier = Modifier.weight(1.5f))
            FaintVerticalDivider()
            WeatherDetailItem(Icons.Default.LightMode, Color(0xFFF18B00), "DAYLIGHT", daylightText, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun WeatherDetailItem(icon: ImageVector, iconTint: Color, label: String, value: String, subValue: String? = null, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subValue != null) Text(subValue, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
fun FaintVerticalDivider() {
    Box(modifier = Modifier.fillMaxHeight(0.6f).width(1.dp).background(Color.LightGray.copy(alpha = 0.3f)))
}

@Composable
fun DailySunFooter(sunrise: String, sunset: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFFF8F9FB), RoundedCornerShape(12.dp)).padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbSunny, null, tint = Color(0xFFF18B00), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(sunrise, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Sunrise", fontSize = 8.sp, color = Color.Gray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbTwilight, null, tint = Color(0xFFF26C33), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(sunset, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Sunset", fontSize = 8.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun WeatherLabelColumn(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxHeight()) {
        Box(modifier = Modifier.weight(4f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) { }
        FaintDivider()
        Box(modifier = Modifier.weight(3f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeviceThermostat, null, tint = Color(0xFF5C6BC0), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Temperature", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("°F", fontSize = 9.sp, color = Color.Gray)
                }
            }
        }
        FaintDivider()
        Box(modifier = Modifier.weight(3f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbSunny, null, tint = Color(0xFFF18B00), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("UV Index", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
        FaintDivider()
        Box(modifier = Modifier.weight(3f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Umbrella, null, tint = Color(0xFF2196F3), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Precipitation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Chance", fontSize = 9.sp, color = Color.Gray)
                }
            }
        }
        FaintDivider()
        Box(modifier = Modifier.weight(3f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cloud, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Conditions", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
    }
}

@Composable
fun UVIndexBars(uvValues: List<Double>, modifier: Modifier = Modifier) {
    val maxUV = uvValues.maxOrNull() ?: 0.0
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "${maxUV.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.height(40.8.dp).padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.8.dp), verticalAlignment = Alignment.Bottom) {
            uvValues.forEach { uv ->
                val minH = 8.0
                val maxH = 36.8
                val barHeight = (minH + (uv / 12.0 * (maxH - minH))).dp
                Box(modifier = Modifier.width(20.dp).height(barHeight).clip(RoundedCornerShape(6.dp)).background(getUVColor(uv)))
            }
        }
        Text(text = getUVCategory(maxUV), fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
    }
}

@Composable
fun TemperatureSparkline(t6a: Double, t12p: Double, t6p: Double, minRange: Double, maxRange: Double, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val orangeColor = Color(0xFFFF9800)
    val samples = listOf(t6a, t12p, t6p)
    
    Canvas(modifier = modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 22.dp)) {
        val width = size.width
        val height = size.height
        val paddedMin = minRange - 5
        val paddedMax = maxRange + 5
        val range = (paddedMax - paddedMin).coerceAtLeast(1.0)
        
        val points = samples.mapIndexed { i, v ->
            val x = (i / 2f) * width
            val y = height - (((v - paddedMin) / range).toFloat() * height)
            Offset(x, y)
        }
        
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            val cp1 = Offset(points[0].x + (points[1].x - points[0].x) * 0.5f, points[0].y)
            val cp2 = Offset(points[0].x + (points[1].x - points[0].x) * 0.5f, points[1].y)
            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, points[1].x, points[1].y)
            
            val cp3 = Offset(points[1].x + (points[2].x - points[1].x) * 0.5f, points[1].y)
            val cp4 = Offset(points[1].x + (points[2].x - points[1].x) * 0.5f, points[2].y)
            cubicTo(cp3.x, cp3.y, cp4.x, cp4.y, points[2].x, points[2].y)
        }
        
        val fillPath = Path().apply {
            addPath(path)
            lineTo(points[2].x, height)
            lineTo(points[0].x, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(orangeColor.copy(alpha = 0.25f), Color.Transparent),
                startY = points.minOf { it.y },
                endY = height
            )
        )
        
        drawPath(path = path, color = orangeColor, style = Stroke(width = 2.dp.toPx()))
        
        points.forEachIndexed { i, p ->
            drawCircle(orangeColor, radius = 3.dp.toPx(), center = p)
            val label = "${samples[i].toInt()}°"
            val measured = textMeasurer.measure(label, TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold))
            drawText(
                textLayoutResult = measured,
                color = Color.DarkGray,
                topLeft = Offset(p.x - measured.size.width / 2, p.y - measured.size.height - 4.dp.toPx())
            )
        }
    }
}

@Composable
fun PrecipitationSparkline(p6a: Int, p12p: Int, p6p: Int, maxRange: Double, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val blueColor = Color(0xFF2196F3)
    val samples = listOf(p6a.toDouble(), p12p.toDouble(), p6p.toDouble())
    
    Canvas(modifier = modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 8.dp)) {
        val width = size.width
        val labelAreaHeight = 16.dp.toPx()
        val height = size.height - labelAreaHeight
        val range = maxRange.coerceAtLeast(1.0)
        
        val points = samples.mapIndexed { i, v ->
            val x = (i / 2f) * width
            val y = height - ((v / range).toFloat() * height)
            Offset(x, y)
        }
        
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val cp1 = Offset(points[i].x + (points[i+1].x - points[i].x) * 0.5f, points[i].y)
                val cp2 = Offset(points[i].x + (points[i+1].x - points[i].x) * 0.5f, points[i+1].y)
                cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, points[i+1].x, points[i+1].y)
            }
        }
        
        val fillPath = Path().apply {
            addPath(path)
            lineTo(points.last().x, height)
            lineTo(points.first().x, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(blueColor.copy(alpha = 0.25f), Color.Transparent),
                startY = points.minOf { it.y },
                endY = height
            )
        )
        
        drawPath(path = path, color = blueColor, style = Stroke(width = 2.dp.toPx()))
        
        val timeLabels = listOf("6 AM", "12 PM", "6 PM")
        points.forEachIndexed { i, p ->
            drawCircle(blueColor, radius = 3.dp.toPx(), center = p)
            val label = "${samples[i].toInt()}%"
            val measured = textMeasurer.measure(label, TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold))
            drawText(
                textLayoutResult = measured,
                color = Color.DarkGray,
                topLeft = Offset(p.x - measured.size.width / 2, p.y - measured.size.height - 4.dp.toPx())
            )
            
            val timeLabel = timeLabels[i]
            val timeMeasured = textMeasurer.measure(timeLabel, TextStyle(fontSize = 8.sp, fontWeight = FontWeight.SemiBold))
            drawText(
                textLayoutResult = timeMeasured,
                color = Color.Gray,
                topLeft = Offset(p.x - timeMeasured.size.width / 2, height + 4.dp.toPx())
            )
        }
    }
}

@Composable
fun WeatherColumn(day: String, date: String, weatherCode: Int, high: Double, low: Double, minTemp: Double, maxTemp: Double, maxPrecip: Double, t6a: Double, t12p: Double, t6p: Double, uv: List<Double>, p6a: Int, p12p: Int, p6p: Int, w6a: Int, w12p: Int, w6p: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxHeight().background(Color.White, RoundedCornerShape(20.dp)).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(4f).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = day.uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF1A1C1E))
                    Text(text = date, fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = getWeatherIcon(weatherCode),
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = Color(0xFFF18B00)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(text = "${high.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF1A1C1E))
                            Text(text = "${low.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4A90E2))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(text = getWeatherDescription(weatherCode), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                }
            }
            FaintDivider()
            Box(modifier = Modifier.weight(3f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                TemperatureSparkline(t6a = t6a, t12p = t12p, t6p = t6p, minRange = minTemp, maxRange = maxTemp)
            }
            FaintDivider()
            Box(modifier = Modifier.weight(3f).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                UVIndexBars(uv)
            }
            FaintDivider()
            Box(modifier = Modifier.weight(3f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                PrecipitationSparkline(p6a = p6a, p12p = p12p, p6p = p6p, maxRange = maxPrecip)
            }
            FaintDivider()
            Box(modifier = Modifier.weight(3f).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    WeatherConditionItem(w6a, true)
                    WeatherConditionItem(w12p, false)
                    WeatherConditionItem(w6p, true)
                }
            }
        }
    }
}

@Composable
fun WeatherConditionItem(code: Int, isNight: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = getWeatherIcon(code, isNight),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isNight) Color(0xFF4A90E2) else Color(0xFFF18B00)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = getWeatherDescription(code).replace(" ", "\n"), 
            fontSize = 8.sp, 
            fontWeight = FontWeight.SemiBold, 
            color = Color.DarkGray, 
            textAlign = TextAlign.Center, 
            lineHeight = 9.sp
        )
    }
}

@Composable
fun FaintDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = Color.Black.copy(alpha = 0.08f))
}
