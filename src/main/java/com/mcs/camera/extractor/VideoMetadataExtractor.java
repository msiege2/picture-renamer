package com.mcs.camera.extractor;

import java.io.File;
import java.util.Date;

public class VideoMetadataExtractor implements MetadataExtractor {
    @Override
    public Date extractDateTaken(File file) throws Exception {
        return new Date(file.lastModified());
    }
}