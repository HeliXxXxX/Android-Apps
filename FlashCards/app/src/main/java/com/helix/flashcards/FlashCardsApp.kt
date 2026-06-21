package com.helix.flashcards

import android.app.Application
import com.helix.flashcards.data.AppDatabase
import com.helix.flashcards.data.SettingsStore

class FlashCardsApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
}