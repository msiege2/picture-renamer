package com.mcs.camera;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlbumDetailsTest {

    @Test
    void basicConstructorSetsAllFields() {
        AlbumDetails details = new AlbumDetails("Vacation", "C:\\Photos", true, "2021-08-15",
                true, false, false, true);

        assertThat(details.getPrefix()).isEqualTo("Vacation");
        assertThat(details.getSourceDir()).isEqualTo("C:\\Photos");
        assertThat(details.isForceDateFlag()).isTrue();
        assertThat(details.getForceDate()).isEqualTo("2021-08-15");
        assertThat(details.isIncludeVideos()).isTrue();
        assertThat(details.isInlineVideos()).isFalse();
        assertThat(details.isKeepOrder()).isFalse();
        assertThat(details.isTryFilenameDateTimeOnMetadataFail()).isTrue();
    }

    @Test
    void extendedConstructorSetsOptionsFields() {
        AlbumDetails details = new AlbumDetails("Vacation", "C:\\Photos", true, "2021-08-15",
                true, false, false, true,
                "D:\\Library", 5, "%04d", "-");

        assertThat(details.getDestinationDir()).isEqualTo("D:\\Library");
        assertThat(details.getCounterStart()).isEqualTo(5);
        assertThat(details.getNumberFormat()).isEqualTo("%04d");
        assertThat(details.getFilenameSeparator()).isEqualTo("-");
    }
}
