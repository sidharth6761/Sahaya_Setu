package com.sid.civilq_1

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController

import com.sid.civilq_1.components.MainScreen
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        Configuration.getInstance().apply {
            load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
            // Optional: Set user agent to prevent getting banned
            userAgentValue = applicationContext.packageName

        }

        setContent {
            val navController = rememberNavController()
            MainScreen(navController)
        }
    }


}