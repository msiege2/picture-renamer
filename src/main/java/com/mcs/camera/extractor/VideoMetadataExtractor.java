package com.mcs.camera.extractor;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class VideoMetadataExtractor implements MetadataExtractor {
    @Override
    public LocalDateTime extractDateTaken(File file) throws Exception {
        return Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
