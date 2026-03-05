package com.mcs.camera;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;

public class PictureRenumbererDryRunTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDryRunDoesNotRenameFiles() throws Exception {
        File vid1 = tempFolder.newFile("clip1.mp4");
        vid1.setLastModified(1629034245000L);
        File vid2 = tempFolder.newFile("clip2.mp4");
        vid2.setLastModified(1629034246000L);

        PictureRenumberer renumberer = new PictureRenumberer(
                tempFolder.getRoot().getAbsolutePath(), "Album",
                true, true, "%03d", " ",
                new DryRunFileOperationTracker());
        renumberer.renumberPictures();

        // Original files should still exist with original names
        assertTrue("clip1.mp4 should still exist", vid1.exists());
        assertTrue("clip2.mp4 should still exist", vid2.exists());
    }
}
