# ex01 (Notes App)

A fully-featured Jetpack Compose Android application acting as a highly functional, offline-first Notes app!

## Features
- **Folders & Subfolders**: Organize your notes with a deeply nested folder structure.
- **Rich Note Types**: 
  - **Free-Text Notes**: Write and format your thoughts.
  - **Checklists**: Keep track of tasks with interactive lists.
  - **S-Notes (Canvas)**: Draw, highlight, and type floating text directly onto a visual canvas.
- **App Widgets**: Pin your favorite notes to the home screen for quick access.
- **Theming**: Full Light and Dark mode support, custom folder colors, and dynamic UI elements.
- **Trash/Recycle Bin**: Soft-delete items and restore them later.

## Tech Stack
- **UI**: Jetpack Compose (100% Kotlin)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Local Database**: Room (SQLite) with Kotlin Coroutines & Flows
- **Navigation**: Jetpack Compose Navigation

## Project Structure
The app operates on a clean, domain-driven package structure:
- `data/` - Room database setup, DAOs, Repositories, and Entity models (Note, Folder, NoteItem).
- `ui/screens/` - Main full-screen composables (Home, Folder Detail, Trash).
- `ui/editor/` - Note editing screens (Free-Text, Checklist, S-Note canvas).
- `ui/dialogs/` - Reusable modal dialogs (Create, Rename, Delete, Color Picker).
- `ui/components/` - Reusable UI elements (Cards, TopBars, Grids).
- `ui/theme/` - Color schemes, Typography, and Shapes.
- `widget/` - Android App Widget providers and configuration screens.

## Recent Updates & Optimizations
- **Massive Structural Overhaul**: Reorganized the entire codebase from a flat folder structure into clean, distinct domain packages. This dramatically improves codebase navigation, separation of concerns, and maintainability.
- **Buttery Smooth Transitions**: Completely eliminated frame drops when opening heavy Canvas (S-Note) or Free-Text notes by perfectly deferring heavy Compose rendering and string deserialization until after Jetpack Navigation slide animations finish.
- **Resolved Import Issues**: Automated python scripts executed a full migration of flat explicit imports and switched to wildcard imports (`.*`) dynamically bridging new modules. Unit tests also updated to reflect interface changes (`getAllSNotesOnce()` mocked).
- **Smart Render Culling**: Navigation "pop-back" jank is gone! Implemented strict Diff-verification (updatedNote != currentNote) before committing to Room databases, stopping massive Flow invalidation spikes and IPC App Widget redraws when backing out of a screen without edits.
- **Expanded Color Options:** Introduced light color variants (Light Red, Light Green, Light Blue) and Yellow to the SNote color palette, perfect for use with the new Highlighter tool.
- **Native Direct Text Input:** Radically upgraded the SNote editor by adding a Text tool. You can now tap anywhere directly on the canvas to instantly open your keyboard and type native text directly onto the page without clunky dialog boxes.
- **Smart Text Editing & Alignment:** Text inserted into SNote can be freely edited by simply tapping on it again! The cursor mathematically aligns itself specifically to where you tapped within the text, and newly inserted text auto-snaps its Y-position to adjacent rows to keep your notes perfectly aligned.
- **Multiline Text Wrapping:** Handled Android Canvas limitations by rendering SNote text nodes via native Compose Layout constraints, guaranteeing fluid word-wrapping that prevents text from spilling over the right edge of the screen.
- **Instant Auto-Saving:** Bulletproofed the SNote text insertion engine. Text edits are now instantly serialized and continuously committed to the database on every keystroke, ensuring absolutely zero data loss even if you instantly background or close the app.
- **Widget Theming Enhancements:** Fixed overlapping issues within the Widget Configuration screen during setup, ensuring items correctly pad themselves under the status bar.
- **Lint Cleanup:** Cleaned up several stale unused imports, test files, and suppressed warnings within for a much cleaner zero-warning build output.
