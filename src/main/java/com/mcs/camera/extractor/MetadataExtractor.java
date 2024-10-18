package com.mcs.camera.extractor;

import java.io.File;
import java.util.Date;

public interface MetadataExtractor {
    Date extractDateTaken(File file) throws Exception;
}