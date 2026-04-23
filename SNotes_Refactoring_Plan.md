# SNotes Refactoring Plan

This document outlines the high-impact refactoring steps to keep the `SNoteEditor` modular, performant, and maintainable.

### Step 1: Split `SNoteEditor.kt` into Modular Components
Currently, the main SNote editor handles too many responsibilities in a single ~1,300-line file. We will divide it into focused components:
* **`SNoteEditorState.kt`**: Extract all complex state variables (`currentCanvasWidthPx`, `lassoMenuPosition`, etc.), mathematical hit-detection, and state-mutating events out of the `SNoteEditor` UI into a dedicated state holder class or remember function.
* **`SNoteToolbar.kt`**: Extract the top action bar containing Undo/Redo, Mode toggles (Pen, Eraser, Text), and formatting options.
* **`SNoteCanvas.kt`**: Extract the core `Canvas` that renders the `drawingLines` and the `PageBackground` boundaries.

### Step 2: Offload Blocking Calculations to the ViewModel
Inside the SNote logic, heavy string measuring (`pEst.measureText`) and text-pagination layout calculations currently run synchronously on the main thread inside Compose lambdas.
* **Action**: Shift algorithms (like text pagination, geometric bounding box calculations, and lasso hit-detection) into `Dispatchers.Default` background coroutines inside the `SNoteViewModel` so they do not drop UI frames.

### Step 3: Normalize Constants & Configuration
There are currently "magic numbers" for scaling, grid gaps, and layout heights (such as `rowHeight = TEXT_LARGE * 1.2f`, `- 24f * currentDensity`) hardcoded deep inside drawing and event functions.
* **Action**: Extract these drawing configuration constraints into a `SNoteConfig` object or a global constants file. This ensures they can be reliably shared among the UI Editor, the Room repository data mappers, and the Android App Widget canvases without drifting out of sync.

### Step 4: Create Reusable Extracted Canvas Tools
The local `NotesWidget.kt` duplicates logic to recreate SNote geometric representations for the home screen grid.
* **Action**: Extract the actual `drawPath()` and text drawing layers into a shared `fun Canvas.drawSNoteLine(...)` extension function. This ensures that both the App Widget providers and the main `SNoteEditor` stay perfectly identical visually and rely on the exact same rendering logic.

