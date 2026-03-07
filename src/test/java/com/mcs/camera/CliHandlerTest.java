package com.mcs.camera;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CliHandlerTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Global Flags")
    class GlobalFlags {

        @Test
        void helpReturnsZero() {
            assertThat(CliHandler.run(new String[]{"--help"})).isZero();
        }

        @Test
        void versionReturnsZero() {
            assertThat(CliHandler.run(new String[]{"--version"})).isZero();
        }

        @Test
        void unknownSubcommandReturnsError() {
            assertThat(CliHandler.run(new String[]{"unknown"})).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Rename Subcommand")
    class RenameSubcommand {

        @Test
        void missingRequiredArgsReturnsError() {
            assertThat(CliHandler.run(new String[]{"rename"})).isEqualTo(1);
        }

        @Test
        void validArgsDryRunReturnsZero() throws Exception {
            Path src = Files.createDirectory(tempDir.resolve("src"));
            Path dest = Files.createDirectory(tempDir.resolve("dest"));
            Files.createFile(src.resolve("test.mp4"));

            int exitCode = CliHandler.run(new String[]{
                    "rename", "--source", src.toString(), "--dest", dest.toString(),
                    "--prefix", "TestAlbum", "--force-date", "2021-08-15",
                    "--include-videos", "--inline-videos", "--dry-run"
            });
            assertThat(exitCode).isZero();
        }

        @Test
        void invalidSourceDirReturnsError() {
            int exitCode = CliHandler.run(new String[]{
                    "rename", "--source", "/nonexistent/path",
                    "--dest", "/some/dest", "--prefix", "Album"
            });
            assertThat(exitCode).isEqualTo(1);
        }

        @Test
        void invalidForceDateReturnsError() throws Exception {
            Path src = Files.createDirectory(tempDir.resolve("src"));
            Path dest = Files.createDirectory(tempDir.resolve("dest"));
            Files.createFile(src.resolve("test.mp4"));

            int exitCode = CliHandler.run(new String[]{
                    "rename", "--source", src.toString(), "--dest", dest.toString(),
                    "--prefix", "Album", "--force-date", "not-a-date"
            });
            assertThat(exitCode).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Renumber Subcommand")
    class RenumberSubcommand {

        @Test
        void missingRequiredArgsReturnsError() {
            assertThat(CliHandler.run(new String[]{"renumber"})).isEqualTo(1);
        }

        @Test
        void validArgsDryRunReturnsZero() throws Exception {
            Path dir = Files.createDirectory(tempDir.resolve("album"));
            Files.createFile(dir.resolve("photo.mp4"));

            int exitCode = CliHandler.run(new String[]{
                    "renumber", "--dir", dir.toString(), "--prefix", "Album", "--dry-run",
                    "--include-videos", "--inline-videos"
            });
            assertThat(exitCode).isZero();
        }

        @Test
        void invalidDirReturnsError() {
            int exitCode = CliHandler.run(new String[]{
                    "renumber", "--dir", "/nonexistent/path", "--prefix", "Album"
            });
            assertThat(exitCode).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void invalidNumberPaddingReturnsError() throws Exception {
            Path src = Files.createDirectory(tempDir.resolve("src"));
            Path dest = Files.createDirectory(tempDir.resolve("dest"));

            int exitCode = CliHandler.run(new String[]{
                    "rename", "--source", src.toString(), "--dest", dest.toString(),
                    "--prefix", "Album", "--number-padding", "5"
            });
            assertThat(exitCode).isEqualTo(1);
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid", "foo", "!!", "tab"})
        void invalidSeparatorReturnsError(String separator) throws Exception {
            Path src = Files.createDirectory(tempDir.resolve("src-" + separator.hashCode()));
            Path dest = Files.createDirectory(tempDir.resolve("dest-" + separator.hashCode()));

            int exitCode = CliHandler.run(new String[]{
                    "rename", "--source", src.toString(), "--dest", dest.toString(),
                    "--prefix", "Album", "--separator", separator
            });
            assertThat(exitCode).isEqualTo(1);
        }
    }
}
