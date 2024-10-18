package com.mcs.camera.extractor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.*;
import com.drew.metadata.file.FileSystemDirectory;
import java.io.File;
import java.util.Date;
import java.util.TimeZone;

public class PngMetadataExtractor implements MetadataExtractor {
    @Override
    public Date extractDateTaken(File file) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        Directory directory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);
        if (directory != null) {
            return directory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE, TimeZone.getDefault());
        }
        return null;
    }
}