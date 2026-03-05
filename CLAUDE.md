# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Picture Renamer is a Windows desktop application (Java Swing + CLI) that batch-renames photos and videos based on EXIF/metadata date-taken timestamps, then moves them into an organized album directory structure under `{pictureLibrary}\{year}\{date}, {albumName}\`. It also includes a built-in renumbering tool for re-sequencing files in existing albums. Both workflows are available via the GUI or the command-line interface.

**Current version:** 4.0

## Build & Test

```bash
mvn compile           # Compile
mvn test              # Run all tests (JUnit 4)
mvn package           # Build fat JAR + exe (output: dist/PictureRenamer.jar, dist/PictureRenamer.exe)
mvn test -Dtest=PictureRenamerTest#testParseDateFromFilename  # Run single test
```

- Java 17 (Eclipse Adoptium), Maven build. No build wrapper checked in — requires system Maven.
- `JAVA_HOME` must point to JDK 17 (JRE 8 may be first on PATH).
- `mvn package` produces a shaded fat JAR via maven-shade-plugin, copies it to `dist/PictureRenamer.jar` via antrun, then wraps it into `dist/PictureRenamer.exe` via Launch4j.
- `app.properties` is Maven-filtered to inject the version number at build time.
- **CI:** GitHub Actions runs `mvn test` and `mvn package` on push/PR to `main` (`.github/workflows/build.yml`).
- **Dependabot:** Automated weekly PRs for Maven and GitHub Actions dependency updates.

## Project Structure

```
src/main/java/com/mcs/camera/
├── Main.java                       # Entry point — CLI dispatch or singleton lock + GUI launch
├── CliHandler.java                 # CLI interface — rename/renumber subcommands via commons-cli
├── AlbumDetails.java               # Immutable DTO for user-specified album settings
├── AppPreferences.java             # Persistent user preferences via java.util.prefs.Preferences
├── PictureRenamer.java             # Core rename engine — metadata → sort → rename → move
├── PictureRenumberer.java          # Re-sequencing engine — metadata → sort → two-pass rename in place
├── FileOperationTracker.java       # Tracks file moves for rollback on failure
├── DryRunFileOperationTracker.java  # Dry-run variant — logs operations without executing
├── UIHandler.java                  # Swing UI — tabbed frame, menu bar, Options dialog, SwingWorker processing
├── FilenameComparator.java         # Natural sort (numeric-aware) comparator
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
├── AlbumDetailsTest.java              # DTO construction test
├── AppPreferencesTest.java            # Preferences persistence and defaults tests
├── CliHandlerTest.java                # CLI argument parsing and dispatch tests
├── FileOperationTrackerTest.java      # Move tracking and rollback tests
├── DryRunFileOperationTrackerTest.java # Dry-run operation logging tests
├── PictureRenamerTest.java            # Filename parsing, metadata extraction, file listing tests
├── PictureRenamerDryRunTest.java      # Rename dry-run integration tests
├── PictureRenumbererTest.java         # Renumbering integration tests
└── PictureRenumbererDryRunTest.java   # Renumber dry-run integration tests

dist/
├── PictureRenamer.jar   # Deployable fat JAR (built by mvn package)
├── PictureRenamer.exe   # Windows exe wrapper (built by mvn package via Launch4j)
└── PictureRenamer.ico   # Windows icon for OS-level association

.github/
├── workflows/
│   └── build.yml        # CI: build + test on push/PR to main
└── dependabot.yml       # Automated dependency update PRs
```

## Architecture

**Entry point:** `Main` — if CLI args are present, delegates to `CliHandler` and exits with the return code. Otherwise, acquires a file-based singleton lock (`~/.PictureRenamer.lock`), uses JNA to focus an existing window if already running, then launches the Swing UI. Exposes `getAppTitle()` (static `"Picture Renamer"`) and `getAppVersion()` (from `app.properties`).

**CLI:** `CliHandler` provides `rename` and `renumber` subcommands with full flag support via Apache Commons CLI. Supports `--help`, `--version`, and `--dry-run`. Constructs `AlbumDetails` from flags and delegates to `PictureRenamer`/`PictureRenumberer`.

**UI:** `UIHandler` builds a `JFrame` with:
- **Menu bar:** File (Exit), Edit (Options...), Help (About)
- **Tabbed pane:** "Rename" tab for the rename-and-move workflow, "Renumber" tab for in-place re-sequencing
- **SwingWorker:** Both tabs run their processing off the EDT to keep the UI responsive
- Form fields are persistent class members

**Rename flow** (via `PictureRenamer`):
1. Reads all files from source directory
2. Extracts date-taken metadata per file type using the `extractor` package
3. Builds a sorted list of `"dateTaken fileName"` temporary keys for ordering
4. Renames files sequentially as `{prefix}{sep}001.{ext}`, `{prefix}{sep}002.{ext}`, etc.
5. Moves renamed files to `{pictureLibrary}\{year}\{albumDirName}\`

**Renumber flow** (via `PictureRenumberer`):
1. Reads all files from an existing album directory
2. Extracts metadata and sorts chronologically
3. Two-pass rename: randomize all names first (avoid collisions), then re-sequence as `{prefix}{sep}001.{ext}`, etc.
4. Files stay in place — no move step

**File operation tracking:** `FileOperationTracker` wraps `Files.move()` and records each operation so the entire batch can be rolled back on failure. `DryRunFileOperationTracker` overrides this to log planned operations without executing them.

**Metadata extraction:** `MetadataExtractor` interface with per-format implementations. Uses the `com.drew:metadata-extractor` library for EXIF reading. Note: JPG and HEIC extractors are currently identical; PNG uses file-modified date; Video uses `file.lastModified()`.

**Date fallback chain** in `PictureRenamer.grabMetadata(File, MetadataExtractor)`: EXIF metadata → parse from filename (`yyyy-MM-dd HH.mm.ss`) → file last-modified → forced date → RuntimeException.

## Key Dependencies

| Library | Purpose |
|---|---|
| `metadata-extractor` 2.19.0 | EXIF/metadata reading |
| `jna` + `jna-platform` 5.18.1 | Windows native calls (User32) |
| `commons-io` 2.21.0 | File utilities |
| `commons-lang3` 3.20.0 | String utilities (RandomStringUtils) |
| `commons-cli` 1.11.0 | CLI argument parsing |
| `flatlaf` 3.7 | Modern Swing Look and Feel |
| `logback-classic` 1.5.32 | SLF4J logging |
| `junit` 4.13.2 | Testing |
| `launch4j-maven-plugin` 2.7.0 | Wraps fat JAR into Windows .exe |

## Key Design Notes

- Windows-only: uses JNA (`User32`) for window focus, FlatLaf Light Look and Feel
- Single-instance enforcement via file lock + JNA `FindWindow` (matches static title `"Picture Renamer"`)
- `FilenameComparator` provides natural sort order (numeric-aware) used when `keepOrder` is enabled
- Tests use JUnit `TemporaryFolder` — no external test resources needed
- `albumDirName` is derived from the first file processed; edge cases exist with `keepOrder` + no `forceDate`
- Edit > Options... persists settings via `java.util.prefs.Preferences` (Windows Registry): picture library dir, default source dir, counter start, number padding, filename separator
- `AppPreferences` encapsulates all preferences access with typed getters and hardcoded defaults as fallbacks
