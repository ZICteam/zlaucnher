package com.novaevent.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LauncherConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_BUNDLE_URL = "https://drive.google.com/file/d/1ld2fB9FpLxIcp8WQaKuI1JRUEp-R9KsU/view?usp=sharing";

    public String username = "";
    public boolean offlineMode = false;
    public String savedAccessToken = "";
    public String savedClientToken = "";
    public String savedProfileUuid = "";
    public String savedProfileName = "";
    public String javaPath = "";
    public int minMemoryMb = 2048;
    public int maxMemoryMb = 4096;
    public int width = 1280;
    public int height = 720;
    public boolean fullscreen = false;
    public String launcherBackgroundPath = "";
    public String launcherLogMode = "file";
    public String onGameLaunchAction = "nothing";
    public int launcherWindowWidth = 1720;
    public int launcherWindowHeight = 980;
    public int launcherWindowX = Integer.MIN_VALUE;
    public int launcherWindowY = Integer.MIN_VALUE;
    public String selectedProfileId = "java-edition";
    public List<ClientProfile> profiles = new ArrayList<>();

    public static LauncherConfig load(Path path) {
        if (!Files.exists(path)) {
            return new LauncherConfig();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            LauncherConfig config = GSON.fromJson(reader, LauncherConfig.class);
            LauncherConfig resolved = config != null ? config : new LauncherConfig();
            resolved.ensureDefaults();
            return resolved;
        } catch (IOException ex) {
            return new LauncherConfig();
        }
    }

    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(this, writer);
        }
    }

    public void ensureDefaults() {
        if (profiles == null) {
            profiles = new ArrayList<>();
        }
        if (profiles.isEmpty()) {
            profiles.add(defaultProfile());
        }
        for (ClientProfile profile : profiles) {
            if (profile.javaRuntime == null || profile.javaRuntime.isBlank()) {
                profile.javaRuntime = "auto";
            }
            if ("java-edition".equals(profile.id)
                    && "bundle".equalsIgnoreCase(profile.installSource)
                    && (profile.bundleUrl == null || profile.bundleUrl.isBlank())) {
                profile.bundleUrl = DEFAULT_BUNDLE_URL;
                if (profile.minecraftVersion == null || profile.minecraftVersion.isBlank()) {
                    profile.minecraftVersion = "1.20.1";
                }
                if (profile.loaderType == null || profile.loaderType.isBlank()) {
                    profile.loaderType = "forge";
                }
                if (profile.loaderVersion == null || profile.loaderVersion.isBlank()) {
                    profile.loaderVersion = "47.4.18";
                }
                if (profile.subtitle == null || profile.subtitle.isBlank()) {
                    profile.subtitle = "1.20.1-forge-47.4.18";
                }
            }
        }
        if (selectedProfileId == null || selectedProfileId.isBlank()) {
            selectedProfileId = profiles.get(0).id;
        }
        if (launcherBackgroundPath == null) {
            launcherBackgroundPath = "";
        }
        if (launcherLogMode == null || launcherLogMode.isBlank()) {
            launcherLogMode = "file";
        }
        if (onGameLaunchAction == null || onGameLaunchAction.isBlank()) {
            onGameLaunchAction = "nothing";
        }
    }

    public ClientProfile getSelectedProfile() {
        ensureDefaults();
        for (ClientProfile profile : profiles) {
            if (profile.id.equals(selectedProfileId)) {
                return profile;
            }
        }
        ClientProfile fallback = profiles.get(0);
        selectedProfileId = fallback.id;
        return fallback;
    }

    public static ClientProfile defaultProfile() {
        ClientProfile profile = new ClientProfile();
        profile.id = "java-edition";
        profile.title = "Minecraft: Java Edition";
        profile.subtitle = "1.20.1-forge-47.4.18";
        profile.folderName = "java-edition";
        profile.bundleUrl = DEFAULT_BUNDLE_URL;
        profile.minecraftVersion = "1.20.1";
        profile.loaderType = "forge";
        profile.loaderVersion = "47.4.18";
        profile.installSource = "bundle";
        profile.javaRuntime = "auto";
        return profile;
    }
}
