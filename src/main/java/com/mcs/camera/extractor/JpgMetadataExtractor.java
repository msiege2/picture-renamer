package com.mcs.camera.extractor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import java.io.File;
import java.util.Date;
import java.util.TimeZone;

public class JpgMetadataExtractor implements MetadataExtractor {
    @Override
    public Date extractDateTaken(File file) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        Directory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (directory != null) {
            return directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
        }
        return null;
    }
}