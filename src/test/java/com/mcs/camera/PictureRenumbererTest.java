package com.mcs.camera;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class PictureRenumbererTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testRenumberWithDefaultOptions() throws IOException {
        File dir = tempFolder.getRoot();
        File vid1 = tempFolder.newFile("old_001.mp4");
        vid1.setLastModified(1629034245000L);
        File vid2 = tempFolder.newFile("old_002.mp4");
        vid2.setLastModified(1629034246000L);

        PictureRenumberer renumberer = new PictureRenumberer(
                dir.getAbsolutePath(), "Album", true, true, "%03d", " ");
        renumberer.renumberPictures();

        String[] files = dir.list();
        assertNotNull(files);
        java.util.Arrays.sort(files);
        assertEquals(2, files.length);
        assertEquals("Album 001.mp4", files[0]);
        assertEquals("Album 002.mp4", files[1]);
    }

    @Test
    public void testRenumberWithDashSeparatorAndFourDigitPadding() throws IOException {
        File dir = tempFolder.getRoot();
        File vid = tempFolder.newFile("clip.mp4");
        vid.setLastModified(1629034245000L);

        PictureRenumberer renumberer = new PictureRenumberer(
                dir.getAbsolutePath(), "Trip", true, true, "%04d", "-");
        renumberer.renumberPictures();

        String[] files = dir.list();
        assertNotNull(files);
        assertEquals(1, files.length);
        assertEquals("Trip-0001.mp4", files[0]);
    }

    @Test
    public void testRenumberWithUnderscoreSeparator() throws IOException {
        File dir = tempFolder.getRoot();
        File vid = tempFolder.newFile("video.mp4");
        vid.setLastModified(1629034245000L);

        PictureRenumberer renumberer = new PictureRenumberer(
                dir.getAbsolutePath(), "Event", true, true, "%02d", "_");
        renumberer.renumberPictures();

        String[] files = dir.list();
        assertNotNull(files);
        assertEquals(1, files.length);
        assertEquals("Event_01.mp4", files[0]);
    }

    @Test
    public void testRenumberWithNoSeparator() throws IOException {
        File dir = tempFolder.getRoot();
        File vid = tempFolder.newFile("a.mp4");
        vid.setLastModified(1629034245000L);

        PictureRenumberer renumberer = new PictureRenumberer(
                dir.getAbsolutePath(), "Album", true, true, "%03d", "");
        renumberer.renumberPictures();

        String[] files = dir.list();
        assertNotNull(files);
        assertEquals(1, files.length);
        assertEquals("Album001.mp4", files[0]);
    }

    @Test
    public void testRenumberWithVideosNotInline() throws IOException {
        File dir = tempFolder.getRoot();
        File vid1 = tempFolder.newFile("clip1.mp4");
        vid1.setLastModified(1629034245000L);
        File vid2 = tempFolder.newFile("clip2.mp4");
        vid2.setLastModified(1629034246000L);

        PictureRenumberer renumberer = new PictureRenumberer(
                dir.getAbsolutePath(), "Test", true, false, "%03d", "-");
        renumberer.renumberPictures();

        String[] files = dir.list();
        assertNotNull(files);
        java.util.Arrays.sort(files);
        assertEquals(2, files.length);
        assertEquals("Test-001.mp4", files[0]);
        assertEquals("Test-002.mp4", files[1]);
    }
}
