package com.mcs.camera;

import org.junit.Test;
import static org.junit.Assert.*;

public class AlbumDetailsTest {

    @Test
    public void testAlbumDetailsCreation() {
        AlbumDetails albumDetails = new AlbumDetails("Vacation", "C:\\Photos", true, "2021-08-15",
                true, false, false, true);

        assertEquals("Vacation", albumDetails.getPrefix());
        assertEquals("C:\\Photos", albumDetails.getSourceDir());
        assertTrue(albumDetails.isForceDateFlag());
        assertEquals("2021-08-15", albumDetails.getForceDate());
        assertTrue(albumDetails.isIncludeVideos());
        assertFalse(albumDetails.isInlineVideos());
        assertFalse(albumDetails.isKeepOrder());
        assertTrue(albumDetails.isTryFilenameDateTimeOnMetadataFail());
    }

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
}