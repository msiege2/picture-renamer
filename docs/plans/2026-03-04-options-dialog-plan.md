# Options Dialog Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Externalize 6 hardcoded settings into a persistent Options dialog accessed from Edit > Options...

**Architecture:** New `AppPreferences` class wraps `java.util.prefs.Preferences` with typed getters and defaults. `AlbumDetails` gains 4 new fields (destinationDir, counterStart, numberPadding, filenameSeparator). `UIHandler` builds the Options dialog and reads prefs for form defaults. `PictureRenamer` and `PictureRenumberer` use the new fields instead of hardcoded constants.

**Tech Stack:** Java 17, Swing, java.util.prefs.Preferences, JUnit 4

---

### Task 1: Create AppPreferences class

**Files:**
- Create: `src/main/java/com/mcs/camera/AppPreferences.java`
- Test: `src/test/java/com/mcs/camera/AppPreferencesTest.java`

**Step 1: Write the test**

```java
package com.mcs.camera;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppPreferencesTest {

    @After
    public void cleanup() throws Exception {
        // Clear prefs so tests don't leak state
        java.util.prefs.Preferences.userNodeForPackage(AppPreferences.class).clear();
    }

    @Test
    public void testDefaults() {
        AppPreferences prefs = new AppPreferences();
        assertEquals("F:\\My Pictures", prefs.getPictureLibraryDir());
        assertEquals("H:\\Picture Merge", prefs.getDefaultSourceDir());
        assertEquals(1, prefs.getCounterStart());
        assertEquals(3, prefs.getNumberPadding());
        assertEquals(" ", prefs.getFilenameSeparator());
    }

    @Test
    public void testSetAndGet() {
        AppPreferences prefs = new AppPreferences();
        prefs.setPictureLibraryDir("D:\\Photos");
        prefs.setDefaultSourceDir("E:\\Import");
        prefs.setCounterStart(5);
        prefs.setNumberPadding(4);
        prefs.setFilenameSeparator("-");

        assertEquals("D:\\Photos", prefs.getPictureLibraryDir());
        assertEquals("E:\\Import", prefs.getDefaultSourceDir());
        assertEquals(5, prefs.getCounterStart());
        assertEquals(4, prefs.getNumberPadding());
        assertEquals("-", prefs.getFilenameSeparator());
    }

    @Test
    public void testPersistenceAcrossInstances() {
        AppPreferences prefs1 = new AppPreferences();
        prefs1.setPictureLibraryDir("D:\\Photos");
        prefs1.setNumberPadding(2);

        AppPreferences prefs2 = new AppPreferences();
        assertEquals("D:\\Photos", prefs2.getPictureLibraryDir());
        assertEquals(2, prefs2.getNumberPadding());
    }

    @Test
    public void testGetNumberFormat() {
        AppPreferences prefs = new AppPreferences();

        prefs.setNumberPadding(2);
        assertEquals("01", String.format(prefs.getNumberFormat(), 1));

        prefs.setNumberPadding(3);
        assertEquals("001", String.format(prefs.getNumberFormat(), 1));

        prefs.setNumberPadding(4);
        assertEquals("0001", String.format(prefs.getNumberFormat(), 1));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=AppPreferencesTest -q`
Expected: FAIL — class does not exist

**Step 3: Write the implementation**

```java
package com.mcs.camera;

import java.util.prefs.Preferences;

public class AppPreferences {
    private static final String KEY_PICTURE_LIBRARY_DIR = "pictureLibraryDir";
    private static final String KEY_DEFAULT_SOURCE_DIR = "defaultSourceDir";
    private static final String KEY_COUNTER_START = "counterStart";
    private static final String KEY_NUMBER_PADDING = "numberPadding";
    private static final String KEY_FILENAME_SEPARATOR = "filenameSeparator";

    private static final String DEFAULT_PICTURE_LIBRARY_DIR = "F:\\My Pictures";
    private static final String DEFAULT_SOURCE_DIR = "H:\\Picture Merge";
    private static final int DEFAULT_COUNTER_START = 1;
    private static final int DEFAULT_NUMBER_PADDING = 3;
    private static final String DEFAULT_FILENAME_SEPARATOR = " ";

    private final Preferences prefs;

    public AppPreferences() {
        this.prefs = Preferences.userNodeForPackage(AppPreferences.class);
    }

    public String getPictureLibraryDir() {
        return prefs.get(KEY_PICTURE_LIBRARY_DIR, DEFAULT_PICTURE_LIBRARY_DIR);
    }

    public void setPictureLibraryDir(String dir) {
        prefs.put(KEY_PICTURE_LIBRARY_DIR, dir);
    }

    public String getDefaultSourceDir() {
        return prefs.get(KEY_DEFAULT_SOURCE_DIR, DEFAULT_SOURCE_DIR);
    }

    public void setDefaultSourceDir(String dir) {
        prefs.put(KEY_DEFAULT_SOURCE_DIR, dir);
    }

    public int getCounterStart() {
        return prefs.getInt(KEY_COUNTER_START, DEFAULT_COUNTER_START);
    }

    public void setCounterStart(int start) {
        prefs.putInt(KEY_COUNTER_START, start);
    }

    public int getNumberPadding() {
        return prefs.getInt(KEY_NUMBER_PADDING, DEFAULT_NUMBER_PADDING);
    }

    public void setNumberPadding(int padding) {
        prefs.putInt(KEY_NUMBER_PADDING, padding);
    }

    public String getFilenameSeparator() {
        return prefs.get(KEY_FILENAME_SEPARATOR, DEFAULT_FILENAME_SEPARATOR);
    }

    public void setFilenameSeparator(String separator) {
        prefs.put(KEY_FILENAME_SEPARATOR, separator);
    }

    public String getNumberFormat() {
        return "%0" + getNumberPadding() + "d";
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=AppPreferencesTest -q`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/mcs/camera/AppPreferences.java src/test/java/com/mcs/camera/AppPreferencesTest.java
git commit -m "feat: add AppPreferences class for persistent user settings"
```

---

### Task 2: Extend AlbumDetails with new fields

**Files:**
- Modify: `src/main/java/com/mcs/camera/AlbumDetails.java`
- Modify: `src/test/java/com/mcs/camera/AlbumDetailsTest.java`

**Step 1: Update the test**

Add a new test method to `AlbumDetailsTest.java` (keep the existing test as-is for backward compat):

```java
@Test
public void testAlbumDetailsWithOptionsFields() {
    AlbumDetails albumDetails = new AlbumDetails("Vacation", "C:\\Photos", true, "2021-08-15",
            true, false, false, true,
            "D:\\Library", 5, "%04d", "-");

    assertEquals("D:\\Library", albumDetails.getDestinationDir());
    assertEquals(5, albumDetails.getCounterStart());
    assertEquals("%04d", albumDetails.getNumberFormat());
    assertEquals("-", albumDetails.getFilenameSeparator());
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=AlbumDetailsTest -q`
Expected: FAIL — constructor and getters don't exist

**Step 3: Implement**

Add 4 new fields to `AlbumDetails.java`: `destinationDir`, `counterStart`, `numberFormat`, `filenameSeparator`. Add a new 12-arg constructor. Keep the existing 8-arg constructor working by delegating with defaults:

```java
package com.mcs.camera;

public class AlbumDetails {
    private final String prefix;
    private final String sourceDir;
    private final boolean forceDateFlag;
    private final String forceDate;
    private final boolean includeVideos;
    private final boolean inlineVideos;
    private final boolean keepOrder;
    private final boolean tryFilenameDateTimeOnMetadataFail;
    private final String destinationDir;
    private final int counterStart;
    private final String numberFormat;
    private final String filenameSeparator;

    public AlbumDetails(String prefix, String sourceDir, boolean forceDateFlag, String forceDate,
                        boolean includeVideos, boolean inlineVideos, boolean keepOrder,
                        boolean tryFilenameDateTimeOnMetadataFail) {
        this(prefix, sourceDir, forceDateFlag, forceDate, includeVideos, inlineVideos, keepOrder,
                tryFilenameDateTimeOnMetadataFail, "F:\\My Pictures", 1, "%03d", " ");
    }

    public AlbumDetails(String prefix, String sourceDir, boolean forceDateFlag, String forceDate,
                        boolean includeVideos, boolean inlineVideos, boolean keepOrder,
                        boolean tryFilenameDateTimeOnMetadataFail,
                        String destinationDir, int counterStart, String numberFormat,
                        String filenameSeparator) {
        this.prefix = prefix;
        this.sourceDir = sourceDir;
        this.forceDateFlag = forceDateFlag;
        this.forceDate = forceDate;
        this.includeVideos = includeVideos;
        this.inlineVideos = inlineVideos;
        this.keepOrder = keepOrder;
        this.tryFilenameDateTimeOnMetadataFail = tryFilenameDateTimeOnMetadataFail;
        this.destinationDir = destinationDir;
        this.counterStart = counterStart;
        this.numberFormat = numberFormat;
        this.filenameSeparator = filenameSeparator;
    }

    // Existing getters unchanged
    public String getPrefix() { return prefix; }
    public String getSourceDir() { return sourceDir; }
    public boolean isForceDateFlag() { return forceDateFlag; }
    public String getForceDate() { return forceDate; }
    public boolean isIncludeVideos() { return includeVideos; }
    public boolean isInlineVideos() { return inlineVideos; }
    public boolean isKeepOrder() { return keepOrder; }
    public boolean isTryFilenameDateTimeOnMetadataFail() { return tryFilenameDateTimeOnMetadataFail; }

    // New getters
    public String getDestinationDir() { return destinationDir; }
    public int getCounterStart() { return counterStart; }
    public String getNumberFormat() { return numberFormat; }
    public String getFilenameSeparator() { return filenameSeparator; }
}
```

**Step 4: Run all tests**

Run: `mvn test -q`
Expected: ALL PASS (existing test still uses 8-arg constructor, new test uses 12-arg)

**Step 5: Commit**

```bash
git add src/main/java/com/mcs/camera/AlbumDetails.java src/test/java/com/mcs/camera/AlbumDetailsTest.java
git commit -m "feat: extend AlbumDetails with destination, counter, padding, separator fields"
```

---

### Task 3: Update PictureRenamer to use AlbumDetails fields

**Files:**
- Modify: `src/main/java/com/mcs/camera/PictureRenamer.java`
- Modify: `src/test/java/com/mcs/camera/PictureRenamerTest.java`

**Step 1: Write a test for configurable formatting**

Add to `PictureRenamerTest.java`:

```java
@Test
public void testCustomNumberFormatAndSeparator() throws IOException {
    AlbumDetails details = new AlbumDetails(
            "Trip", tempFolder.getRoot().getAbsolutePath(), false, "",
            false, false, false, true,
            tempFolder.getRoot().getAbsolutePath(), 1, "%04d", "-");
    PictureRenamer renamer = new PictureRenamer(details);

    File videoFile = tempFolder.newFile("test.mp4");
    videoFile.setLastModified(1629034245000L);
    renamer.grabMetadata(videoFile, new VideoMetadataExtractor());

    assertNotNull(renamer.albumDirName);
    assertTrue(renamer.albumDirName.contains("Trip"));
}
```

**Step 2: Run test to verify it passes with new constructor** (this is a compatibility check — should pass once Task 2 is done)

Run: `mvn test -Dtest=PictureRenamerTest#testCustomNumberFormatAndSeparator -q`
Expected: PASS

**Step 3: Update PictureRenamer to use AlbumDetails fields instead of hardcoded constants**

Changes to `PictureRenamer.java`:
- Remove `DEFAULT_DESTINATION_DIR` constant (line 19)
- Remove `forceCounter` constant (line 20) — dead code
- Remove `counterStart` constant (line 21) — now from AlbumDetails
- Remove `moveWhenFinished` field (line 27) — always true, inline the behavior
- Remove `allowAlternateParsing` field (line 31) — dead code
- Remove `getCurrentCounter()` method (lines 148-150) — dead code
- Add fields: `destinationDir`, `counterStart`, `numberFormat`, `filenameSeparator`
- Read them from `albumDetails` in constructor
- Replace `DEFAULT_DESTINATION_DIR` usage (line 55) with `this.destinationDir`
- Replace `forceCounter ? counterStart : getCurrentCounter()` (line 93) with `this.counterStart`
- Replace `" "` separator (lines 107, 121) with `this.filenameSeparator`
- Replace `"%03d"` (lines 108, 122) with `this.numberFormat`

The updated constructor and field section:

```java
public class PictureRenamer {
    static final Logger log = LoggerFactory.getLogger(PictureRenamer.class.getName());

    private final String sourceDir;
    private final boolean forceDateFlag;
    private final String forceDate;
    private final boolean keepOrder;
    private final boolean includeVideos;
    private final boolean inlineVideos;
    private final boolean tryFilenameDateTimeOnMetadataFail;
    private final String prefix;
    private final String destinationDir;
    private final int counterStart;
    private final String numberFormat;
    private final String filenameSeparator;

    DateTimeFormatter dateTimeFormatter;
    String albumDirName = null;
    String albumYear = null;
    List<String> temporaryNames = new ArrayList<>();
    Map<String, File> fileMap = new HashMap<>();

    public PictureRenamer(AlbumDetails albumDetails) {
        this.prefix = albumDetails.getPrefix();
        this.sourceDir = albumDetails.getSourceDir();
        this.forceDateFlag = albumDetails.isForceDateFlag();
        this.forceDate = albumDetails.getForceDate();
        this.includeVideos = albumDetails.isIncludeVideos();
        this.inlineVideos = albumDetails.isInlineVideos();
        this.keepOrder = albumDetails.isKeepOrder();
        this.tryFilenameDateTimeOnMetadataFail = albumDetails.isTryFilenameDateTimeOnMetadataFail();
        this.destinationDir = albumDetails.getDestinationDir();
        this.counterStart = albumDetails.getCounterStart();
        this.numberFormat = albumDetails.getNumberFormat();
        this.filenameSeparator = albumDetails.getFilenameSeparator();
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
```

In `renamePictures()`:
- Line 55: `String destDirPath = destinationDir;`
- Line 93: `int currentPictureCounter = counterStart;`
- Lines 107-108: `prefix + filenameSeparator + String.format(numberFormat, currentPictureCounter)`
- Lines 121-122: same pattern for videos

**Step 4: Run all tests**

Run: `mvn test -q`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/main/java/com/mcs/camera/PictureRenamer.java src/test/java/com/mcs/camera/PictureRenamerTest.java
git commit -m "refactor: PictureRenamer uses AlbumDetails for destination, counter, padding, separator"
```

---

### Task 4: Update PictureRenumberer to accept padding and separator

**Files:**
- Modify: `src/main/java/com/mcs/camera/PictureRenumberer.java`

**Step 1: Update constructor and rename logic**

Add `numberFormat` and `filenameSeparator` parameters to the constructor. Replace hardcoded `" "` and `"%03d"` in `renumberPictures()`.

Updated constructor:

```java
public PictureRenumberer(String directory, String prefix, boolean includeVideos, boolean inlineVideos,
                         String numberFormat, String filenameSeparator) {
    this.directory = directory;
    this.prefix = prefix;
    this.includeVideos = includeVideos;
    this.inlineVideos = inlineVideos;
    this.numberFormat = numberFormat;
    this.filenameSeparator = filenameSeparator;
    this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
```

Add fields:

```java
private final String numberFormat;
private final String filenameSeparator;
```

In `renumberPictures()`:
- Line 95: `prefix + filenameSeparator + String.format(numberFormat, counter)`
- Line 105: same for videos

**Step 2: Run all tests**

Run: `mvn test -q`
Expected: ALL PASS (no tests directly construct PictureRenumberer)

**Step 3: Commit**

```bash
git add src/main/java/com/mcs/camera/PictureRenumberer.java
git commit -m "refactor: PictureRenumberer accepts number format and filename separator"
```

---

### Task 5: Build Options dialog in UIHandler and wire everything together

**Files:**
- Modify: `src/main/java/com/mcs/camera/UIHandler.java`

**Step 1: Add AppPreferences field and update UIHandler**

Changes:
1. Add `private final AppPreferences appPreferences = new AppPreferences();` field
2. Remove `DEFAULT_SOURCE_DIR` and `DEFAULT_RENUMBER_BROWSE_DIR` constants — replaced by `appPreferences` calls
3. Replace `DEFAULT_SOURCE_DIR` usage (line 198) with `appPreferences.getDefaultSourceDir()`
4. Replace `DEFAULT_RENUMBER_BROWSE_DIR` usage (line 355) with `appPreferences.getPictureLibraryDir()`
5. Replace `DEFAULT_SOURCE_DIR` in `resetRenameForm()` (line 540) with `appPreferences.getDefaultSourceDir()`
6. Enable the Options menu item (remove `setEnabled(false)` at line 115) and add action listener
7. In `processRename()`: build AlbumDetails with 12-arg constructor, passing prefs values
8. In `processRenumber()`: pass `appPreferences.getNumberFormat()` and `appPreferences.getFilenameSeparator()` to PictureRenumberer

**Step 2: Build the Options dialog method**

```java
private void showOptionsDialog() {
    JDialog dialog = new JDialog(mainFrame, "Options", true);
    dialog.setLayout(new BorderLayout());

    JPanel formPanel = new JPanel(new GridBagLayout());
    formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;

    // Picture Library Directory
    gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
    JLabel libDirLabel = new JLabel("Picture library:");
    libDirLabel.setToolTipText("Home directory of your picture library. Used as the destination for renamed files and the default browse location for renumbering.");
    formPanel.add(libDirLabel, gbc);

    gbc.gridx = 1; gbc.weightx = 1.0;
    JPanel libDirPanel = new JPanel(new BorderLayout());
    JTextField libDirField = new JTextField(appPreferences.getPictureLibraryDir(), 30);
    JButton libDirBrowse = new JButton("Browse");
    libDirPanel.add(libDirField, BorderLayout.CENTER);
    libDirPanel.add(libDirBrowse, BorderLayout.EAST);
    formPanel.add(libDirPanel, gbc);

    libDirBrowse.addActionListener(e -> {
        JFileChooser chooser = new JFileChooser(libDirField.getText().trim());
        chooser.setDialogTitle("Select Picture Library Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
            libDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    });

    // Default Source Directory
    gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
    JLabel srcDirLabel = new JLabel("Default source directory:");
    srcDirLabel.setToolTipText("Default directory to import photos from.");
    formPanel.add(srcDirLabel, gbc);

    gbc.gridx = 1; gbc.weightx = 1.0;
    JPanel srcDirPanel = new JPanel(new BorderLayout());
    JTextField srcDirField = new JTextField(appPreferences.getDefaultSourceDir(), 30);
    JButton srcDirBrowse = new JButton("Browse");
    srcDirPanel.add(srcDirField, BorderLayout.CENTER);
    srcDirPanel.add(srcDirBrowse, BorderLayout.EAST);
    formPanel.add(srcDirPanel, gbc);

    srcDirBrowse.addActionListener(e -> {
        JFileChooser chooser = new JFileChooser(srcDirField.getText().trim());
        chooser.setDialogTitle("Select Default Source Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
            srcDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    });

    // Counter Start
    gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
    formPanel.add(new JLabel("Counter start:"), gbc);

    gbc.gridx = 1; gbc.weightx = 0;
    JSpinner counterSpinner = new JSpinner(new SpinnerNumberModel(
            appPreferences.getCounterStart(), 1, 999, 1));
    formPanel.add(counterSpinner, gbc);

    // Number Padding
    gbc.gridx = 0; gbc.gridy = 3;
    formPanel.add(new JLabel("Number padding:"), gbc);

    gbc.gridx = 1;
    JComboBox<String> paddingCombo = new JComboBox<>(new String[]{"2 digits", "3 digits", "4 digits"});
    paddingCombo.setSelectedIndex(appPreferences.getNumberPadding() - 2);
    formPanel.add(paddingCombo, gbc);

    // Filename Separator
    gbc.gridx = 0; gbc.gridy = 4;
    formPanel.add(new JLabel("Filename separator:"), gbc);

    gbc.gridx = 1;
    String[] separatorLabels = {"Space", "Dash", "Underscore", "None"};
    JComboBox<String> separatorCombo = new JComboBox<>(separatorLabels);
    String currentSep = appPreferences.getFilenameSeparator();
    switch (currentSep) {
        case " " -> separatorCombo.setSelectedIndex(0);
        case "-" -> separatorCombo.setSelectedIndex(1);
        case "_" -> separatorCombo.setSelectedIndex(2);
        case "" -> separatorCombo.setSelectedIndex(3);
        default -> separatorCombo.setSelectedIndex(0);
    }
    formPanel.add(separatorCombo, gbc);

    // Buttons
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton okButton = new JButton("OK");
    JButton cancelButton = new JButton("Cancel");
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);

    okButton.addActionListener(e -> {
        appPreferences.setPictureLibraryDir(libDirField.getText().trim());
        appPreferences.setDefaultSourceDir(srcDirField.getText().trim());
        appPreferences.setCounterStart((Integer) counterSpinner.getValue());
        appPreferences.setNumberPadding(paddingCombo.getSelectedIndex() + 2);
        String[] separatorValues = {" ", "-", "_", ""};
        appPreferences.setFilenameSeparator(separatorValues[separatorCombo.getSelectedIndex()]);
        // Update form defaults to reflect new prefs
        sourceDirField.setText(appPreferences.getDefaultSourceDir());
        dialog.dispose();
    });

    cancelButton.addActionListener(e -> dialog.dispose());

    dialog.add(formPanel, BorderLayout.CENTER);
    dialog.add(buttonPanel, BorderLayout.SOUTH);
    dialog.getRootPane().setDefaultButton(okButton);
    dialog.pack();
    dialog.setResizable(false);
    dialog.setLocationRelativeTo(mainFrame);
    dialog.setVisible(true);
}
```

**Step 3: Wire Options menu item**

In `createMenuBar()`, replace lines 113-116:

```java
JMenuItem optionsItem = new JMenuItem("Options...");
optionsItem.setMnemonic(KeyEvent.VK_O);
optionsItem.addActionListener(e -> showOptionsDialog());
editMenu.add(optionsItem);
```

**Step 4: Wire AlbumDetails construction in processRename()**

Replace the AlbumDetails construction at line 492:

```java
AlbumDetails albumDetails = new AlbumDetails(prefix, sourceDir, forceDateFlag, forceDate,
        includeVideos, inlineVideos, keepOrder, tryFilenameDateTimeOnMetadataFail,
        appPreferences.getPictureLibraryDir(), appPreferences.getCounterStart(),
        appPreferences.getNumberFormat(), appPreferences.getFilenameSeparator());
```

**Step 5: Wire PictureRenumberer construction in processRenumber()**

Replace lines 616-617:

```java
PictureRenumberer renumberer = new PictureRenumberer(directory, prefix,
        includeVideos, inlineVideos,
        appPreferences.getNumberFormat(), appPreferences.getFilenameSeparator());
```

**Step 6: Run all tests**

Run: `mvn test -q`
Expected: ALL PASS

**Step 7: Build and manually verify**

Run: `mvn package -q`

Manual checks:
1. Launch exe, Edit > Options... opens dialog
2. All fields show defaults
3. Change picture library dir, click OK, relaunch — setting persists
4. Change padding to 4, separator to Dash — process a test rename and verify filenames like `Album-0001.jpg`

**Step 8: Commit**

```bash
git add src/main/java/com/mcs/camera/UIHandler.java
git commit -m "feat: add Options dialog with persistent settings for paths, counter, padding, separator"
```

---

### Task 6: Update CLAUDE.md and tracking

**Files:**
- Modify: `CLAUDE.md`
- Modify (memory): `improvements.md`

**Step 1: Update CLAUDE.md**

- Add `AppPreferences.java` to project structure
- Update Architecture section to mention Options dialog and AppPreferences
- Note that Edit > Options... is now functional (remove "disabled placeholder" references)

**Step 2: Update improvements.md**

- Mark "Implement Edit > Options... dialog" as done
- Mark "Configurable destination directory" as done

**Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md for Options dialog and AppPreferences"
```
