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

    public AlbumDetails(String prefix, String sourceDir, boolean forceDateFlag, String forceDate,
                        boolean includeVideos, boolean inlineVideos, boolean keepOrder,
                        boolean tryFilenameDateTimeOnMetadataFail) {
        this.prefix = prefix;
        this.sourceDir = sourceDir;
        this.forceDateFlag = forceDateFlag;
        this.forceDate = forceDate;
        this.includeVideos = includeVideos;
        this.inlineVideos = inlineVideos;
        this.keepOrder = keepOrder;
        this.tryFilenameDateTimeOnMetadataFail = tryFilenameDateTimeOnMetadataFail;
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
}