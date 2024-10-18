package com.mcs.camera;

import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import static org.junit.Assert.*;

public class PictureRenamerTest {
    private PictureRenamer pictureRenamer;
    private AlbumDetails albumDetails;

    @Before
    public void setUp() {
        albumDetails = new AlbumDetails("TestAlbum", "test_directory", false, "", true, true, false, true);
        pictureRenamer = new PictureRenamer(albumDetails);
    }

    @Test
    public void testGetFilesInDir() {
        Collection<File> files = pictureRenamer.getFilesInDir("test_resources");
        assertNotNull(files);
    }

    @Test
    public void testGrabJpgMetadata() {
        File file = new File("test_resources/test.jpg");
        pictureRenamer.grabJpgMetadata(file);
        assertFalse(pictureRenamer.temporaryNames.isEmpty());
        assertTrue(pictureRenamer.fileMap.containsKey(pictureRenamer.temporaryNames.get(0)));
    }

    @Test
    public void testGrabPngMetadata() {
        File file = new File("test_resources/test.png");
        pictureRenamer.grabPngMetadata(file);
        assertFalse(pictureRenamer.temporaryNames.isEmpty());
        assertTrue(pictureRenamer.fileMap.containsKey(pictureRenamer.temporaryNames.get(0)));
    }

    @Test
    public void testGrabMovieMetadata() {
        File file = new File("test_resources/test.mp4");
        pictureRenamer.grabMovieMetadata(file);
        assertFalse(pictureRenamer.temporaryNames.isEmpty());
        assertTrue(pictureRenamer.fileMap.containsKey(pictureRenamer.temporaryNames.get(0)));
    }

    @Test
    public void testParseDateFromFilename() {
        Date date = pictureRenamer.parseDateFromFilename("2021-08-15 12.30.45.jpg");
        assertNotNull(date);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
        assertEquals("2021-08-15 12.30.45", sdf.format(date));
    }
}