# Ex01 Notes App

Ex01 is an Android notes app built with Kotlin, Jetpack Compose, Room, and Navigation Compose.

It lets you organize content with folders, subfolders, notes, and lists, then expand or collapse items as needed.


## Features

- Create folders, subfolders, notes, and lists
- Organize content in a nested folder tree
- Collapse and expand folders and items
- Rename, delete, and move content
- Preview notes and lists from the main screen
- Edit free-text notes with rich-text formatting

## Rich-text editor

Free-text notes currently support:

- Bold
- Italic
- Underline

Formatting is applied from the in-app toolbar and rendered live inside the editor.

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Navigation Compose
- Coroutines / Flow

## Key files

- `app/src/main/java/com/example/ex01/MainActivity.kt` — app entry point and main screens
- `app/src/main/java/com/example/ex01/NoteViewModel.kt` — app state and actions
- `app/src/main/java/com/example/ex01/NoteDatabase.kt` — Room entities, DAO, and database
- `app/src/main/java/com/example/ex01/NoteUi.kt` — reusable UI components
- `app/src/main/java/com/example/ex01/RichTextMarkup.kt` — rich-text parsing and formatting helpers
- `app/src/main/java/com/example/ex01/RichTextEditorController.kt` — editor state controller
- `app/src/main/java/com/example/ex01/NoteWritingToolbar.kt` — formatting toolbar

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

## Notes

- The app is designed for local use and development.
- The rich-text editor currently focuses on the visible toolbar actions rather than a full document editor feature set.

## Roadmap

- Continue refining the rich-text editor behavior and toolbar UX
- Improve folder, list, and note layout consistency across the app
- Add more small quality-of-life features as the UI stabilizes

