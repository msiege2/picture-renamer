package com.mcs.camera.extractor;

import java.io.File;
import java.time.LocalDateTime;

public interface MetadataExtractor {
    LocalDateTime extractDateTaken(File file) throws Exception;
}
