package com.mcs.camera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class UIHandler {
    static final Logger log = LoggerFactory.getLogger(UIHandler.class.getName());
    private JFrame mainFrame;
    private final String appTitle;
    private final FileLock lock;
    private final RandomAccessFile randomAccessFile;
    private final File lockFile;

    // Fields to store user input
    private String prefix = "";
    private String sourceDir = "H:\\Picture Merge";
    private boolean forceDateFlag = false;
    private String forceDate = "";
    private boolean includeVideos = true;
    private boolean inlineVideos = true;
    private boolean keepOrder = false;
    private boolean tryFilenameDateTimeOnMetadataFail = true;

    public UIHandler(String appTitle, FileLock lock, RandomAccessFile randomAccessFile, File lockFile) {
        this.appTitle = appTitle;
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

        mainFrame = new JFrame(appTitle);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1, 1);  // Minimal size to ensure it's not visible
        mainFrame.setLocationRelativeTo(null);

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    lock.release();
                    randomAccessFile.close();
                    lockFile.delete();
                } catch (Exception ex) {
                    log.error("Error releasing lock", ex);
                }
            }
        });

        // Make the frame visible to ensure taskbar icon appears
        mainFrame.setVisible(true);

        // Start the picture renaming process immediately
        SwingUtilities.invokeLater(this::startPictureRenaming);
    }

    private void startPictureRenaming() {
        boolean runAgain = true;
        while (runAgain) {
            AlbumDetails albumDetails = getAlbumDetails();
            if (albumDetails == null) {
                // User cancelled, exit the application
                System.exit(0);
            }
            PictureRenamer pictureRenamer = new PictureRenamer(albumDetails);
            pictureRenamer.renamePictures();
            int choice = JOptionPane.showConfirmDialog(mainFrame,
                    "Operation completed successfully. Do you want to process another album?", "Success",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                runAgain = false;
            }
        }
        // Exit the application when done
        System.exit(0);
    }

    public AlbumDetails getAlbumDetails() {
        boolean validInput = false;

        while (!validInput) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            // Album Details Section
            JPanel albumPanel = new JPanel(new GridBagLayout());
            albumPanel.setBorder(BorderFactory.createTitledBorder("Album Details"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            JLabel albumNameLabel = new JLabel("Album Name:");
            albumNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
            albumNameLabel.setToolTipText("Enter the name of the album.");
            albumPanel.add(albumNameLabel, gbc);

            gbc.gridx = 1;
            JTextField albumNameField = new JTextField(prefix, 20);
            albumPanel.add(albumNameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            JLabel sourceDirLabel = new JLabel("Source Directory:");
            sourceDirLabel.setFont(new Font("Arial", Font.BOLD, 14));
            sourceDirLabel.setToolTipText("Select the directory containing media files.");
            albumPanel.add(sourceDirLabel, gbc);

            gbc.gridx = 1;
            JPanel sourceDirPanel = new JPanel(new BorderLayout());
            JTextField sourceDirField = new JTextField(sourceDir, 20);
            JButton browseButton = new JButton("Browse");
            sourceDirPanel.add(sourceDirField, BorderLayout.CENTER);
            sourceDirPanel.add(browseButton, BorderLayout.EAST);
            albumPanel.add(sourceDirPanel, gbc);

            browseButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setCurrentDirectory(new java.io.File("."));
                    chooser.setDialogTitle("Select Source Directory");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setAcceptAllFileFilterUsed(false);
                    if (chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                        sourceDirField.setText(chooser.getSelectedFile().getAbsolutePath());
                    }
                }
            });

            panel.add(albumPanel);

            // Options Section
            JPanel optionsPanel = new JPanel(new GridBagLayout());
            optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));

            gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 5, 2, 5);  // Reduced vertical padding
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            JCheckBox forceDateFlagCheckBox = new JCheckBox("Force Date", forceDateFlag);
            forceDateFlagCheckBox.setToolTipText("Select to use a specific date for all files.");
            optionsPanel.add(forceDateFlagCheckBox, gbc);

            gbc.gridy = 1;
            gbc.gridwidth = 1;
            gbc.insets = new Insets(0, 27, 2, 5);  // Added left padding to align with checkbox
            JLabel forceDateLabel = new JLabel("Forced Date (YYYY-MM-DD):");
            forceDateLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            forceDateLabel.setToolTipText("Enter the date to use when 'Force Date' is selected.");
            optionsPanel.add(forceDateLabel, gbc);

            gbc.gridx = 1;
            gbc.insets = new Insets(0, 5, 2, 5);  // Reset left padding
            JTextField forceDateField = new JTextField(forceDate, 10);
            forceDateField.setEnabled(forceDateFlag);
            optionsPanel.add(forceDateField, gbc);

            forceDateFlagCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    forceDateField.setEnabled(forceDateFlagCheckBox.isSelected());
                    forceDateLabel.setEnabled(forceDateFlagCheckBox.isSelected());
                }
            });

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(10, 5, 2, 5);  // Added top padding to separate from force date group
            JCheckBox includeVideosCheckBox = new JCheckBox("Include videos", includeVideos);
            includeVideosCheckBox.setToolTipText("Include video files in the processing.");
            optionsPanel.add(includeVideosCheckBox, gbc);


            gbc.gridy = 3;
            JCheckBox inlineVideosCheckBox = new JCheckBox("Number videos inline", inlineVideos);
            inlineVideosCheckBox.setToolTipText("Include videos in the numbering sequence.");
            optionsPanel.add(inlineVideosCheckBox, gbc);

            gbc.gridy = 4;
            JCheckBox keepOrderCheckBox = new JCheckBox("Keep order", keepOrder);
            keepOrderCheckBox.setToolTipText("Keep the original order of files.");
            optionsPanel.add(keepOrderCheckBox, gbc);

            gbc.gridy = 5;
            JCheckBox tryFilenameDateTimeCheckBox = new JCheckBox(
                    "Use filename date/time if metadata unavailable", tryFilenameDateTimeOnMetadataFail);
            tryFilenameDateTimeCheckBox
                    .setToolTipText("Attempt to extract date/time from filename if metadata is missing.");
            optionsPanel.add(tryFilenameDateTimeCheckBox, gbc);

            panel.add(optionsPanel);

            int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Please Provide Album Creator Details",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                prefix = albumNameField.getText().trim();
                sourceDir = sourceDirField.getText().trim();
                forceDateFlag = forceDateFlagCheckBox.isSelected();
                forceDate = forceDateField.getText().trim();
                includeVideos = includeVideosCheckBox.isSelected();
                inlineVideos = inlineVideosCheckBox.isSelected();
                keepOrder = keepOrderCheckBox.isSelected();
                tryFilenameDateTimeOnMetadataFail = tryFilenameDateTimeCheckBox.isSelected();

                if (prefix.isEmpty()) {
                    JOptionPane.showMessageDialog(mainFrame, "Album name cannot be empty.", "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                if (forceDateFlag) {
                    if (forceDate.isEmpty()) {
                        JOptionPane.showMessageDialog(mainFrame,
                                "Forced Date is required when Force Date is selected.", "Input Error",
                                JOptionPane.ERROR_MESSAGE);
                        continue;
                    } else {
                        if (!forceDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            JOptionPane.showMessageDialog(mainFrame,
                                    "Forced Date must be in YYYY-MM-DD format.", "Input Error",
                                    JOptionPane.ERROR_MESSAGE);
                            continue;
                        }
                    }
                }

                File srcDir = new File(sourceDir);
                if (!srcDir.exists() || !srcDir.isDirectory()) {
                    JOptionPane.showMessageDialog(mainFrame, "Source directory does not exist.", "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                validInput = true;
            } else {
                return null; // User cancelled
            }
        }

        StringBuilder confirmationMessage = new StringBuilder();
        confirmationMessage.append("<html>Do you want to proceed?<br><table>");
        confirmationMessage.append("<tr><td align='left'><b>Album Name:</b></td><td align='right'>").append(prefix)
                .append("</td></tr>");
        confirmationMessage.append(
                        "<tr><td align='left'><b>Source Directory:</b></td><td align='right'>").append(sourceDir)
                .append("</td></tr>");
        confirmationMessage.append(
                        "<tr><td align='left'><b>Force Date Flag:</b></td><td align='right'>").append(forceDateFlag)
                .append("</td></tr>");
        if (forceDateFlag) {
            confirmationMessage.append(
                            "<tr><td align='left'><b>Forced Date:</b></td><td align='right'>").append(forceDate)
                    .append("</td></tr>");
        }
        confirmationMessage.append(
                        "<tr><td align='left'><b>Include Videos:</b></td><td align='right'>").append(includeVideos)
                .append("</td></tr>");
        confirmationMessage.append(
                        "<tr><td align='left'><b>Number Videos Inline:</b></td><td align='right'>").append(inlineVideos)
                .append("</td></tr>");
        confirmationMessage.append("<tr><td align='left'><b>Keep Order:</b></td><td align='right'>")
                .append(keepOrder).append("</td></tr>");
        confirmationMessage.append(
                        "<tr><td align='left'><b>Use Filename Date/Time If Metadata Fails:</b></td><td align='right'>")
                .append(tryFilenameDateTimeOnMetadataFail).append("</td></tr>");
        confirmationMessage.append("</table></html>");

        JLabel confirmationLabel = new JLabel(confirmationMessage.toString());
        confirmationLabel.setFont(new Font("Arial", Font.BOLD, 18));
        int confirmationInput = JOptionPane.showConfirmDialog(mainFrame, confirmationLabel, "Confirm Your Inputs",
                JOptionPane.YES_NO_OPTION);

        if (confirmationInput != JOptionPane.YES_OPTION) {
            log.error("Confirmation failed.");
            return null;
        }

        return new AlbumDetails(prefix, sourceDir, forceDateFlag, forceDate, includeVideos, inlineVideos, keepOrder,
                tryFilenameDateTimeOnMetadataFail);
    }
}