package com.mcs.camera;

public class AlbumDetails {
    private final String prefix;
    private final String sourceDir;
    private final boolean forceDateFlag;
    private final String forceDate;
    private final boolean includeVideos;
    private final boolean inlineVideos;
    private final boolean keepOrder;
    private final boolean tryFilenameDateTimeOnMetadataFail;
    private final String destinationDir;
    private final int counterStart;
    private final String numberFormat;
    private final String filenameSeparator;

    public AlbumDetails(String prefix, String sourceDir, boolean forceDateFlag, String forceDate,
                        boolean includeVideos, boolean inlineVideos, boolean keepOrder,
                        boolean tryFilenameDateTimeOnMetadataFail) {
        this(prefix, sourceDir, forceDateFlag, forceDate, includeVideos, inlineVideos, keepOrder,
                tryFilenameDateTimeOnMetadataFail, "", 1, "%03d", " ");
    }

    public AlbumDetails(String prefix, String sourceDir, boolean forceDateFlag, String forceDate,
                        boolean includeVideos, boolean inlineVideos, boolean keepOrder,
                        boolean tryFilenameDateTimeOnMetadataFail,
                        String destinationDir, int counterStart, String numberFormat,
                        String filenameSeparator) {
        this.prefix = prefix;
        this.sourceDir = sourceDir;
        this.forceDateFlag = forceDateFlag;
        this.forceDate = forceDate;
        this.includeVideos = includeVideos;
        this.inlineVideos = inlineVideos;
        this.keepOrder = keepOrder;
        this.tryFilenameDateTimeOnMetadataFail = tryFilenameDateTimeOnMetadataFail;
        this.destinationDir = destinationDir;
        this.counterStart = counterStart;
        this.numberFormat = numberFormat;
        this.filenameSeparator = filenameSeparator;
    }

    // Getters
    public String getPrefix() { return prefix; }
    public String getSourceDir() { return sourceDir; }
    public boolean isForceDateFlag() { return forceDateFlag; }
    public String getForceDate() { return forceDate; }
    public boolean isIncludeVideos() { return includeVideos; }
    public boolean isInlineVideos() { return inlineVideos; }
    public boolean isKeepOrder() { return keepOrder; }
    public boolean isTryFilenameDateTimeOnMetadataFail() { return tryFilenameDateTimeOnMetadataFail; }
    public String getDestinationDir() { return destinationDir; }
    public int getCounterStart() { return counterStart; }
    public String getNumberFormat() { return numberFormat; }
    public String getFilenameSeparator() { return filenameSeparator; }
}