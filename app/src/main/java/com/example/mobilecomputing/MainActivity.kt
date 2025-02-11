package com.example.mobilecomputing

import android.os.Bundle
import com.example.mobilecomputing.ui.theme.MobileComputingTheme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            MobileComputingTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "mainScreen", builder = {
                    composable("mainScreen") { MainScreen(SampleData, navController) }
                    composable("settingsScreen") { SettingsScreen(navController) }
                })
            }
        }
    }

   override fun onStop() {
       super.onStop()
       sendNotification()
   }

    private fun sendNotification() {
        val notificationWorkRequest = OneTimeWorkRequestBuilder<AutoNotification>()
            .build()

        WorkManager.getInstance(this).enqueue(notificationWorkRequest)
    }
}



