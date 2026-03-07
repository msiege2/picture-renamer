# Picture Renamer

A Windows desktop application that batch-renames photos and videos by date taken, then organizes them into a clean album directory structure. Also includes a built-in renumbering tool for re-sequencing existing albums. Available as both a GUI (Java Swing) and a CLI.

## What It Does

### Rename Mode

Takes a folder of unorganized camera files and:

1. **Reads metadata** — Extracts the date-taken timestamp from each file's EXIF data (JPG, HEIC, PNG) or filesystem metadata (videos)
2. **Sorts chronologically** — Orders all files by when they were taken
3. **Renames sequentially** — Gives each file a clean, numbered name: `Beach Trip 001.jpg`, `Beach Trip 002.jpg`, etc.
4. **Organizes into albums** — Moves everything into `{picture library}\{year}\{date}, {album name}\`

```
Source folder:                          Album folder:
  IMG_4521.jpg                            Beach Trip 001.jpg
  DSC_0032.jpg           ──────►          Beach Trip 002.jpg
  IMG_4519.jpg                            Beach Trip 003.heic
  20240715_143022.heic                    Beach Trip 004.mp4
  VID_20240715.mp4
```

### Renumber Mode

Re-sequences files in an existing album directory. Useful when you need to reorder photos after adding or removing files. Uses a two-pass rename strategy (randomize then re-sequence) to avoid filename collisions. Files stay in place — no move step.

## Features

- **GUI and CLI** — Use the tabbed Swing interface or script operations from the command line
- **Persistent options** — Configure picture library directory, default source directory, counter start, number padding, and filename separator via Edit > Options... (stored in Windows Registry)
- **EXIF-aware sorting** — Photos are ordered by the actual moment they were captured, not by filename
- **Multi-format support** — Handles JPG, HEIC, PNG, and common video formats (MP4, MOV, AVI, MTS, M2TS)
- **Smart date fallback** — If EXIF is missing, falls back to parsing the filename, then file modified date
- **Force date override** — Manually set a date for files with no usable timestamp
- **Keep order mode** — Skip metadata entirely and sort by filename (natural/numeric sort)
- **Video handling options** — Include or exclude videos; number them inline with photos or append at the end
- **Dry run** — Preview what would happen without making any changes (CLI)
- **Rollback on failure** — File moves are tracked and automatically reversed if an error occurs mid-operation
- **Single-instance enforcement** — Only one copy of the app runs at a time; re-launching focuses the existing window
- **Menu bar** — File (Exit), Edit (Options...), Help (About with version info)

## Requirements

- **OS:** Windows (uses Windows-native APIs)
- **Runtime:** Java 17+

## Installation

Download `PictureRenamer.exe` from the [latest release](https://github.com/msiege2/picture-renamer/releases) and double-click to run. Requires Java 17+ installed.

Alternatively, run the JAR directly:

```
java -jar PictureRenamer.jar
```

## First Run

On first launch, configure your directories via **Edit > Options...** before processing:

- **Picture library** — Home directory of your picture library (e.g., `F:\My Pictures`). Renamed files are moved here, and the Renumber tab browses here by default.
- **Default source directory** — Where your camera/phone dumps files for import (e.g., `H:\Picture Merge`).

## CLI Usage

Pass any argument to use the CLI instead of the GUI.

```
java -jar PictureRenamer.jar <command> [options]
```

### Global Options

| Flag | Description |
|---|---|
| `--help`, `-h` | Show help message |
| `--version`, `-v` | Show version |

### Rename Command

```
java -jar PictureRenamer.jar rename --source <dir> --dest <dir> --prefix <name> [options]
```

| Flag | Description |
|---|---|
| `--source <dir>` | **(required)** Source directory containing photos/videos |
| `--dest <dir>` | **(required)** Destination base directory (e.g., `F:\My Pictures`) |
| `--prefix <name>` | **(required)** Album name prefix |
| `--force-date <YYYY-MM-DD>` | Force a specific date for all files |
| `--keep-order` | Keep original file order (natural sort, skips metadata) |
| `--include-videos` | Include video files |
| `--inline-videos` | Sort videos by date alongside photos (requires `--include-videos`) |
| `--try-filename-date` | Try parsing date from filename when metadata fails |
| `--counter-start <n>` | Starting counter (default: 1) |
| `--number-padding <n>` | Number padding digits: 2, 3, or 4 (default: 3) |
| `--separator <type>` | Filename separator: space, dash, underscore, none (default: space) |
| `--dry-run` | Show what would happen without making changes |

### Renumber Command

```
java -jar PictureRenamer.jar renumber --dir <dir> --prefix <name> [options]
```

| Flag | Description |
|---|---|
| `--dir <dir>` | **(required)** Album directory to renumber |
| `--prefix <name>` | **(required)** New filename prefix |
| `--include-videos` | Include video files |
| `--inline-videos` | Sort videos by date alongside photos |
| `--number-padding <n>` | Number padding digits: 2, 3, or 4 (default: 3) |
| `--separator <type>` | Filename separator: space, dash, underscore, none (default: space) |
| `--dry-run` | Show what would happen without making changes |

## GUI Options

### Edit > Options...

| Setting | Description | Default |
|---|---|---|
| **Picture library** | Home directory of your picture library — rename destination and renumber browse root | *(must be configured)* |
| **Default source directory** | Default folder to import photos/videos from | *(must be configured)* |
| **Counter start** | Starting number for file sequences | `1` |
| **Number padding** | Number of digits (2, 3, or 4) for zero-padded file numbers | `3` |
| **Filename separator** | Character between prefix and number: Space, Dash, Underscore, or None | `Space` |

### Rename Tab

| Option | Description |
|---|---|
| **Album Name** | The prefix used for renamed files (e.g., "Beach Trip") |
| **Source Directory** | Folder containing the photos/videos to process (set via Options) |
| **Force Date** | Override all file dates with a specific `YYYY-MM-DD` value |
| **Include Videos** | Whether to process video files alongside photos |
| **Number Videos Inline** | Sequence videos among photos by timestamp, or append them after all photos |
| **Keep Order** | Sort by filename instead of metadata (uses natural/numeric sort) |
| **Use Filename Date/Time** | Fall back to parsing `yyyy-MM-dd HH.mm.ss` from filenames when EXIF is missing |

### Renumber Tab

| Option | Description |
|---|---|
| **Album Directory** | The existing album directory to re-sequence |
| **Album Prefix** | Auto-extracted from directory name (editable); used for renamed files |
| **Include Videos** | Whether to include video files in the renumbering |
| **Number Videos Inline** | Sequence videos among photos by timestamp, or append them after all photos |

## Supported File Types

| Type | Formats | Metadata Source |
|---|---|---|
| Photos | `.jpg`, `.jpeg` | EXIF SubIFD (DateTimeOriginal) |
| Photos | `.heic` | EXIF SubIFD (DateTimeOriginal) |
| Photos | `.png` | File system modified date |
| Videos | `.mp4`, `.mov`, `.avi`, `.mts`, `.m2ts` | File system modified date |

## Building from Source

### Prerequisites

- JDK 17+ ([Eclipse Adoptium](https://adoptium.net/) recommended)
- [Maven](https://maven.apache.org/) 3.9+

### Build

```bash
mvn package
```

This produces a fat JAR at `dist/PictureRenamer.jar` and a Windows exe wrapper at `dist/PictureRenamer.exe`.

### Run Tests

```bash
mvn test
```

## Tech Stack

- **Language:** Java 17
- **UI:** Swing with [FlatLaf](https://www.formdev.com/flatlaf/) Look and Feel
- **CLI:** [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/)
- **Build:** Maven with shade plugin (fat JAR) + Launch4j (exe wrapper)
- **EXIF:** [metadata-extractor](https://github.com/drewnoakes/metadata-extractor) by Drew Noakes
- **Native:** [JNA](https://github.com/java-native-access/jna) for Win32 API calls
- **Logging:** SLF4J + Logback
- **Testing:** [JUnit 6](https://junit.org/) (Jupiter) + [Mockito](https://site.mockito.org/) + [AssertJ](https://assertj.github.io/doc/)
- **CI:** GitHub Actions on push/PR to main

## License

[MIT](LICENSE)
