package com.mcs.camera;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PictureRenumbererDryRunTest {

    @TempDir
    Path tempDir;

    @Test
    void dryRunDoesNotRenameFiles() throws Exception {
        Path vid1 = Files.createFile(tempDir.resolve("clip1.mp4"));
        vid1.toFile().setLastModified(1629034245000L);
        Path vid2 = Files.createFile(tempDir.resolve("clip2.mp4"));
        vid2.toFile().setLastModified(1629034246000L);

        PictureRenumberer renumberer = new PictureRenumberer(
                tempDir.toString(), "Album",
                true, true, "%03d", " ",
                new DryRunFileOperationTracker());
        renumberer.renumberPictures();

        assertThat(vid1).exists();
        assertThat(vid2).exists();
    }
}
