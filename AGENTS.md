# AI Agent Guidelines for ex01 (Notes App)

## Architecture & Frameworks
- **UI:** Jetpack Compose (100% Kotlin).
- **Architecture:** MVVM (Model-View-ViewModel).
- **Local Storage:** Room (SQLite) with Kotlin Coroutines & Flows.
- **Navigation:** Jetpack Compose Navigation.

## Folder Structure
- `app/src/main/java/.../data/` - Room database setup, DAOs, Repositories, and Entity models (Note, Folder, NoteItem).
- `app/src/main/java/.../ui/screens/` - Main full-screen composables (Home, Folder Detail, Trash).
- `app/src/main/java/.../ui/editor/` - Note editing screens (Free-Text, Checklist, S-Note canvas).
- `app/src/main/java/.../ui/dialogs/` - Reusable modal dialogs.
- `app/src/main/java/.../ui/components/` - Reusable UI elements.
- `app/src/main/java/.../ui/theme/` - Color schemes, Typography, and Shapes.
- `app/src/main/java/.../widget/` - Android App Widget providers & config screens.

## Project Conventions & Performance Best Practices
- **Navigation Flow:** Defer heavy Compose rendering and string deserialization until *after* Jetpack Navigation slide animations finish to ensure absolute buttery smooth transitions and eliminate frame drops.
- **Database Operations & Saves:**
  - Implement strict Diff-verification (`updatedNote != currentNote`) before committing to the Room databases. This prevents massive Flow invalidation spikes and IPC App Widget redraws when backing out without edits.
  - SNote tool edits are instantly serialized and continuously committed to the database on keystrokes using smooth debounced background auto-saves. Always use `DisposableEffect` to catch back-button exits without lag.
- **UI & Grid Performance:**
  - Do not use heavy rich-text markdown rendering and dynamic layout animations from the lazy grid, keep note previews lightweight.
  - Parse heavy drawing paths in a background `Dispatchers.Default` coroutine, then cleanly scale down and paint through pure, hardware-accelerated Compose Canvas directly onto the note card layout.
  - SNote Text nodes must be rendered via native Compose Layout constraints to guarantee fluid word-wrapping.
- **Imports:** Rely on wildcard imports (`.*`) dynamically bridging new modules, particularly when interacting with python scripts that automate flat explicit imports.

## Scripts & Tooling
- The workspace root contains numerous Python automation scripts (`do_*.py`, `fix_*.py`) used for mass refactors, data extraction, layout checks, undoing fixes, zoom modifications, etc.
- When performing wide-scale regex imports or refactoring, check if there's an existing Python script tailored for that pattern.
