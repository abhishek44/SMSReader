package com.smsreader.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smsreader.app.SmsReaderApplication
import com.smsreader.app.ui.analytics.AnalyticsScreen
import com.smsreader.app.ui.analytics.AnalyticsViewModel
import com.smsreader.app.ui.budget.BudgetScreen
import com.smsreader.app.ui.budget.BudgetViewModel
import com.smsreader.app.ui.manage.ManageScreen
import com.smsreader.app.ui.review.ReviewScreen
import com.smsreader.app.ui.review.ReviewViewModel

private data class AppDestination(val route: String, val label: String, val glyph: String)

private val destinations = listOf(
    AppDestination("review", "Review", "R"),
    AppDestination("analytics", "Analytics", "A"),
    AppDestination("budget", "Budget", "B")
)

@Composable
fun SmsReaderApp() {
    val app = LocalContext.current.applicationContext as SmsReaderApplication
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != "manage") {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(destination.glyph) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "review",
            modifier = Modifier.padding(padding)
        ) {
            composable("review") {
                val vm: ReviewViewModel = viewModel(factory = reviewViewModelFactory(app.container))
                ReviewScreen(vm, onNavigateToManage = { navController.navigate("manage") })
            }
            composable("manage") {
                val vm: ReviewViewModel = viewModel(factory = reviewViewModelFactory(app.container))
                ManageScreen(vm, onBack = { navController.popBackStack() })
            }
            composable("analytics") {
                val vm: AnalyticsViewModel = viewModel(factory = analyticsViewModelFactory(app.container))
                AnalyticsScreen(vm)
            }
            composable("budget") {
                val vm: BudgetViewModel = viewModel(factory = budgetViewModelFactory(app.container))
                BudgetScreen(vm)
            }
        }
    }
}
