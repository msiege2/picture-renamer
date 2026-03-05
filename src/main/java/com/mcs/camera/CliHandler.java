package com.mcs.camera;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class CliHandler {

    private static final Logger log = LoggerFactory.getLogger(CliHandler.class);

    private static final Map<String, String> SEPARATOR_MAP = Map.of(
            "space", " ",
            "dash", "-",
            "underscore", "_",
            "none", ""
    );

    public static int run(String[] args) {
        if (args.length == 0) {
            printUsage();
            return 1;
        }

        String first = args[0];

        if ("--help".equals(first) || "-h".equals(first)) {
            printUsage();
            return 0;
        }
        if ("--version".equals(first) || "-v".equals(first)) {
            System.out.println(Main.getAppTitle() + " " + Main.getAppVersion());
            return 0;
        }

        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        return switch (first) {
            case "rename" -> handleRename(subArgs);
            case "renumber" -> handleRenumber(subArgs);
            default -> {
                System.err.println("Unknown command: " + first);
                printUsage();
                yield 1;
            }
        };
    }

    private static int handleRename(String[] args) {
        Options options = buildRenameOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            new HelpFormatter().printHelp("PictureRenamer rename", options, true);
            return 1;
        }

        String source = cmd.getOptionValue("source");
        String dest = cmd.getOptionValue("dest");
        String prefix = cmd.getOptionValue("prefix");

        if (source == null || dest == null || prefix == null) {
            System.err.println("Error: --source, --dest, and --prefix are required.");
            new HelpFormatter().printHelp("PictureRenamer rename", options, true);
            return 1;
        }

        if (!new File(source).isDirectory()) {
            System.err.println("Error: Source directory does not exist: " + source);
            return 1;
        }
        if (!new File(dest).isDirectory()) {
            System.err.println("Error: Destination directory does not exist: " + dest);
            return 1;
        }

        boolean forceDateFlag = cmd.hasOption("force-date");
        String forceDate = cmd.getOptionValue("force-date", "");
        if (forceDateFlag && !forceDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            System.err.println("Error: --force-date must be in YYYY-MM-DD format.");
            return 1;
        }

        boolean keepOrder = cmd.hasOption("keep-order");
        boolean includeVideos = cmd.hasOption("include-videos");
        boolean inlineVideos = cmd.hasOption("inline-videos");
        boolean tryFilenameDate = cmd.hasOption("try-filename-date");
        boolean dryRun = cmd.hasOption("dry-run");

        int counterStart = 1;
        if (cmd.hasOption("counter-start")) {
            try {
                counterStart = Integer.parseInt(cmd.getOptionValue("counter-start"));
            } catch (NumberFormatException e) {
                System.err.println("Error: --counter-start must be a number.");
                return 1;
            }
        }

        int numberPadding = 3;
        if (cmd.hasOption("number-padding")) {
            try {
                numberPadding = Integer.parseInt(cmd.getOptionValue("number-padding"));
                if (numberPadding < 2 || numberPadding > 4) {
                    System.err.println("Error: --number-padding must be 2, 3, or 4.");
                    return 1;
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: --number-padding must be a number.");
                return 1;
            }
        }

        String separator = " ";
        if (cmd.hasOption("separator")) {
            String sepKey = cmd.getOptionValue("separator").toLowerCase();
            if (!SEPARATOR_MAP.containsKey(sepKey)) {
                System.err.println("Error: --separator must be one of: space, dash, underscore, none.");
                return 1;
            }
            separator = SEPARATOR_MAP.get(sepKey);
        }

        String numberFormat = "%0" + numberPadding + "d";

        AlbumDetails details = new AlbumDetails(prefix, source, forceDateFlag, forceDate,
                includeVideos, inlineVideos, keepOrder, tryFilenameDate,
                dest, counterStart, numberFormat, separator);

        FileOperationTracker tracker = dryRun ? new DryRunFileOperationTracker() : null;

        try {
            PictureRenamer renamer = new PictureRenamer(details, tracker);
            renamer.renamePictures();
            log.info("Rename completed successfully.");
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            log.error("Rename failed", e);
            return 1;
        }
    }

    private static int handleRenumber(String[] args) {
        Options options = buildRenumberOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            new HelpFormatter().printHelp("PictureRenamer renumber", options, true);
            return 1;
        }

        String dir = cmd.getOptionValue("dir");
        String prefix = cmd.getOptionValue("prefix");

        if (dir == null || prefix == null) {
            System.err.println("Error: --dir and --prefix are required.");
            new HelpFormatter().printHelp("PictureRenamer renumber", options, true);
            return 1;
        }

        if (!new File(dir).isDirectory()) {
            System.err.println("Error: Directory does not exist: " + dir);
            return 1;
        }

        boolean includeVideos = cmd.hasOption("include-videos");
        boolean inlineVideos = cmd.hasOption("inline-videos");
        boolean dryRun = cmd.hasOption("dry-run");

        int numberPadding = 3;
        if (cmd.hasOption("number-padding")) {
            try {
                numberPadding = Integer.parseInt(cmd.getOptionValue("number-padding"));
                if (numberPadding < 2 || numberPadding > 4) {
                    System.err.println("Error: --number-padding must be 2, 3, or 4.");
                    return 1;
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: --number-padding must be a number.");
                return 1;
            }
        }

        String separator = " ";
        if (cmd.hasOption("separator")) {
            String sepKey = cmd.getOptionValue("separator").toLowerCase();
            if (!SEPARATOR_MAP.containsKey(sepKey)) {
                System.err.println("Error: --separator must be one of: space, dash, underscore, none.");
                return 1;
            }
            separator = SEPARATOR_MAP.get(sepKey);
        }

        String numberFormat = "%0" + numberPadding + "d";
        FileOperationTracker tracker = dryRun ? new DryRunFileOperationTracker() : null;

        try {
            PictureRenumberer renumberer = new PictureRenumberer(dir, prefix,
                    includeVideos, inlineVideos, numberFormat, separator, tracker);
            renumberer.renumberPictures();
            log.info("Renumber completed successfully.");
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            log.error("Renumber failed", e);
            return 1;
        }
    }

    private static Options buildRenameOptions() {
        Options options = new Options();
        options.addOption(Option.builder().longOpt("source").hasArg().desc("Source directory").build());
        options.addOption(Option.builder().longOpt("dest").hasArg().desc("Destination base directory").build());
        options.addOption(Option.builder().longOpt("prefix").hasArg().desc("Album name prefix").build());
        options.addOption(Option.builder().longOpt("force-date").hasArg().desc("Force date (YYYY-MM-DD)").build());
        options.addOption(Option.builder().longOpt("keep-order").desc("Keep original file order").build());
        options.addOption(Option.builder().longOpt("include-videos").desc("Include video files").build());
        options.addOption(Option.builder().longOpt("inline-videos").desc("Sort videos inline by date").build());
        options.addOption(Option.builder().longOpt("try-filename-date").desc("Parse date from filename if metadata fails").build());
        options.addOption(Option.builder().longOpt("counter-start").hasArg().desc("Starting counter (default: 1)").build());
        options.addOption(Option.builder().longOpt("number-padding").hasArg().desc("Padding digits: 2, 3, or 4 (default: 3)").build());
        options.addOption(Option.builder().longOpt("separator").hasArg().desc("Separator: space, dash, underscore, none (default: space)").build());
        options.addOption(Option.builder().longOpt("dry-run").desc("Show planned operations without executing").build());
        return options;
    }

    private static Options buildRenumberOptions() {
        Options options = new Options();
        options.addOption(Option.builder().longOpt("dir").hasArg().desc("Album directory to renumber").build());
        options.addOption(Option.builder().longOpt("prefix").hasArg().desc("New filename prefix").build());
        options.addOption(Option.builder().longOpt("include-videos").desc("Include video files").build());
        options.addOption(Option.builder().longOpt("inline-videos").desc("Sort videos inline by date").build());
        options.addOption(Option.builder().longOpt("number-padding").hasArg().desc("Padding digits: 2, 3, or 4 (default: 3)").build());
        options.addOption(Option.builder().longOpt("separator").hasArg().desc("Separator: space, dash, underscore, none (default: space)").build());
        options.addOption(Option.builder().longOpt("dry-run").desc("Show planned operations without executing").build());
        return options;
    }

    private static void printUsage() {
        System.out.println(Main.getAppTitle() + " " + Main.getAppVersion());
        System.out.println();
        System.out.println("Usage: java -jar PictureRenamer.jar <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  rename     Rename photos by metadata date and move to album directory");
        System.out.println("  renumber   Re-sequence files in an existing album directory");
        System.out.println();
        System.out.println("Global options:");
        System.out.println("  --help     Show this help message");
        System.out.println("  --version  Show version");
        System.out.println();
        System.out.println("Run 'java -jar PictureRenamer.jar <command> --help' for command-specific help.");
        System.out.println();
        System.out.println("With no arguments, the GUI is launched.");
    }
}
