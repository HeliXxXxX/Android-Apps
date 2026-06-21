# My Android Apps

A collection of personal Android apps, built with **Kotlin** and **Jetpack Compose**.
Both share the same foundations: a dark, minimal UI with selectable accent colours,
fully offline local storage via **Room**, and no login or tracking.

| App | What it does |
|-----|--------------|
| [**FlashCards**](FlashCards/) | Study with flippable, swipeable flashcards organised into decks. |
| [**Budget**](Budget/) | Track your money — log spending fast and see how much you can spend per day. |

---

## 📇 FlashCards

A dark-themed flashcard app for studying.

- **Decks** of cards (create / rename / delete)
- **Cards** with front & back, each supporting text, a gallery image, or a hand-drawn sketch
- **Study mode** — tap to flip; swipe **right** = correct, **left** = wrong, **down** = mastered (removed from rotation)
- **Progress counter** — correct / wrong / mastered / remaining, with an end-of-round score
- **Import / Export** — import cards from a `.txt` file, export a deck as `.txt` or `.json`
- Fully offline, data stored locally via Room

→ See [FlashCards/README.md](FlashCards/README.md) for details.

## 💰 Budget

A personal money-budgeting app.

- **Balance** tracked from a running list of income & expense transactions
- **Quick entry** — a calculator-style keypad to log what you spent (and what it was) in seconds; supports adding `+` items in one go
- **Add money** from the same place when income comes in
- **Budget period** — set how long your money must last (e.g. 2 weeks) and see a live **per-day allowance** that recalculates as you spend
- **Today** — a progress bar showing what you've spent today vs today's allowance
- **History** — every transaction grouped by day, with daily totals; tap to edit or delete
- Money stored as integer cents to avoid rounding errors. Fully offline via Room.

→ See [Budget/README.md](Budget/README.md) for details.

---

## Building either app

Each app is a standalone Gradle project. To run one:

1. Open **Android Studio** (Hedgehog / 2023.1 or newer)
2. **File → Open** → select the `FlashCards/` *or* `Budget/` folder (open them separately)
3. Let Gradle sync and download dependencies
4. **Run** on an emulator or a connected device

### Requirements
- Android Studio Hedgehog (2023.1+) or newer
- **JDK 17** (Android Studio bundles one — Settings → Build Tools → Gradle → Gradle JDK)
- Min SDK 26 (Android 8.0)

## Tech stack
- Kotlin · Jetpack Compose · Material 3
- Navigation Compose
- Room (local database)
- MVVM-ish, single-Activity architecture

## License
Personal projects — feel free to learn from the code.
