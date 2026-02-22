package com.streamliners.timify.ui.main

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.streamliners.base.ext.koinBaseViewModel
import com.streamliners.pickers.date.showDatePickerDialog
import com.streamliners.timify.feature.chat.ChatScreen
import com.streamliners.timify.feature.home.HomeScreen
import com.streamliners.timify.feature.insights.InsightsScreen
import com.streamliners.timify.feature.pieChart.PieChartScreen
import com.streamliners.timify.feature.stats.StatsScreen
import com.streamliners.timify.feature.voiceCapture.VoiceCaptureScreen

@Composable
fun MainActivity.NavHostGraph(
    navController: NavHostController
) {
    NavHost(
        startDestination = Screen.Home.route,
        navController = navController
    ) {

        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                viewModel = koinBaseViewModel()
            )
        }

        composable(Screen.Voice.route) {
            VoiceCaptureScreen(
                navController = navController,
                viewModel = koinBaseViewModel()
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                navController = navController,
                viewModel = koinBaseViewModel()
            )
        }

        composable(Screen.Insights.route) {
            InsightsScreen(
                navController = navController,
                viewModel = koinBaseViewModel()
            )
        }

        // Legacy screens (kept for backwards compatibility)
        composable(Screen.Chat.route) {
            ChatScreen(
                navController = navController,
                viewModel = koinBaseViewModel()
            )
        }

        composable(Screen.PieChart.route) {
            PieChartScreen(
                navController = navController,
                viewModel = koinBaseViewModel(),
                showDatePicker = ::showDatePickerDialog
            )
        }
    }
}
