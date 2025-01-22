package com.example.mobilecomputing

import android.os.Bundle
import com.example.mobilecomputing.ui.theme.MobileComputingTheme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            MobileComputingTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "MainScreen", builder = {
                    composable("mainScreen") { MainScreen(SampleData, navController) }
                    composable("settingsScreen") { SettingsScreen() }
                    }

                )
            }
        }
    }
}

