package com.mcs.camera;

import com.mcs.camera.extractor.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
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

public class PictureRenamer {
	static final Logger log = LoggerFactory.getLogger(PictureRenamer.class.getName());
	private final String sourceDir;
	private final boolean forceDateFlag;
	private final String forceDate;
	private final boolean keepOrder;
	private final boolean includeVideos;
	private final boolean inlineVideos;
	private final boolean tryFilenameDateTimeOnMetadataFail;
	private final String prefix;
	private final String destinationDir;
	private final int counterStart;
	private final String numberFormat;
	private final String filenameSeparator;

	DateTimeFormatter dateTimeFormatter;
	String albumDirName = null;
	String albumYear = null;
	List<String> temporaryNames = new ArrayList<>();
	Map<String, File> fileMap = new HashMap<>();

	public PictureRenamer(AlbumDetails albumDetails) {
		this.prefix = albumDetails.getPrefix();
		this.sourceDir = albumDetails.getSourceDir();
		this.forceDateFlag = albumDetails.isForceDateFlag();
		this.forceDate = albumDetails.getForceDate();
		this.includeVideos = albumDetails.isIncludeVideos();
		this.inlineVideos = albumDetails.isInlineVideos();
		this.keepOrder = albumDetails.isKeepOrder();
		this.tryFilenameDateTimeOnMetadataFail = albumDetails.isTryFilenameDateTimeOnMetadataFail();
		this.destinationDir = albumDetails.getDestinationDir();
		this.counterStart = albumDetails.getCounterStart();
		this.numberFormat = albumDetails.getNumberFormat();
		this.filenameSeparator = albumDetails.getFilenameSeparator();
		this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	}

	public void renamePictures() {
		List<File> videos = new ArrayList<>();
		String homeDirPath = sourceDir;
		String destDirPath = destinationDir;
		Collection<File> filesInHomeDir = getFilesInDir(homeDirPath);

		if (filesInHomeDir.isEmpty()) {
			log.warn("No files found in home directory -- " + homeDirPath);
			return;
		}

		if (keepOrder) {
			log.info("KEEP_ORDER on. Skipping metadata checks. This forces FORCE_DATE to true.");
			albumDirName = forceDate + ", " + prefix;
			for (File f : filesInHomeDir) {
				temporaryNames.add(f.getName());
				fileMap.put(f.getName(), f);
			}
			Collections.sort(temporaryNames, new FilenameComparator());
		} else {
			for (File f : filesInHomeDir) {
				String fileExtension = FilenameUtils.getExtension(f.getName()).toLowerCase();
				switch (fileExtension) {
					case "jpg", "jpeg" -> grabMetadata(f, new JpgMetadataExtractor());
					case "heic" -> grabMetadata(f, new HeicMetadataExtractor());
					case "png" -> grabMetadata(f, new PngMetadataExtractor());
					case "mov", "avi", "mkv", "mp4" -> {
						if (includeVideos) {
							if (inlineVideos) {
								grabMetadata(f, new VideoMetadataExtractor());
							} else {
								videos.add(f);
							}
						}
					}
					default -> log.warn("Ignoring unknown file type: " + f.getName());
				}
			}
			Collections.sort(temporaryNames);
		}

		int currentPictureCounter = counterStart;

		for (String orderedPicture : temporaryNames) {
			File origFile = fileMap.get(orderedPicture);
			if (origFile == null || !origFile.exists()) {
				log.error("Prior to renaming -- cannot find file " + orderedPicture);
				return;
			}
		}

		FileOperationTracker tracker = new FileOperationTracker();
		try {
			for (String orderedPicture : temporaryNames) {
				File origFile = fileMap.get(orderedPicture);
				String newFileName;
				if (prefix != null && !prefix.isEmpty()) {
					newFileName = homeDirPath + File.separator + prefix + filenameSeparator
							+ String.format(numberFormat, currentPictureCounter) + "."
							+ FilenameUtils.getExtension(origFile.getName()).toLowerCase();
				} else {
					newFileName = homeDirPath + File.separator
							+ orderedPicture.replaceAll("\\s", "_").replaceAll(":", "_");
				}
				tracker.move(origFile.toPath(), Path.of(newFileName));
				currentPictureCounter++;
			}

			if (includeVideos && !inlineVideos) {
				for (File f : videos) {
					String extension = FilenameUtils.getExtension(f.getName()).toLowerCase();
					String newFileName = homeDirPath + File.separator + prefix + filenameSeparator
							+ String.format(numberFormat, currentPictureCounter) + "." + extension;
					log.debug("Renaming video file " + f.getName() + " to " + newFileName);
					tracker.move(f.toPath(), Path.of(newFileName));
					currentPictureCounter++;
				}
			}

			try {
				albumYear = albumDirName.substring(0, 4);
			} catch (Exception e) {
				albumYear = null;
				log.warn("Failed to parse year from album name.");
			}
			File dir = new File(destDirPath + File.separator + albumYear + File.separator + albumDirName);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			filesInHomeDir = getFilesInDir(homeDirPath);
			for (File f : filesInHomeDir) {
				tracker.move(f.toPath(), Path.of(dir.getAbsolutePath() + File.separator + f.getName()));
				log.debug("Moving file to: " + dir.getAbsolutePath() + File.separator + f.getName());
			}
		} catch (IOException e) {
			log.error("File operation failed, rolling back all changes", e);
			tracker.rollback();
			throw new RuntimeException("File operation failed: " + e.getMessage(), e);
		}
	}

	Collection<File> getFilesInDir(String directoryPath) {
		log.info("Using home directory: " + directoryPath);
		final File homeDir = new File(directoryPath);
		return FileUtils.listFiles(homeDir, TrueFileFilter.INSTANCE, null);
	}

	void grabMetadata(File f, MetadataExtractor extractor) {
		try {
			LocalDateTime dateTaken = extractor.extractDateTaken(f);

			if (dateTaken == null) {
				if (tryFilenameDateTimeOnMetadataFail) {
					dateTaken = parseDateFromFilename(f.getName());
				}
			}

			if (dateTaken == null) {
				dateTaken = Instant.ofEpochMilli(f.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime();
			}

			if (dateTaken == null && forceDateFlag) {
				dateTaken = LocalDateTime.parse(forceDate + " 00:00:00", dateTimeFormatter);
			}

			if (dateTaken == null) {
				throw new RuntimeException("Can't read date taken: " + f.getName());
			}

			String dateTakenStr = dateTaken.format(dateTimeFormatter);

			if (albumDirName == null) {
				albumDirName = (forceDateFlag ? forceDate : dateTakenStr.substring(0, 10)) + ", " + prefix;
			}

			String tempName = dateTakenStr + " " + f.getName();
			log.debug("Date taken: " + f.getName() + " --> " + dateTakenStr);
			temporaryNames.add(tempName);
			fileMap.put(tempName, f);
		} catch (Exception e) {
			log.error("Error processing file " + f.getName(), e);
			throw new RuntimeException("Failed to process file: " + f.getName(), e);
		}
	}

	LocalDateTime parseDateFromFilename(String fileName) {
		try {
			fileName = fileName.substring(0, fileName.lastIndexOf('.'));
			DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
			return LocalDateTime.parse(fileName, sdf);
		} catch (Exception e) {
			log.warn("Could not parse date from filename: " + fileName);
			return null;
		}
	}
}
