package com.novaevent.launcher;

public final class LauncherMetadata {
    public static final String PRODUCT_NAME = "zlauncher";
    public static final String DISPLAY_NAME = "zlauncher";
    public static final String VERSION = "2.0.1";
    public static final String USER_AGENT = PRODUCT_NAME + "/" + VERSION;
    public static final String JAR_NAME = "zlauncher.jar";
    public static final String REPOSITORY = "ZICteam/zlaucnher";
    public static final String RELEASES_URL = "https://github.com/" + REPOSITORY + "/releases";
    public static final String LATEST_RELEASE_API_URL = "https://api.github.com/repos/" + REPOSITORY + "/releases/latest";

    private LauncherMetadata() {
    }
}
