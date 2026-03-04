# UI Refactor Design

**Date:** 2026-03-04
**Branch:** ui-refactor

## Goal

Replace the dialog-driven UI (invisible JFrame + JOptionPane loops) with a standard Windows desktop app pattern: a real JFrame with embedded form, menu bar, and event-driven processing.

## Current State

- `UIHandler` creates a 1x1 invisible `JFrame` used only as a dialog parent
- All user interaction happens through `JOptionPane.showConfirmDialog()`
- A `while(runAgain)` loop in `startPictureRenaming()` blocks the EDT
- Version is displayed in the title bar (`"Picture Renamer v3.2"`)

## Design

### Frame Layout

```
+-- Picture Renamer --------------------------+
| File   Edit   Help                          |
+---------------------------------------------+
|                                             |
|  +- Album Details ------------------------+ |
|  |  Album Name:       [________________]  | |
|  |  Source Directory:  [____________][...] | |
|  +-----------------------------------------+ |
|                                             |
|  +- Options ------------------------------+ |
|  |  [ ] Force Date     [YYYY-MM-DD]       | |
|  |  [x] Include Videos                    | |
|  |  [x] Number Videos Inline              | |
|  |  [ ] Keep Order                        | |
|  |  [x] Use Filename Date/Time            | |
|  +-----------------------------------------+ |
|                                             |
|              [ Process ]  [ Reset ]         |
|                                             |
+---------------------------------------------+
```

- Frame size: ~450x400, centered on screen
- Title: "Picture Renamer" (no version)
- Form fields are persistent components, not recreated each cycle

### Menu Bar

```
File          Edit            Help
+- Exit       +- Options...   +- About
```

- **File > Exit** (Ctrl+Q): cleanup (release lock, close RAF, delete lock file), System.exit(0)
- **Edit > Options...**: disabled/greyed out placeholder for future settings dialog
- **Help > About**: modal dialog with app icon (48px), "Picture Renamer", version string, OK button
- Standard mnemonics: Alt+F, Alt+E, Alt+H

### Processing Flow

Event-driven, replaces the synchronous while loop:

1. User fills in form, clicks "Process"
2. Validate fields (same checks as today: non-empty album name, date format, source dir exists/non-empty)
3. Show confirmation dialog (same HTML table as today)
4. If confirmed: disable form + buttons, run `PictureRenamer.renamePictures()` on a SwingWorker
5. On success: show success message, re-enable form, reset fields to defaults
6. On error: show error message, re-enable form (fields preserved for correction)

No more "process another?" prompt — the form stays open and resets automatically.

### Title Bar & Version

- `Main.loadAppTitle()` returns `"Picture Renamer"` (no version suffix)
- New `Main.loadAppVersion()` returns `"3.2"` for the About dialog
- `FindWindow` now matches the static title — more robust across version bumps

### Files Changed

- **`UIHandler.java`** — Major rewrite: frame-based UI, menu bar, SwingWorker processing
- **`Main.java`** — Split loadAppTitle/loadAppVersion, remove version from title

### Files Unchanged

- `PictureRenamer.java` — core rename logic untouched
- `AlbumDetails.java` — DTO unchanged
- `FilenameComparator.java` — comparator unchanged
- `extractor/*` — all extractors unchanged
