package com.helix.budget

import android.app.Application
import com.helix.budget.data.AppDatabase
import com.helix.budget.data.SettingsStore

class BudgetApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
}
