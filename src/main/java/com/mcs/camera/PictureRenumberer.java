package com.mcs.camera;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;

public class PictureRenumberer {
	static final Logger log = LoggerFactory.getLogger(PictureRenumberer.class.getName());

	private static final String RENUMBER_LOCATION = "F:\\My Pictures\\2020-02-08, Atlanta Snow 2020";
	private static final int counterStart = 1;
	private static final boolean forceDate = false;
	private static final String FORCE_DATE = "2017-10-07";
	private static final boolean INCLUDE_VIDEOS = true;
	private static final boolean INLINE_VIDEOS = true;
	private static final boolean TRY_FILENAME_DATETIME_ON_METADATA_FAIL = true;
	private static final boolean ALLOW_ALTERNATE_PARSING = false;

	/** Used to match timestamp in filename */
	private static final String DATETIME_FILENAME_REGEX = "(?m)(19|20)[0-9]{2}[- /.](0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01]) [0-9]{2}\\\\.[0-9]{2}\\\\.[0-9]{2}";

	SimpleDateFormat dateFormat;
	String albumDirName = null;
	List<String> temporaryNames = new ArrayList<>();
	Map<String, File> fileMap = new HashMap<>();

	public static void main(String[] args) {
		log.info("Starting Picture Renumberer v1.0");
		PictureRenumberer pictureRenumber = new PictureRenumberer();
		pictureRenumber.renumberPictures();
	}

	public PictureRenumberer() {
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	public PictureRenumberer(SimpleDateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	private void renumberPictures() {
		List<File> videos = new ArrayList<>();

		String homeDirPath = RENUMBER_LOCATION;
		String prefix = RENUMBER_LOCATION.substring(RENUMBER_LOCATION.lastIndexOf(',')+1).trim();

		Collection<File> filesInHomeDir = getFilesInDir(homeDirPath);
		// If there is nothing to do, just exit.
		if (filesInHomeDir.isEmpty()) {
			log.warn("No files found in home directory -- " + homeDirPath);
			return;
		}

		// Step 1, Go each file and rename as a date based on exif information.
		for (File f : filesInHomeDir) {
			String fileExtension = FilenameUtils.getExtension(f.getName()).toLowerCase();
			switch (fileExtension) {
			case "jpg":
				grabJpgMetadata(f, prefix);
				break;
			case "png":
				grabPngMetadata(f, prefix);
				break;
			case "mov":
			case "avi":
			case "mkv":
				if (INLINE_VIDEOS) {
					grabMovieMetadata(f, prefix);
				} else {
					videos.add(f);
				}
				break;
			default:
				log.warn("Ignoring non-JPEG file: " + f.getName());
				break;
			}
		}

		// Sort all the files to be in date order.
		Collections.sort(temporaryNames);

		int currentPictureCounter = counterStart;
		for (String orderedPicture : temporaryNames) {
			File origFile = fileMap.get(orderedPicture);
			if (!origFile.exists()) {
				log.error("Prior to renaming -- cannot find file " + origFile.getName());
				return;
			}
			//origFile.renameTo(new File(homeDirPath + File.separator + RandomStringUtils.randomAlphanumeric(8) + "." + FilenameUtils.getExtension(origFile.getName()).toLowerCase()));
			File renamedFile = new File(homeDirPath + File.separator + RandomStringUtils.randomAlphanumeric(8) + "." + FilenameUtils.getExtension(origFile.getName()).toLowerCase());
			origFile.renameTo(renamedFile);
			fileMap.put(orderedPicture, renamedFile);
		}

		for (String orderedPicture : temporaryNames) {
			File origFile = fileMap.get(orderedPicture);

			String newFileName = null;
			if (prefix != null && !prefix.isEmpty()) {
				newFileName = homeDirPath + File.separator + prefix + " " + String.format("%03d", currentPictureCounter)
						+ "." + FilenameUtils.getExtension(origFile.getName()).toLowerCase();
			} else {
				newFileName = homeDirPath + File.separator + orderedPicture.replaceAll("\\s", "_").replaceAll(":", "_");
			}
			origFile.renameTo(new File(newFileName));
			currentPictureCounter++;
		}

		if (INCLUDE_VIDEOS && !INLINE_VIDEOS) {
			for (File f : videos) {
				String extension = f.getName().substring(f.getName().length() - 3);
				String newFileName = homeDirPath + File.separator + prefix + " "
					+ String.format("%03d", currentPictureCounter) + "." + extension.toLowerCase();
				log.debug("Renaming video file " + f.getName() + " to " + newFileName);
				f.renameTo(new File(newFileName));
				currentPictureCounter++;
			}
		}

	}

	private Collection<File> getFilesInDir(String directoryPath) {
		log.info("Using home directory: " + directoryPath);

		final File homeDir = new File(directoryPath);
		return FileUtils.listFiles(homeDir, TrueFileFilter.INSTANCE, null);
	}

	private void grabJpgMetadata(File f, String prefix) {
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(f);
			Directory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

			Date dateTaken = null;
			// there is no metadata available
			if (directory != null) {
				dateTaken = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
			} else {
				if (TRY_FILENAME_DATETIME_ON_METADATA_FAIL) {
					String fileName = f.getName();
					fileName = fileName.substring(0,fileName.lastIndexOf('.'));
					try {
						boolean foundMatch = fileName.matches(DATETIME_FILENAME_REGEX);
						if (foundMatch) {
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
							dateTaken = sdf.parse(f.getName());
						}
					} catch (Exception ex) {
						log.warn("Could not parse timestamp from filename.", ex);
					}

				}
				if (dateTaken == null && ALLOW_ALTERNATE_PARSING) {
					String altDate = f.getName().replaceAll("-", "").substring(0, 11);
					dateTaken = new Date(Long.parseLong(altDate));
				}
				if (dateTaken == null) {
					log.error("Error in image processing at file " + f.getName() + ".  Cannot continue.");
					System.exit(1);
				}
			}

			if (dateTaken == null) {
				// fall back on file modification date
				Directory backupDirectory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);
				if (backupDirectory != null) {
					dateTaken = backupDirectory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE);
				}

			}

			String dateTakenStr = null;

			if (dateTaken != null)
				dateTakenStr = dateFormat.format(dateTaken);
			if (dateTakenStr == null && !forceDate) {
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
					Date filenameDate = sdf.parse(f.getName());
					dateTakenStr = dateFormat.format(filenameDate);
				} catch (Exception e) {
					dateTakenStr = null;
				}
			}
			if (dateTakenStr == null && !forceDate)
				throw new RuntimeException("Can't read date taken: " + f.getName());
			if (albumDirName == null) {
				if (forceDate) {
					albumDirName = FORCE_DATE + ", " + prefix;
					dateTakenStr = FORCE_DATE;
				} else {
					albumDirName = dateTakenStr.substring(0, dateTakenStr.indexOf(" ")) + ", " + prefix;
				}
			}
			String tempName = dateTakenStr + " " + f.getName();
			log.debug("Date taken: " + f.getName() + "-->" + dateTakenStr);
			temporaryNames.add(tempName);
			fileMap.put(tempName, f);
		} catch (Exception e) {
			log.error("Error in image processing at file " + f.getName() + ".  Cannot continue.", e);
			System.exit(1);
		}
	}

	private void grabPngMetadata(File f, String prefix) {
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(f);
			Directory directory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);

			Date dateTaken = null;
			// there is no metadata available
			if (directory != null) {
				dateTaken = directory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE, TimeZone.getDefault());
			} else {
				if (TRY_FILENAME_DATETIME_ON_METADATA_FAIL) {
					String fileName = f.getName();
					fileName = fileName.substring(0,fileName.lastIndexOf('.'));
					try {
						boolean foundMatch = fileName.matches(DATETIME_FILENAME_REGEX);
						if (foundMatch) {
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
							dateTaken = sdf.parse(f.getName());
						}
					} catch (Exception ex) {
						log.warn("Could not parse timestamp from filename.", ex);
					}

				}
				if (dateTaken == null && ALLOW_ALTERNATE_PARSING) {
					String altDate = f.getName().replaceAll("-", "").substring(0, 11);
					dateTaken = new Date(Long.parseLong(altDate));
				}
				if (dateTaken == null) {
					log.error("Error in image processing at file " + f.getName() + ".  Cannot continue.");
					System.exit(1);
				}
			}

			if (dateTaken == null) {
				// fall back on file modification date
				Directory backupDirectory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);
				if (backupDirectory != null) {
					dateTaken = backupDirectory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE);
				}

			}

			String dateTakenStr = null;

			if (dateTaken != null)
				dateTakenStr = dateFormat.format(dateTaken);
			if (dateTakenStr == null && !forceDate) {
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
					Date filenameDate = sdf.parse(f.getName());
					dateTakenStr = dateFormat.format(filenameDate);
				} catch (Exception e) {
					dateTakenStr = null;
				}
			}
			if (dateTakenStr == null && !forceDate)
				throw new RuntimeException("Can't read date taken: " + f.getName());
			if (albumDirName == null) {
				if (forceDate) {
					albumDirName = FORCE_DATE + ", " + prefix;
					dateTakenStr = FORCE_DATE;
				} else {
					albumDirName = dateTakenStr.substring(0, dateTakenStr.indexOf(" ")) + ", " + prefix;
				}
			}
			String tempName = dateTakenStr + " " + f.getName();
			log.debug("Date taken: " + f.getName() + "-->" + dateTakenStr);
			temporaryNames.add(tempName);
			fileMap.put(tempName, f);
		} catch (Exception e) {
			log.error("Error in image processing at file " + f.getName() + ".  Cannot continue.", e);
			System.exit(1);
		}
	}

	private void grabMovieMetadata(File f, String prefix) {
		try {
			// Path p = Paths.get(f.getAbsolutePath());
			// BasicFileAttributes view = Files.getFileAttributeView(p,
			// BasicFileAttributeView.class).readAttributes();
			// FileTime fileTime = view.creationTime();

			Date dateTaken = new Date(f.lastModified());
			String dateTakenStr = null;

			if (dateTaken != null)
				dateTakenStr = dateFormat.format(dateTaken);
			if (dateTakenStr == null && !forceDate) {
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
					Date filenameDate = sdf.parse(f.getName());
					dateTakenStr = dateFormat.format(filenameDate);
				} catch (Exception e) {
					dateTakenStr = null;
				}
			}
			if (dateTakenStr == null && !forceDate)
				throw new RuntimeException("Can't read date taken: " + f.getName());
			if (albumDirName == null) {
				if (forceDate) {
					albumDirName = FORCE_DATE + ", " + prefix;
					dateTakenStr = FORCE_DATE;
				} else {
					albumDirName = dateTakenStr.substring(0, dateTakenStr.indexOf(" ")) + ", " + prefix;
				}
			}
			String tempName = dateTakenStr + " " + f.getName();
			log.debug("Date taken: " + f.getName() + "-->" + dateTakenStr);
			temporaryNames.add(tempName);
			fileMap.put(tempName, f);
		} catch (Exception e) {
			log.error("Error in image processing at file " + f.getName() + ".  Cannot continue.", e);
			System.exit(1);
		}
	}

}
