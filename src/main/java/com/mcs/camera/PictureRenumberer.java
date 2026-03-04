package com.mcs.camera;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

public class PictureRenumberer {

	static final Logger log = LoggerFactory.getLogger(PictureRenumberer.class.getName());

	private static final int counterStart = 1;
	private static final boolean forceDate = false;
	private static final String FORCE_DATE = "2017-10-07";
	private static final boolean INCLUDE_VIDEOS = true;
	private static final boolean INLINE_VIDEOS = true;
	private static final boolean TRY_FILENAME_DATETIME_ON_METADATA_FAIL = true;
	private static final boolean ALLOW_ALTERNATE_PARSING = false;

	/**
	 * Used to match timestamp in filename
	 */
	private static final String DATETIME_FILENAME_REGEX = "(?m)(19|20)[0-9]{2}[- /.](0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01]) [0-9]{2}\\\\.[0-9]{2}\\\\.[0-9]{2}";

	DateTimeFormatter dateTimeFormatter;
	String albumDirName = null;
	List<String> temporaryNames = new ArrayList<>();
	Map<String, File> fileMap = new HashMap<>();

	public static void main(String[] args) {
		log.info("Starting Picture Renumberer v1.0");
		PictureRenumberer pictureRenumber = new PictureRenumberer();

		String directory = null;
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			directory = pictureRenumber.getHomeDirPath();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		pictureRenumber.renumberPictures(directory);
	}

	public PictureRenumberer() {
		this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	}

	public PictureRenumberer(DateTimeFormatter dateTimeFormatter) {
		this.dateTimeFormatter = dateTimeFormatter;
	}

	private String getHomeDirPath() throws IllegalArgumentException {
		String directory = "";
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("F:\\My Pictures\\2025"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setApproveButtonText("Select");
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			directory = file.getAbsolutePath();
			if (!(directory.endsWith(File.separator) || directory.endsWith("/"))) {
				directory += File.separator;
			}
		}
		if (directory.isEmpty()) {
			throw new IllegalArgumentException("No directory chosen for renumbering.");
		}
		return directory;
	}

	private void renumberPictures(String homeDirPath) {
		List<File> videos = new ArrayList<>();
		String prefix = homeDirPath.substring(homeDirPath.lastIndexOf(',') + 1, homeDirPath.lastIndexOf(File.separator)).trim();
		if (homeDirPath.startsWith(prefix)) {
			// there was no comma in the path
			prefix = prefix.substring(prefix.lastIndexOf(File.separator)+1);
		}

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
				case "jpg" -> grabJpgMetadata(f, prefix);
				case "png" -> grabPngMetadata(f, prefix);
				case "mov", "avi", "mkv" -> {
					if (INLINE_VIDEOS) {
						grabMovieMetadata(f, prefix);
					} else {
						videos.add(f);
					}
				}
				default -> log.warn("Ignoring non-JPEG file: " + f.getName());
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
				newFileName = homeDirPath + prefix + " " + String.format("%03d", currentPictureCounter)
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

			LocalDateTime dateTaken = null;
			// there is no metadata available
			if (directory != null) {
				Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getDefault());
				if (date != null) {
					dateTaken = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				}
			} else {
				if (TRY_FILENAME_DATETIME_ON_METADATA_FAIL) {
					String fileName = f.getName();
					fileName = fileName.substring(0, fileName.lastIndexOf('.'));
					try {
						boolean foundMatch = fileName.matches(DATETIME_FILENAME_REGEX);
						if (foundMatch) {
							DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
							dateTaken = LocalDateTime.parse(f.getName(), sdf);
						}
					} catch (Exception ex) {
						log.warn("Could not parse timestamp from filename.", ex);
					}

				}
				if (dateTaken == null && ALLOW_ALTERNATE_PARSING) {
					String altDate = f.getName().replaceAll("-", "").substring(0, 11);
					dateTaken = Instant.ofEpochMilli(Long.parseLong(altDate)).atZone(ZoneId.systemDefault()).toLocalDateTime();
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
					Date date = backupDirectory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE);
					if (date != null) {
						dateTaken = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
					}
				}

			}

			String dateTakenStr = null;

			if (dateTaken != null) {
				dateTakenStr = dateTaken.format(dateTimeFormatter);
			}
			if (dateTakenStr == null && !forceDate) {
				try {
					DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
					LocalDateTime filenameDate = LocalDateTime.parse(f.getName(), sdf);
					dateTakenStr = filenameDate.format(dateTimeFormatter);
				} catch (Exception e) {
					dateTakenStr = null;
				}
			}
			if (dateTakenStr == null && !forceDate) {
				throw new RuntimeException("Can't read date taken: " + f.getName());
			}
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

			LocalDateTime dateTaken = null;
			// there is no metadata available
			if (directory != null) {
				Date date = directory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE, TimeZone.getDefault());
				if (date != null) {
					dateTaken = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				}
			} else {
				if (TRY_FILENAME_DATETIME_ON_METADATA_FAIL) {
					String fileName = f.getName();
					fileName = fileName.substring(0, fileName.lastIndexOf('.'));
					try {
						boolean foundMatch = fileName.matches(DATETIME_FILENAME_REGEX);
						if (foundMatch) {
							DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
							dateTaken = LocalDateTime.parse(f.getName(), sdf);
						}
					} catch (Exception ex) {
						log.warn("Could not parse timestamp from filename.", ex);
					}

				}
				if (dateTaken == null && ALLOW_ALTERNATE_PARSING) {
					String altDate = f.getName().replaceAll("-", "").substring(0, 11);
					dateTaken = Instant.ofEpochMilli(Long.parseLong(altDate)).atZone(ZoneId.systemDefault()).toLocalDateTime();
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
					Date date = backupDirectory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE);
					if (date != null) {
						dateTaken = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
					}
				}

			}

			String dateTakenStr = null;

			if (dateTaken != null) {
				dateTakenStr = dateTaken.format(dateTimeFormatter);
			}
			if (dateTakenStr == null && !forceDate) {
				try {
					DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
					LocalDateTime filenameDate = LocalDateTime.parse(f.getName(), sdf);
					dateTakenStr = filenameDate.format(dateTimeFormatter);
				} catch (Exception e) {
					dateTakenStr = null;
				}
			}
			if (dateTakenStr == null && !forceDate) {
				throw new RuntimeException("Can't read date taken: " + f.getName());
			}
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
			LocalDateTime dateTaken = Instant.ofEpochMilli(f.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime();
			String dateTakenStr = null;

			if (dateTaken != null) {
				dateTakenStr = dateTaken.format(dateTimeFormatter);
			}
			if (dateTakenStr == null && !forceDate) {
				try {
					DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");
					LocalDateTime filenameDate = LocalDateTime.parse(f.getName(), sdf);
					dateTakenStr = filenameDate.format(dateTimeFormatter);
				} catch (Exception e) {
					dateTakenStr = null;
				}
			}
			if (dateTakenStr == null && !forceDate) {
				throw new RuntimeException("Can't read date taken: " + f.getName());
			}
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

	class MyJFileChooser extends JPanel
		implements ActionListener {

		JButton go;

		JFileChooser chooser;
		String choosertitle;

		public MyJFileChooser() {
			go = new JButton("Do it");
			go.addActionListener(this);
			add(go);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int result;

			chooser = new JFileChooser();
			chooser.setCurrentDirectory(new java.io.File("F:\\My Pictures"));
			chooser.setDialogTitle(choosertitle);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			//
			// disable the "All files" option.
			//
			chooser.setAcceptAllFileFilterUsed(false);
			//
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				System.out.println("getCurrentDirectory(): "
					+ chooser.getCurrentDirectory());
				System.out.println("getSelectedFile() : "
					+ chooser.getSelectedFile());
			} else {
				System.out.println("No Selection ");
			}
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(200, 200);
		}
	}
}
