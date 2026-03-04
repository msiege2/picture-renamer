# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Picture Renamer is a Windows desktop application (Java Swing) that batch-renames photos and videos based on EXIF/metadata date-taken timestamps, then moves them into an organized album directory structure under `F:\My Pictures\{year}\{date}, {albumName}\`.

**Current version:** 3.2

## Build & Test

```bash
mvn compile           # Compile
mvn test              # Run all tests (JUnit 4)
mvn package           # Build fat JAR (output: dist/PictureRenamer.jar)
mvn test -Dtest=PictureRenamerTest#testParseDateFromFilename  # Run single test
```

- Java 17 (Eclipse Adoptium), Maven build. No build wrapper checked in — requires system Maven.
- `JAVA_HOME` must point to JDK 17 (JRE 8 may be first on PATH).
- `mvn package` produces a shaded fat JAR via maven-shade-plugin, then copies it to `dist/PictureRenamer.jar` via antrun.
- `app.properties` is Maven-filtered to inject the version number at build time.

## Project Structure

```
src/main/java/com/mcs/camera/
├── Main.java                  # Entry point — singleton lock, JNA window focus
├── AlbumDetails.java          # Immutable DTO for user-specified album settings
├── PictureRenamer.java        # Core rename engine — metadata → sort → rename → move
├── PictureRenumberer.java     # Standalone re-sequencing utility (has own main)
├── UIHandler.java             # Swing UI — dialogs, options, confirmation
├── FilenameComparator.java    # Natural sort (numeric-aware) comparator
└── extractor/
    ├── MetadataExtractor.java      # Interface: extractDateTaken(File) → LocalDateTime
    ├── JpgMetadataExtractor.java   # EXIF SubIFD TAG_DATETIME_ORIGINAL
    ├── HeicMetadataExtractor.java  # Same as JPG (identical implementation)
    ├── PngMetadataExtractor.java   # FileSystemDirectory TAG_FILE_MODIFIED_DATE
    └── VideoMetadataExtractor.java # file.lastModified() only (no container metadata)

src/main/resources/
├── app.properties     # Maven-filtered: app.version=${project.version}
├── logback.xml        # Console logging, DEBUG level
├── icon-16.png        # App icons (16/32/48/256px)
├── icon-32.png
├── icon-48.png
└── icon-256.png

src/test/java/com/mcs/camera/
├── AlbumDetailsTest.java      # DTO construction test
└── PictureRenamerTest.java    # Metadata extraction + filename parsing tests

dist/
├── PictureRenamer.jar   # Deployable fat JAR (built by mvn package)
└── PictureRenamer.ico   # Windows icon for OS-level association
```

## Architecture

**Entry point:** `Main` — acquires a file-based singleton lock (`~/.PictureRenamer.lock`), uses JNA to focus an existing window if already running, then launches the Swing UI.

**Core flow:** `UIHandler` → collects `AlbumDetails` via Swing dialogs → passes to `PictureRenamer.renamePictures()` which:
1. Reads all files from source directory
2. Extracts date-taken metadata per file type using the `extractor` package
3. Builds a sorted list of `"dateTaken fileName"` temporary keys for ordering
4. Renames files sequentially as `{prefix} 001.{ext}`, `{prefix} 002.{ext}`, etc.
5. Moves renamed files to `F:\My Pictures\{year}\{albumDirName}\`

**`PictureRenumberer`** is a standalone utility (has its own `main`) for re-sequencing files in an existing album directory. It uses a two-pass rename (randomize then re-sequence) to avoid collisions. Contains hardcoded constants rather than UI-driven options.

**Metadata extraction:** `MetadataExtractor` interface with per-format implementations. Uses the `com.drew:metadata-extractor` library for EXIF reading. Note: JPG and HEIC extractors are currently identical; PNG uses file-modified date; Video uses `file.lastModified()`.

**Date fallback chain** in `PictureRenamer`: EXIF metadata → parse from filename (`yyyy-MM-dd HH.mm.ss`) → file last-modified → forced date → error (`System.exit(1)`).

## Key Dependencies

| Library | Purpose |
|---|---|
| `metadata-extractor` 2.19.0 | EXIF/metadata reading |
| `jna` + `jna-platform` 5.18.1 | Windows native calls (User32) |
| `commons-io` 2.21.0 | File utilities |
| `commons-lang3` 3.17.0 | String utilities (RandomStringUtils) |
| `logback-classic` 1.5.32 | SLF4J logging |
| `commons-cli` 1.11.0 | CLI parsing (declared but unused) |
| `junit` 4.13.2 | Testing |

## Key Design Notes

- Windows-only: uses JNA (`User32`) for window focus, Windows Look and Feel, and hardcoded Windows paths (`F:\My Pictures`, `H:\Picture Merge`)
- Single-instance enforcement via file lock + JNA `FindWindow`
- `FilenameComparator` provides natural sort order (numeric-aware) used when `keepOrder` is enabled
- Tests require a `test_resources/` directory with sample media files (not checked into git)
- `System.exit(1)` in production catch blocks propagates into tests — test isolation concern
- `albumDirName` is derived from the first file processed; edge cases exist with `keepOrder` + no `forceDate`
