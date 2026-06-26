package com.traincompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.traincompanion.app.data.AppContainer
import com.traincompanion.app.ui.BOTTOM_NAV_ITEMS
import com.traincompanion.app.ui.Destination
import com.traincompanion.app.ui.screens.AlarmsScreen
import com.traincompanion.app.ui.screens.DashboardScreen
import com.traincompanion.app.ui.screens.DocumentsScreen
import com.traincompanion.app.ui.screens.EmergencyScreen
import com.traincompanion.app.ui.screens.LiveStatusScreen
import com.traincompanion.app.ui.screens.PnrScreen
import com.traincompanion.app.ui.screens.SettingsScreen
import com.traincompanion.app.ui.theme.TrainCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = AppContainer.get(this)
        setContent {
            TrainCompanionTheme {
                TrainCompanionRoot(container)
            }
        }
    }
}

@Composable
fun TrainCompanionRoot(container: AppContainer) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                BOTTOM_NAV_ITEMS.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Dashboard.route) {
                DashboardScreen(container) { route -> navController.navigate(route) }
            }
            composable(Destination.LiveStatus.route) { LiveStatusScreen(container) }
            composable(Destination.Pnr.route) { PnrScreen(container) }
            composable(Destination.Documents.route) { DocumentsScreen(container) }
            composable(Destination.Alarms.route) { AlarmsScreen(container) }
            composable(Destination.Emergency.route) { EmergencyScreen(container) }
            composable(Destination.Settings.route) { SettingsScreen(container) }
        }
    }
}
