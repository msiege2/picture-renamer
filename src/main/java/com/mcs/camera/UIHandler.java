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
