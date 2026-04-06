package com.novaevent.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class MinecraftLaunchService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final URI VERSION_MANIFEST_URI = URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
    private static final String AUTHLIB_INJECTOR_URL = "https://repo1.maven.org/maven2/org/glavo/hmcl/authlib-injector/1.2.7/authlib-injector-1.2.7.jar";
    private static final String AUTHLIB_INJECTOR_NAME = "authlib-injector-1.2.7.jar";
    private static final String ELY_AUTHLIB_SYSTEM_ID_120 = "1.20-authlib";
    private static final String ELY_AUTHLIB_DOWNLOAD_URL = "https://ely.by/load/system?minecraftVersion=%s";

    private final Path projectDir;
    private final Path gameDir;
    private final ClientProfile profile;
    private final HttpClient httpClient;
    private final Consumer<String> log;

    public MinecraftLaunchService(Path projectDir, Path gameDir, ClientProfile profile, HttpClient httpClient, Consumer<String> log) {
        this.projectDir = projectDir;
        this.gameDir = gameDir;
        this.profile = profile;
        this.httpClient = httpClient;
        this.log = log;
    }

    public Process launch(ElySession session, LauncherConfig config) throws Exception {
        PreparedLaunch prepared = prepareLaunch();
        ElyRuntimeSupport runtimeSupport = ensureElyRuntimeSupport();
        if (runtimeSupport.mode() == ElySupportMode.OFFICIAL_AUTHLIB_PATCH) {
            log.accept("Using official Ely.by patched authlib: " + runtimeSupport.path().getFileName());
        } else {
            log.accept("Using authlib-injector fallback: " + runtimeSupport.path().getFileName());
        }
        String javaBinary = resolveJavaBinary(config);
        log.accept("Using Java runtime: " + javaBinary + " (mode: " + safeTrim(profile.javaRuntime, "auto") + ")");
        List<String> command = buildCommand(prepared.mergedVersion(), session, config, prepared.nativesDir(), runtimeSupport);

        log.accept("Launching " + profile.resolvedVersionId());
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(gameDir.toFile());
        processBuilder.redirectErrorStream(true);
        return processBuilder.start();
    }

    public void prepareInstallation() throws Exception {
        prepareLaunch();
    }

    public Path getGameDir() {
        return gameDir;
    }

    public boolean hasRequiredVersionMetadata() {
        if (profile.isForge()) {
            return Files.exists(versionJsonPath(profile.resolvedVersionId()));
        }
        return true;
    }

    private PreparedLaunch prepareLaunch() throws Exception {
        Files.createDirectories(gameDir);
        ensureVanillaVersionJson(profile.minecraftVersion);
        JsonObject vanilla = readJson(versionJsonPath(profile.minecraftVersion));

        JsonObject merged = vanilla;
        if (profile.isForge()) {
            Path loaderJsonPath = versionJsonPath(profile.resolvedVersionId());
            if (!Files.exists(loaderJsonPath)) {
                throw new IOException("Missing Forge metadata for " + profile.resolvedVersionId() + ". Install the client first.");
            }
            JsonObject loaderJson = readJson(loaderJsonPath);
            merged = mergeVersions(vanilla, loaderJson);
        }

        ensureClientJar(profile.minecraftVersion, vanilla);
        ensureLibraries(merged);
        ensureAssets(vanilla);
        Path nativesDir = extractNatives(merged, profile.resolvedVersionId());
        return new PreparedLaunch(vanilla, merged, nativesDir);
    }

    private void ensureVanillaVersionJson(String versionId) throws Exception {
        Path path = versionJsonPath(versionId);
        if (Files.exists(path)) {
            return;
        }

        log.accept("Downloading official metadata for Minecraft " + versionId);
        JsonObject manifest = fetchJson(VERSION_MANIFEST_URI);
        JsonArray versions = manifest.getAsJsonArray("versions");
        String versionUrl = null;
        for (JsonElement versionElement : versions) {
            JsonObject version = versionElement.getAsJsonObject();
            if (versionId.equals(version.get("id").getAsString())) {
                versionUrl = version.get("url").getAsString();
                break;
            }
        }
        if (versionUrl == null) {
            throw new IOException("Could not find official metadata for " + versionId);
        }

        JsonObject versionJson = fetchJson(URI.create(versionUrl));
        writeJson(path, versionJson);
    }

    private void ensureClientJar(String versionId, JsonObject vanilla) throws Exception {
        Path clientJar = versionJarPath(versionId);
        if (Files.exists(clientJar)) {
            return;
        }

        JsonObject client = vanilla.getAsJsonObject("downloads").getAsJsonObject("client");
        downloadFile(client.get("url").getAsString(), clientJar);
    }

    private void ensureLibraries(JsonObject merged) throws Exception {
        Set<String> downloaded = new LinkedHashSet<>();
        for (JsonElement element : merged.getAsJsonArray("libraries")) {
            JsonObject library = element.getAsJsonObject();
            if (!isAllowedByRules(library)) {
                continue;
            }

            JsonObject downloads = library.has("downloads") ? library.getAsJsonObject("downloads") : null;
            if (downloads == null) {
                continue;
            }

            JsonObject artifact = downloads.has("artifact") ? downloads.getAsJsonObject("artifact") : null;
            if (artifact != null) {
                Path artifactPath = gameDir.resolve("libraries").resolve(artifact.get("path").getAsString());
                if (downloaded.add(artifactPath.toString()) && !Files.exists(artifactPath)) {
                    downloadFile(artifact.get("url").getAsString(), artifactPath);
                }
            }

            JsonObject classifier = resolveNativeClassifier(library, downloads);
            if (classifier != null) {
                Path nativePath = gameDir.resolve("libraries").resolve(classifier.get("path").getAsString());
                if (downloaded.add(nativePath.toString()) && !Files.exists(nativePath)) {
                    downloadFile(classifier.get("url").getAsString(), nativePath);
                }
            }
        }
    }

    private void ensureAssets(JsonObject vanilla) throws Exception {
        JsonObject assetIndex = vanilla.getAsJsonObject("assetIndex");
        String assetId = assetIndex.get("id").getAsString();
        Path assetIndexPath = gameDir.resolve("assets").resolve("indexes").resolve(assetId + ".json");
        if (!Files.exists(assetIndexPath)) {
            log.accept("Downloading asset index " + assetId);
            downloadFile(assetIndex.get("url").getAsString(), assetIndexPath);
        }

        JsonObject assetIndexJson = readJson(assetIndexPath);
        JsonObject objects = assetIndexJson.getAsJsonObject("objects");
        int total = objects.size();
        int completed = 0;
        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            completed++;
            JsonObject object = entry.getValue().getAsJsonObject();
            String hash = object.get("hash").getAsString();
            Path objectPath = gameDir.resolve("assets").resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
            if (!Files.exists(objectPath)) {
                String url = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
                downloadFile(url, objectPath);
            }
            if (completed % 500 == 0 || completed == total) {
                log.accept("Assets ready: " + completed + "/" + total);
            }
        }
    }

    private Path extractNatives(JsonObject merged, String versionId) throws Exception {
        Path nativesDir = gameDir.resolve("natives").resolve(versionId);
        if (Files.exists(nativesDir)) {
            try (var stream = Files.walk(nativesDir)) {
                stream.sorted(Comparator.reverseOrder()).filter(path -> !path.equals(nativesDir)).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
        Files.createDirectories(nativesDir);

        for (JsonElement element : merged.getAsJsonArray("libraries")) {
            JsonObject library = element.getAsJsonObject();
            if (!isAllowedByRules(library)) {
                continue;
            }

            JsonObject downloads = library.has("downloads") ? library.getAsJsonObject("downloads") : null;
            JsonObject classifier = resolveNativeClassifier(library, downloads);
            if (classifier == null) {
                continue;
            }

            Path nativeJar = gameDir.resolve("libraries").resolve(classifier.get("path").getAsString());
            try (InputStream input = Files.newInputStream(nativeJar);
                 ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String name = entry.getName();
                    if (name.startsWith("META-INF/")) {
                        continue;
                    }
                    Path out = nativesDir.resolve(name);
                    Files.createDirectories(out.getParent());
                    Files.copy(zip, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        return nativesDir;
    }

    private ElyRuntimeSupport ensureElyRuntimeSupport() throws Exception {
        Path patchedAuthlib = ensureOfficialElyAuthlibIfSupported();
        if (patchedAuthlib != null) {
            return new ElyRuntimeSupport(ElySupportMode.OFFICIAL_AUTHLIB_PATCH, patchedAuthlib);
        }

        Path authlibDir = projectDir.resolve("launcher").resolve("cache").resolve("ely").resolve("injector");
        Path authlibPath = authlibDir.resolve(AUTHLIB_INJECTOR_NAME);
        if (Files.exists(authlibPath)) {
            return new ElyRuntimeSupport(ElySupportMode.AUTHLIB_INJECTOR, authlibPath);
        }

        log.accept("Downloading authlib-injector for Ely.by skin support");
        downloadFile(AUTHLIB_INJECTOR_URL, authlibPath);
        return new ElyRuntimeSupport(ElySupportMode.AUTHLIB_INJECTOR, authlibPath);
    }

    private Path ensureOfficialElyAuthlibIfSupported() throws Exception {
        String systemId = resolveElyAuthlibSystemId();
        if (systemId == null) {
            return null;
        }

        Path cacheDir = projectDir.resolve("launcher").resolve("cache").resolve("ely").resolve("system").resolve(systemId);
        Path archivePath = cacheDir.resolve(systemId + ".zip");
        if (!Files.exists(archivePath)) {
            log.accept("Downloading official Ely.by Authlib patch for " + profile.minecraftVersion);
            downloadFile(ELY_AUTHLIB_DOWNLOAD_URL.formatted(systemId), archivePath);
        }

        Path extractedJar = extractSingleJarFromZip(archivePath, cacheDir.resolve("files"));
        if (extractedJar == null || !Files.exists(extractedJar)) {
            throw new IOException("Official Ely.by Authlib archive does not contain a jar.");
        }

        Path targetAuthlib = gameDir.resolve("libraries").resolve("com").resolve("mojang").resolve("authlib")
                .resolve("4.0.43").resolve("authlib-4.0.43.jar");
        if (!Files.exists(targetAuthlib)) {
            return null;
        }

        Path backupAuthlib = targetAuthlib.resolveSibling("authlib-4.0.43.mojang.jar");
        if (!Files.exists(backupAuthlib)) {
            Files.createDirectories(backupAuthlib.getParent());
            Files.copy(targetAuthlib, backupAuthlib, StandardCopyOption.REPLACE_EXISTING);
        }

        if (Files.mismatch(extractedJar, targetAuthlib) != -1) {
            log.accept("Installing official Ely.by Authlib patch into the selected client");
            Files.copy(extractedJar, targetAuthlib, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetAuthlib;
    }

    private String resolveElyAuthlibSystemId() {
        return switch (profile.minecraftVersion) {
            case "1.20", "1.20.1" -> ELY_AUTHLIB_SYSTEM_ID_120;
            default -> null;
        };
    }

    private Path extractSingleJarFromZip(Path archivePath, Path targetDir) throws Exception {
        Files.createDirectories(targetDir);
        Path foundJar = null;
        try (InputStream input = Files.newInputStream(archivePath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                Path out = targetDir.resolve(Path.of(entry.getName()).getFileName().toString());
                Files.createDirectories(out.getParent());
                Files.copy(zip, out, StandardCopyOption.REPLACE_EXISTING);
                if (out.getFileName().toString().endsWith(".jar")) {
                    foundJar = out;
                }
            }
        }
        return foundJar;
    }

    private List<String> buildCommand(
            JsonObject merged,
            ElySession session,
            LauncherConfig config,
            Path nativesDir,
            ElyRuntimeSupport runtimeSupport
    ) {
        Map<String, String> vars = new HashMap<>();
        Map<String, Boolean> features = new HashMap<>();
        vars.put("auth_player_name", session.username());
        vars.put("version_name", profile.resolvedVersionId());
        vars.put("game_directory", gameDir.toAbsolutePath().toString());
        vars.put("assets_root", gameDir.resolve("assets").toAbsolutePath().toString());
        vars.put("assets_index_name", merged.get("assets").getAsString());
        vars.put("auth_uuid", session.uuid());
        vars.put("auth_access_token", session.accessToken());
        vars.put("auth_session", session.accessToken());
        vars.put("clientid", UUID.randomUUID().toString());
        vars.put("auth_xuid", "0");
        vars.put("user_type", session.offline() ? "legacy" : "mojang");
        vars.put("version_type", merged.has("type") ? merged.get("type").getAsString() : "release");
        vars.put("natives_directory", nativesDir.toAbsolutePath().toString());
        vars.put("launcher_name", LauncherMetadata.PRODUCT_NAME);
        vars.put("launcher_version", LauncherMetadata.VERSION);
        vars.put("classpath_separator", System.getProperty("path.separator"));
        vars.put("library_directory", gameDir.resolve("libraries").toAbsolutePath().toString());
        vars.put("classpath", buildClasspath(merged));
        vars.put("user_properties", "{}");
        vars.put("resolution_width", Integer.toString(config.width));
        vars.put("resolution_height", Integer.toString(config.height));
        vars.put("quickPlayPath", gameDir.resolve("quickPlay").resolve("log.json").toAbsolutePath().toString());
        vars.put("quickPlaySingleplayer", "");
        vars.put("quickPlayMultiplayer", "");
        vars.put("quickPlayRealms", "");
        features.put("has_custom_resolution", true);

        applyAutoConnectQuickPlay(vars, features);

        List<String> command = new ArrayList<>();
        command.add(resolveJavaBinary(config));
        if ("osx".equals(mapOsName())) {
            command.add("-XstartOnFirstThread");
        }
        if (runtimeSupport.mode() == ElySupportMode.AUTHLIB_INJECTOR) {
            command.add("-javaagent:" + runtimeSupport.path().toAbsolutePath() + "=ely.by");
        }
        if (profile.isForge()) {
            command.add("-DlegacyClassPath=" + buildLibrariesClasspath(merged));
        }

        for (JsonElement element : merged.getAsJsonObject("arguments").getAsJsonArray("jvm")) {
            appendArgument(command, element, vars, features);
        }
        removeConflictingMemoryArgs(command);
        command.add("-Xms" + config.minMemoryMb + "M");
        command.add("-Xmx" + config.maxMemoryMb + "M");
        command.add(merged.get("mainClass").getAsString());
        for (JsonElement element : merged.getAsJsonObject("arguments").getAsJsonArray("game")) {
            appendArgument(command, element, vars, features);
        }
        command.addAll(parseCommandLine(safeTrim(profile.minecraftArguments)));
        if (config.fullscreen) {
            command.add("--fullscreen");
        }
        return applyLaunchWrapper(command);
    }

    private void applyAutoConnectQuickPlay(Map<String, String> vars, Map<String, Boolean> features) {
        features.put("has_quick_plays_support", false);
        features.put("is_quick_play_singleplayer", false);
        features.put("is_quick_play_multiplayer", false);
        features.put("is_quick_play_realms", false);
        if (!profile.autoConnectEnabled) {
            return;
        }
        MinecraftServerStatusFetcher.ServerAddress serverAddress = MinecraftServerStatusFetcher.parseAddress(profile.serverAddress);
        if (serverAddress == null) {
            return;
        }
        features.put("has_quick_plays_support", true);
        features.put("is_quick_play_multiplayer", true);
        vars.put("quickPlayMultiplayer", serverAddress.host() + ":" + serverAddress.port());
        log.accept("Auto-connect via quickPlayMultiplayer: " + vars.get("quickPlayMultiplayer"));
    }

    private String buildClasspath(JsonObject merged) {
        List<String> parts = buildLibraryPathParts(merged);
        parts.add(versionJarPath(profile.minecraftVersion).toAbsolutePath().toString());
        return String.join(System.getProperty("path.separator"), parts);
    }

    private String buildLibrariesClasspath(JsonObject merged) {
        return String.join(System.getProperty("path.separator"), buildLibraryPathParts(merged));
    }

    private List<String> buildLibraryPathParts(JsonObject merged) {
        List<String> parts = new ArrayList<>();
        for (JsonElement element : merged.getAsJsonArray("libraries")) {
            JsonObject library = element.getAsJsonObject();
            if (!isAllowedByRules(library)) {
                continue;
            }
            JsonObject downloads = library.has("downloads") ? library.getAsJsonObject("downloads") : null;
            if (downloads == null || !downloads.has("artifact")) {
                continue;
            }
            Path artifactPath = gameDir.resolve("libraries").resolve(downloads.getAsJsonObject("artifact").get("path").getAsString());
            if (Files.exists(artifactPath)) {
                parts.add(artifactPath.toAbsolutePath().toString());
            }
        }
        return parts;
    }

    private void appendArgument(List<String> target, JsonElement element, Map<String, String> vars, Map<String, Boolean> features) {
        if (element.isJsonPrimitive()) {
            target.add(replaceVars(element.getAsString(), vars));
            return;
        }

        JsonObject object = element.getAsJsonObject();
        if (!isAllowedByRules(object, features)) {
            return;
        }

        JsonElement value = object.get("value");
        if (value == null) {
            return;
        }

        if (value.isJsonArray()) {
            for (JsonElement item : value.getAsJsonArray()) {
                target.add(replaceVars(item.getAsString(), vars));
            }
        } else {
            target.add(replaceVars(value.getAsString(), vars));
        }
    }

    private boolean isAllowedByRules(JsonObject object, Map<String, Boolean> featuresState) {
        if (!object.has("rules")) {
            return true;
        }

        boolean allowed = false;
        for (JsonElement ruleElement : object.getAsJsonArray("rules")) {
            JsonObject rule = ruleElement.getAsJsonObject();
            boolean matches = true;

            if (rule.has("os")) {
                JsonObject os = rule.getAsJsonObject("os");
                if (os.has("name")) {
                    matches &= mapOsName().equals(os.get("name").getAsString());
                }
                if (os.has("arch")) {
                    matches &= normalizeArch(System.getProperty("os.arch")).equals(normalizeArch(os.get("arch").getAsString()));
                }
                if (os.has("version")) {
                    matches &= Pattern.compile(os.get("version").getAsString()).matcher(System.getProperty("os.version")).find();
                }
            }

            if (rule.has("features")) {
                JsonObject features = rule.getAsJsonObject("features");
                for (Map.Entry<String, JsonElement> feature : features.entrySet()) {
                    boolean actual = featuresState.getOrDefault(feature.getKey(), false);
                    matches &= feature.getValue().getAsBoolean() == actual;
                }
            }

            if (!matches) {
                continue;
            }
            allowed = "allow".equals(rule.get("action").getAsString());
        }
        return allowed;
    }

    private boolean isAllowedByRules(JsonObject object) {
        return isAllowedByRules(object, Map.of("has_custom_resolution", true));
    }

    private JsonObject resolveNativeClassifier(JsonObject library, JsonObject downloads) {
        if (downloads == null || !library.has("natives") || !downloads.has("classifiers")) {
            return null;
        }

        String classifierKey = library.getAsJsonObject("natives").get(mapOsName()).getAsString();
        classifierKey = classifierKey.replace("${arch}", normalizeArch(System.getProperty("os.arch")));
        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
        return classifiers.has(classifierKey) ? classifiers.getAsJsonObject(classifierKey) : null;
    }

    private String replaceVars(String value, Map<String, String> vars) {
        String result = value;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", Objects.toString(entry.getValue(), ""));
        }
        return result;
    }

    private String resolveJavaBinary(LauncherConfig config) {
        return JavaRuntimeResolver.resolve(projectDir, config, profile);
    }

    private String safeTrim(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private void removeConflictingMemoryArgs(List<String> command) {
        command.removeIf(argument -> argument.startsWith("-Xms") || argument.startsWith("-Xmx"));
    }

    private List<String> applyLaunchWrapper(List<String> baseCommand) {
        List<String> wrapper = parseCommandLine(safeTrim(profile.launchWrapperCommand));
        if (wrapper.isEmpty()) {
            return baseCommand;
        }

        List<String> result = new ArrayList<>();
        boolean inserted = false;
        for (String token : wrapper) {
            if ("%command%".equals(token)) {
                result.addAll(baseCommand);
                inserted = true;
            } else {
                result.add(token);
            }
        }
        if (!inserted) {
            result.addAll(baseCommand);
        }
        return result;
    }

    private List<String> parseCommandLine(String raw) {
        List<String> tokens = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return tokens;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if ((ch == '"' || ch == '\'') && (!inQuotes || ch == quoteChar)) {
                if (inQuotes && ch == quoteChar) {
                    inQuotes = false;
                    quoteChar = 0;
                } else if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = ch;
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (Character.isWhitespace(ch) && !inQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private JsonObject mergeVersions(JsonObject parent, JsonObject child) {
        JsonObject merged = deepCopy(parent);
        boolean childHasArguments = child.has("arguments") && child.get("arguments").isJsonObject();
        for (Map.Entry<String, JsonElement> entry : child.entrySet()) {
            String key = entry.getKey();
            if ("libraries".equals(key) || "arguments".equals(key) || "_comment_".equals(key)) {
                continue;
            }
            if ("logging".equals(key) && entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().size() == 0) {
                continue;
            }
            merged.add(key, deepCopy(entry.getValue()));
        }

        JsonArray libraries = new JsonArray();
        parent.getAsJsonArray("libraries").forEach(element -> libraries.add(deepCopy(element)));
        child.getAsJsonArray("libraries").forEach(element -> libraries.add(deepCopy(element)));
        merged.add("libraries", libraries);

        JsonObject arguments = new JsonObject();
        JsonArray gameArgs = new JsonArray();
        parent.getAsJsonObject("arguments").getAsJsonArray("game").forEach(element -> gameArgs.add(deepCopy(element)));
        if (childHasArguments) {
            child.getAsJsonObject("arguments").getAsJsonArray("game").forEach(element -> gameArgs.add(deepCopy(element)));
        }
        JsonArray jvmArgs = new JsonArray();
        if (childHasArguments && child.getAsJsonObject("arguments").has("jvm")) {
            child.getAsJsonObject("arguments").getAsJsonArray("jvm").forEach(element -> jvmArgs.add(deepCopy(element)));
        } else {
            parent.getAsJsonObject("arguments").getAsJsonArray("jvm").forEach(element -> jvmArgs.add(deepCopy(element)));
        }
        arguments.add("game", gameArgs);
        arguments.add("jvm", jvmArgs);
        merged.add("arguments", arguments);

        return merged;
    }

    private JsonObject fetchJson(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + " for " + uri);
        }
        return GSON.fromJson(response.body(), JsonObject.class);
    }

    private void downloadFile(String url, Path target) throws Exception {
        Files.createDirectories(target.getParent());
        log.accept("Downloading " + target.getFileName());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        try (InputStream input = response.body()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private JsonObject readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, JsonObject.class);
        }
    }

    private void writeJson(Path path, JsonObject object) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(object), StandardCharsets.UTF_8);
    }

    private JsonObject deepCopy(JsonObject object) {
        return GSON.fromJson(GSON.toJson(object), JsonObject.class);
    }

    private JsonElement deepCopy(JsonElement element) {
        return GSON.fromJson(GSON.toJson(element), JsonElement.class);
    }

    private Path versionJsonPath(String versionId) {
        return gameDir.resolve("versions").resolve(versionId).resolve(versionId + ".json");
    }

    private Path versionJarPath(String versionId) {
        return gameDir.resolve("versions").resolve(versionId).resolve(versionId + ".jar");
    }

    private String mapOsName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return "osx";
        }
        if (os.contains("win")) {
            return "windows";
        }
        return "linux";
    }

    private String normalizeArch(String arch) {
        String value = arch.toLowerCase();
        if (value.equals("x86_64") || value.equals("amd64")) {
            return "64";
        }
        if (value.equals("aarch64") || value.equals("arm64")) {
            return "arm64";
        }
        if (value.contains("64")) {
            return "64";
        }
        return "32";
    }

    private record PreparedLaunch(JsonObject vanillaVersion, JsonObject mergedVersion, Path nativesDir) {
    }

    private record ElyRuntimeSupport(ElySupportMode mode, Path path) {
    }

    private enum ElySupportMode {
        AUTHLIB_INJECTOR,
        OFFICIAL_AUTHLIB_PATCH
    }
}
