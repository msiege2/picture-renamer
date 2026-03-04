package com.mcs.camera.extractor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

public class JpgMetadataExtractor implements MetadataExtractor {
    @Override
    public LocalDateTime extractDateTaken(File file) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        Directory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (directory != null) {
            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
            if (date != null) {
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }
        return null;
    }
}
