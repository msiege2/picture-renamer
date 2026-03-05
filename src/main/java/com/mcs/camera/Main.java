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

        if (args.length > 0) {
            System.exit(CliHandler.run(args));
            return;
        }

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
