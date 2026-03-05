package com.mcs.camera;

import com.mcs.camera.extractor.MetadataExtractor;
import com.mcs.camera.extractor.VideoMetadataExtractor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import static org.junit.Assert.*;

public class PictureRenamerTest {
    private PictureRenamer pictureRenamer;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        AlbumDetails albumDetails = new AlbumDetails(
                "TestAlbum", "test_directory", false, "",
                true, true, false, true);
        pictureRenamer = new PictureRenamer(albumDetails);
    }

    @Test
    public void testParseDateFromFilename() {
        LocalDateTime date = pictureRenamer.parseDateFromFilename("2021-08-15 12.30.45.jpg");
        assertNotNull(date);
        DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
        assertEquals("2021-08-15 12.30.45", date.format(sdf));
    }

    @Test
    public void testParseDateFromFilenameReturnsNullOnBadInput() {
        LocalDateTime date = pictureRenamer.parseDateFromFilename("not-a-date.jpg");
        assertNull(date);
    }

    @Test
    public void testGetFilesInDir() throws IOException {
        tempFolder.newFile("photo1.jpg");
        tempFolder.newFile("photo2.png");
        tempFolder.newFile("video.mp4");

        Collection<File> files = pictureRenamer.getFilesInDir(tempFolder.getRoot().getAbsolutePath());
        assertNotNull(files);
        assertEquals(3, files.size());
    }

    @Test
    public void testGetFilesInDirEmpty() {
        Collection<File> files = pictureRenamer.getFilesInDir(tempFolder.getRoot().getAbsolutePath());
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    public void testGrabMetadataWithVideoExtractor() throws IOException {
        File videoFile = tempFolder.newFile("test.mp4");
        long timestamp = 1629034245000L; // 2021-08-15 in millis
        videoFile.setLastModified(timestamp);

        pictureRenamer.grabMetadata(videoFile, new VideoMetadataExtractor());

        assertFalse(pictureRenamer.temporaryNames.isEmpty());
        assertEquals(1, pictureRenamer.temporaryNames.size());
        assertTrue(pictureRenamer.fileMap.containsKey(pictureRenamer.temporaryNames.get(0)));
        assertEquals(videoFile, pictureRenamer.fileMap.get(pictureRenamer.temporaryNames.get(0)));
    }

    @Test(expected = RuntimeException.class)
    public void testGrabMetadataThrowsOnFailure() {
        File nonExistentFile = new File(tempFolder.getRoot(), "does_not_exist.jpg");
        MetadataExtractor failingExtractor = f -> {
            throw new RuntimeException("Simulated extraction failure");
        };

        pictureRenamer.grabMetadata(nonExistentFile, failingExtractor);
    }

    @Test
    public void testCustomNumberFormatAndSeparator() throws IOException {
        AlbumDetails details = new AlbumDetails(
                "Trip", tempFolder.getRoot().getAbsolutePath(), false, "",
                false, false, false, true,
                tempFolder.getRoot().getAbsolutePath(), 1, "%04d", "-");
        PictureRenamer renamer = new PictureRenamer(details);

        File videoFile = tempFolder.newFile("test.mp4");
        videoFile.setLastModified(1629034245000L);
        renamer.grabMetadata(videoFile, new VideoMetadataExtractor());

        assertNotNull(renamer.albumDirName);
        assertTrue(renamer.albumDirName.contains("Trip"));
    }

    @Test
    public void testRenamePicturesWithDefaultOptions() throws IOException {
        String src = tempFolder.getRoot().getAbsolutePath();
        String dest = tempFolder.newFolder("dest").getAbsolutePath();
        AlbumDetails details = new AlbumDetails(
                "Vacation", src, true, "2021-08-15",
                true, true, false, true,
                dest, 1, "%03d", " ");

        File vid1 = tempFolder.newFile("clip1.mp4");
        vid1.setLastModified(1629034245000L);
        File vid2 = tempFolder.newFile("clip2.mp4");
        vid2.setLastModified(1629034246000L);

        PictureRenamer renamer = new PictureRenamer(details);
        renamer.renamePictures();

        File albumDir = new File(dest + File.separator + "2021" + File.separator + "2021-08-15, Vacation");
        assertTrue("Album dir should exist", albumDir.exists());
        String[] files = albumDir.list();
        assertNotNull(files);
        java.util.Arrays.sort(files);
        assertEquals(2, files.length);
        assertEquals("Vacation 001.mp4", files[0]);
        assertEquals("Vacation 002.mp4", files[1]);
    }

    @Test
    public void testRenamePicturesWithDashSeparatorAndFourDigitPadding() throws IOException {
        String src = tempFolder.getRoot().getAbsolutePath();
        String dest = tempFolder.newFolder("dest").getAbsolutePath();
        AlbumDetails details = new AlbumDetails(
                "Trip", src, true, "2023-01-10",
                true, true, false, true,
                dest, 1, "%04d", "-");

        File vid = tempFolder.newFile("video.mp4");
        vid.setLastModified(1629034245000L);

        PictureRenamer renamer = new PictureRenamer(details);
        renamer.renamePictures();

        File albumDir = new File(dest + File.separator + "2023" + File.separator + "2023-01-10, Trip");
        assertTrue("Album dir should exist", albumDir.exists());
        String[] files = albumDir.list();
        assertNotNull(files);
        assertEquals(1, files.length);
        assertEquals("Trip-0001.mp4", files[0]);
    }

    @Test
    public void testRenamePicturesWithUnderscoreSeparatorAndCustomCounter() throws IOException {
        String src = tempFolder.getRoot().getAbsolutePath();
        String dest = tempFolder.newFolder("dest").getAbsolutePath();
        AlbumDetails details = new AlbumDetails(
                "Event", src, true, "2024-06-01",
                true, true, false, true,
                dest, 10, "%02d", "_");

        File vid = tempFolder.newFile("a.mp4");
        vid.setLastModified(1629034245000L);

        PictureRenamer renamer = new PictureRenamer(details);
        renamer.renamePictures();

        File albumDir = new File(dest + File.separator + "2024" + File.separator + "2024-06-01, Event");
        String[] files = albumDir.list();
        assertNotNull(files);
        assertEquals(1, files.length);
        assertEquals("Event_10.mp4", files[0]);
    }

    @Test
    public void testRenamePicturesWithNoSeparator() throws IOException {
        String src = tempFolder.getRoot().getAbsolutePath();
        String dest = tempFolder.newFolder("dest").getAbsolutePath();
        AlbumDetails details = new AlbumDetails(
                "Album", src, true, "2025-03-04",
                true, true, false, true,
                dest, 1, "%03d", "");

        File vid = tempFolder.newFile("x.mp4");
        vid.setLastModified(1629034245000L);

        PictureRenamer renamer = new PictureRenamer(details);
        renamer.renamePictures();

        File albumDir = new File(dest + File.separator + "2025" + File.separator + "2025-03-04, Album");
        String[] files = albumDir.list();
        assertNotNull(files);
        assertEquals(1, files.length);
        assertEquals("Album001.mp4", files[0]);
    }

    @Test
    public void testAlbumDirNameSetFromFirstFile() throws IOException {
        File videoFile = tempFolder.newFile("test.mp4");
        // Set to a known date: 2021-08-15
        long timestamp = 1629034245000L;
        videoFile.setLastModified(timestamp);

        assertNull(pictureRenamer.albumDirName);
        pictureRenamer.grabMetadata(videoFile, new VideoMetadataExtractor());
        assertNotNull(pictureRenamer.albumDirName);
        assertTrue(pictureRenamer.albumDirName.contains("TestAlbum"));
    }

    @Test
    public void testRollbackOnMoveFailure() throws IOException {
        String src = tempFolder.getRoot().getAbsolutePath();
        File destFolder = tempFolder.newFolder("dest");
        String dest = destFolder.getAbsolutePath();
        AlbumDetails details = new AlbumDetails(
                "Trip", src, true, "2021-08-15",
                true, true, false, true,
                dest, 1, "%03d", " ");

        File vid1 = tempFolder.newFile("clip1.mp4");
        vid1.setLastModified(1629034245000L);

        PictureRenamer renamer = new PictureRenamer(details);

        // Pre-create the album dir, then put a directory with the same name as
        // the file that will be moved — Files.move throws when target is a directory
        File albumDir = new File(dest + File.separator + "2021" + File.separator + "2021-08-15, Trip");
        albumDir.mkdirs();
        File blocker = new File(albumDir, "Trip 001.mp4");
        blocker.mkdir(); // a directory, not a file — will block the move

        try {
            renamer.renamePictures();
            fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("File operation failed"));
        }

        // After rollback, original file should be back in source dir
        File[] srcFiles = new File(src).listFiles(f -> !f.isDirectory());
        assertNotNull(srcFiles);
        assertEquals("Original file should be restored", 1, srcFiles.length);
        assertEquals("clip1.mp4", srcFiles[0].getName());
    }
}
