# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Picture Renamer is a Windows desktop application (Java Swing) that batch-renames photos and videos based on EXIF/metadata date-taken timestamps, then moves them into an organized album directory structure under `F:\My Pictures\{year}\{date}, {albumName}\`.

## Build & Test

```bash
mvn compile           # Compile
mvn test              # Run all tests (JUnit 4)
mvn package           # Build JAR
mvn test -Dtest=PictureRenamerTest#testParseDateFromFilename  # Run single test
```

Java 17, Maven build. No build wrapper checked in — requires system Maven.

## Architecture

**Entry point:** `Main` — acquires a file-based singleton lock (`~/.PictureRenamer.lock`), uses JNA to focus an existing window if already running, then launches the Swing UI.

**Core flow:** `UIHandler` → collects `AlbumDetails` via Swing dialogs → passes to `PictureRenamer.renamePictures()` which:
1. Reads all files from source directory
2. Extracts date-taken metadata per file type using the `extractor` package
3. Builds a sorted list of `"dateTaken fileName"` temporary keys for ordering
4. Renames files sequentially as `{prefix} 001.{ext}`, `{prefix} 002.{ext}`, etc.
5. Moves renamed files to `F:\My Pictures\{year}\{albumDirName}\`

**`PictureRenumberer`** is a standalone utility (has its own `main`) for re-sequencing files in an existing album directory. It's an older component with hardcoded constants rather than UI-driven options.

**Metadata extraction:** `MetadataExtractor` interface with per-format implementations (`JpgMetadataExtractor`, `HeicMetadataExtractor`, `PngMetadataExtractor`, `VideoMetadataExtractor`). Uses the `com.drew:metadata-extractor` library. The JPG and HEIC extractors are currently identical (both read EXIF SubIFD).

**Date fallback chain** in `PictureRenamer`: EXIF metadata → parse from filename (`yyyy-MM-dd HH.mm.ss`) → file last-modified → forced date → error.

## Key Design Notes

- Windows-only: uses JNA (`User32`) for window focus, Windows Look and Feel, and hardcoded Windows paths (`F:\My Pictures`, `H:\Picture Merge`)
- Single-instance enforcement via file lock + JNA `FindWindow`
- `FilenameComparator` provides natural sort order (numeric-aware) used when `keepOrder` is enabled
- Tests require a `test_resources/` directory with sample media files (not checked into git)
