package com.mcs.camera;

import java.util.prefs.Preferences;

public class AppPreferences {

    private static final String KEY_PICTURE_LIBRARY_DIR = "pictureLibraryDir";
    private static final String KEY_DEFAULT_SOURCE_DIR = "defaultSourceDir";
    private static final String KEY_COUNTER_START = "counterStart";
    private static final String KEY_NUMBER_PADDING = "numberPadding";
    private static final String KEY_FILENAME_SEPARATOR = "filenameSeparator";

    private static final String DEFAULT_PICTURE_LIBRARY_DIR = "";
    private static final String DEFAULT_DEFAULT_SOURCE_DIR = "";
    private static final int DEFAULT_COUNTER_START = 1;
    private static final int DEFAULT_NUMBER_PADDING = 3;
    private static final String DEFAULT_FILENAME_SEPARATOR = " ";

    private final Preferences prefs;

    public AppPreferences() {
        prefs = Preferences.userNodeForPackage(AppPreferences.class);
    }

    public String getPictureLibraryDir() {
        return prefs.get(KEY_PICTURE_LIBRARY_DIR, DEFAULT_PICTURE_LIBRARY_DIR);
    }

    public void setPictureLibraryDir(String dir) {
        prefs.put(KEY_PICTURE_LIBRARY_DIR, dir);
    }

    public String getDefaultSourceDir() {
        return prefs.get(KEY_DEFAULT_SOURCE_DIR, DEFAULT_DEFAULT_SOURCE_DIR);
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

    public boolean isConfigured() {
        return !getPictureLibraryDir().isEmpty() && !getDefaultSourceDir().isEmpty();
    }
}
