# FlashCards

A minimal dark-themed flashcard app for Android.

## Features
- **Decks** — organize cards into decks (create / rename / delete)
- **Cards** — front & back, each with optional text, a gallery image, or a hand-drawn sketch
- **Study mode** — tap to flip, swipe to answer:
  - → **Right** = I know it (correct)
  - ← **Left** = I don't know (wrong)
  - ↓ **Down** = mastered (removes from rotation)
- **Progress counter** — correct / wrong / mastered / remaining, with an end-of-round score
- **Import / Export** — import cards from a `.txt` file (`front | back` per line), export a deck as `.txt` or share it as `.json`
- **Reset** — brings all cards back into rotation
- **Accent colours** — pick a theme accent in Settings
- No login, fully offline, data stored locally via Room

## Setup

1. Open **Android Studio** (Hedgehog or newer)
2. **File → Open** → select the `FlashCards/` folder
3. Let Gradle sync (it will download dependencies)
4. If prompted for Gradle wrapper, let Android Studio create it
5. **Run** on your phone/tablet or emulator

### Requirements
- Android Studio Hedgehog (2023.1+) or newer
- JDK 17
- Min SDK 26 (Android 8.0)

## Project Structure
```
app/src/main/java/com/helix/flashcards/
├── FlashCardsApp.kt          # Application class
├── MainActivity.kt            # Entry + navigation
├── data/
│   └── Database.kt            # Room entities, DAOs, database
└── ui/
    ├── Theme.kt               # Dark color scheme
    ├── HomeScreen.kt           # Deck list
    ├── DeckScreen.kt           # Cards list per deck
    ├── AddEditCardScreen.kt    # Create/edit cards with images
    └── StudyScreen.kt          # Flip + swipe study mode
```
