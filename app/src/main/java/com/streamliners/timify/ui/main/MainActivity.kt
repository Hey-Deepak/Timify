package com.streamliners.timify.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.streamliners.base.BaseActivity
import com.streamliners.base.uiEvent.UiEventDialogs
import com.streamliners.timify.BuildConfig
import com.streamliners.timify.data.local.dao.TaskInfoDao
import com.streamliners.timify.ui.theme.TimifyTheme
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity() {

    override var buildType: String = BuildConfig.BUILD_TYPE

    val taskInfoDao by inject<TaskInfoDao>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TimifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    Scaffold(
                        bottomBar = {
                            // Show bottom nav only for main tabs
                            val showBottomBar = Screen.bottomNavItems.any {
                                currentDestination?.hierarchy?.any { dest ->
                                    dest.route == it.route
                                } == true
                            }
                            if (showBottomBar) {
                                NavigationBar {
                                    Screen.bottomNavItems.forEach { screen ->
                                        NavigationBarItem(
                                            icon = {
                                                screen.icon?.let {
                                                    Icon(it, contentDescription = screen.label)
                                                }
                                            },
                                            label = { Text(screen.label) },
                                            selected = currentDestination?.hierarchy?.any {
                                                it.route == screen.route
                                            } == true,
                                            onClick = {
                                                navController.navigate(screen.route) {
                                                    popUpTo(Screen.Home.route) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Surface(modifier = Modifier.padding(innerPadding)) {
                            NavHostGraph(navController)
                        }
                    }

                    UiEventDialogs()
                }
            }
        }
    }
}
