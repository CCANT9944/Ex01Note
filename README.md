# Ex01 Notes App

Ex01 is a personal Android notes app built with Kotlin, Jetpack Compose, Room, and Navigation Compose.

It supports folders, subfolders, notes, lists, collapsing items, list previews, and basic formatting tools.

## What it does

- Create folders, subfolders, lists, and notes
- Organize content in a nested folder tree
- Collapse and expand items
- Rename, delete, and move items
- Preview list and note content on the main screen
- Edit free-text notes and structured lists

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Navigation Compose
- Coroutines / Flow

## Key files

- `app/src/main/java/com/example/ex01/MainActivity.kt` — app entry point and main UI
- `app/src/main/java/com/example/ex01/NoteViewModel.kt` — app state and actions
- `app/src/main/java/com/example/ex01/NoteDatabase.kt` — Room entities, DAO, and database
- `app/src/main/java/com/example/ex01/NoteUi.kt` — reusable UI components
- `app/src/main/java/com/example/ex01/RichTextMarkup.kt` — text formatting helpers
- `app/src/main/java/com/example/ex01/NoteWritingToolbar.kt` — note formatting toolbar

## Requirements

- Android Studio
- Android SDK
- A valid Java installation, or the Android Studio bundled JBR
- An Android phone or emulator

## Build and run

From the project root:

```powershell
.\gradlew.bat assembleDebug --no-daemon
```

If you use the deploy script on Windows:

```powershell
.\deploy-debug.ps1
```

## Backup note

I keep a local restore copy of stable work outside the project folder when needed. That makes it easy to roll back if a change breaks the app.

## Git

If you want to save this state to GitHub:

```powershell
git status
git add README.md
git commit -m "Update README"
git push
```
