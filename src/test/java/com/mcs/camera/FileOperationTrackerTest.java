package com.mcs.camera;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileOperationTrackerTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Move Operations")
    class MoveOperations {

        @Test
        void movesFileToTarget() throws IOException {
            Path source = Files.createFile(tempDir.resolve("original.txt"));
            Path target = tempDir.resolve("renamed.txt");

            FileOperationTracker tracker = new FileOperationTracker();
            tracker.move(source, target);

            assertThat(source).doesNotExist();
            assertThat(target).exists();
            assertThat(tracker.completedCount()).isEqualTo(1);
        }

        @Test
        void throwsOnNonexistentSource() {
            Path source = tempDir.resolve("nonexistent.txt");
            Path target = tempDir.resolve("target.txt");

            FileOperationTracker tracker = new FileOperationTracker();

            assertThatThrownBy(() -> tracker.move(source, target))
                    .isInstanceOf(IOException.class);
            assertThat(tracker.completedCount()).isZero();
        }

        @Test
        void completedCountStartsAtZero() {
            FileOperationTracker tracker = new FileOperationTracker();
            assertThat(tracker.completedCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Rollback")
    class Rollback {

        @Test
        void reversesSingleMove() throws IOException {
            Path source = Files.createFile(tempDir.resolve("original.txt"));
            Path target = tempDir.resolve("renamed.txt");

            FileOperationTracker tracker = new FileOperationTracker();
            tracker.move(source, target);

            assertThat(source).doesNotExist();
            assertThat(target).exists();

            tracker.rollback();

            assertThat(source).exists();
            assertThat(target).doesNotExist();
        }

        @Test
        void reversesMultipleMovesInOrder() throws IOException {
            Path file1 = Files.createFile(tempDir.resolve("file1.txt"));
            Path file2 = Files.createFile(tempDir.resolve("file2.txt"));
            Path target1 = tempDir.resolve("moved1.txt");
            Path target2 = tempDir.resolve("moved2.txt");

            FileOperationTracker tracker = new FileOperationTracker();
            tracker.move(file1, target1);
            tracker.move(file2, target2);

            assertThat(tracker.completedCount()).isEqualTo(2);

            tracker.rollback();

            assertThat(file1).exists();
            assertThat(file2).exists();
            assertThat(target1).doesNotExist();
            assertThat(target2).doesNotExist();
        }

        @Test
        void isBestEffortWhenSomeMovesCantBeReversed() throws IOException {
            Path file1 = Files.createFile(tempDir.resolve("file1.txt"));
            Path file2 = Files.createFile(tempDir.resolve("file2.txt"));
            Path target1 = tempDir.resolve("moved1.txt");
            Path target2 = tempDir.resolve("moved2.txt");

            FileOperationTracker tracker = new FileOperationTracker();
            tracker.move(file1, target1);
            tracker.move(file2, target2);

            Files.delete(target1); // make rollback of first move impossible

            tracker.rollback(); // should not throw

            assertThat(file2).exists();
        }

        @Test
        void onEmptyTrackerDoesNothing() {
            FileOperationTracker tracker = new FileOperationTracker();
            tracker.rollback();
            assertThat(tracker.completedCount()).isZero();
        }
    }
}
