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
}
