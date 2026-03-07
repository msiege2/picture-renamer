package com.mcs.camera;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DryRunFileOperationTrackerTest {

    @TempDir
    Path tempDir;

    @Test
    void moveDoesNotActuallyMoveFile() throws IOException {
        Path source = Files.createFile(tempDir.resolve("source.jpg"));
        Path target = tempDir.resolve("target.jpg");

        DryRunFileOperationTracker tracker = new DryRunFileOperationTracker();
        tracker.move(source, target);

        assertThat(source).exists();
        assertThat(target).doesNotExist();
        assertThat(tracker.completedCount()).isEqualTo(1);
    }

    @Test
    void rollbackIsNoOp() throws IOException {
        Path source = Files.createFile(tempDir.resolve("source.jpg"));
        Path target = tempDir.resolve("target.jpg");

        DryRunFileOperationTracker tracker = new DryRunFileOperationTracker();
        tracker.move(source, target);
        tracker.rollback();

        assertThat(source).exists();
    }
}
