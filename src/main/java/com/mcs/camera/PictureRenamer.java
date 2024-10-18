package com.mcs.camera;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import com.mcs.camera.extractor.JpgMetadataExtractor;
import com.mcs.camera.extractor.MetadataExtractor;
import com.mcs.camera.extractor.PngMetadataExtractor;
import com.mcs.camera.extractor.VideoMetadataExtractor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.drew.metadata.file.FileSystemDirectory;

public class PictureRenamer {
	static final Logger log = LoggerFactory.getLogger(PictureRenamer.class.getName());
	private static final String DEFAULT_DESTINATION_DIR = "F:\\My Pictures";
	private static final boolean forceCounter = false;
	private static final int counterStart = 1;

	private final String sourceDir;
	private final boolean forceDateFlag;
	private final String forceDate;
	private final boolean keepOrder;
	private final boolean moveWhenFinished = true;
	private final boolean includeVideos;
	private final boolean inlineVideos;
	private final boolean tryFilenameDateTimeOnMetadataFail;
	private final boolean allowAlternateParsing = false;
	private final String prefix;

	SimpleDateFormat dateFormat;
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
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	public void renamePictures() {
		List<File> videos = new ArrayList<>();
		String homeDirPath = sourceDir;
		String destDirPath = DEFAULT_DESTINATION_DIR;
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
					case "jpg":
						grabJpgMetadata(f);
						break;
					case "png":
						grabPngMetadata(f);
						break;
					case "mov":
					case "avi":
					case "mkv":
					case "mp4":
						if (includeVideos) {
							if (inlineVideos) {
								grabMovieMetadata(f);
							} else {
								videos.add(f);
							}
						}
						break;
					default:
						log.warn("Ignoring unknown file type: " + f.getName());
						break;
				}
			}
			Collections.sort(temporaryNames);
		}

		int currentPictureCounter = forceCounter ? counterStart : getCurrentCounter();

		for (String orderedPicture : temporaryNames) {
			File origFile = fileMap.get(orderedPicture);
			if (origFile == null || !origFile.exists()) {
				log.error("Prior to renaming -- cannot find file " + orderedPicture);
				return;
			}
		}

		for (String orderedPicture : temporaryNames) {
			File origFile = fileMap.get(orderedPicture);
			String newFileName;
			if (prefix != null && !prefix.isEmpty()) {
				newFileName = homeDirPath + File.separator + prefix + " "
						+ String.format("%03d", currentPictureCounter) + "."
						+ FilenameUtils.getExtension(origFile.getName()).toLowerCase();
			} else {
				newFileName = homeDirPath + File.separator
						+ orderedPicture.replaceAll("\\s", "_").replaceAll(":", "_");
			}
			origFile.renameTo(new File(newFileName));
			currentPictureCounter++;
		}

		if (includeVideos && !inlineVideos) {
			for (File f : videos) {
				String extension = FilenameUtils.getExtension(f.getName()).toLowerCase();
				String newFileName = homeDirPath + File.separator + prefix + " "
						+ String.format("%03d", currentPictureCounter) + "." + extension;
				log.debug("Renaming video file " + f.getName() + " to " + newFileName);
				f.renameTo(new File(newFileName));
				currentPictureCounter++;
			}
		}

		if (moveWhenFinished) {
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
				f.renameTo(new File(dir.getAbsolutePath() + File.separator + f.getName()));
				log.debug("Moving file to: " + dir.getAbsolutePath() + File.separator + f.getName());
			}
		}
	}

	private int getCurrentCounter() {
		return 1;
	}

	Collection<File> getFilesInDir(String directoryPath) {
		log.info("Using home directory: " + directoryPath);
		final File homeDir = new File(directoryPath);
		return FileUtils.listFiles(homeDir, TrueFileFilter.INSTANCE, null);
	}

	void grabJpgMetadata(File f) {
		try {
			MetadataExtractor extractor = new JpgMetadataExtractor();
			Date dateTaken = extractor.extractDateTaken(f);

			if (dateTaken == null) {
				if (tryFilenameDateTimeOnMetadataFail) {
					dateTaken = parseDateFromFilename(f.getName());
				}
			}

			if (dateTaken == null) {
				dateTaken = new Date(f.lastModified());
			}

			if (dateTaken == null && forceDateFlag) {
				dateTaken = dateFormat.parse(forceDate + " 00:00:00");
			}

			if (dateTaken == null) {
				throw new RuntimeException("Can't read date taken: " + f.getName());
			}

			String dateTakenStr = dateFormat.format(dateTaken);

			if (albumDirName == null) {
				albumDirName = (forceDateFlag ? forceDate : dateTakenStr.substring(0, 10)) + ", " + prefix;
			}

			String tempName = dateTakenStr + " " + f.getName();
			log.debug("Date taken: " + f.getName() + " --> " + dateTakenStr);
			temporaryNames.add(tempName);
			fileMap.put(tempName, f);
		} catch (Exception e) {
			log.error("Error processing image file " + f.getName(), e);
			System.exit(1);
		}
	}

	void grabPngMetadata(File f) {
		try {
			MetadataExtractor extractor = new PngMetadataExtractor();
			Date dateTaken = extractor.extractDateTaken(f);

			if (dateTaken == null) {
				if (tryFilenameDateTimeOnMetadataFail) {
					dateTaken = parseDateFromFilename(f.getName());
				}
			}

			if (dateTaken == null) {
				dateTaken = new Date(f.lastModified());
			}

			if (dateTaken == null && forceDateFlag) {
				dateTaken = dateFormat.parse(forceDate + " 00:00:00");
			}

			if (dateTaken == null) {
				throw new RuntimeException("Can't read date taken: " + f.getName());
			}

			String dateTakenStr = dateFormat.format(dateTaken);

			if (albumDirName == null) {
				albumDirName = (forceDateFlag ? forceDate : dateTakenStr.substring(0, 10)) + ", " + prefix;
			}

			String tempName = dateTakenStr + " " + f.getName();
			log.debug("Date taken: " + f.getName() + " --> " + dateTakenStr);
			temporaryNames.add(tempName);
			fileMap.put(tempName, f);
		} catch (Exception e) {
			log.error("Error processing PNG file " + f.getName(), e);
			System.exit(1);
		}
	}

	void grabMovieMetadata(File f) {
		try {
			MetadataExtractor extractor = new VideoMetadataExtractor();
			Date dateTaken = extractor.extractDateTaken(f);

			if (dateTaken == null && forceDateFlag) {
				dateTaken = dateFormat.parse(forceDate + " 00:00:00");
			}

			if (dateTaken == null) {
				throw new RuntimeException("Can't read date taken: " + f.getName());
			}

			String dateTakenStr = dateFormat.format(dateTaken);

			if (albumDirName == null) {
				albumDirName = (forceDateFlag ? forceDate : dateTakenStr.substring(0, 10)) + ", " + prefix;
			}

			String tempName = dateTakenStr + " " + f.getName();
			log.debug("Date taken: " + f.getName() + " --> " + dateTakenStr);
			temporaryNames.add(tempName);
			fileMap.put(tempName, f);
		} catch (Exception e) {
			log.error("Error processing video file " + f.getName(), e);
			System.exit(1);
		}
	}

	Date parseDateFromFilename(String fileName) {
		try {
			fileName = fileName.substring(0, fileName.lastIndexOf('.'));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
			return sdf.parse(fileName);
		} catch (Exception e) {
			log.warn("Could not parse date from filename: " + fileName);
			return null;
		}
	}
}