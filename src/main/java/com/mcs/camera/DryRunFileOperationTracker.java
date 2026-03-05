package com.mcs.camera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class DryRunFileOperationTracker extends FileOperationTracker {

    private static final Logger log = LoggerFactory.getLogger(DryRunFileOperationTracker.class);
    private int count = 0;

    @Override
    public void move(Path source, Path target) throws IOException {
        log.info("[DRY RUN] Would move: {} → {}", source, target);
        count++;
    }

    @Override
    public void rollback() {
        // no-op in dry run
    }

    @Override
    public int completedCount() {
        return count;
    }
}
