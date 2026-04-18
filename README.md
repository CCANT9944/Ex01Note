# ex01 (Notes App)

A fully-featured Jetpack Compose Android application acting as a highly functional, offline-first Notes app!

## Features
- **Folders & Subfolders**: Organize your notes with a deeply nested folder structure.
- **Rich Note Types**:
  - **Free-Text Notes**: Write and format your thoughts.
  - **Checklists**: Keep track of tasks with interactive lists.
  - **S-Notes (Canvas)**: Draw, highlight, and type floating text directly onto a visual canvas.
- **App Widgets**: Pin your favorite notes to the home screen for quick access.
- **Theming**: Custom folder colors and dynamic UI elements. Light mode focused for optimum readability.
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
- `ui/dialogs/` - Reusable modal dialogs modularized into `CommonDialogs.kt`, `FolderDialogs.kt`, `NoteDialogs.kt`, etc.
- `ui/components/` - Reusable UI elements modularized into `FolderCard.kt`, `NoteCard.kt`, `HomeTopAppBar.kt`, `EmptyStateCard.kt`, etc.
- `ui/theme/` - Color schemes, Typography, and Shapes.
- `widget/` - Android App Widget providers and configuration screens.

## Recent Updates & Optimizations
- **Modular Codebase Strategy (Refactor)**: Successfully eliminated massive monolithic UI files (`AppDialogs.kt`, `NoteComponents.kt`) by extracting and distributing structurally related UI boundaries (cards, top bars, modular dialog chunks) into independent files utilizing custom deterministic recursive AST regex scripts (`split_final.py`). Resolves navigation bottlenecking and dramatically improves overall developer experience.
- **Enhanced Note Visualization & Grid Performance**: Completely redesigned the note cards in the main grid view. Card heights were doubled for better content previews, and their titles and icons were elegantly extracted to sit just above the card bounds. We also replaced the classic 3-dot dropdown menu with an intuitive touch-and-hold (long-press) interaction, harmonizing it with folder behavior. Drastically improved scrolling performance by removing heavy rich-text markdown rendering and dynamic layout animations from the lazy grid.
- **Massive Structural Overhaul**: Reorganized the entire codebase from a flat folder structure into clean, distinct domain packages. This dramatically improves codebase navigation, separation of concerns, and maintainability.
- **Buttery Smooth Transitions**: Completely eliminated frame drops when opening heavy Canvas (S-Note) or Free-Text notes by perfectly deferring heavy Compose rendering and string deserialization until after Jetpack Navigation slide animations finish.
- **Resolved Import Issues**: Automated python scripts executed a full migration of flat explicit imports and switched to wildcard imports (`.*`) dynamically bridging new modules. Unit tests also updated to reflect interface changes (`getAllSNotesOnce()` mocked).
- **Smart Render Culling**: Navigation "pop-back" jank is gone! Implemented strict Diff-verification (updatedNote != currentNote) before committing to Room databases, stopping massive Flow invalidation spikes and IPC App Widget redraws when backing out of a screen without edits.
- **Block-Based Lazy Free-Text Editor**: Refactored the core Free-Text note editor from a monolithic `BasicTextField` (which lagged on massive texts) into a high-performance block-based `LazyColumn` (Notion-style). Text is now dynamically chunked by paragraphs (`\n\n`), drastically improving scrolling and typing framerates by unmounting off-screen text while retaining full Rich-Text formatting capabilities. Added smart `<Enter>` and `<Backspace>` interceptions to split and merge blocks automatically.
- **Keyboard & Scroll Layout Physics**: Eliminated layout measurement lag when the keyboard animates up by removing outer `imePadding` container constraints in favor of a smooth bottom `Spacer` synchronized with `WindowInsets.ime`. Triggering scroll now immediately clears focus, instantly collapsing the keyboard to return full screen real-estate.
- **Checklist Editor Enhancements**: Fixed keyboard autoscroll logic in the Checklist view, ensuring the newly added item input and checklist bounds perfectly track the keyboard's opening animation without visual clipping.
- **UI Polish**: Removed the restrictive nested paper-card styling from the free-text note screen in favor of a clean, edge-to-edge seamless native editing canvas.
