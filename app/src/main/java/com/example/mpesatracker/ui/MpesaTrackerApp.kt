package com.example.mpesatracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mpesatracker.ui.add.AddTransactionScreen
import com.example.mpesatracker.ui.dashboard.DashboardScreen
import com.example.mpesatracker.ui.history.HistoryScreen
import kotlinx.coroutines.launch

/** The three tabs in the bottom navigation bar. */
private enum class Screen(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Dashboard", Icons.Filled.Home),
    History("history", "History", Icons.AutoMirrored.Filled.List),
    Add("add", "Add", Icons.Filled.Add),
}

/** Switches tab the standard way: one copy of each screen, state kept per tab. */
private fun NavHostController.openTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** The app's frame: top bar, bottom tabs, snackbar messages and the screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaTrackerApp() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Screen.entries.firstOrNull { it.route == backStackEntry?.destination?.route }
        ?: Screen.Dashboard

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (current == Screen.Dashboard) "M-Pesa Tracker" else current.label) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = current == screen,
                        onClick = { navController.openTab(screen) },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    showMessage = showMessage,
                    onSeeAll = { navController.openTab(Screen.History) },
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(showMessage = showMessage)
            }
            composable(Screen.Add.route) {
                AddTransactionScreen(
                    onSaved = {
                        showMessage("Transaction saved")
                        navController.openTab(Screen.Dashboard)
                    },
                )
            }
        }
    }
}
