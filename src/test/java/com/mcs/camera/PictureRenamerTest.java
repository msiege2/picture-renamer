package com.mcs.camera;

import com.mcs.camera.extractor.MetadataExtractor;
import com.mcs.camera.extractor.VideoMetadataExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PictureRenamerTest {

    @TempDir
    Path tempDir;

    private PictureRenamer pictureRenamer;

    @BeforeEach
    void setUp() {
        AlbumDetails albumDetails = new AlbumDetails(
                "TestAlbum", "test_directory", false, "",
                true, true, false, true);
        pictureRenamer = new PictureRenamer(albumDetails);
    }

    @Nested
    @DisplayName("Date Parsing")
    class DateParsing {

        @Test
        void parsesDateFromValidFilename() {
            LocalDateTime date = pictureRenamer.parseDateFromFilename("2021-08-15 12.30.45.jpg");
            assertThat(date).isNotNull();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
            assertThat(date.format(fmt)).isEqualTo("2021-08-15 12.30.45");
        }

        @Test
        void returnsNullForInvalidFilename() {
            LocalDateTime date = pictureRenamer.parseDateFromFilename("not-a-date.jpg");
            assertThat(date).isNull();
        }
    }

    @Nested
    @DisplayName("File Listing")
    class FileListing {

        @Test
        void listsAllFilesInDirectory() throws IOException {
            Files.createFile(tempDir.resolve("photo1.jpg"));
            Files.createFile(tempDir.resolve("photo2.png"));
            Files.createFile(tempDir.resolve("video.mp4"));

            Collection<File> files = pictureRenamer.getFilesInDir(tempDir.toString());
            assertThat(files).hasSize(3);
        }

        @Test
        void returnsEmptyCollectionForEmptyDir() {
            Collection<File> files = pictureRenamer.getFilesInDir(tempDir.toString());
            assertThat(files).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Metadata Extraction")
    class MetadataExtraction {

        @Test
        void grabsMetadataFromVideoFile() throws IOException {
            Path videoFile = Files.createFile(tempDir.resolve("test.mp4"));
            videoFile.toFile().setLastModified(1629034245000L);

            pictureRenamer.grabMetadata(videoFile.toFile(), new VideoMetadataExtractor());

            assertThat(pictureRenamer.temporaryNames).hasSize(1);
            assertThat(pictureRenamer.fileMap).containsKey(pictureRenamer.temporaryNames.get(0));
            assertThat(pictureRenamer.fileMap.get(pictureRenamer.temporaryNames.get(0)))
                    .isEqualTo(videoFile.toFile());
        }

        @Test
        void throwsWhenMetadataExtractionFails() throws Exception {
            File nonExistent = tempDir.resolve("does_not_exist.jpg").toFile();
            MetadataExtractor failingExtractor = Mockito.mock(MetadataExtractor.class);
            when(failingExtractor.extractDateTaken(any())).thenThrow(new RuntimeException("Simulated failure"));

            assertThatThrownBy(() -> pictureRenamer.grabMetadata(nonExistent, failingExtractor))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void setsAlbumDirNameFromFirstFile() throws IOException {
            Path videoFile = Files.createFile(tempDir.resolve("test.mp4"));
            videoFile.toFile().setLastModified(1629034245000L);

            assertThat(pictureRenamer.albumDirName).isNull();
            pictureRenamer.grabMetadata(videoFile.toFile(), new VideoMetadataExtractor());
            assertThat(pictureRenamer.albumDirName).isNotNull().contains("TestAlbum");
        }

        @Test
        void setsAlbumDirNameWithCustomFormat() throws IOException {
            AlbumDetails details = new AlbumDetails(
                    "Trip", tempDir.toString(), false, "",
                    false, false, false, true,
                    tempDir.toString(), 1, "%04d", "-");
            PictureRenamer renamer = new PictureRenamer(details);

            Path videoFile = Files.createFile(tempDir.resolve("test.mp4"));
            videoFile.toFile().setLastModified(1629034245000L);
            renamer.grabMetadata(videoFile.toFile(), new VideoMetadataExtractor());

            assertThat(renamer.albumDirName).isNotNull().contains("Trip");
        }
    }

    @Nested
    @DisplayName("Rename with Formatting Options")
    class RenameFormatting {

        @Test
        void renamesMultipleFilesInOrder() throws IOException {
            Path dest = Files.createDirectory(tempDir.resolve("dest"));
            AlbumDetails details = new AlbumDetails(
                    "Vacation", tempDir.toString(), true, "2021-08-15",
                    true, true, false, true,
                    dest.toString(), 1, "%03d", " ");

            Path vid1 = Files.createFile(tempDir.resolve("clip1.mp4"));
            vid1.toFile().setLastModified(1629034245000L);
            Path vid2 = Files.createFile(tempDir.resolve("clip2.mp4"));
            vid2.toFile().setLastModified(1629034246000L);

            PictureRenamer renamer = new PictureRenamer(details);
            renamer.renamePictures();

            File albumDir = new File(dest.toString(), "2021" + File.separator + "2021-08-15, Vacation");
            assertThat(albumDir).exists();
            String[] files = albumDir.list();
            assertThat(files).isNotNull().hasSize(2);
            Arrays.sort(files);
            assertThat(files[0]).isEqualTo("Vacation 001.mp4");
            assertThat(files[1]).isEqualTo("Vacation 002.mp4");
        }

        @Test
        void renamesWithDashSeparatorAndFourDigitPadding() throws IOException {
            Path dest = Files.createDirectory(tempDir.resolve("dest"));
            AlbumDetails details = new AlbumDetails(
                    "Trip", tempDir.toString(), true, "2023-01-10",
                    true, true, false, true,
                    dest.toString(), 1, "%04d", "-");

            Path vid = Files.createFile(tempDir.resolve("video.mp4"));
            vid.toFile().setLastModified(1629034245000L);

            PictureRenamer renamer = new PictureRenamer(details);
            renamer.renamePictures();

            File albumDir = new File(dest.toString(), "2023" + File.separator + "2023-01-10, Trip");
            assertThat(albumDir).exists();
            String[] files = albumDir.list();
            assertThat(files).isNotNull().hasSize(1);
            assertThat(files[0]).isEqualTo("Trip-0001.mp4");
        }

        @Test
        void renamesWithUnderscoreSeparatorAndCustomCounter() throws IOException {
            Path dest = Files.createDirectory(tempDir.resolve("dest"));
            AlbumDetails details = new AlbumDetails(
                    "Event", tempDir.toString(), true, "2024-06-01",
                    true, true, false, true,
                    dest.toString(), 10, "%02d", "_");

            Path vid = Files.createFile(tempDir.resolve("a.mp4"));
            vid.toFile().setLastModified(1629034245000L);

            PictureRenamer renamer = new PictureRenamer(details);
            renamer.renamePictures();

            File albumDir = new File(dest.toString(), "2024" + File.separator + "2024-06-01, Event");
            String[] files = albumDir.list();
            assertThat(files).isNotNull().hasSize(1);
            assertThat(files[0]).isEqualTo("Event_10.mp4");
        }

        @Test
        void renamesWithNoSeparator() throws IOException {
            Path dest = Files.createDirectory(tempDir.resolve("dest"));
            AlbumDetails details = new AlbumDetails(
                    "Album", tempDir.toString(), true, "2025-03-04",
                    true, true, false, true,
                    dest.toString(), 1, "%03d", "");

            Path vid = Files.createFile(tempDir.resolve("x.mp4"));
            vid.toFile().setLastModified(1629034245000L);

            PictureRenamer renamer = new PictureRenamer(details);
            renamer.renamePictures();

            File albumDir = new File(dest.toString(), "2025" + File.separator + "2025-03-04, Album");
            String[] files = albumDir.list();
            assertThat(files).isNotNull().hasSize(1);
            assertThat(files[0]).isEqualTo("Album001.mp4");
        }
    }

    @Nested
    @DisplayName("Rollback")
    class RollbackTests {

        @Test
        void rollsBackOnMoveFailure() throws IOException {
            Path dest = Files.createDirectory(tempDir.resolve("dest"));
            AlbumDetails details = new AlbumDetails(
                    "Trip", tempDir.toString(), true, "2021-08-15",
                    true, true, false, true,
                    dest.toString(), 1, "%03d", " ");

            Path vid1 = Files.createFile(tempDir.resolve("clip1.mp4"));
            vid1.toFile().setLastModified(1629034245000L);

            PictureRenamer renamer = new PictureRenamer(details);

            // Pre-create blocking directory at target location
            File albumDir = new File(dest.toString(), "2021" + File.separator + "2021-08-15, Trip");
            albumDir.mkdirs();
            new File(albumDir, "Trip 001.mp4").mkdir();

            assertThatThrownBy(renamer::renamePictures)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File operation failed");

            File[] srcFiles = tempDir.toFile().listFiles(f -> !f.isDirectory());
            assertThat(srcFiles).isNotNull().hasSize(1);
            assertThat(srcFiles[0].getName()).isEqualTo("clip1.mp4");
        }
    }
}
