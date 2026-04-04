# ex01 Notes App

ex01 is an Android notes app for keeping folders, free-text notes, and lists organized in one place.

It is built with Kotlin, Jetpack Compose, Room, and Navigation Compose.

## What’s new

- A compact settings button now sits on the left side of Home
- Theme switching is faster with a one-tap light/dark toggle in settings
- Drafts are saved automatically when the app is backgrounded or closed
- Free-text notes now support rich-text styling, including bullets
- Folder cards were refined for better readability in light mode
- Edge-to-edge layout remains enabled, with improved status bar visibility in light mode

## Main features

- Create folders and subfolders to keep content grouped
- Create free-text notes and checklist-style lists
- Organize everything in a nested folder tree
- Expand or collapse folders and previews as needed
- Rename, delete, and move folders, notes, and lists
- Preview notes and lists directly from the Home screen
- Edit free-text notes with live rich-text formatting
- Use formatting tools for bold, italic, underline, strikethrough, and bullets
- Keep recent edits safe with automatic draft saving

## Rich-text formatting

Free-text notes support:

- Bold
- Italic
- Underline
- Strikethrough
- Bullets

Formatting is applied from the toolbar while editing a note. Select text, then tap a formatting button to apply or remove it.

## Notes and layout

- Folders group related notes and lists together
- Notes are for free-text content with rich-text styling
- Lists are for checklist-style content and other simple item lists
- The Home screen shows folder cards and note cards with quick actions
- The editor saves your draft when you leave the app, so recent changes are not lost

## Theme

The app supports two theme modes:

- Light
- Dark

You can switch theme mode from the settings panel on the Home screen.

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
- Compile SDK: API 36
- Built with Jetpack Compose, Room, and Navigation Compose

## Development notes

- The app is designed for local use and development.
- The rich-text editor focuses on the visible toolbar actions rather than a full document editor.
- Folder, note, and list layouts are still being refined for consistency and readability.

