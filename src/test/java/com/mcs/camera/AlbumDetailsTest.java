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
}