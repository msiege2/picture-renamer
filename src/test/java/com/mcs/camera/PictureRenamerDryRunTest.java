package com.mcs.camera;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;

public class PictureRenamerDryRunTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDryRunDoesNotMoveFiles() throws Exception {
        String src = tempFolder.getRoot().getAbsolutePath();
        String dest = tempFolder.newFolder("dest").getAbsolutePath();
        AlbumDetails details = new AlbumDetails(
                "Vacation", src, true, "2021-08-15",
                true, true, false, true,
                dest, 1, "%03d", " ");

        File vid = tempFolder.newFile("clip.mp4");
        vid.setLastModified(1629034245000L);

        PictureRenamer renamer = new PictureRenamer(details, new DryRunFileOperationTracker());
        renamer.renamePictures();

        // Source file should still be in source dir (not moved)
        File[] srcFiles = new File(src).listFiles(f -> !f.isDirectory());
        assertNotNull(srcFiles);
        assertEquals("File should still be in source dir", 1, srcFiles.length);
    }
}
