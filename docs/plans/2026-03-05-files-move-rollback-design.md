# Design: Replace renameTo() with Files.move() + Rollback

Date: 2026-03-05

## Problem

All 6 `File.renameTo()` calls in PictureRenamer (3) and PictureRenumberer (3) silently ignore the boolean return value. A failed rename/move goes undetected, leaving files in an inconsistent state. Additionally, the rename and move phases are separate loops — a crash between them leaves files partially processed with no recovery.

## Solution

1. Replace `File.renameTo()` with `Files.move()`, which throws `IOException` on failure.
2. Track all completed file operations and rollback on failure.
3. Extract rollback logic into a shared `FileOperationTracker` utility class.

## New class: FileOperationTracker

Location: `src/main/java/com/mcs/camera/FileOperationTracker.java`

Responsibilities:
- Wrap `Files.move(source, target)` and record each completed operation
- On failure, reverse all completed operations in reverse order (best-effort)
- Log any rollback failures (rollback itself is best-effort — nothing more can be done)

Public API:
- `void move(Path source, Path target) throws IOException` — move and track
- `void rollback()` — best-effort reverse all completed ops
- `int completedCount()` — number of successfully completed operations

Internal state:
- `List<Map.Entry<Path, Path>> completedOps` — ordered list of (source, target) pairs

## Changes to PictureRenamer

- Create a `FileOperationTracker` at the start of `renamePictures()`
- Replace all 3 `renameTo()` calls (line 116, 126, 143) with `tracker.move()`
- Wrap the entire rename+move flow in try/catch(IOException)
- On IOException: call `tracker.rollback()`, then rethrow as RuntimeException
- All three phases (rename photos, rename videos, move to album) share one tracker

## Changes to PictureRenumberer

- Create a `FileOperationTracker` at the start of `renumberPictures()`
- Replace all 3 `renameTo()` calls (line 93, 103, 113) with `tracker.move()`
- Wrap the entire flow in try/catch(IOException)
- On IOException: call `tracker.rollback()`, then rethrow as RuntimeException
- All three phases (randomize, sequentialize, videos) share one tracker

## Error reporting

Both classes already throw RuntimeException, caught by SwingWorker. The exception message will include which file failed. No UI changes needed.

## Testing

### FileOperationTrackerTest (new)
- Successful move tracking
- IOException propagation on move failure
- Rollback reverses completed operations in reverse order
- Rollback is best-effort (logs failures, doesn't throw)
- completedCount accuracy

### PictureRenamerTest (updated)
- Verify rollback restores files after simulated mid-batch failure

### PictureRenumbererTest (new)
- Verify rollback restores files after failure in either pass of two-pass rename

## Out of scope

- Atomic/transactional guarantees beyond best-effort rollback
- Shared metadata extraction refactor (separate TODO)
- UI changes
- The `mkdirs()` call for album directory creation (not a rename operation)
