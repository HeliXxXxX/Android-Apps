package com.helix.flashcards

import android.content.Intent
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
import com.helix.flashcards.ui.*

private const val DUR = 280
private val easing = FastOutSlowInEasing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle share intent
        val sharedText = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        setContent {
            FlashCardsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppNavGraph(sharedText = sharedText)
                }
            }
        }
    }
}

@Composable
fun AppNavGraph(sharedText: String? = null) {
    val nav = rememberNavController()

    // If launched via share, go straight to deck picker
    val startDest = if (sharedText != null) "pick_deck" else "home"

    NavHost(
        navController = nav,
        startDestination = startDest,
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
                onDeckClick = { nav.navigate("deck/${it.id}") },
                onSettings = { nav.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() }, onAbout = { nav.navigate("about") })
        }

        composable("about") {
            AboutScreen(onBack = { nav.popBackStack() })
        }

        // Share flow: pick a deck then create a card with pre-filled text
        composable("pick_deck") {
            DeckPickerScreen(
                onPick = { deckId ->
                    nav.navigate("card/$deckId/new?shared=${java.net.URLEncoder.encode(sharedText ?: "", "UTF-8")}") {
                        popUpTo("pick_deck") { inclusive = true }
                    }
                },
                onCancel = {
                    nav.navigate("home") { popUpTo("pick_deck") { inclusive = true } }
                }
            )
        }

        composable(
            "deck/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
        ) { entry ->
            val deckId = entry.arguments?.getLong("deckId") ?: return@composable
            DeckScreen(
                deckId = deckId,
                onBack = { nav.popBackStack() },
                onStudy = { nav.navigate("study/$deckId") },
                onAddCard = { nav.navigate("card/$deckId/new") },
                onCardClick = { nav.navigate("view/$deckId/${it.id}") }
            )
        }

        composable(
            "view/{deckId}/{cardId}",
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType },
                navArgument("cardId") { type = NavType.LongType }
            )
        ) { entry ->
            val deckId = entry.arguments?.getLong("deckId") ?: return@composable
            val cardId = entry.arguments?.getLong("cardId") ?: return@composable
            CardViewScreen(
                deckId = deckId,
                cardId = cardId,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("card/$deckId/edit/$cardId") }
            )
        }

        composable(
            "card/{deckId}/new?shared={shared}",
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType },
                navArgument("shared") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val deckId = entry.arguments?.getLong("deckId") ?: return@composable
            val shared = entry.arguments?.getString("shared")?.let {
                java.net.URLDecoder.decode(it, "UTF-8")
            }?.ifBlank { null }
            AddEditCardScreen(deckId = deckId, cardId = null, sharedText = shared, onDone = { nav.popBackStack() })
        }

        composable(
            "card/{deckId}/edit/{cardId}",
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType },
                navArgument("cardId") { type = NavType.LongType }
            )
        ) { entry ->
            val deckId = entry.arguments?.getLong("deckId") ?: return@composable
            val cardId = entry.arguments?.getLong("cardId") ?: return@composable
            AddEditCardScreen(deckId = deckId, cardId = cardId, onDone = { nav.popBackStack() })
        }

        composable(
            "study/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.LongType })
        ) { entry ->
            val deckId = entry.arguments?.getLong("deckId") ?: return@composable
            StudyScreen(deckId = deckId, onBack = { nav.popBackStack() })
        }
    }
}