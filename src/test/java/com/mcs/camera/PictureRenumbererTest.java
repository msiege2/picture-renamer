package com.mcs.camera;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PictureRenumbererTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Formatting Options")
    class FormattingOptions {

        @Test
        void renumbersWithDefaultSpaceSeparator() throws IOException {
            Path vid1 = Files.createFile(tempDir.resolve("old_001.mp4"));
            vid1.toFile().setLastModified(1629034245000L);
            Path vid2 = Files.createFile(tempDir.resolve("old_002.mp4"));
            vid2.toFile().setLastModified(1629034246000L);

            PictureRenumberer renumberer = new PictureRenumberer(
                    tempDir.toString(), "Album", true, true, "%03d", " ");
            renumberer.renumberPictures();

            String[] files = tempDir.toFile().list();
            assertThat(files).isNotNull().hasSize(2);
            Arrays.sort(files);
            assertThat(files[0]).isEqualTo("Album 001.mp4");
            assertThat(files[1]).isEqualTo("Album 002.mp4");
        }

        @Test
        void renumbersWithDashSeparatorAndFourDigitPadding() throws IOException {
            Path vid = Files.createFile(tempDir.resolve("clip.mp4"));
            vid.toFile().setLastModified(1629034245000L);

            PictureRenumberer renumberer = new PictureRenumberer(
                    tempDir.toString(), "Trip", true, true, "%04d", "-");
            renumberer.renumberPictures();

            String[] files = tempDir.toFile().list();
            assertThat(files).isNotNull().hasSize(1);
            assertThat(files[0]).isEqualTo("Trip-0001.mp4");
        }

        @Test
        void renumbersWithUnderscoreSeparator() throws IOException {
            Path vid = Files.createFile(tempDir.resolve("video.mp4"));
            vid.toFile().setLastModified(1629034245000L);

            PictureRenumberer renumberer = new PictureRenumberer(
                    tempDir.toString(), "Event", true, true, "%02d", "_");
            renumberer.renumberPictures();

            String[] files = tempDir.toFile().list();
            assertThat(files).isNotNull().hasSize(1);
            assertThat(files[0]).isEqualTo("Event_01.mp4");
        }

        @Test
        void renumbersWithNoSeparator() throws IOException {
            Path vid = Files.createFile(tempDir.resolve("a.mp4"));
            vid.toFile().setLastModified(1629034245000L);

            PictureRenumberer renumberer = new PictureRenumberer(
                    tempDir.toString(), "Album", true, true, "%03d", "");
            renumberer.renumberPictures();

            String[] files = tempDir.toFile().list();
            assertThat(files).isNotNull().hasSize(1);
            assertThat(files[0]).isEqualTo("Album001.mp4");
        }
    }

    @Nested
    @DisplayName("Multiple Files")
    class MultipleFiles {

        @Test
        void sortsFilesByModifiedTimeNotName() throws IOException {
            Path file1 = Files.createFile(tempDir.resolve("b.mp4"));
            file1.toFile().setLastModified(1629034246000L);
            Path file2 = Files.createFile(tempDir.resolve("a.mp4"));
            file2.toFile().setLastModified(1629034245000L);

            PictureRenumberer renumberer = new PictureRenumberer(
                    tempDir.toString(), "Vacation", true, true, "%03d", " ");
            renumberer.renumberPictures();

            String[] files = tempDir.toFile().list();
            assertThat(files).isNotNull().hasSize(2);
            Arrays.sort(files);
            assertThat(files[0]).isEqualTo("Vacation 001.mp4");
            assertThat(files[1]).isEqualTo("Vacation 002.mp4");
        }

        @Test
        void videosNotInlineStillRenumbers() throws IOException {
            Path vid1 = Files.createFile(tempDir.resolve("clip1.mp4"));
            vid1.toFile().setLastModified(1629034245000L);
            Path vid2 = Files.createFile(tempDir.resolve("clip2.mp4"));
            vid2.toFile().setLastModified(1629034246000L);

            PictureRenumberer renumberer = new PictureRenumberer(
                    tempDir.toString(), "Test", true, false, "%03d", "-");
            renumberer.renumberPictures();

            String[] files = tempDir.toFile().list();
            assertThat(files).isNotNull().hasSize(2);
            Arrays.sort(files);
            assertThat(files[0]).isEqualTo("Test-001.mp4");
            assertThat(files[1]).isEqualTo("Test-002.mp4");
        }
    }

    @Nested
    @DisplayName("Rollback")
    class RollbackTests {

        @Test
        void rollsBackOnRenumberFailure() throws IOException {
            Path file1 = Files.createFile(tempDir.resolve("clip1.mp4"));
            file1.toFile().setLastModified(1629034245000L);
            Path file2 = Files.createFile(tempDir.resolve("clip2.mp4"));
            file2.toFile().setLastModified(1629034246000L);

            PictureRenumberer renumberer = new PictureRenumberer(
                    tempDir.toString(), "Trip", true, true, "%03d", " ");

            // Block second rename with a directory
            new File(tempDir.toFile(), "Trip 002.mp4").mkdir();

            assertThatThrownBy(renumberer::renumberPictures)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File operation failed");

            File[] restored = tempDir.toFile().listFiles(f -> !f.isDirectory());
            assertThat(restored).isNotNull().hasSize(2);
            String[] names = Arrays.stream(restored).map(File::getName).sorted().toArray(String[]::new);
            assertThat(names[0]).isEqualTo("clip1.mp4");
            assertThat(names[1]).isEqualTo("clip2.mp4");
        }
    }
}
