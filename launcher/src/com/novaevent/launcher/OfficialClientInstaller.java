package com.novaevent.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class OfficialClientInstaller {
    private static final String FORGE_INSTALLER_URL_TEMPLATE =
            "https://maven.minecraftforge.net/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar";

    private final HttpClient httpClient;
    private final Consumer<String> log;

    public OfficialClientInstaller(HttpClient httpClient, Consumer<String> log) {
        this.httpClient = httpClient;
        this.log = log;
    }

    public void installClient(ClientProfile profile, Path projectDir, Path gameDir, String javaBinary) throws Exception {
        if (!profile.isOfficialManaged()) {
            return;
        }

        if (profile.isVanilla()) {
            log.accept("Preparing official Minecraft " + profile.minecraftVersion);
            MinecraftLaunchService launchService = new MinecraftLaunchService(projectDir, gameDir, profile, httpClient, log);
            launchService.prepareInstallation();
            return;
        }

        if (profile.isForge()) {
            MinecraftLaunchService launchService = new MinecraftLaunchService(projectDir, gameDir, profile, httpClient, log);
            if (!launchService.hasRequiredVersionMetadata()) {
                runForgeInstaller(profile, projectDir, gameDir, javaBinary);
            }
            launchService.prepareInstallation();
            return;
        }

        throw new IOException("Unsupported loader type: " + profile.loaderType);
    }

    private void runForgeInstaller(ClientProfile profile, Path projectDir, Path gameDir, String javaBinary) throws Exception {
        log.accept("Installing official Forge " + profile.loaderVersion + " for Minecraft " + profile.minecraftVersion);
        Path installerJar = ensureForgeInstaller(profile, projectDir);
        Files.createDirectories(gameDir);
        prepareForgeInstallerContext(projectDir, gameDir);

        ProcessBuilder builder = new ProcessBuilder(
                resolveJavaBinary(projectDir, javaBinary, profile),
                "-jar",
                installerJar.toAbsolutePath().toString(),
                "--installClient",
                gameDir.toAbsolutePath().toString()
        );
        builder.directory(projectDir.toFile());
        builder.redirectErrorStream(true);

        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.accept(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Forge installer failed with exit code " + exitCode);
        }
    }

    private void prepareForgeInstallerContext(Path projectDir, Path gameDir) throws IOException {
        Path launcherProfiles = gameDir.resolve("launcher_profiles.json");
        if (Files.exists(launcherProfiles)) {
            return;
        }

        Path legacyLauncherProfiles = projectDir.resolve("client").resolve("launcher_profiles.json");
        if (Files.exists(legacyLauncherProfiles)) {
            Files.copy(legacyLauncherProfiles, launcherProfiles, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        Files.writeString(launcherProfiles, "{\"profiles\":{},\"settings\":{}}", StandardCharsets.UTF_8);
    }

    private Path ensureForgeInstaller(ClientProfile profile, Path projectDir) throws Exception {
        String fileName = "forge-" + profile.minecraftVersion + "-" + profile.loaderVersion + "-installer.jar";
        Path localInstaller = projectDir.resolve("client").resolve(fileName);
        if (Files.exists(localInstaller)) {
            return localInstaller;
        }

        Path target = projectDir.resolve("launcher").resolve("downloads").resolve(fileName);
        if (Files.exists(target)) {
            return target;
        }

        Files.createDirectories(target.getParent());
        String url = FORGE_INSTALLER_URL_TEMPLATE.formatted(
                profile.minecraftVersion,
                profile.loaderVersion,
                profile.minecraftVersion,
                profile.loaderVersion
        );
        log.accept("Downloading official Forge installer");

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", LauncherMetadata.USER_AGENT)
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Could not download Forge installer: HTTP " + response.statusCode());
        }

        try (InputStream input = response.body()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private String resolveJavaBinary(Path projectDir, String configuredJavaPath, ClientProfile profile) {
        LauncherConfig config = new LauncherConfig();
        config.javaPath = configuredJavaPath == null ? "" : configuredJavaPath;
        return JavaRuntimeResolver.resolve(projectDir, config, profile);
    }
}
