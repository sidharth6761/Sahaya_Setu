package com.sid.civilq_1

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.sid.civilq_1.Navigation.AppNavigation
import com.sid.civilq_1.ui.theme.CivilQ_1Theme // Ensure you use your project's actual theme name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize OSMDroid Configuration for Map functionality
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPrefs = getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
            Configuration.getInstance().load(applicationContext, sharedPrefs)
            Configuration.getInstance().userAgentValue = packageName
        }

        enableEdgeToEdge()

        setContent {
            // Apply your App Theme wrapper here
            CivilQ_1Theme {
                // 2. Call AppNavigation instead of MainScreen
                // AppNavigation already contains its own rememberNavController()
                // and shared ReportViewModel.
                AppNavigation()
            }
        }
    }
}