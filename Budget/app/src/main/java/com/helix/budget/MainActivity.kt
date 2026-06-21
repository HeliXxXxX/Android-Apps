package com.helix.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.helix.budget.ui.*

private const val DUR = 280
private val easing = FastOutSlowInEasing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppNavGraph()
                }
            }
        }
    }
}

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = {
            slideInHorizontally(tween(DUR, easing = easing)) { it / 4 } + fadeIn(tween(DUR))
        },
        exitTransition = {
            slideOutHorizontally(tween(DUR, easing = easing)) { -it / 6 } + fadeOut(tween(DUR / 2))
        },
        popEnterTransition = {
            slideInHorizontally(tween(DUR, easing = easing)) { -it / 6 } + fadeIn(tween(DUR))
        },
        popExitTransition = {
            slideOutHorizontally(tween(DUR, easing = easing)) { it / 4 } + fadeOut(tween(DUR / 2))
        }
    ) {
        composable("home") {
            HomeScreen(
                onSpend = { nav.navigate("entry/EXPENSE") },
                onAddMoney = { nav.navigate("entry/INCOME") },
                onHistory = { nav.navigate("history") },
                onSettings = { nav.navigate("settings") }
            )
        }

        composable(
            "entry/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { entry ->
            val type = entry.arguments?.getString("type") ?: "EXPENSE"
            QuickEntryScreen(
                initialType = type,
                editId = null,
                onDone = { nav.popBackStack() }
            )
        }

        composable(
            "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            QuickEntryScreen(
                initialType = "EXPENSE",
                editId = id,
                onDone = { nav.popBackStack() }
            )
        }

        composable("history") {
            HistoryScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate("edit/$id") }
            )
        }

        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
