package com.mcs.camera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileOperationTracker {

    private static final Logger log = LoggerFactory.getLogger(FileOperationTracker.class);
    private final List<Map.Entry<Path, Path>> completedOps = new ArrayList<>();

    public void move(Path source, Path target) throws IOException {
        Files.move(source, target);
        completedOps.add(Map.entry(source, target));
    }

    public void rollback() {
        for (int i = completedOps.size() - 1; i >= 0; i--) {
            Map.Entry<Path, Path> op = completedOps.get(i);
            try {
                Files.move(op.getValue(), op.getKey());
            } catch (IOException e) {
                log.error("Rollback failed: {} → {}", op.getValue(), op.getKey(), e);
            }
        }
    }

    public int completedCount() {
        return completedOps.size();
    }
}
