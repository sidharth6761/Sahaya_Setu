package com.sid.civilq_1

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.sid.civilq_1.components.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize OSMDroid Configuration
        // We use lifecycleScope to run this on a background thread (Dispatchers.IO)
        // This prevents the "Skipped frames" warning during app startup.
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPrefs = getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
            Configuration.getInstance().load(applicationContext, sharedPrefs)

            // Required: Set user agent to prevent getting banned from OSM servers
            Configuration.getInstance().userAgentValue = packageName
        }

        setContent {
            val navController = rememberNavController()
            // Ensure your MainScreen accepts the navController if needed for your architecture
            MainScreen(navController)
        }
    }
}