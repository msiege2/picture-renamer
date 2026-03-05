package com.mcs.camera;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class FileOperationTrackerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testMoveRenamesFile() throws IOException {
        File src = tempFolder.newFile("original.txt");
        Path target = tempFolder.getRoot().toPath().resolve("renamed.txt");

        FileOperationTracker tracker = new FileOperationTracker();
        tracker.move(src.toPath(), target);

        assertFalse("Source should not exist", src.exists());
        assertTrue("Target should exist", Files.exists(target));
        assertEquals(1, tracker.completedCount());
    }

    @Test
    public void testMoveThrowsOnNonexistentSource() {
        Path src = tempFolder.getRoot().toPath().resolve("nonexistent.txt");
        Path target = tempFolder.getRoot().toPath().resolve("target.txt");

        FileOperationTracker tracker = new FileOperationTracker();
        try {
            tracker.move(src, target);
            fail("Should have thrown IOException");
        } catch (IOException e) {
            assertEquals(0, tracker.completedCount());
        }
    }

    @Test
    public void testRollbackReversesSingleMove() throws IOException {
        File src = tempFolder.newFile("original.txt");
        Path srcPath = src.toPath();
        Path target = tempFolder.getRoot().toPath().resolve("renamed.txt");

        FileOperationTracker tracker = new FileOperationTracker();
        tracker.move(srcPath, target);

        assertFalse(Files.exists(srcPath));
        assertTrue(Files.exists(target));

        tracker.rollback();

        assertTrue("Source should be restored", Files.exists(srcPath));
        assertFalse("Target should be gone", Files.exists(target));
    }

    @Test
    public void testRollbackReversesMultipleMovesInOrder() throws IOException {
        File file1 = tempFolder.newFile("file1.txt");
        File file2 = tempFolder.newFile("file2.txt");
        Path target1 = tempFolder.getRoot().toPath().resolve("moved1.txt");
        Path target2 = tempFolder.getRoot().toPath().resolve("moved2.txt");

        FileOperationTracker tracker = new FileOperationTracker();
        tracker.move(file1.toPath(), target1);
        tracker.move(file2.toPath(), target2);

        assertEquals(2, tracker.completedCount());

        tracker.rollback();

        assertTrue("file1 should be restored", file1.exists());
        assertTrue("file2 should be restored", file2.exists());
        assertFalse("target1 should be gone", Files.exists(target1));
        assertFalse("target2 should be gone", Files.exists(target2));
    }

    @Test
    public void testRollbackIsBestEffort() throws IOException {
        File file1 = tempFolder.newFile("file1.txt");
        File file2 = tempFolder.newFile("file2.txt");
        Path target1 = tempFolder.getRoot().toPath().resolve("moved1.txt");
        Path target2 = tempFolder.getRoot().toPath().resolve("moved2.txt");

        FileOperationTracker tracker = new FileOperationTracker();
        tracker.move(file1.toPath(), target1);
        tracker.move(file2.toPath(), target2);

        // Delete target1 so its rollback will fail
        Files.delete(target1);

        // Should not throw — rollback is best-effort
        tracker.rollback();

        // file2 should still be restored even though file1 rollback failed
        assertTrue("file2 should be restored", file2.exists());
    }

    @Test
    public void testCompletedCountStartsAtZero() {
        FileOperationTracker tracker = new FileOperationTracker();
        assertEquals(0, tracker.completedCount());
    }

    @Test
    public void testRollbackOnEmptyTrackerDoesNothing() {
        FileOperationTracker tracker = new FileOperationTracker();
        tracker.rollback(); // should not throw
        assertEquals(0, tracker.completedCount());
    }
}
