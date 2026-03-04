# Picture Renamer

A Windows desktop application that batch-renames photos and videos by date taken, then organizes them into a clean album directory structure. Built with Java Swing.

## What It Does

Picture Renamer takes a folder of unorganized camera files and:

1. **Reads metadata** — Extracts the date-taken timestamp from each file's EXIF data (JPG, HEIC, PNG) or filesystem metadata (videos)
2. **Sorts chronologically** — Orders all files by when they were taken
3. **Renames sequentially** — Gives each file a clean, numbered name: `Beach Trip 001.jpg`, `Beach Trip 002.jpg`, etc.
4. **Organizes into albums** — Moves everything into `F:\My Pictures\{year}\{date}, {album name}\`

### Before & After

```
Source folder:                          Album folder:
  IMG_4521.jpg                            Beach Trip 001.jpg
  DSC_0032.jpg           ──────►          Beach Trip 002.jpg
  IMG_4519.jpg                            Beach Trip 003.heic
  20240715_143022.heic                    Beach Trip 004.mp4
  VID_20240715.mp4
```

## Features

- **EXIF-aware sorting** — Photos are ordered by the actual moment they were captured, not by filename
- **Multi-format support** — Handles JPG, HEIC, PNG, and common video formats (MP4, MOV, AVI, MTS, M2TS)
- **Smart date fallback** — If EXIF is missing, falls back to parsing the filename, then file modified date
- **Force date override** — Manually set a date for files with no usable timestamp
- **Keep order mode** — Skip metadata entirely and sort by filename (natural/numeric sort)
- **Video handling options** — Include or exclude videos; number them inline with photos or append at the end
- **Single-instance enforcement** — Only one copy of the app runs at a time; re-launching focuses the existing window
- **Batch processing** — Process multiple albums in one session without restarting

## Screenshots

The app uses a dialog-driven workflow:

1. Enter album details and options
2. Review a confirmation summary
3. Files are renamed and moved automatically

## Requirements

- **OS:** Windows (uses Windows-native APIs and hardcoded paths)
- **Runtime:** Java 17+
- **Destination:** Files are moved to `F:\My Pictures\` — this path is currently hardcoded

## Installation

Download `PictureRenamer.jar` from the `dist/` directory and run it:

```
java -jar PictureRenamer.jar
```

Or double-click the JAR if `.jar` files are associated with Java on your system.

## Building from Source

### Prerequisites

- JDK 17+ ([Eclipse Adoptium](https://adoptium.net/) recommended)
- [Maven](https://maven.apache.org/) 3.9+

### Build

```bash
mvn package
```

This produces a fat JAR with all dependencies bundled at `dist/PictureRenamer.jar`.

### Run Tests

```bash
mvn test
```

> **Note:** Some tests require a `test_resources/` directory containing sample media files (`test.jpg`, `test.png`, `test.mp4`). These are not included in the repository.

## Configuration Options

| Option | Description |
|---|---|
| **Album Name** | The prefix used for renamed files (e.g., "Beach Trip") |
| **Source Directory** | Folder containing the photos/videos to process |
| **Force Date** | Override all file dates with a specific `YYYY-MM-DD` value |
| **Include Videos** | Whether to process video files alongside photos |
| **Number Videos Inline** | Sequence videos among photos by timestamp, or append them after all photos |
| **Keep Order** | Sort by filename instead of metadata (uses natural/numeric sort) |
| **Use Filename Date/Time** | Fall back to parsing `yyyy-MM-dd HH.mm.ss` from filenames when EXIF is missing |

## Supported File Types

| Type | Formats | Metadata Source |
|---|---|---|
| Photos | `.jpg`, `.jpeg` | EXIF SubIFD (DateTimeOriginal) |
| Photos | `.heic` | EXIF SubIFD (DateTimeOriginal) |
| Photos | `.png` | File system modified date |
| Videos | `.mp4`, `.mov`, `.avi`, `.mts`, `.m2ts` | File system modified date |

## Utilities

### PictureRenumberer

A standalone utility for re-sequencing files in an existing album directory. Useful when you need to reorder photos after the initial rename. Run it directly:

```bash
java -cp PictureRenamer.jar com.mcs.camera.PictureRenumberer
```

It uses a two-pass rename strategy (randomize then re-sequence) to avoid filename collisions.

## Tech Stack

- **Language:** Java 17
- **UI:** Swing with Windows Look and Feel
- **Build:** Maven with shade plugin (fat JAR)
- **EXIF:** [metadata-extractor](https://github.com/drewnoakes/metadata-extractor) by Drew Noakes
- **Native:** [JNA](https://github.com/java-native-access/jna) for Win32 API calls
- **Logging:** SLF4J + Logback

## License

Private project — not licensed for redistribution.
