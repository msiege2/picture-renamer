# Options Dialog Design

## Scope

Externalize six hardcoded values into a user-configurable Options dialog, persisted across sessions.

## Settings

| Label | Control | Default | Pref Key |
|---|---|---|---|
| Picture library directory | Text field + Browse button | `F:\My Pictures` | `pictureLibraryDir` |
| Default source directory | Text field + Browse button | `H:\Picture Merge` | `defaultSourceDir` |
| Counter start | Spinner (1-999) | `1` | `counterStart` |
| Number padding | Dropdown (2, 3, 4) | `3` | `numberPadding` |
| Filename separator | Dropdown (Space, Dash, Underscore, None) | `Space` | `filenameSeparator` |

## Persistence

`java.util.prefs.Preferences` user node under `com.mcs.camera`. Loaded at startup, written on OK.

## Dialog

Modal `JDialog` opened from Edit > Options... (currently a disabled placeholder). OK saves to Preferences and closes. Cancel discards and closes.

"Picture library directory" is the home of the picture library — used as both the rename destination base dir and the renumber tab's default browse directory. Label or tooltip should clarify this.

## New Files

- `AppPreferences.java` — encapsulates all `Preferences` access with typed getters and hardcoded defaults

## Modified Files

- `UIHandler.java` — enable Options menu item, build Options dialog, read prefs for default directory fields
- `AlbumDetails.java` — add fields: `destinationDir`, `counterStart`, `numberPadding`, `filenameSeparator`
- `PictureRenamer.java` — remove `DEFAULT_DESTINATION_DIR` constant, accept destination/counter/padding/separator from `AlbumDetails`; build format string dynamically from padding; use separator instead of hardcoded space
- `PictureRenumberer.java` — accept padding/separator, same dynamic format string and separator changes

## Integration Flow

1. `AppPreferences` provides defaults with fallback to hardcoded values
2. `UIHandler` reads `AppPreferences` to populate form field defaults and to pass values into `AlbumDetails`
3. `AlbumDetails` carries the settings to `PictureRenamer` and `PictureRenumberer`
4. Renamer/Renumberer use the values from `AlbumDetails` instead of constants

## Design Decisions

- Picture library dir and renumber browse dir are linked (single setting)
- Number padding offered as dropdown (2/3/4) — no realistic use case outside that range
- Filename separator offered as preset dropdown (Space, Dash, Underscore, None) — avoids invalid filename characters
- No Reset to Defaults button
