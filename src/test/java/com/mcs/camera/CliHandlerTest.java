package com.mcs.camera;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class CliHandlerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testHelpFlag() {
        int exitCode = CliHandler.run(new String[]{"--help"});
        assertEquals(0, exitCode);
    }

    @Test
    public void testVersionFlag() {
        int exitCode = CliHandler.run(new String[]{"--version"});
        assertEquals(0, exitCode);
    }

    @Test
    public void testUnknownSubcommandReturnsError() {
        int exitCode = CliHandler.run(new String[]{"unknown"});
        assertEquals(1, exitCode);
    }

    @Test
    public void testRenameMissingRequiredArgs() {
        int exitCode = CliHandler.run(new String[]{"rename"});
        assertEquals(1, exitCode);
    }

    @Test
    public void testRenameWithValidArgs() throws Exception {
        String src = tempFolder.newFolder("src").getAbsolutePath();
        String dest = tempFolder.newFolder("dest").getAbsolutePath();
        new java.io.File(src, "test.mp4").createNewFile();

        int exitCode = CliHandler.run(new String[]{
                "rename", "--source", src, "--dest", dest,
                "--prefix", "TestAlbum", "--force-date", "2021-08-15",
                "--include-videos", "--inline-videos", "--dry-run"
        });
        assertEquals(0, exitCode);
    }

    @Test
    public void testRenameInvalidSourceDir() {
        int exitCode = CliHandler.run(new String[]{
                "rename", "--source", "/nonexistent/path",
                "--dest", "/some/dest", "--prefix", "Album"
        });
        assertEquals(1, exitCode);
    }

    @Test
    public void testRenumberMissingRequiredArgs() {
        int exitCode = CliHandler.run(new String[]{"renumber"});
        assertEquals(1, exitCode);
    }

    @Test
    public void testRenumberWithValidArgs() throws Exception {
        String dir = tempFolder.newFolder("album").getAbsolutePath();
        new java.io.File(dir, "photo.mp4").createNewFile();

        int exitCode = CliHandler.run(new String[]{
                "renumber", "--dir", dir, "--prefix", "Album", "--dry-run",
                "--include-videos", "--inline-videos"
        });
        assertEquals(0, exitCode);
    }

    @Test
    public void testRenumberInvalidDir() {
        int exitCode = CliHandler.run(new String[]{
                "renumber", "--dir", "/nonexistent/path", "--prefix", "Album"
        });
        assertEquals(1, exitCode);
    }

    @Test
    public void testInvalidNumberPadding() throws Exception {
        String src = tempFolder.newFolder("src").getAbsolutePath();
        String dest = tempFolder.newFolder("dest").getAbsolutePath();

        int exitCode = CliHandler.run(new String[]{
                "rename", "--source", src, "--dest", dest,
                "--prefix", "Album", "--number-padding", "5"
        });
        assertEquals(1, exitCode);
    }

    @Test
    public void testInvalidSeparator() throws Exception {
        String src = tempFolder.newFolder("src").getAbsolutePath();
        String dest = tempFolder.newFolder("dest").getAbsolutePath();

        int exitCode = CliHandler.run(new String[]{
                "rename", "--source", src, "--dest", dest,
                "--prefix", "Album", "--separator", "invalid"
        });
        assertEquals(1, exitCode);
    }

    @Test
    public void testInvalidForceDateFormat() throws Exception {
        String src = tempFolder.newFolder("src").getAbsolutePath();
        String dest = tempFolder.newFolder("dest").getAbsolutePath();
        new java.io.File(src, "test.mp4").createNewFile();

        int exitCode = CliHandler.run(new String[]{
                "rename", "--source", src, "--dest", dest,
                "--prefix", "Album", "--force-date", "not-a-date"
        });
        assertEquals(1, exitCode);
    }
}
