package com.novaevent.launcher;

public final class ClientProfile {
    public String id = "java-edition";
    public String title = "Minecraft: Java Edition";
    public String subtitle = "1.20.1-forge-47.4.18";
    public String folderName = "java-edition";
    public String bundleUrl = "";
    public String minecraftVersion = "1.20.1";
    public String loaderType = "forge";
    public String loaderVersion = "47.4.18";
    public String installSource = "bundle";
    public String javaRuntime = "auto";
    public String minecraftArguments = "";
    public String launchWrapperCommand = "";
    public String serverAddress = "";
    public boolean autoConnectEnabled = false;

    public ClientProfile copy() {
        ClientProfile profile = new ClientProfile();
        profile.id = this.id;
        profile.title = this.title;
        profile.subtitle = this.subtitle;
        profile.folderName = this.folderName;
        profile.bundleUrl = this.bundleUrl;
        profile.minecraftVersion = this.minecraftVersion;
        profile.loaderType = this.loaderType;
        profile.loaderVersion = this.loaderVersion;
        profile.installSource = this.installSource;
        profile.javaRuntime = this.javaRuntime;
        profile.minecraftArguments = this.minecraftArguments;
        profile.launchWrapperCommand = this.launchWrapperCommand;
        profile.serverAddress = this.serverAddress;
        profile.autoConnectEnabled = this.autoConnectEnabled;
        return profile;
    }

    public String resolvedVersionId() {
        if ("forge".equalsIgnoreCase(loaderType) && loaderVersion != null && !loaderVersion.isBlank()) {
            return minecraftVersion + "-forge-" + loaderVersion;
        }
        return minecraftVersion;
    }

    public boolean isForge() {
        return "forge".equalsIgnoreCase(loaderType);
    }

    public boolean isVanilla() {
        return loaderType == null || loaderType.isBlank() || "vanilla".equalsIgnoreCase(loaderType);
    }

    public boolean isOfficialManaged() {
        return "official".equalsIgnoreCase(installSource);
    }
}
