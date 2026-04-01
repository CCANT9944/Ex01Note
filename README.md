# Ex01 Notes App

A simple Android notes app built with Kotlin, Jetpack Compose, Room, and Navigation Compose.

It supports folders, subfolders, notes, lists, collapsing items, previews, folder actions, and basic note styling.

## Features

- Create folders, subfolders, lists, and notes
- Organize content in nested folder trees
- Expand and collapse folders and note cards
- Rename, delete, and move items
- Preview lists and free-text notes
- List styles:
  - Checklist
  - Bulleted
  - Numbered
- Free-text note editing
- Folder icon color customization
- Drag lists and notes into folders from the main screen

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Navigation Compose
- Coroutines / Flow

## Project Structure

- `app/src/main/java/com/example/ex01/MainActivity.kt` — app entry point, navigation, and main UI
- `app/src/main/java/com/example/ex01/NoteDatabase.kt` — Room entities, DAO, and database
- `app/src/main/java/com/example/ex01/NoteViewModel.kt` — state and data actions
- `app/src/main/java/com/example/ex01/NoteUi.kt` — reusable UI components
- `app/src/main/java/com/example/ex01/RichTextMarkup.kt` — rich text formatting helpers

## Requirements

- Android Studio
- Android SDK
- A valid Java installation or the Android Studio bundled JBR
- An Android phone or emulator for testing

## Build

From the project root:

```powershell
.\gradlew.bat assembleDebug --no-daemon




