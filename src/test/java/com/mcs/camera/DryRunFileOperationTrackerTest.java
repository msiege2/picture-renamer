package com.mcs.camera;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class DryRunFileOperationTrackerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testMoveDoesNotMoveFile() throws Exception {
        File source = tempFolder.newFile("source.jpg");
        Path target = tempFolder.getRoot().toPath().resolve("target.jpg");

        DryRunFileOperationTracker tracker = new DryRunFileOperationTracker();
        tracker.move(source.toPath(), target);

        assertTrue("Source file should still exist", source.exists());
        assertFalse("Target file should not exist", target.toFile().exists());
        assertEquals(1, tracker.completedCount());
    }

    @Test
    public void testRollbackIsNoOp() throws Exception {
        File source = tempFolder.newFile("source.jpg");
        Path target = tempFolder.getRoot().toPath().resolve("target.jpg");

        DryRunFileOperationTracker tracker = new DryRunFileOperationTracker();
        tracker.move(source.toPath(), target);
        tracker.rollback(); // should not throw

        assertTrue("Source file should still exist", source.exists());
    }
}
