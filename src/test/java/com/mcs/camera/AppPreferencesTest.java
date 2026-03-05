package com.mcs.camera;

import org.junit.After;
import org.junit.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;

public class AppPreferencesTest {

    @After
    public void tearDown() throws BackingStoreException {
        Preferences.userNodeForPackage(AppPreferences.class).clear();
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

        prefs.setPictureLibraryDir("C:\\Photos");
        prefs.setDefaultSourceDir("D:\\Import");
        prefs.setCounterStart(0);
        prefs.setNumberPadding(5);
        prefs.setFilenameSeparator("_");

        assertEquals("C:\\Photos", prefs.getPictureLibraryDir());
        assertEquals("D:\\Import", prefs.getDefaultSourceDir());
        assertEquals(0, prefs.getCounterStart());
        assertEquals(5, prefs.getNumberPadding());
        assertEquals("_", prefs.getFilenameSeparator());
    }

    @Test
    public void testPersistenceAcrossInstances() {
        AppPreferences prefs1 = new AppPreferences();
        prefs1.setPictureLibraryDir("C:\\Photos");
        prefs1.setDefaultSourceDir("D:\\Import");
        prefs1.setCounterStart(10);
        prefs1.setNumberPadding(4);
        prefs1.setFilenameSeparator("-");

        AppPreferences prefs2 = new AppPreferences();
        assertEquals("C:\\Photos", prefs2.getPictureLibraryDir());
        assertEquals("D:\\Import", prefs2.getDefaultSourceDir());
        assertEquals(10, prefs2.getCounterStart());
        assertEquals(4, prefs2.getNumberPadding());
        assertEquals("-", prefs2.getFilenameSeparator());
    }

    @Test
    public void testGetNumberFormat() {
        AppPreferences prefs = new AppPreferences();

        prefs.setNumberPadding(2);
        assertEquals("%02d", prefs.getNumberFormat());

        prefs.setNumberPadding(3);
        assertEquals("%03d", prefs.getNumberFormat());

        prefs.setNumberPadding(4);
        assertEquals("%04d", prefs.getNumberFormat());
    }
}
