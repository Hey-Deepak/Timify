package com.streamliners.timify.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String = "",
    val icon: ImageVector? = null
) {
    // Bottom nav tabs
    data object Home : Screen("Home", "Home", Icons.Default.Home)
    data object Voice : Screen("Voice", "Voice", Icons.Default.KeyboardVoice)
    data object Stats : Screen("Stats", "Stats", Icons.Default.PieChart)
    data object Insights : Screen("Insights", "Insights", Icons.Default.Insights)

    // Legacy (kept for backwards compatibility during migration)
    data object Chat : Screen("Chat")
    data object PieChart : Screen("PieChart")
    data object SheetSync : Screen("SheetSync")

    companion object {
        val bottomNavItems = listOf(Home, Voice, Stats, Insights)
    }
}
