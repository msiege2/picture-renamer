# Files.move() with Rollback — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace all silent `File.renameTo()` calls with `Files.move()` + automatic rollback on failure, preventing data loss from partial operations.

**Architecture:** A shared `FileOperationTracker` utility wraps `Files.move()`, records every completed operation, and reverses them in order on failure. Both `PictureRenamer` and `PictureRenumberer` create a tracker instance, use it for all file operations, and call `rollback()` in their catch blocks.

**Tech Stack:** Java 17 NIO (`java.nio.file.Files`, `java.nio.file.Path`), JUnit 4, SLF4J logging

---

### Task 1: Create FileOperationTracker with tests

**Files:**
- Create: `src/test/java/com/mcs/camera/FileOperationTrackerTest.java`
- Create: `src/main/java/com/mcs/camera/FileOperationTracker.java`

**Step 1: Write the failing tests**

Create the test file with all tests for the tracker:

```java
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
```

**Step 2: Run tests to verify they fail**

Run: `mvn test -Dtest=FileOperationTrackerTest`
Expected: Compilation failure — `FileOperationTracker` class does not exist

**Step 3: Write minimal implementation**

Create `src/main/java/com/mcs/camera/FileOperationTracker.java`:

```java
package com.mcs.camera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileOperationTracker {

    private static final Logger log = LoggerFactory.getLogger(FileOperationTracker.class);
    private final List<Map.Entry<Path, Path>> completedOps = new ArrayList<>();

    public void move(Path source, Path target) throws IOException {
        Files.move(source, target);
        completedOps.add(Map.entry(source, target));
    }

    public void rollback() {
        for (int i = completedOps.size() - 1; i >= 0; i--) {
            Map.Entry<Path, Path> op = completedOps.get(i);
            try {
                Files.move(op.getValue(), op.getKey());
            } catch (IOException e) {
                log.error("Rollback failed: {} → {}", op.getValue(), op.getKey(), e);
            }
        }
    }

    public int completedCount() {
        return completedOps.size();
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `mvn test -Dtest=FileOperationTrackerTest`
Expected: All 7 tests PASS

**Step 5: Commit**

```bash
git add src/main/java/com/mcs/camera/FileOperationTracker.java src/test/java/com/mcs/camera/FileOperationTrackerTest.java
git commit -m "feat: add FileOperationTracker with Files.move() and rollback support"
```

---

### Task 2: Update PictureRenamer to use FileOperationTracker

**Files:**
- Modify: `src/main/java/com/mcs/camera/PictureRenamer.java:54-146` (the `renamePictures()` method)

**Step 1: Run existing tests to confirm green baseline**

Run: `mvn test -Dtest=PictureRenamerTest`
Expected: All 11 tests PASS

**Step 2: Modify PictureRenamer.renamePictures()**

Add import at top of file:

```java
import java.io.IOException;
import java.nio.file.Path;
```

Replace the body of `renamePictures()` from the line `int currentPictureCounter = counterStart;` (line 95) through the end of the method (line 146). Wrap everything from the counter initialization through the move loop in a try/catch. The metadata collection and sorting code above line 95 stays unchanged.

The new code from line 95 onward:

```java
		int currentPictureCounter = counterStart;

		for (String orderedPicture : temporaryNames) {
			File origFile = fileMap.get(orderedPicture);
			if (origFile == null || !origFile.exists()) {
				log.error("Prior to renaming -- cannot find file " + orderedPicture);
				return;
			}
		}

		FileOperationTracker tracker = new FileOperationTracker();
		try {
			for (String orderedPicture : temporaryNames) {
				File origFile = fileMap.get(orderedPicture);
				String newFileName;
				if (prefix != null && !prefix.isEmpty()) {
					newFileName = homeDirPath + File.separator + prefix + filenameSeparator
							+ String.format(numberFormat, currentPictureCounter) + "."
							+ FilenameUtils.getExtension(origFile.getName()).toLowerCase();
				} else {
					newFileName = homeDirPath + File.separator
							+ orderedPicture.replaceAll("\\s", "_").replaceAll(":", "_");
				}
				tracker.move(origFile.toPath(), Path.of(newFileName));
				currentPictureCounter++;
			}

			if (includeVideos && !inlineVideos) {
				for (File f : videos) {
					String extension = FilenameUtils.getExtension(f.getName()).toLowerCase();
					String newFileName = homeDirPath + File.separator + prefix + filenameSeparator
							+ String.format(numberFormat, currentPictureCounter) + "." + extension;
					log.debug("Renaming video file " + f.getName() + " to " + newFileName);
					tracker.move(f.toPath(), Path.of(newFileName));
					currentPictureCounter++;
				}
			}

			try {
				albumYear = albumDirName.substring(0, 4);
			} catch (Exception e) {
				albumYear = null;
				log.warn("Failed to parse year from album name.");
			}
			File dir = new File(destDirPath + File.separator + albumYear + File.separator + albumDirName);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			filesInHomeDir = getFilesInDir(homeDirPath);
			for (File f : filesInHomeDir) {
				tracker.move(f.toPath(), Path.of(dir.getAbsolutePath() + File.separator + f.getName()));
				log.debug("Moving file to: " + dir.getAbsolutePath() + File.separator + f.getName());
			}
		} catch (IOException e) {
			log.error("File operation failed, rolling back all changes", e);
			tracker.rollback();
			throw new RuntimeException("File operation failed: " + e.getMessage(), e);
		}
```

**Step 3: Run tests to verify they still pass**

Run: `mvn test -Dtest=PictureRenamerTest`
Expected: All 11 tests PASS (behavior is identical for the happy path)

**Step 4: Commit**

```bash
git add src/main/java/com/mcs/camera/PictureRenamer.java
git commit -m "refactor: use FileOperationTracker in PictureRenamer for safe file operations"
```

---

### Task 3: Update PictureRenumberer to use FileOperationTracker

**Files:**
- Modify: `src/main/java/com/mcs/camera/PictureRenumberer.java:48-117` (the `renumberPictures()` method)

**Step 1: Run existing tests to confirm green baseline**

Run: `mvn test`
Expected: All tests PASS

**Step 2: Modify PictureRenumberer.renumberPictures()**

Add import at top of file:

```java
import java.io.IOException;
import java.nio.file.Path;
```

Replace the body of `renumberPictures()` from `int counter = 1;` (line 81) through the end of the method (line 117). Wrap the three passes in a try/catch. The metadata collection and sorting above line 81 stays unchanged.

The new code from line 81 onward:

```java
        int counter = 1;

        FileOperationTracker tracker = new FileOperationTracker();
        try {
            // Pass 1: rename all to random names to avoid collisions
            for (String orderedPicture : temporaryNames) {
                File origFile = fileMap.get(orderedPicture);
                if (!origFile.exists()) {
                    log.error("Prior to renaming -- cannot find file " + origFile.getName());
                    return;
                }
                File renamedFile = new File(origFile.getParent() + File.separator
                        + UUID.randomUUID() + "."
                        + FilenameUtils.getExtension(origFile.getName()).toLowerCase());
                tracker.move(origFile.toPath(), renamedFile.toPath());
                fileMap.put(orderedPicture, renamedFile);
            }

            // Pass 2: rename to final sequential names
            for (String orderedPicture : temporaryNames) {
                File origFile = fileMap.get(orderedPicture);
                String newFileName = origFile.getParent() + File.separator + prefix + filenameSeparator
                        + String.format(numberFormat, counter) + "."
                        + FilenameUtils.getExtension(origFile.getName()).toLowerCase();
                tracker.move(origFile.toPath(), Path.of(newFileName));
                counter++;
            }

            if (includeVideos && !inlineVideos) {
                for (File f : videos) {
                    String extension = FilenameUtils.getExtension(f.getName()).toLowerCase();
                    String newFileName = f.getParent() + File.separator + prefix + filenameSeparator
                            + String.format(numberFormat, counter) + "." + extension;
                    log.debug("Renaming video file " + f.getName() + " to " + newFileName);
                    tracker.move(f.toPath(), Path.of(newFileName));
                    counter++;
                }
            }
        } catch (IOException e) {
            log.error("File operation failed, rolling back all changes", e);
            tracker.rollback();
            throw new RuntimeException("File operation failed: " + e.getMessage(), e);
        }
```

**Step 3: Run tests to verify they pass**

Run: `mvn test`
Expected: All tests PASS

**Step 4: Commit**

```bash
git add src/main/java/com/mcs/camera/PictureRenumberer.java
git commit -m "refactor: use FileOperationTracker in PictureRenumberer for safe file operations"
```

---

### Task 4: Add rollback integration test for PictureRenamer

**Files:**
- Modify: `src/test/java/com/mcs/camera/PictureRenamerTest.java`

**Step 1: Write the failing test**

Add this test to `PictureRenamerTest.java`. It creates a read-only destination directory to trigger an `IOException` during the move phase, after renames succeed. The test verifies that files are rolled back to their original names in the source directory.

```java
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

    // Delete the dest folder so mkdirs can't create the album subdir,
    // then make it read-only so the move phase fails
    File albumDir = new File(dest + File.separator + "2021" + File.separator + "2021-08-15, Trip");
    // Pre-create the album dir, then put a conflicting file to cause IOException
    albumDir.mkdirs();
    // Create a directory with same name as the file that will be moved — Files.move throws
    // DirectoryNotEmptyException or FileAlreadyExistsException
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
```

**Step 2: Run test to verify it passes**

Run: `mvn test -Dtest=PictureRenamerTest#testRollbackOnMoveFailure`
Expected: PASS — the rollback restores the file

**Step 3: Commit**

```bash
git add src/test/java/com/mcs/camera/PictureRenamerTest.java
git commit -m "test: add rollback integration test for PictureRenamer"
```

---

### Task 5: Add rollback integration test for PictureRenumberer

**Files:**
- Create: `src/test/java/com/mcs/camera/PictureRenumbererTest.java`

**Step 1: Write the tests**

```java
package com.mcs.camera;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class PictureRenumbererTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testRenumberHappyPath() throws IOException {
        File file1 = tempFolder.newFile("b.mp4");
        file1.setLastModified(1629034246000L);
        File file2 = tempFolder.newFile("a.mp4");
        file2.setLastModified(1629034245000L);

        PictureRenumberer renumberer = new PictureRenumberer(
                tempFolder.getRoot().getAbsolutePath(),
                "Vacation", true, true, "%03d", " ");
        renumberer.renumberPictures();

        String[] files = tempFolder.getRoot().list();
        assertNotNull(files);
        Arrays.sort(files);
        assertEquals(2, files.length);
        assertEquals("Vacation 001.mp4", files[0]);
        assertEquals("Vacation 002.mp4", files[1]);
    }

    @Test
    public void testRollbackOnRenumberFailure() throws IOException {
        File file1 = tempFolder.newFile("clip1.mp4");
        file1.setLastModified(1629034245000L);
        File file2 = tempFolder.newFile("clip2.mp4");
        file2.setLastModified(1629034246000L);

        PictureRenumberer renumberer = new PictureRenumberer(
                tempFolder.getRoot().getAbsolutePath(),
                "Trip", true, true, "%03d", " ");

        // Pre-create a directory named "Trip 002.mp4" to block the second rename in pass 2
        File blocker = new File(tempFolder.getRoot(), "Trip 002.mp4");
        blocker.mkdir();

        try {
            renumberer.renumberPictures();
            fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("File operation failed"));
        }

        // After rollback, original files should be restored
        File[] restored = tempFolder.getRoot().listFiles(f -> !f.isDirectory());
        assertNotNull(restored);
        String[] names = Arrays.stream(restored).map(File::getName).sorted().toArray(String[]::new);
        assertEquals(2, names.length);
        assertEquals("clip1.mp4", names[0]);
        assertEquals("clip2.mp4", names[1]);
    }
}
```

**Step 2: Run tests to verify they pass**

Run: `mvn test -Dtest=PictureRenumbererTest`
Expected: All 2 tests PASS

**Step 3: Commit**

```bash
git add src/test/java/com/mcs/camera/PictureRenumbererTest.java
git commit -m "test: add PictureRenumbererTest with happy path and rollback tests"
```

---

### Task 6: Run full test suite and verify

**Step 1: Run all tests**

Run: `mvn test`
Expected: All tests PASS (original 11 from PictureRenamerTest + 7 FileOperationTrackerTest + 1 rollback test in PictureRenamerTest + 2 PictureRenumbererTest + existing AlbumDetailsTest + AppPreferencesTest = ~28+ tests)

**Step 2: Run mvn package to ensure build works**

Run: `mvn package`
Expected: BUILD SUCCESS, produces dist/PictureRenamer.jar

---

### Task 7: Create feature branch, squash/organize, and prepare PR

**Step 1: Ensure all work is on a feature branch**

Branch name: `feature/files-move-rollback`

**Step 2: Push and create PR**

PR title: `feat: replace File.renameTo() with Files.move() and rollback support`

PR body should reference:
- The design doc at `docs/plans/2026-03-05-files-move-rollback-design.md`
- That this addresses the "Check File.renameTo() return value" and "Partial rename + move is not atomic" TODOs
- Close any related GitHub issues
