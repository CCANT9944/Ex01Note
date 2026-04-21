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
- **S-Note Canvas Pagination**: Re-engineered the drawing canvas to feature visually distinct paginated cards. Implemented a massive precise text layout pagination algorithm tracking cumulative page gap offsets that automatically and mathematically fractures massive pasted blocks of text cleanly across physical page boundaries.
- **S-Note Context Menu**: In the S-Note Editor, an interactive handle is now placed at the top-right corner of the bounding box when using the Lasso tool. Tapping this opens a context menu allowing users to delete the selected items. We additionally added a "Colour" option in this context menu, allowing for an effortless bulk-change of the color of any captured free-form drawings, highlighters, or text blocks simultaneously.
- **S-Note Boundary Restrictions**: Implemented strict geometric hit-testing for both the Pen tool and Lasso selection tool to prevent elements from crossing, dropping, or scaling outside canvas boundaries. Drawing strokes are now dynamically terminated when intersecting the 24dp vertical page break gaps, keeping pages visually distinct. Additionally, Lasso drag-and-drop and resizing operations now intelligently calculate geometric bounding boxes to snap selections back to their origin (both position and scale) if moved or enlarged horizontally or vertically out of bounds (such as within the page gaps or outside the left/right screen edges).
- **S-Note Text Hit-Testing & Auto-Scroll**: Refined tap detection bounding boxes in the S-Note canvas to gracefully accommodate native Compose line-wrapping offsets. Integrated exact native `TextLayoutResult` measurements for pixel-perfect cursor placement within paragraphs, and flawlessly disambiguated scroll vs. tap gestures to prevent accidental keyboard pop-ups during canvas panning. Further perfected typing ergonomics by migrating to absolute mathematical `ScrollState` coordinates bound directly to dynamic viewport intersections (e.g. `availableHeight`), guaranteeing that tapping or typing text below the software keyboard seamlessly auto-scrolls the active row into perfect focus across any paginated canvas.
- **S-Note Text Lasso Bounds & Scaling**: Fixed a critical bounding box calculation bug where the Lasso tool only tracked the single origin coordinate of textual elements. The tool now correctly leverages Jetpack Compose's native `staticTextLayouts` to accurately map the full dynamic width, height, and center points of text blocks. This guarantees the selection lasso perfectly hit-tests and wraps around text strings of any length, and ensures that dragging or scaling operations properly anchor from the mathematical center of the text, preventing visual skewing and allowing precise manipulation of words within the S-Note canvas.
- **Dark Mode Support & UI Polish**: Restored the Dark Mode feature fully functional throughout the app! The Dark/Light toggle has been relocated to an elegant, space-efficient `ModalNavigationDrawer` acting as a unified side menu alongside the Trash Bin. Implemented an asynchronous loading dialogue when toggling themes to protect the UI thread and completely eliminate home screen layout freeze during the global recomposition sweep. In the S-Note Editor, the Lasso tool's selection bounding box bounding colors were updated to dynamically adapt to `Color.Unspecified`, `Color.Black`, and `Color.White`, ensuring dragged strokes and text remain perfectly visible against the dark canvas background. Also removed the restrictive nested paper-card styling from the free-text note screen in favor of a clean, edge-to-edge seamless native editing canvas. Updated the S-Note Editor Toolbar to use a sleek dark grey background with crisp white icons and a high-contrast material green highlight for the active tool state, making it immediately obvious which mode (Pen, Eraser, Highlighter, Text, Lasso) is currently selected, and defaulting the generated Text mode font immediately to a bolder scale (64).
- **App Branding**: Refreshed the app launcher icon using a bold purple background with crisp white folder accents, drastically improving its visual presence on the Android home screen.
