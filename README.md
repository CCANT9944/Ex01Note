# ex01 Notes App

`ex01` is a simple Android notes app for keeping folders, notes, and lists organized in one place.

It is built to feel quick and practical: open Home, edit a note, format it, and rely on the app to save your latest changes when you leave the screen.

## What you can do

- Organize content with folders and subfolders
- Write free-text notes with multiple pages and rich-text formatting tools
- Create checklist-style lists for simple task tracking
- Move, rename, delete, and manage items from the Home screen
- Collapse or expand folders and previews for a cleaner view
- Switch between light and dark themes from Settings
- Open to Home on a fresh launch, then continue where you left off when you switch away and come back

## Rich-text editing

Free-text notes support:

- Multiple pages
- Undo
- Bold
- Italic
- Underline
- Strikethrough
- Bullets
- Indent and outdent

Select the text you want, then tap a toolbar button to apply, remove, or undo formatting changes. The full-width toolbar is designed to work together as one note-editing set.

Bullets work for free-text notes only.

## Editing and saving

- Recent changes are saved automatically when the app is backgrounded or closed
- The app starts on Home when launched fresh, but keeps your current screen when you switch apps and return
- The editor keeps the cursor visible above the keyboard when you return to a note
- Typing is grouped into sensible undo steps, so Undo stays useful without stepping through every character
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
- The rich-text editor focuses on the current toolbar actions, undo flow, and note-editing experience.

### Recent Updates

- **Trash Bin added:** A safety net has been added for deleted Notes and Folders. You can now restore or permanently remove items via the new Trash Bin accessible from the App Settings menu.
- **Native Settings Sidebar:** Upgraded the App Settings sidebar menu to use standard Material 3 NavigationDrawerItems for a more native look and feel.
- **Fixed Page Tab Bleeding Bug:** Resolved an issue where text from an old page would bleed onto newly created pages due to internal component recycling. Text inputs now stay firmly contained within their own page tabs.
- **Custom Named Note Pages:** Free-text note pages can now be custom named! Long-press on any page tab to rename or delete the page.
- **Improved Text Selection Area:** Fixed an issue where swiping to highlight text inside a note incorrectly triggered horizontal page navigation.
- **Robust Persistence & Saving:** Fixed a bug to ensure page renames, new pages, and text inputs securely save when the app is minimized or closed.
- **Under-the-Hood Editor Refactoring:** Drastically modularized the internal codebase, splitting the massive `NoteEditScreen` out into separate, lightweight `ChecklistEditor` and `PageBodyEditor` components for better long-term app maintenance.
- **Smooth Screen Transitions:** Navigating between the Home screen, Folders, and Notes now uses seamless slide-in and slide-out animations.
- **Card Expansion Animations:** Expanding and collapsing Folder and Note cards on the Home screen now animates smoothly rather than instantly snapping.
- **Improved UI in Dark Mode:** Fixed the visibility of the "3 dots" (More options) menu icon on Folders and Notes so it no longer blends into the dark background.
- **Enhanced "+" Create Menu:** The floating action button's dropdown menu has been upgraded to a wider, better-aligned design featuring Material icons for Notes, Lists, and Folders, complete with a clean separator between item types.
