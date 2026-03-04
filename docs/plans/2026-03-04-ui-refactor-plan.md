# UI Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the dialog-driven UI with a standard frame-based Windows desktop app that combines both PictureRenamer and PictureRenumberer into one unified window with tabs, a menu bar, and event-driven SwingWorker processing.

**Architecture:** `Main.java` splits app title from version. `UIHandler.java` is rewritten as a frame-based UI with a `JTabbedPane` holding two tabs: "Rename" (current PictureRenamer workflow) and "Renumber" (current PictureRenumberer workflow). Both share a common menu bar (File/Edit/Help) and use SwingWorker for background processing. `PictureRenumberer` gets a new constructor accepting parameters instead of hardcoded constants, and its `main()` and UI code are removed.

**Tech Stack:** Java 17, Swing (JFrame, JTabbedPane, JMenuBar, GridBagLayout, SwingWorker), JNA (User32 for FindWindow)

---

### Task 1: Split Title and Version in Main.java

**Files:**
- Modify: `src/main/java/com/mcs/camera/Main.java`

**Step 1: Update Main.java**

```java
package com.mcs.camera;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class Main {
    static final Logger log = LoggerFactory.getLogger(Main.class.getName());
    private static final String LOCK_FILE = System.getProperty("user.home") + File.separator + ".PictureRenamer.lock";
    private static final String APP_TITLE = "Picture Renamer";
    private static final String APP_VERSION = loadAppVersion();

    public static String getAppTitle() {
        return APP_TITLE;
    }

    public static String getAppVersion() {
        return APP_VERSION;
    }

    private static String loadAppVersion() {
        try (InputStream is = Main.class.getResourceAsStream("/app.properties")) {
            Properties props = new Properties();
            props.load(is);
            return props.getProperty("app.version", "?");
        } catch (Exception e) {
            return "?";
        }
    }

    public static void main(String[] args) {
        log.info("Starting {} v{}", APP_TITLE, APP_VERSION);

        try {
            File file = new File(LOCK_FILE);
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            FileChannel channel = randomAccessFile.getChannel();
            FileLock lock = channel.tryLock();

            if (lock == null) {
                log.info("Another instance of PictureRenamer is already running.");
                focusExistingWindow();
                System.exit(0);
            }

            SwingUtilities.invokeLater(() -> {
                UIHandler uiHandler = new UIHandler(lock, randomAccessFile, file);
                uiHandler.createAndShowGUI();
            });

        } catch (Exception e) {
            log.error("An error occurred", e);
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void focusExistingWindow() {
        User32 user32 = User32.INSTANCE;
        HWND hwnd = user32.FindWindow(null, APP_TITLE);
        if (hwnd != null) {
            user32.ShowWindow(hwnd, User32.SW_RESTORE);
            user32.SetForegroundWindow(hwnd);
            Toolkit.getDefaultToolkit().beep();
            log.info("Switched focus to existing PictureRenamer window.");
        } else {
            log.warn("Another instance of PictureRenamer is running, but the window couldn't be found.");
        }
    }
}
```

Key changes from current `Main.java`:
- `APP_TITLE` is now a static string `"Picture Renamer"` (no version)
- `loadAppTitle()` replaced with `loadAppVersion()` returning just the version number
- Public getters `getAppTitle()` and `getAppVersion()` for UIHandler to use
- `UIHandler` constructor no longer receives `appTitle`
- Log message uses SLF4J parameterized format

**Step 2: Commit** (compilation will temporarily fail until Task 2)

```bash
git add src/main/java/com/mcs/camera/Main.java
git commit -m "refactor: split app title and version in Main, remove version from title"
```

---

### Task 2: Refactor PictureRenumberer to Accept Parameters

**Files:**
- Modify: `src/main/java/com/mcs/camera/PictureRenumberer.java`

**Step 1: Refactor PictureRenumberer**

Remove `main()`, the `MyJFileChooser` inner class, the `getHomeDirPath()` method, and all hardcoded constants. Add a constructor that accepts parameters and make `renumberPictures()` public.

The core renumber logic stays the same: read metadata, sort, two-pass rename (randomize then re-sequence). The key behavioral difference from PictureRenamer is: **no move step** — files stay in place.

```java
package com.mcs.camera;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PictureRenumberer {

    static final Logger log = LoggerFactory.getLogger(PictureRenumberer.class.getName());

    private final String directory;
    private final String prefix;
    private final boolean includeVideos;
    private final boolean inlineVideos;

    DateTimeFormatter dateTimeFormatter;
    List<String> temporaryNames = new ArrayList<>();
    Map<String, File> fileMap = new HashMap<>();

    public PictureRenumberer(String directory, String prefix, boolean includeVideos, boolean inlineVideos) {
        this.directory = directory;
        this.prefix = prefix;
        this.includeVideos = includeVideos;
        this.inlineVideos = inlineVideos;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    public void renumberPictures() {
        List<File> videos = new ArrayList<>();
        String homeDirPath = directory;
        if (!homeDirPath.endsWith(File.separator)) {
            homeDirPath += File.separator;
        }

        Collection<File> filesInHomeDir = getFilesInDir(homeDirPath);
        if (filesInHomeDir.isEmpty()) {
            log.warn("No files found in directory -- " + homeDirPath);
            return;
        }

        for (File f : filesInHomeDir) {
            String fileExtension = FilenameUtils.getExtension(f.getName()).toLowerCase();
            switch (fileExtension) {
                case "jpg", "jpeg", "heic" -> grabExifMetadata(f);
                case "png" -> grabPngMetadata(f);
                case "mov", "avi", "mkv", "mp4", "mts", "m2ts" -> {
                    if (includeVideos) {
                        if (inlineVideos) {
                            grabMovieMetadata(f);
                        } else {
                            videos.add(f);
                        }
                    }
                }
                default -> log.warn("Ignoring unknown file type: " + f.getName());
            }
        }

        Collections.sort(temporaryNames);

        int counter = 1;

        // Pass 1: rename all to random names to avoid collisions
        for (String orderedPicture : temporaryNames) {
            File origFile = fileMap.get(orderedPicture);
            if (!origFile.exists()) {
                log.error("Prior to renaming -- cannot find file " + origFile.getName());
                return;
            }
            File renamedFile = new File(origFile.getParent() + File.separator
                    + RandomStringUtils.randomAlphanumeric(8) + "."
                    + FilenameUtils.getExtension(origFile.getName()).toLowerCase());
            origFile.renameTo(renamedFile);
            fileMap.put(orderedPicture, renamedFile);
        }

        // Pass 2: rename to final sequential names
        for (String orderedPicture : temporaryNames) {
            File origFile = fileMap.get(orderedPicture);
            String newFileName = origFile.getParent() + File.separator + prefix + " "
                    + String.format("%03d", counter) + "."
                    + FilenameUtils.getExtension(origFile.getName()).toLowerCase();
            origFile.renameTo(new File(newFileName));
            counter++;
        }

        if (includeVideos && !inlineVideos) {
            for (File f : videos) {
                String extension = FilenameUtils.getExtension(f.getName()).toLowerCase();
                String newFileName = f.getParent() + File.separator + prefix + " "
                        + String.format("%03d", counter) + "." + extension;
                log.debug("Renaming video file " + f.getName() + " to " + newFileName);
                f.renameTo(new File(newFileName));
                counter++;
            }
        }
    }

    private Collection<File> getFilesInDir(String directoryPath) {
        log.info("Using directory: " + directoryPath);
        final File dir = new File(directoryPath);
        return FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, null);
    }

    private void grabExifMetadata(File f) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(f);
            Directory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            LocalDateTime dateTaken = null;
            if (directory != null) {
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
                if (date != null) {
                    dateTaken = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }

            if (dateTaken == null) {
                dateTaken = Instant.ofEpochMilli(f.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }

            String dateTakenStr = dateTaken.format(dateTimeFormatter);
            String tempName = dateTakenStr + " " + f.getName();
            log.debug("Date taken: " + f.getName() + " --> " + dateTakenStr);
            temporaryNames.add(tempName);
            fileMap.put(tempName, f);
        } catch (Exception e) {
            log.error("Error processing image file " + f.getName(), e);
            throw new RuntimeException("Error processing " + f.getName(), e);
        }
    }

    private void grabPngMetadata(File f) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(f);
            Directory directory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);

            LocalDateTime dateTaken = null;
            if (directory != null) {
                Date date = directory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE, TimeZone.getDefault());
                if (date != null) {
                    dateTaken = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }

            if (dateTaken == null) {
                dateTaken = Instant.ofEpochMilli(f.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }

            String dateTakenStr = dateTaken.format(dateTimeFormatter);
            String tempName = dateTakenStr + " " + f.getName();
            log.debug("Date taken: " + f.getName() + " --> " + dateTakenStr);
            temporaryNames.add(tempName);
            fileMap.put(tempName, f);
        } catch (Exception e) {
            log.error("Error processing PNG file " + f.getName(), e);
            throw new RuntimeException("Error processing " + f.getName(), e);
        }
    }

    private void grabMovieMetadata(File f) {
        try {
            LocalDateTime dateTaken = Instant.ofEpochMilli(f.lastModified())
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            String dateTakenStr = dateTaken.format(dateTimeFormatter);
            String tempName = dateTakenStr + " " + f.getName();
            log.debug("Date taken: " + f.getName() + " --> " + dateTakenStr);
            temporaryNames.add(tempName);
            fileMap.put(tempName, f);
        } catch (Exception e) {
            log.error("Error processing video file " + f.getName(), e);
            throw new RuntimeException("Error processing " + f.getName(), e);
        }
    }
}
```

Key changes from current `PictureRenumberer.java`:
- Removed `main()`, `getHomeDirPath()`, and `MyJFileChooser` inner class
- Removed all hardcoded constants — constructor accepts `directory`, `prefix`, `includeVideos`, `inlineVideos`
- `renumberPictures()` is now public
- Consolidated `grabJpgMetadata()` into `grabExifMetadata()` (works for JPG and HEIC)
- Added HEIC, MP4, MTS, M2TS to file extension handling
- Replaced `System.exit(1)` with `throw new RuntimeException(...)` for proper error propagation
- Simplified metadata fallback: EXIF → `file.lastModified()` (no filename parsing or force date — renumber operates on already-named files)
- Uses `origFile.getParent()` instead of concatenating homeDirPath

**Step 2: Commit**

```bash
git add src/main/java/com/mcs/camera/PictureRenumberer.java
git commit -m "refactor: PictureRenumberer accepts parameters, remove main and hardcoded constants"
```

---

### Task 3: Rewrite UIHandler — Tabbed Frame UI with Menu Bar

**Files:**
- Modify: `src/main/java/com/mcs/camera/UIHandler.java`

**Step 1: Rewrite UIHandler.java**

Full rewrite. The new UIHandler builds a visible JFrame with:
- Menu bar: File (Exit), Edit (Options... disabled), Help (About)
- JTabbedPane with two tabs: "Rename" and "Renumber"
- Rename tab: album name, source dir, options, Process/Reset buttons (current PictureRenamer workflow)
- Renumber tab: album directory picker, auto-extracted prefix (editable), video options, Renumber/Reset buttons
- SwingWorker for both processing paths

```java
package com.mcs.camera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class UIHandler {
    static final Logger log = LoggerFactory.getLogger(UIHandler.class.getName());
    private JFrame mainFrame;
    private final FileLock lock;
    private final RandomAccessFile randomAccessFile;
    private final File lockFile;

    // ── Rename tab fields ──
    private JTextField albumNameField;
    private JTextField sourceDirField;
    private JCheckBox forceDateFlagCheckBox;
    private JTextField forceDateField;
    private JLabel forceDateLabel;
    private JCheckBox includeVideosCheckBox;
    private JCheckBox inlineVideosCheckBox;
    private JCheckBox keepOrderCheckBox;
    private JCheckBox tryFilenameDateTimeCheckBox;
    private JButton processButton;
    private JButton resetButton;

    // ── Renumber tab fields ──
    private JTextField renumberDirField;
    private JTextField renumberPrefixField;
    private JCheckBox renumberIncludeVideosCheckBox;
    private JCheckBox renumberInlineVideosCheckBox;
    private JButton renumberButton;
    private JButton renumberResetButton;

    // Default values
    private static final String DEFAULT_SOURCE_DIR = "H:\\Picture Merge";
    private static final String DEFAULT_RENUMBER_BROWSE_DIR = "F:\\My Pictures";

    public UIHandler(FileLock lock, RandomAccessFile randomAccessFile, File lockFile) {
        this.lock = lock;
        this.randomAccessFile = randomAccessFile;
        this.lockFile = lockFile;
    }

    public void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            log.warn("Could not set Look and Feel", e);
        }

        mainFrame = new JFrame(Main.getAppTitle());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set app icon
        try {
            java.util.List<Image> icons = new java.util.ArrayList<>();
            for (int size : new int[]{16, 32, 48, 256}) {
                BufferedImage img = ImageIO.read(getClass().getResourceAsStream("/icon-" + size + ".png"));
                if (img != null) icons.add(img);
            }
            if (!icons.isEmpty()) mainFrame.setIconImages(icons);
        } catch (Exception e) {
            log.warn("Could not load app icon", e);
        }

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });

        mainFrame.setJMenuBar(createMenuBar());
        mainFrame.setContentPane(createContentPane());

        mainFrame.pack();
        mainFrame.setMinimumSize(mainFrame.getSize());
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    // ── Menu Bar ──────────────────────────────────────────────

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> {
            cleanup();
            System.exit(0);
        });
        fileMenu.add(exitItem);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        JMenuItem optionsItem = new JMenuItem("Options...");
        optionsItem.setMnemonic(KeyEvent.VK_O);
        optionsItem.setEnabled(false);
        editMenu.add(optionsItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setMnemonic(KeyEvent.VK_A);
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    // ── Content Pane with Tabs ────────────────────────────────

    private JPanel createContentPane() {
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Rename", createRenameTab());
        tabbedPane.addTab("Renumber", createRenumberTab());

        contentPane.add(tabbedPane, BorderLayout.CENTER);
        return contentPane;
    }

    // ── Rename Tab ────────────────────────────────────────────

    private JPanel createRenameTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.add(createAlbumDetailsPanel());
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(createRenameOptionsPanel());

        tab.add(formPanel, BorderLayout.CENTER);
        tab.add(createRenameButtonPanel(), BorderLayout.SOUTH);

        return tab;
    }

    private JPanel createAlbumDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Album Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Album Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel albumNameLabel = new JLabel("Album Name:");
        albumNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        albumNameLabel.setToolTipText("Enter the name of the album.");
        panel.add(albumNameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        albumNameField = new JTextField(20);
        panel.add(albumNameField, gbc);

        // Source Directory
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel sourceDirLabel = new JLabel("Source Directory:");
        sourceDirLabel.setFont(new Font("Arial", Font.BOLD, 14));
        sourceDirLabel.setToolTipText("Select the directory containing media files.");
        panel.add(sourceDirLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel sourceDirPanel = new JPanel(new BorderLayout());
        sourceDirField = new JTextField(DEFAULT_SOURCE_DIR, 20);
        JButton browseButton = new JButton("Browse");
        sourceDirPanel.add(sourceDirField, BorderLayout.CENTER);
        sourceDirPanel.add(browseButton, BorderLayout.EAST);
        panel.add(sourceDirPanel, gbc);

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(sourceDirField.getText().trim()));
            chooser.setDialogTitle("Select Source Directory");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                sourceDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        return panel;
    }

    private JPanel createRenameOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Options"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        forceDateFlagCheckBox = new JCheckBox("Force Date", false);
        forceDateFlagCheckBox.setToolTipText("Select to use a specific date for all files.");
        panel.add(forceDateFlagCheckBox, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 27, 2, 5);
        forceDateLabel = new JLabel("Forced Date (YYYY-MM-DD):");
        forceDateLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        forceDateLabel.setToolTipText("Enter the date to use when 'Force Date' is selected.");
        forceDateLabel.setEnabled(false);
        panel.add(forceDateLabel, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 5, 2, 5);
        forceDateField = new JTextField(10);
        forceDateField.setEnabled(false);
        panel.add(forceDateField, gbc);

        forceDateFlagCheckBox.addItemListener(e -> {
            boolean selected = forceDateFlagCheckBox.isSelected();
            forceDateField.setEnabled(selected);
            forceDateLabel.setEnabled(selected);
        });

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 5, 2, 5);
        includeVideosCheckBox = new JCheckBox("Include videos", true);
        includeVideosCheckBox.setToolTipText("Include video files in the processing.");
        panel.add(includeVideosCheckBox, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(2, 5, 2, 5);
        inlineVideosCheckBox = new JCheckBox("Number videos inline", true);
        inlineVideosCheckBox.setToolTipText("Include videos in the numbering sequence.");
        panel.add(inlineVideosCheckBox, gbc);

        gbc.gridy = 4;
        keepOrderCheckBox = new JCheckBox("Keep order", false);
        keepOrderCheckBox.setToolTipText("Keep the original order of files.");
        panel.add(keepOrderCheckBox, gbc);

        gbc.gridy = 5;
        tryFilenameDateTimeCheckBox = new JCheckBox("Use filename date/time if metadata unavailable", true);
        tryFilenameDateTimeCheckBox.setToolTipText("Attempt to extract date/time from filename if metadata is missing.");
        panel.add(tryFilenameDateTimeCheckBox, gbc);

        return panel;
    }

    private JPanel createRenameButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        processButton = new JButton("Process");
        processButton.setFont(new Font("Arial", Font.BOLD, 13));
        processButton.addActionListener(e -> processRename());

        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetRenameForm());

        panel.add(processButton);
        panel.add(resetButton);

        return panel;
    }

    // ── Renumber Tab ──────────────────────────────────────────

    private JPanel createRenumberTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.add(createRenumberDetailsPanel());
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(createRenumberOptionsPanel());

        tab.add(formPanel, BorderLayout.CENTER);
        tab.add(createRenumberButtonPanel(), BorderLayout.SOUTH);

        return tab;
    }

    private JPanel createRenumberDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Album Directory"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Album Directory
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel dirLabel = new JLabel("Album Directory:");
        dirLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dirLabel.setToolTipText("Select the existing album directory to renumber.");
        panel.add(dirLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel dirPanel = new JPanel(new BorderLayout());
        renumberDirField = new JTextField(20);
        JButton browseButton = new JButton("Browse");
        dirPanel.add(renumberDirField, BorderLayout.CENTER);
        dirPanel.add(browseButton, BorderLayout.EAST);
        panel.add(dirPanel, gbc);

        // Prefix (auto-extracted from directory name, editable)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel prefixLabel = new JLabel("Album Prefix:");
        prefixLabel.setFont(new Font("Arial", Font.BOLD, 14));
        prefixLabel.setToolTipText("Auto-extracted from directory name. Edit to override.");
        panel.add(prefixLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        renumberPrefixField = new JTextField(20);
        panel.add(renumberPrefixField, gbc);

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(DEFAULT_RENUMBER_BROWSE_DIR));
            chooser.setDialogTitle("Select Album Directory");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                renumberDirField.setText(path);
                // Auto-extract prefix from directory name (text after last comma)
                String dirName = chooser.getSelectedFile().getName();
                int commaIndex = dirName.lastIndexOf(',');
                if (commaIndex >= 0 && commaIndex < dirName.length() - 1) {
                    renumberPrefixField.setText(dirName.substring(commaIndex + 1).trim());
                } else {
                    renumberPrefixField.setText(dirName);
                }
            }
        });

        return panel;
    }

    private JPanel createRenumberOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Options"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        renumberIncludeVideosCheckBox = new JCheckBox("Include videos", true);
        renumberIncludeVideosCheckBox.setToolTipText("Include video files in the renumbering.");
        panel.add(renumberIncludeVideosCheckBox, gbc);

        gbc.gridy = 1;
        renumberInlineVideosCheckBox = new JCheckBox("Number videos inline", true);
        renumberInlineVideosCheckBox.setToolTipText("Include videos in the numbering sequence.");
        panel.add(renumberInlineVideosCheckBox, gbc);

        return panel;
    }

    private JPanel createRenumberButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

        renumberButton = new JButton("Renumber");
        renumberButton.setFont(new Font("Arial", Font.BOLD, 13));
        renumberButton.addActionListener(e -> processRenumber());

        renumberResetButton = new JButton("Reset");
        renumberResetButton.addActionListener(e -> resetRenumberForm());

        panel.add(renumberButton);
        panel.add(renumberResetButton);

        return panel;
    }

    // ── Rename Processing ─────────────────────────────────────

    private void processRename() {
        String prefix = albumNameField.getText().trim();
        String sourceDir = sourceDirField.getText().trim();
        boolean forceDateFlag = forceDateFlagCheckBox.isSelected();
        String forceDate = forceDateField.getText().trim();
        boolean includeVideos = includeVideosCheckBox.isSelected();
        boolean inlineVideos = inlineVideosCheckBox.isSelected();
        boolean keepOrder = keepOrderCheckBox.isSelected();
        boolean tryFilenameDateTimeOnMetadataFail = tryFilenameDateTimeCheckBox.isSelected();

        // Validation
        if (prefix.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Album name cannot be empty.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            albumNameField.requestFocusInWindow();
            return;
        }

        if (forceDateFlag) {
            if (forceDate.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Forced Date is required when Force Date is selected.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                forceDateField.requestFocusInWindow();
                return;
            }
            if (!forceDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(mainFrame, "Forced Date must be in YYYY-MM-DD format.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                forceDateField.requestFocusInWindow();
                return;
            }
        }

        File srcDir = new File(sourceDir);
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            JOptionPane.showMessageDialog(mainFrame, "Source directory does not exist.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            sourceDirField.requestFocusInWindow();
            return;
        }
        if (srcDir.listFiles() == null || srcDir.listFiles().length == 0) {
            JOptionPane.showMessageDialog(mainFrame, "Source directory is empty.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            sourceDirField.requestFocusInWindow();
            return;
        }

        // Confirmation
        StringBuilder msg = new StringBuilder();
        msg.append("<html><body style='width: 450px; font-family: Arial, sans-serif;'>");
        msg.append("<h2 style='color: #4a4a4a; font-size: 18px;'>Confirm Rename</h2>");
        msg.append("<table style='width: 100%; border-collapse: collapse;'>");
        msg.append(formatRow("Album Name", prefix));
        msg.append(formatRow("Source Directory", sourceDir));
        msg.append(formatRow("Force Date", forceDateFlag ? "Yes" : "No"));
        if (forceDateFlag) {
            msg.append(formatRow("Forced Date", forceDate));
        }
        msg.append(formatRow("Include Videos", includeVideos ? "Yes" : "No"));
        msg.append(formatRow("Number Videos Inline", inlineVideos ? "Yes" : "No"));
        msg.append(formatRow("Keep Order", keepOrder ? "Yes" : "No"));
        msg.append(formatRow("Use Filename Date/Time", tryFilenameDateTimeOnMetadataFail ? "Yes" : "No"));
        msg.append("</table>");
        msg.append("<p style='color: #4a4a4a; font-weight: bold; font-size: 14px; margin-top: 15px;'>");
        msg.append("Do you want to proceed?</p>");
        msg.append("</body></html>");

        int confirm = JOptionPane.showConfirmDialog(mainFrame, new JLabel(msg.toString()),
                "Confirm Rename", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        AlbumDetails albumDetails = new AlbumDetails(prefix, sourceDir, forceDateFlag, forceDate,
                includeVideos, inlineVideos, keepOrder, tryFilenameDateTimeOnMetadataFail);

        setRenameFormEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                PictureRenamer renamer = new PictureRenamer(albumDetails);
                renamer.renamePictures();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(mainFrame, "Rename completed successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    resetRenameForm();
                } catch (Exception ex) {
                    log.error("Error during rename", ex);
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(mainFrame,
                            "An error occurred: " + cause.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                setRenameFormEnabled(true);
            }
        }.execute();
    }

    private void setRenameFormEnabled(boolean enabled) {
        albumNameField.setEnabled(enabled);
        sourceDirField.setEnabled(enabled);
        forceDateFlagCheckBox.setEnabled(enabled);
        forceDateField.setEnabled(enabled && forceDateFlagCheckBox.isSelected());
        forceDateLabel.setEnabled(enabled && forceDateFlagCheckBox.isSelected());
        includeVideosCheckBox.setEnabled(enabled);
        inlineVideosCheckBox.setEnabled(enabled);
        keepOrderCheckBox.setEnabled(enabled);
        tryFilenameDateTimeCheckBox.setEnabled(enabled);
        processButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
    }

    private void resetRenameForm() {
        albumNameField.setText("");
        sourceDirField.setText(DEFAULT_SOURCE_DIR);
        forceDateFlagCheckBox.setSelected(false);
        forceDateField.setText("");
        forceDateField.setEnabled(false);
        forceDateLabel.setEnabled(false);
        includeVideosCheckBox.setSelected(true);
        inlineVideosCheckBox.setSelected(true);
        keepOrderCheckBox.setSelected(false);
        tryFilenameDateTimeCheckBox.setSelected(true);
        albumNameField.requestFocusInWindow();
    }

    // ── Renumber Processing ───────────────────────────────────

    private void processRenumber() {
        String directory = renumberDirField.getText().trim();
        String prefix = renumberPrefixField.getText().trim();
        boolean includeVideos = renumberIncludeVideosCheckBox.isSelected();
        boolean inlineVideos = renumberInlineVideosCheckBox.isSelected();

        // Validation
        if (directory.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Album directory cannot be empty.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            renumberDirField.requestFocusInWindow();
            return;
        }

        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            JOptionPane.showMessageDialog(mainFrame, "Album directory does not exist.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            renumberDirField.requestFocusInWindow();
            return;
        }
        if (dir.listFiles() == null || dir.listFiles().length == 0) {
            JOptionPane.showMessageDialog(mainFrame, "Album directory is empty.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            renumberDirField.requestFocusInWindow();
            return;
        }

        if (prefix.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Album prefix cannot be empty.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            renumberPrefixField.requestFocusInWindow();
            return;
        }

        // Confirmation
        StringBuilder msg = new StringBuilder();
        msg.append("<html><body style='width: 450px; font-family: Arial, sans-serif;'>");
        msg.append("<h2 style='color: #4a4a4a; font-size: 18px;'>Confirm Renumber</h2>");
        msg.append("<table style='width: 100%; border-collapse: collapse;'>");
        msg.append(formatRow("Album Directory", directory));
        msg.append(formatRow("Album Prefix", prefix));
        msg.append(formatRow("Include Videos", includeVideos ? "Yes" : "No"));
        msg.append(formatRow("Number Videos Inline", inlineVideos ? "Yes" : "No"));
        msg.append("</table>");
        msg.append("<p style='color: #4a4a4a; font-weight: bold; font-size: 14px; margin-top: 15px;'>");
        msg.append("Do you want to proceed?</p>");
        msg.append("</body></html>");

        int confirm = JOptionPane.showConfirmDialog(mainFrame, new JLabel(msg.toString()),
                "Confirm Renumber", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setRenumberFormEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                PictureRenumberer renumberer = new PictureRenumberer(directory, prefix,
                        includeVideos, inlineVideos);
                renumberer.renumberPictures();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(mainFrame, "Renumber completed successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    resetRenumberForm();
                } catch (Exception ex) {
                    log.error("Error during renumber", ex);
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(mainFrame,
                            "An error occurred: " + cause.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                setRenumberFormEnabled(true);
            }
        }.execute();
    }

    private void setRenumberFormEnabled(boolean enabled) {
        renumberDirField.setEnabled(enabled);
        renumberPrefixField.setEnabled(enabled);
        renumberIncludeVideosCheckBox.setEnabled(enabled);
        renumberInlineVideosCheckBox.setEnabled(enabled);
        renumberButton.setEnabled(enabled);
        renumberResetButton.setEnabled(enabled);
    }

    private void resetRenumberForm() {
        renumberDirField.setText("");
        renumberPrefixField.setText("");
        renumberIncludeVideosCheckBox.setSelected(true);
        renumberInlineVideosCheckBox.setSelected(true);
        renumberDirField.requestFocusInWindow();
    }

    // ── Dialogs ───────────────────────────────────────────────

    private void showAboutDialog() {
        JPanel aboutPanel = new JPanel();
        aboutPanel.setLayout(new BoxLayout(aboutPanel, BoxLayout.Y_AXIS));
        aboutPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        try {
            BufferedImage icon = ImageIO.read(getClass().getResourceAsStream("/icon-48.png"));
            if (icon != null) {
                JLabel iconLabel = new JLabel(new ImageIcon(icon));
                iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                aboutPanel.add(iconLabel);
                aboutPanel.add(Box.createVerticalStrut(10));
            }
        } catch (Exception e) {
            log.warn("Could not load icon for About dialog", e);
        }

        JLabel titleLabel = new JLabel(Main.getAppTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        aboutPanel.add(titleLabel);

        aboutPanel.add(Box.createVerticalStrut(5));

        JLabel versionLabel = new JLabel("Version " + Main.getAppVersion());
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        aboutPanel.add(versionLabel);

        JOptionPane.showMessageDialog(mainFrame, aboutPanel, "About " + Main.getAppTitle(),
                JOptionPane.PLAIN_MESSAGE);
    }

    // ── Helpers ───────────────────────────────────────────────

    private void cleanup() {
        try {
            lock.release();
            randomAccessFile.close();
            lockFile.delete();
        } catch (Exception ex) {
            log.error("Error releasing lock", ex);
        }
    }

    private String formatRow(String label, String value) {
        boolean isImportant = label.equals("Album Name") || label.equals("Source Directory")
                || label.equals("Album Directory") || label.equals("Album Prefix");
        String backgroundColor = isImportant ? "#f0f0f0" : "#ffffff";
        String fontSize = isImportant ? "16px" : "11px";
        String fontWeight = isImportant ? "bold" : "normal";

        return String.format("<tr style='background-color: %s;'>" +
                        "<td style='padding: %s; font-weight: bold; color: #333; font-size: %s;'>%s:</td>" +
                        "<td style='padding: %s; color: #0066cc; font-size: %s; font-weight: %s;'>%s</td></tr>",
                backgroundColor,
                isImportant ? "10px 8px" : "6px 8px",
                fontSize, label,
                isImportant ? "10px 8px" : "6px 8px",
                fontSize, fontWeight, value);
    }
}
```

**Step 2: Verify compilation**

Run: `mvn compile`
Expected: BUILD SUCCESS

**Step 3: Verify tests still pass**

Run: `mvn test -Dtest=AlbumDetailsTest`
Expected: 1 test passes

**Step 4: Commit**

```bash
git add src/main/java/com/mcs/camera/Main.java src/main/java/com/mcs/camera/UIHandler.java src/main/java/com/mcs/camera/PictureRenumberer.java
git commit -m "refactor: rewrite UI as tabbed frame with menu bar, integrate PictureRenumberer"
```

---

### Task 4: Manual Smoke Test and Build

**Step 1: Build the fat JAR**

Run: `mvn package -DskipTests`
Expected: BUILD SUCCESS, `dist/PictureRenamer.jar` produced

**Step 2: Manual smoke test checklist**

Launch `dist/PictureRenamer.jar` and verify:

**General:**
- [ ] Window appears centered with title "Picture Renamer"
- [ ] Two tabs visible: "Rename" and "Renumber"
- [ ] Menu bar shows: File, Edit, Help
- [ ] File > Exit (and Ctrl+Q) closes the app
- [ ] Edit > Options... is greyed out
- [ ] Help > About shows icon, "Picture Renamer", version number
- [ ] Second instance focuses the existing window

**Rename tab:**
- [ ] Form fields match the old dialog layout
- [ ] Force Date checkbox enables/disables the date field
- [ ] Browse button opens a directory chooser starting at source dir
- [ ] Reset button clears album name, restores defaults
- [ ] Validation: empty album name, bad date format, missing source dir
- [ ] Process button shows confirmation dialog
- [ ] Cancelling confirmation returns to form (fields preserved)
- [ ] Confirming runs the rename with a small set of test files
- [ ] After success: form resets, success message shown

**Renumber tab:**
- [ ] Browse button opens chooser starting at F:\My Pictures
- [ ] Selecting a directory auto-extracts prefix from name (after comma)
- [ ] Prefix field is editable for override
- [ ] Validation: empty directory, empty prefix
- [ ] Renumber button shows confirmation dialog
- [ ] Confirming runs renumber on test files (two-pass rename)
- [ ] After success: form resets, success message shown

**Step 3: Commit any fixes**

```bash
git add -u
git commit -m "fix: address issues found during smoke testing"
```

---

### Task 5: Bump Version

**Files:**
- Modify: `pom.xml`

**Step 1: Bump version to 4.0**

Change `<version>` from `3.2` to `4.0`. This is a major UI overhaul — the app now has a fundamentally different interaction model (frame-based with tabs vs dialog-driven) and integrates PictureRenumberer.

**Step 2: Build and verify**

Run: `mvn package -DskipTests`
Expected: BUILD SUCCESS. Launch JAR and verify Help > About shows "Version 4.0".

**Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: bump version to 4.0 for UI refactor"
```
