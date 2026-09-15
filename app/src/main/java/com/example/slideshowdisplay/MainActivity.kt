package com.example.slideshowdisplay

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.slideshowdisplay.data.PreferenceManager
import com.example.slideshowdisplay.ui.SlideshowAppContainer
import com.example.slideshowdisplay.ui.theme.SlideshowDisplayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle dynamic configuration via ADB Intent extras
        val zip = intent.getStringExtra("zip")
        if (zip != null) {
            PreferenceManager.saveConfig(this, zip)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            SlideshowDisplayTheme {
                SlideshowAppContainer()
            }
        }
    }
}
