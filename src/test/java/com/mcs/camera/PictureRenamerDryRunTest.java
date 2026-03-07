package com.mcs.camera;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PictureRenamerDryRunTest {

    @TempDir
    Path tempDir;

    @Test
    void dryRunDoesNotMoveFiles() throws Exception {
        Path dest = Files.createDirectory(tempDir.resolve("dest"));
        AlbumDetails details = new AlbumDetails(
                "Vacation", tempDir.toString(), true, "2021-08-15",
                true, true, false, true,
                dest.toString(), 1, "%03d", " ");

        Path vid = Files.createFile(tempDir.resolve("clip.mp4"));
        vid.toFile().setLastModified(1629034245000L);

        PictureRenamer renamer = new PictureRenamer(details, new DryRunFileOperationTracker());
        renamer.renamePictures();

        File[] srcFiles = tempDir.toFile().listFiles(f -> !f.isDirectory());
        assertThat(srcFiles).isNotNull().hasSize(1);
    }
}
