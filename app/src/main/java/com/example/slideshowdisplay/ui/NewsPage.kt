package com.example.slideshowdisplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slideshowdisplay.data.NewsArticle
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NewsPage(articles: List<NewsArticle>) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF1F6FC)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (articles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No news available", color = Color.Gray)
            }
        } else {
            articles.forEach { article ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = article.title ?: "No Title",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E),
                            maxLines = 2
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = article.description ?: "No Description available",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 3
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = article.source_name ?: "Unknown Source",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                            Text(" • ", color = Color.LightGray, fontSize = 11.sp)
                            val cleanDate = remember(article.pubDate) {
                                if (article.pubDate.isNullOrEmpty()) "" else {
                                    try {
                                        val sourceFormatter = DateTimeFormatter.RFC_1123_DATE_TIME
                                        val targetFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy", Locale.US)
                                        val zonedDateTime = ZonedDateTime.parse(article.pubDate, sourceFormatter)
                                        zonedDateTime.format(targetFormatter)
                                    } catch (e: Exception) {
                                        article.pubDate.substringBefore(":")
                                            .substringBeforeLast(" ")
                                            .trim()
                                    }
                                }
                            }
                            Text(
                                text = cleanDate,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
