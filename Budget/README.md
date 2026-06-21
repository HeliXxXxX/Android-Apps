# Budget

A minimal dark-themed money-budgeting app for Android. Log what you spend in
seconds and always know how much you can afford per day.

## Features
- **Balance** — derived from a running ledger of income & expense transactions, so it's always accurate
- **Quick entry** — a built-in calculator keypad to record an expense fast:
  - Type the amount, add a note ("what was it?"), save
  - Supports simple sums like `12 + 5` so you can total a few items at once
- **Add money** — the same screen logs income when more money comes in
- **Budget period** — choose how long your balance must last (1 / 2 / 3 / 4 weeks or custom):
  - Shows a live **per-day allowance** that recalculates every time you spend or add money
  - Counts down the days remaining in the period
- **Today** — a progress bar of what you've spent today vs today's allowance, with "left for today" / "over budget"
- **Quick reference** — what you could spend per day to stretch your balance over 1, 2, or 3 weeks
- **History** — every transaction grouped by day with per-day totals; tap a row to edit or delete
- **Settings** — accent colour, currency symbol, and the budget period
- No login, fully offline, data stored locally via Room

## How money is stored
All amounts are kept as **integer minor units (cents)**, never floating-point,
to avoid rounding errors. Display formatting and a small expression parser live
in [`util/Money.kt`](app/src/main/java/com/helix/budget/util/Money.kt).

## Setup
1. Open **Android Studio** (Hedgehog 2023.1+ or newer)
2. **File → Open** → select the `Budget/` folder
3. Let Gradle sync (it will download dependencies)
4. **Run** on your phone/tablet or emulator

### Requirements
- Android Studio Hedgehog (2023.1+) or newer
- JDK 17
- Min SDK 26 (Android 8.0)

## Project structure
```
app/src/main/java/com/helix/budget/
├── BudgetApp.kt              # Application class (database + settings)
├── MainActivity.kt           # Entry point + navigation graph
├── data/
│   ├── Database.kt           # Room: Transaction entity, DAO, database
│   └── Settings.kt           # Accent, currency, budget period (SharedPreferences)
├── util/
│   ├── Money.kt              # Cents formatting + "12 + 5" expression parsing
│   └── TimeRange.kt          # Today / day-boundary helpers
└── ui/
    ├── Theme.kt              # Dark colour scheme + typography
    ├── Components.kt         # Shared header + layout helpers
    ├── HomeScreen.kt         # Dashboard: balance, today, period, recent
    ├── QuickEntryScreen.kt   # Calculator-style expense/income entry
    ├── HistoryScreen.kt      # All transactions grouped by day
    ├── SettingsScreen.kt     # Accent, currency, period
    └── PeriodDialog.kt       # Set/clear the budget period
```

## Tech
Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room
