package com.mcs.camera;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class AppPreferencesTest {

    @AfterEach
    void tearDown() throws BackingStoreException {
        Preferences.userNodeForPackage(AppPreferences.class).clear();
    }

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        void defaultValuesAreEmpty() {
            AppPreferences prefs = new AppPreferences();

            assertThat(prefs.getPictureLibraryDir()).isEmpty();
            assertThat(prefs.getDefaultSourceDir()).isEmpty();
            assertThat(prefs.getCounterStart()).isEqualTo(1);
            assertThat(prefs.getNumberPadding()).isEqualTo(3);
            assertThat(prefs.getFilenameSeparator()).isEqualTo(" ");
        }

        @Test
        void isNotConfiguredByDefault() {
            AppPreferences prefs = new AppPreferences();
            assertThat(prefs.isConfigured()).isFalse();
        }
    }

    @Nested
    @DisplayName("Configuration State")
    class ConfigurationState {

        @Test
        void requiresBothPathsToBeConfigured() {
            AppPreferences prefs = new AppPreferences();

            prefs.setPictureLibraryDir("C:\\Photos");
            assertThat(prefs.isConfigured()).isFalse();

            prefs.setDefaultSourceDir("D:\\Import");
            assertThat(prefs.isConfigured()).isTrue();
        }
    }

    @Test
    void setAndGetAllPreferences() {
        AppPreferences prefs = new AppPreferences();

        prefs.setPictureLibraryDir("C:\\Photos");
        prefs.setDefaultSourceDir("D:\\Import");
        prefs.setCounterStart(0);
        prefs.setNumberPadding(5);
        prefs.setFilenameSeparator("_");

        assertThat(prefs.getPictureLibraryDir()).isEqualTo("C:\\Photos");
        assertThat(prefs.getDefaultSourceDir()).isEqualTo("D:\\Import");
        assertThat(prefs.getCounterStart()).isZero();
        assertThat(prefs.getNumberPadding()).isEqualTo(5);
        assertThat(prefs.getFilenameSeparator()).isEqualTo("_");
    }

    @Test
    void preferencesPersistAcrossInstances() {
        AppPreferences prefs1 = new AppPreferences();
        prefs1.setPictureLibraryDir("C:\\Photos");
        prefs1.setDefaultSourceDir("D:\\Import");
        prefs1.setCounterStart(10);
        prefs1.setNumberPadding(4);
        prefs1.setFilenameSeparator("-");

        AppPreferences prefs2 = new AppPreferences();
        assertThat(prefs2.getPictureLibraryDir()).isEqualTo("C:\\Photos");
        assertThat(prefs2.getDefaultSourceDir()).isEqualTo("D:\\Import");
        assertThat(prefs2.getCounterStart()).isEqualTo(10);
        assertThat(prefs2.getNumberPadding()).isEqualTo(4);
        assertThat(prefs2.getFilenameSeparator()).isEqualTo("-");
    }

    @ParameterizedTest
    @CsvSource({
            "2, %02d",
            "3, %03d",
            "4, %04d"
    })
    void numberFormatMatchesPadding(int padding, String expectedFormat) {
        AppPreferences prefs = new AppPreferences();
        prefs.setNumberPadding(padding);
        assertThat(prefs.getNumberFormat()).isEqualTo(expectedFormat);
    }
}
