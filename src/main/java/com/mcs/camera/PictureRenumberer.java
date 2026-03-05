package com.mcs.camera;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PictureRenumberer {

    static final Logger log = LoggerFactory.getLogger(PictureRenumberer.class.getName());

    private final String directory;
    private final String prefix;
    private final boolean includeVideos;
    private final boolean inlineVideos;
    private final String numberFormat;
    private final String filenameSeparator;

    DateTimeFormatter dateTimeFormatter;
    List<String> temporaryNames = new ArrayList<>();
    Map<String, File> fileMap = new HashMap<>();

    public PictureRenumberer(String directory, String prefix, boolean includeVideos, boolean inlineVideos,
                             String numberFormat, String filenameSeparator) {
        this.directory = directory;
        this.prefix = prefix;
        this.includeVideos = includeVideos;
        this.inlineVideos = inlineVideos;
        this.numberFormat = numberFormat;
        this.filenameSeparator = filenameSeparator;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    public void renumberPictures() {
        List<File> videos = new ArrayList<>();
        String homeDirPath = directory;
        if (!homeDirPath.endsWith(File.separator)) {
            homeDirPath += File.separator;
        }

        Collection<File> filesInHomeDir = getFilesInDir(homeDirPath);
        if (filesInHomeDir.isEmpty()) {
            log.warn("No files found in directory -- " + homeDirPath);
            return;
        }

        for (File f : filesInHomeDir) {
            String fileExtension = FilenameUtils.getExtension(f.getName()).toLowerCase();
            switch (fileExtension) {
                case "jpg", "jpeg", "heic" -> grabExifMetadata(f);
                case "png" -> grabPngMetadata(f);
                case "mov", "avi", "mkv", "mp4", "mts", "m2ts" -> {
                    if (includeVideos) {
                        if (inlineVideos) {
                            grabMovieMetadata(f);
                        } else {
                            videos.add(f);
                        }
                    }
                }
                default -> log.warn("Ignoring unknown file type: " + f.getName());
            }
        }

        Collections.sort(temporaryNames);

        int counter = 1;

        FileOperationTracker tracker = new FileOperationTracker();
        try {
            // Pass 1: rename all to random names to avoid collisions
            for (String orderedPicture : temporaryNames) {
                File origFile = fileMap.get(orderedPicture);
                if (!origFile.exists()) {
                    log.error("Prior to renaming -- cannot find file " + origFile.getName());
                    return;
                }
                File renamedFile = new File(origFile.getParent() + File.separator
                        + UUID.randomUUID() + "."
                        + FilenameUtils.getExtension(origFile.getName()).toLowerCase());
                tracker.move(origFile.toPath(), renamedFile.toPath());
                fileMap.put(orderedPicture, renamedFile);
            }

            // Pass 2: rename to final sequential names
            for (String orderedPicture : temporaryNames) {
                File origFile = fileMap.get(orderedPicture);
                String newFileName = origFile.getParent() + File.separator + prefix + filenameSeparator
                        + String.format(numberFormat, counter) + "."
                        + FilenameUtils.getExtension(origFile.getName()).toLowerCase();
                tracker.move(origFile.toPath(), Path.of(newFileName));
                counter++;
            }

            if (includeVideos && !inlineVideos) {
                for (File f : videos) {
                    String extension = FilenameUtils.getExtension(f.getName()).toLowerCase();
                    String newFileName = f.getParent() + File.separator + prefix + filenameSeparator
                            + String.format(numberFormat, counter) + "." + extension;
                    log.debug("Renaming video file " + f.getName() + " to " + newFileName);
                    tracker.move(f.toPath(), Path.of(newFileName));
                    counter++;
                }
            }
        } catch (IOException e) {
            log.error("File operation failed, rolling back all changes", e);
            tracker.rollback();
            throw new RuntimeException("File operation failed: " + e.getMessage(), e);
        }
    }

    private Collection<File> getFilesInDir(String directoryPath) {
        log.info("Using directory: " + directoryPath);
        final File dir = new File(directoryPath);
        return FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, null);
    }

    private void grabExifMetadata(File f) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(f);
            Directory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            LocalDateTime dateTaken = null;
            if (directory != null) {
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
                if (date != null) {
                    dateTaken = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }

            if (dateTaken == null) {
                dateTaken = Instant.ofEpochMilli(f.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }

            String dateTakenStr = dateTaken.format(dateTimeFormatter);
            String tempName = dateTakenStr + " " + f.getName();
            log.debug("Date taken: " + f.getName() + " --> " + dateTakenStr);
            temporaryNames.add(tempName);
            fileMap.put(tempName, f);
        } catch (Exception e) {
            log.error("Error processing image file " + f.getName(), e);
            throw new RuntimeException("Error processing " + f.getName(), e);
        }
    }

    private void grabPngMetadata(File f) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(f);
            Directory directory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);

            LocalDateTime dateTaken = null;
            if (directory != null) {
                Date date = directory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE, TimeZone.getDefault());
                if (date != null) {
                    dateTaken = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }

            if (dateTaken == null) {
                dateTaken = Instant.ofEpochMilli(f.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }

            String dateTakenStr = dateTaken.format(dateTimeFormatter);
            String tempName = dateTakenStr + " " + f.getName();
            log.debug("Date taken: " + f.getName() + " --> " + dateTakenStr);
            temporaryNames.add(tempName);
            fileMap.put(tempName, f);
        } catch (Exception e) {
            log.error("Error processing PNG file " + f.getName(), e);
            throw new RuntimeException("Error processing " + f.getName(), e);
        }
    }

    private void grabMovieMetadata(File f) {
        try {
            LocalDateTime dateTaken = Instant.ofEpochMilli(f.lastModified())
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            String dateTakenStr = dateTaken.format(dateTimeFormatter);
            String tempName = dateTakenStr + " " + f.getName();
            log.debug("Date taken: " + f.getName() + " --> " + dateTakenStr);
            temporaryNames.add(tempName);
            fileMap.put(tempName, f);
        } catch (Exception e) {
            log.error("Error processing video file " + f.getName(), e);
            throw new RuntimeException("Error processing " + f.getName(), e);
        }
    }
}
