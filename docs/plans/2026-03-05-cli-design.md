# CLI Mode Design

## Goal

Add a command-line interface to Picture Renamer so it can be run from scripts or used for quick one-off operations without launching the GUI.

## Mode Selection

Auto-detect based on arguments: no args launches the GUI (as today), any args activates CLI mode. CLI mode skips the singleton lock, JNA window focus, and Swing initialization.

## CLI via JAR Only

The `.exe` remains GUI-only (Launch4j `gui` header unchanged). CLI users run:

```
java -jar PictureRenamer.jar rename --source <dir> --dest <dir> --prefix <name> [options]
```

## Commands

### rename

Rename photos by metadata date and move to album directory.

```
Required:
  --source <dir>        Source directory containing photos
  --dest <dir>          Destination base directory (e.g., F:\My Pictures)
  --prefix <name>       Album name prefix

Optional:
  --force-date <yyyy-MM-dd>   Force a specific date (also sets album dir name)
  --keep-order                 Keep original file order (natural sort, skips metadata)
  --include-videos             Include video files
  --inline-videos              Sort videos by date alongside photos (requires --include-videos)
  --try-filename-date          Try parsing date from filename when metadata fails
  --counter-start <n>          Starting counter (default: 1)
  --number-padding <n>         Number padding digits: 2, 3, or 4 (default: 3)
  --separator <type>           Filename separator: space, dash, underscore, none (default: space)
  --dry-run                    Show what would happen without making changes
```

### renumber

Re-sequence files in an existing album directory.

```
Required:
  --dir <dir>           Directory to renumber
  --prefix <name>       New filename prefix

Optional:
  --include-videos             Include video files
  --inline-videos              Sort videos by date alongside photos
  --number-padding <n>         Number padding digits: 2, 3, or 4 (default: 3)
  --separator <type>           Filename separator: space, dash, underscore, none (default: space)
  --dry-run                    Show what would happen without making changes
```

### Global Options

```
--help      Show usage
--version   Show version
```

## Architecture

### New class: `CliHandler`

- Parses args using `commons-cli` (already a declared dependency)
- First positional arg is the subcommand (`rename` or `renumber`)
- Validates required flags, directory existence, value ranges
- Constructs `AlbumDetails` (for rename) or `PictureRenumberer` (for renumber) and invokes the engine
- `--dry-run` requires a thin wrapper or flag on the engines to log planned operations without executing

### Modified: `Main.java`

- `main(String[] args)`: if `args.length > 0`, delegate to `CliHandler.run(args)` and skip GUI startup
- No other changes to Main

### Unchanged

- `PictureRenamer`, `PictureRenumberer`, `AlbumDetails`, `AppPreferences`, `UIHandler` — no modifications needed

## Settings Source

All settings come from CLI arguments only. No `AppPreferences` (Windows Registry) interaction in CLI mode. This keeps CLI invocations fully self-contained and portable.

## Error Handling & Output

- Progress logged to stdout via SLF4J/logback (already configured for console)
- Validation errors (missing flags, invalid directory, bad values) print a message + usage help to stderr, exit code 1
- Runtime errors (file operation failures) print to stderr, exit code 1
- Success exits with code 0
- No Swing dialogs or JOptionPane in CLI path

## Dry Run

`--dry-run` prints the planned renames/moves without executing them. Implementation approach TBD in planning phase (likely a flag passed to the engine or a wrapper that intercepts file operations).

## Testing

- Unit tests for `CliHandler` argument parsing (valid args, missing required flags, invalid values)
- Integration test: CLI rename with `--dry-run` against a temp directory
- Existing engine tests remain unchanged
