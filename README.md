# Ex01 Notes App

Ex01 is an Android notes app for organizing folders, notes, and lists in one place.

It is built with Kotlin, Jetpack Compose, Room, and Navigation Compose.

## What you can do

- Create folders and subfolders
- Create notes and lists
- Organize content in a nested folder tree
- Expand or collapse folders and items
- Rename, delete, and move folders, notes, and lists
- Preview notes and lists from the main screen
- Edit free-text notes with live rich-text formatting

## Rich-text formatting

Free-text notes support:

- Bold
- Italic
- Underline
- Strikethrough

Formatting is applied from the toolbar while editing a note.

## Theme

The app supports a light note view with white backgrounds and black text.
This keeps notes easy to read and the interface clean.

## How it works

- **Folders** help you group related content
- **Notes** are used for free-text content
- **Lists** are used for checklist-style content
- **Preview mode** lets you quickly view note content before opening it

## Getting started

### Requirements

- Android Studio
- Android SDK
- A valid Java installation, or the bundled JBR from Android Studio
- An Android phone or emulator

### Build and run

From the project root:

```powershell
.\gradlew.bat assembleDebug --no-daemon
```

If you use the deploy script on Windows:

```powershell
.\deploy-debug.ps1
```

or

```powershell
.\deploy-debug.cmd
```

## Notes

- The app is designed for local use and development.
- The rich-text editor currently focuses on the visible toolbar actions rather than a full document editor feature set.

## Project focus

- Keep the note experience simple and readable
- Improve the rich-text editor behavior and toolbar UX
- Improve folder, list, and note layout consistency across the app

