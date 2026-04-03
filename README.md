# ex01 Notes App

ex01 is a simple Android notes app for organizing folders, notes, and lists in one place.

It is built with Kotlin, Jetpack Compose, Room, and Navigation Compose.

## What’s new

- Added a Home settings button with a compact bottom-sheet style settings view
- Added light/dark theme switching from settings
- Added autosave when the app is backgrounded or dismissed from the app switcher
- Improved folder visuals in light mode so the containers look cleaner and easier to read
- Kept the app’s edge-to-edge layout, while fixing status bar icon visibility in light mode

## Main features

- Create folders and subfolders
- Create free-text notes and checklist-style lists
- Organize content in a nested folder tree
- Expand or collapse folders and note previews
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

<<<<<<< HEAD
=======
## Notes and layout

- Folders group related notes and lists together
- Notes are used for free-text content
- Lists are used for checklist-style content
- The main screen shows folder cards and note cards with quick actions
- The editor saves your draft when you leave the app, so you do not lose recent changes

## Theme

The app supports two theme modes:
>>>>>>> 4b5d84c (Polish notes app UI and README)

- Light
- Dark

Theme mode can be changed from the settings sheet on the Home screen.

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

If you want to deploy from Windows, you can use:

```powershell
.\deploy-debug.ps1
```

or:

```powershell
.\deploy-debug.cmd
```

## Project details

- App name: `ex01`
- Package / namespace: `com.example.ex01`
- Minimum Android version: API 30

## Development notes

- The app is designed for local use and development.
- The rich-text editor focuses on the visible toolbar actions rather than a full document editor.
- Folder, note, and list layouts are still being refined for consistency and readability.

