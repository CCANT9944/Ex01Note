# ex01 Notes App

`ex01` is an Android notes app for keeping folders, notes, and lists organized in one place.

It is designed to feel fast and practical: you can open a note, edit it, format it, and trust that your latest changes are saved when you leave the screen.

## What you can do

- Create folders and subfolders to keep content grouped
- Create free-text notes with rich-text formatting tools
- Create checklist-style lists for simple task tracking
- Move, rename, delete, and organize your content from the Home screen
- Collapse or expand folders and previews when you want a cleaner view
- Switch between light and dark themes from settings
- Reopen the last note you were editing when you come back to the app

## Rich-text notes

Free-text notes support:

- Bold
- Italic
- Underline
- Strikethrough
- Bullets
- Indent and outdent

Select the text you want, then tap a toolbar button to apply or remove formatting. The toolbar is designed to work together as one set of note-editing tools.

Bullets work for free-text notes only.

## Notes and saving

- The editor saves recent changes automatically when the app is backgrounded or closed
- The editor keeps the cursor visible above the keyboard when you return to a note
- The notes editor is focused on quick, simple editing rather than a full document editor
- The Home screen shows your folders and notes with quick actions for everyday use

## Theme

The app includes:

- Light mode
- Dark mode

You can toggle the theme from the settings button on the Home screen.

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

- The app is still evolving, and the UI is being refined over time.
- The rich-text editor focuses on the current toolbar actions and note-editing flow.

