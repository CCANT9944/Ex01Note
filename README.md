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
- **The Home Screen Widget is still a work in progress and requires further layout and feature refinements.**

### Recent Updates

- **UI Transition & Animation Polish:** Finetuned all screen navigation animations to perfectly balance snappiness with smooth native-feeling 250ms glide transitions.
- **Under-the-Hood App Dialogs Refactoring:** Extracted all popup dialogs (renaming, color selection, item creation, deletion confirmations) from the main activity into a dedicated `AppDialogs.kt` file, vastly cleaning up the root codebase for easier maintenance.
- **Performance & Widget Optimization:** Implemented a smart debounce loop on database text entries to prevent the background widget renderer from overloading the UI thread during rapid checklist typing, maximizing responsiveness.
- **SNote Theme & Eraser Fixes:** Resolved an issue where SNote backgrounds and strokes didn't properly adapt when switching between Light and Dark themes, and completely fixed the eraser tool's functionality to ensure smooth corrections.
- **Rich-Text Formatting Fix:** Resolved a catastrophic bug in the Free-text editor where deleting text right after formatting toggles (like Bold or Italic) caused the formatting to bleed out and corrupt the entire document. Input intercepts now perfectly protect hidden style boundaries.
- **Trash Bin added:** A safety net has been added for deleted Notes and Folders. You can now restore or permanently remove items via the new Trash Bin accessible from the App Settings menu.
- **Native Settings Sidebar:** Upgraded the App Settings sidebar menu to use standard Material 3 NavigationDrawerItems for a more native look and feel.
- **Fixed Page Tab Bleeding Bug:** Resolved an issue where text from an old page would bleed onto newly created pages due to internal component recycling. Text inputs now stay firmly contained within their own page tabs.
- **Bullet List Formatting Stability:** Resolved an issue in the rich-text editor where pressing Enter/Return or typing at the end of a note would unexpectedly propagate bullet formatting across blank lines or throughout the entire page. Bullet toggling is now strictly constrained to the user's explicit selection and cleanly splits across newlines.
- **SNote Floating Button Layout:** The "Add Page" (+) button inside SNotes has been explicitly moved to the left side for easier access, and individual page heights have been carefully trimmed to precisely fit the screen for natural pagination.

- **Custom Named Note Pages:** Free-text note pages can now be custom named! Long-press on any page tab to rename or delete the page.
- **Improved Text Selection Area:** Fixed an issue where swiping to highlight text inside a note incorrectly triggered horizontal page navigation.
- **Robust Persistence & Saving:** Fixed a bug to ensure page renames, new pages, and text inputs securely save when the app is minimized or closed.
- **Checklist Input Bug Fix:** Resolved a Samsung Keyboard specific issue ("IME composition bleeding") where the "Add item" text box wouldn't clear its text properly after an item was submitted to the list. Added items now correctly clear the input box across all keyboards!
- **Under-the-Hood Editor Refactoring:** Drastically modularized the internal codebase, splitting the massive `NoteEditScreen` out into separate, lightweight `ChecklistEditor` and `PageBodyEditor` components for better long-term app maintenance.
- **Smooth Screen Transitions:** Navigating between the Home screen, Folders, and Notes now uses seamless slide-in and slide-out animations.
- **Card Expansion Animations:** Expanding and collapsing Folder and Note cards on the Home screen now animates smoothly rather than instantly snapping.
- **Improved UI in Dark Mode:** Fixed the visibility of the "3 dots" (More options) menu icon on Folders and Notes so it no longer blends into the dark background.
- **Enhanced "+" Create Menu:** The floating action button's dropdown menu has been upgraded to a wider, better-aligned design featuring Material icons for Notes, Lists, and Folders, complete with a clean separator between item types.
- **A4 Document Presentation:** The notes editor now renders pages physically simulating an A4 sheet. Pages start with a minimum drop-shadowed surface height and expand beautifully as you type.
- **Smart Pagination for Long Notes:** You can now instantly split excessively long notes! Long-press on a page tab to access the pagination tool, which intelligently chops large text blocks into perfectly fitting ~500 character chunks, distributing them into cleanly formatted sequential pages (e.g., Pt 1, Pt 2, Pt 3).
- **Stylus S-Note Drawing Tool:** Added a brand new "S-Note" item type built specifically for S-Pen and stylus drawing. Access it from the "+" create menu or inside folders.
- **Finger-Rejection Canvas:** The drawing surface intelligently rejects finger touches, allowing you to naturally rest your hand on the screen to draw without leaving smudges.
- **Thickness Controls:** Both the Pen and Eraser tools have integrated dropdown menus to select Thin, Medium, or Thick stroke sizes.
- **Multi-Page SNote Canvas:** The SNote editor now supports infinite vertical scrolling. The drawing canvas extends downward automatically as you draw, and you can manually extend it with the "Add Page" button.
- **Realistic Page Separation:** SNote pages are visually segmented with gray page dividers and include automatically assigned a "Page X" watermark at the bottom right corner of each segment to simulate continuous traditional document editing.
- **Hybrid Input (Scroll & Draw):** Your fingers are freely able to swipe up and down the viewport to scroll the pages, while your S-Pen will seamlessly continue to draw!

### Home Screen Widget Updates
- **Widget Memory Optimization:** Fixed a severe Android memory crash (TransactionTooLargeException) where large SNotes with extensive vertical scrolling would generate rendering chunks too large for the launcher's IPC limits. SNote chunks are now capped at a safe height.
- **Improved Widget Defaults:** The widget now gracefully defaults to a 4x2 layout on the home screen when added, ensuring both the Checklist and SNote fit perfectly without requiring immediate manual resizing.
- **Status Bar Overlap Fix:** Corrected a UI bug in the "Widget Configuration" screen where the content would overlap with the device's transparent status bar; it now properly respects system window insets (`systemBarsPadding`).
- **Home Screen Checklist Widgets:** Keep your most important lists right on your home screen! You can now deploy Jetpack Glance app widgets that instantly sync with specific Checklists for rapid viewing.
- **Instant Widget Syncing:** Modifying your Checklist notes (adding items, toggling checkboxes) now safely triggers an immediate real-time update push mechanism to your home screen widget. No more re-opening the app just to force a visual refresh!
- **Dynamic Widget Theming:** Built robust support for Android's System Night Mode directly into the Glance home screen widget, dynamically shifting the background and text color of the Checklist widget to pure dark gray + white text whenever Dark Mode is active for comfortable viewing.
- **Dual-Pane Widget Layout:** Fully redesigned the Home Screen Widget to feature a horizontal dual-pane layout. The left pane securely docks your active Checklist while the right pane intelligently renders your selected SNote.
- **SNote Drawing Widget Preview:** Added an intelligent renderer that reconstructs your SNote vector drawings directly inside the home screen widget. Includes zoomed-out boundary detection so thick strokes and extensive SNote doodles automatically fit within the widget's constraints!
- **Widget Canvas Rendering Fixes:** Corrected the SNote widget's bounding box calculation to ignore invisible eraser strokes and single-tap ghost marks, preventing massive empty spaces and scaling issues.
- **Improved Widget Layout Proportions:** Refined the Jetpack Glance dual-pane widget to use proper scaling weights, eliminating weird list padding and stopping the system from pushing images to the bottom of the widget.
- **Widget Checklist Strikethrough:** Syncs the visual state of checked items in your checklist widget, displaying them securely greyed out and struck-through so you can instantly recognize completed tasks off your home screen.
- **Stable Navigation Layouts:** Resolved a layout shift bug in the Note editor where horizontal scrolling would cause the entire page to wobble up and down. The page tab row is now strictly locked in place.
- **Refined Folder UI:** Enlarged folder icons for better visual hierarchy while squeezing folder names to perfectly fit inside the standard card size.
- **Streamlined Folder Interactions:** Removed the old 3-dot "More options" button from folder cards in favor of a modern touch-and-hold (long-press) gesture to access the folder actions menu.
- **Cleaner Action Menus:** Simplified the folder action menu by removing redundant "Expand/Collapse" options.
- **SNote Tool Memory:** The SNote editor now persistently remembers your preferred pen and eraser thickness across sessions, so you don't have to re-select your stroke size every time you open a note.
- **Iconography Polish:** Updated the SNote eraser tool to use a much more visually accurate and distinct custom eraser icon.
- **Codebase Optimization:** Cleaned up several Kotlin compiler warnings across the project, including primitive state autoboxing issues and shifting to optimized KTX extension functions for Canvas and SharedPreferences.
- **Buttery Smooth Navigation:** Greatly smoothed out the transition animations when entering and exiting notes by offloading aggressive database saves and heavy JSON drawing parsing onto background threads, completely eliminating UI stutter.
- **Luxurious Transitions:** Increased the default Jetpack Compose slide transition speeds from 250ms to 400ms for a more polished feel.
- **Visual SNote Menus:** Upgraded the SNote Pen and Eraser selection dropdowns from plain text to rich visual canvas previews. You can now clearly see the physical line thickness of your selection and which tool is currently active.
- **SNote True Erasing (BlendMode.Clear):** Re-architected the SNote drawing surface into dual hardware layers. Erasing now uses true transparent wiping rather than painting over previous strokes with background colors, preventing ugly graphical glitches when swapping between Light and Dark themes.
- **Intelligent Dark Mode Ink:** Fixed an issue where default black or white pen strokes would become invisible permanently when switching between System Light Mode and Dark Mode. Ink now intelligently inverts.
- **Unlimited Undo & Redo:** Fully implemented endless Undo and Redo functionality into the SNote editor so you can easily step back and correctly fix your exact drawing history.
- **Scrollable SNote Toolbar:** Refitted the top SNote toolbar into a polished, elevated, horizontally scrollable layout with vertical dividers. This allows additional drawing tools to seamlessly slide in sideways while keeping the "Clear All" safety button permanently pinned in sight.
- **Color Palette Control:** Brought colorful drawing to life! Added 4 distinct ink color options (Red, Blue, Green, Brown) directly into the SNote editor palette.
- **Clear All Safety Warning:** Integrated a mandatory confirmation `AlertDialog` directly into the SNote "Clear All" action to prevent accidental permanent deletion of your artwork.
- **Highlighter Tool:** Added a specialized Highlighter tool with customizable thickness and multiplied blend modes to perfectly overlay and tint existing text and drawings without obscuring them.
- **Expanded Color Options:** Introduced light color variants (Light Red, Light Green, Light Blue) and Yellow to the SNote color palette, perfect for use with the new Highlighter tool.
- **Native Direct Text Input:** Radically upgraded the SNote editor by adding a Text tool. You can now tap anywhere directly on the canvas to instantly open your keyboard and type native text directly onto the page without clunky dialog boxes.
- **Smart Text Editing & Alignment:** Text inserted into SNote can be freely edited by simply tapping on it again! The cursor mathematically aligns itself specifically to where you tapped within the text, and newly inserted text auto-snaps its Y-position to adjacent rows to keep your notes perfectly aligned.
- **Multiline Text Wrapping:** Handled Android Canvas limitations by rendering SNote text nodes via native Compose Layout constraints, guaranteeing fluid word-wrapping that prevents text from spilling over the right edge of the screen.
- **Instant Auto-Saving:** Bulletproofed the SNote text insertion engine. Text edits are now instantly serialized and continuously committed to the database on every keystroke, ensuring absolutely zero data loss even if you instantly background or close the app.
- **Widget Theming Enhancements:** Fixed overlapping issues within the Widget Configuration screen during setup, ensuring items correctly pad themselves under the status bar.
- **Lint Cleanup:** Cleaned up several stale unused imports and suppressed non-applicable Android 12+ API warnings within the Widget XML manifests for a much cleaner build output.
