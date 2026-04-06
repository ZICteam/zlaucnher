package com.novaevent.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ModSyncInstaller {
    private static final Gson GSON = new Gson();
    private static final Type ENTRY_LIST_TYPE = new TypeToken<List<ManifestEntry>>() { }.getType();
    private static final URI MODRINTH_VERSION_FILES_URI = URI.create("https://api.modrinth.com/v2/version_files");
    private static final String MODRINTH_USER_AGENT = LauncherMetadata.USER_AGENT;

    private final HttpClient httpClient;
    private final Consumer<String> log;

    public interface ProgressListener {
        void onProgress(ProgressInfo info);
    }

    public record ProgressInfo(
            long completedBytes,
            long totalBytes,
            int completedFiles,
            int totalFiles,
            String currentFile,
            String message,
            boolean indeterminate
    ) {
    }

    public record SyncSummary(int downloadedFiles, int skippedFiles, int deletedFiles, long downloadedBytes) {
    }

    private record ManifestEntry(
            String category,
            String relativePath,
            String fileName,
            long fileSize,
            String sha256,
            String sha1,
            boolean required,
            boolean restartRequired,
            String downloadUrl
    ) {
        ManifestEntry withDownloadUrl(String newDownloadUrl) {
            return new ManifestEntry(category, relativePath, fileName, fileSize, sha256, sha1, required, restartRequired, newDownloadUrl);
        }
    }

    private record DownloadJob(ManifestEntry entry, Path targetPath) {
    }

    public ModSyncInstaller(HttpClient httpClient, Consumer<String> log) {
        this.httpClient = httpClient;
        this.log = log;
    }

    public SyncSummary syncFromLatestState(Path gameDir, String serverAddress, ProgressListener progressListener) throws Exception {
        Path stateFile = findBestStateFile(gameDir, serverAddress);
        if (stateFile == null) {
            throw new IOException("ModSync state file not found. Join the server once to cache its sync manifest.");
        }

        log.accept("ModSync state: " + stateFile);
        List<ManifestEntry> entries = readEntries(stateFile);
        if (entries.isEmpty()) {
            throw new IOException("ModSync state file is empty.");
        }

        List<DownloadJob> jobs = new ArrayList<>();
        int skipped = 0;
        long totalBytes = 0L;
        int deleted = 0;

        for (ManifestEntry entry : entries) {
            if (entry == null || isBlank(entry.downloadUrl()) || isBlank(entry.relativePath()) || isBlank(entry.category())) {
                continue;
            }

            Path baseDir = resolveClientFolder(gameDir, entry.category());
            Path targetPath = safeResolve(baseDir, entry.relativePath());
            if (Files.exists(targetPath) && matchesSha256(targetPath, entry.sha256())) {
                skipped++;
                continue;
            }

            jobs.add(new DownloadJob(entry, targetPath));
            if (entry.fileSize() > 0) {
                totalBytes += entry.fileSize();
            }
        }

        deleted = deleteExtraneousFiles(gameDir, entries);

        if (jobs.isEmpty()) {
            emit(progressListener, 0L, 0L, entries.size(), entries.size(), "", "Все файлы ModSync уже актуальны", false);
            log.accept("ModSync synchronized. Downloaded 0, skipped " + skipped + ", deleted " + deleted + ".");
            return new SyncSummary(0, skipped, deleted, 0L);
        }

        long completedBytes = 0L;
        int completedFiles = 0;
        emit(progressListener, 0L, totalBytes, 0, jobs.size(), "", "Подготовка синхронизации ModSync", totalBytes <= 0);

        for (DownloadJob job : jobs) {
            ManifestEntry entry = job.entry();
            Path targetPath = job.targetPath();
            Files.createDirectories(targetPath.getParent());
            String fileName = isBlank(entry.fileName()) ? targetPath.getFileName().toString() : entry.fileName();
            log.accept("ModSync downloading: " + fileName);

            HttpRequest request = HttpRequest.newBuilder(URI.create(entry.downloadUrl()))
                    .GET()
                    .timeout(Duration.ofMinutes(5))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("ModSync download failed with HTTP " + response.statusCode() + " for " + fileName);
            }

            long expectedSize = entry.fileSize() > 0 ? entry.fileSize() : response.headers().firstValueAsLong("content-length").orElse(-1L);
            Path tempPath = targetPath.resolveSibling(targetPath.getFileName() + ".part");
            try (InputStream input = response.body(); OutputStream output = Files.newOutputStream(tempPath)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                long fileCompleted = 0L;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    fileCompleted += read;
                    completedBytes += read;
                    emit(
                            progressListener,
                            completedBytes,
                            totalBytes,
                            completedFiles,
                            jobs.size(),
                            fileName,
                            "Скачивание ModSync: " + fileName,
                            totalBytes <= 0 && expectedSize <= 0
                    );
                }
            }

            if (!matchesSha256(tempPath, entry.sha256())) {
                Files.deleteIfExists(tempPath);
                throw new IOException("SHA-256 mismatch after ModSync download: " + fileName);
            }

            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            completedFiles++;
            emit(progressListener, completedBytes, totalBytes, completedFiles, jobs.size(), fileName, "Загружен " + fileName, totalBytes <= 0);
        }

        log.accept("ModSync synchronized. Downloaded " + completedFiles + ", skipped " + skipped + ", deleted " + deleted + ".");
        return new SyncSummary(completedFiles, skipped, deleted, completedBytes);
    }

    public SyncSummary syncFromManifestJson(Path gameDir, String manifestJson, ProgressListener progressListener) throws Exception {
        List<ManifestEntry> entries = parseEntries(manifestJson);
        if (entries.isEmpty()) {
            throw new IOException("Live ModSync manifest is empty.");
        }
        log.accept("ModSync live manifest entries: " + entries.size());
        return syncFromEntries(gameDir, entries, progressListener);
    }

    private SyncSummary syncFromEntries(Path gameDir, List<ManifestEntry> entries, ProgressListener progressListener) throws Exception {
        List<ManifestEntry> resolvedEntries = preferModrinthDownloads(entries);
        List<DownloadJob> jobs = new ArrayList<>();
        int skipped = 0;
        long totalBytes = 0L;
        int deleted = deleteExtraneousFiles(gameDir, resolvedEntries);

        for (ManifestEntry entry : resolvedEntries) {
            if (entry == null || isBlank(entry.downloadUrl()) || isBlank(entry.relativePath()) || isBlank(entry.category())) {
                continue;
            }

            Path baseDir = resolveClientFolder(gameDir, entry.category());
            Path targetPath = safeResolve(baseDir, entry.relativePath());
            if (Files.exists(targetPath) && matchesSha256(targetPath, entry.sha256())) {
                skipped++;
                continue;
            }

            jobs.add(new DownloadJob(entry, targetPath));
            if (entry.fileSize() > 0) {
                totalBytes += entry.fileSize();
            }
        }

        if (jobs.isEmpty()) {
            emit(progressListener, 0L, 0L, resolvedEntries.size(), resolvedEntries.size(), "", "Все файлы ModSync уже актуальны", false);
            log.accept("ModSync synchronized. Downloaded 0, skipped " + skipped + ", deleted " + deleted + ".");
            return new SyncSummary(0, skipped, deleted, 0L);
        }

        long completedBytes = 0L;
        int completedFiles = 0;
        emit(progressListener, 0L, totalBytes, 0, jobs.size(), "", "Подготовка синхронизации ModSync", totalBytes <= 0);

        for (DownloadJob job : jobs) {
            ManifestEntry entry = job.entry();
            Path targetPath = job.targetPath();
            Files.createDirectories(targetPath.getParent());
            String fileName = isBlank(entry.fileName()) ? targetPath.getFileName().toString() : entry.fileName();
            log.accept("ModSync downloading: " + fileName);

            HttpRequest request = HttpRequest.newBuilder(URI.create(entry.downloadUrl()))
                    .GET()
                    .timeout(Duration.ofMinutes(5))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("ModSync download failed with HTTP " + response.statusCode() + " for " + fileName);
            }

            long expectedSize = entry.fileSize() > 0 ? entry.fileSize() : response.headers().firstValueAsLong("content-length").orElse(-1L);
            Path tempPath = targetPath.resolveSibling(targetPath.getFileName() + ".part");
            try (InputStream input = response.body(); OutputStream output = Files.newOutputStream(tempPath)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                long fileCompleted = 0L;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    fileCompleted += read;
                    completedBytes += read;
                    emit(
                            progressListener,
                            completedBytes,
                            totalBytes,
                            completedFiles,
                            jobs.size(),
                            fileName,
                            "Скачивание ModSync: " + fileName,
                            totalBytes <= 0 && expectedSize <= 0
                    );
                }
            }

            if (!matchesSha256(tempPath, entry.sha256())) {
                Files.deleteIfExists(tempPath);
                throw new IOException("SHA-256 mismatch after ModSync download: " + fileName);
            }

            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            completedFiles++;
            emit(progressListener, completedBytes, totalBytes, completedFiles, jobs.size(), fileName, "Загружен " + fileName, totalBytes <= 0);
        }

        verifySynchronizedFiles(gameDir, resolvedEntries, progressListener);
        log.accept("ModSync synchronized. Downloaded " + completedFiles + ", skipped " + skipped + ", deleted " + deleted + ".");
        return new SyncSummary(completedFiles, skipped, deleted, completedBytes);
    }

    private void verifySynchronizedFiles(Path gameDir, List<ManifestEntry> entries, ProgressListener progressListener) throws Exception {
        int total = 0;
        for (ManifestEntry entry : entries) {
            if (entry != null && !isBlank(entry.relativePath()) && !isBlank(entry.category())) {
                total++;
            }
        }

        int checked = 0;
        emit(progressListener, 0L, 0L, 0, Math.max(total, 1), "", "Проверка файлов ModSync", true);
        for (ManifestEntry entry : entries) {
            if (entry == null || isBlank(entry.relativePath()) || isBlank(entry.category())) {
                continue;
            }
            Path baseDir = resolveClientFolder(gameDir, entry.category());
            Path targetPath = safeResolve(baseDir, entry.relativePath());
            String fileName = isBlank(entry.fileName()) ? targetPath.getFileName().toString() : entry.fileName();
            checked++;
            emit(progressListener, 0L, 0L, checked, total, fileName, "Проверка ModSync: " + fileName, true);
            if (!Files.exists(targetPath)) {
                throw new IOException("ModSync verification failed: missing file " + fileName);
            }
            if (!matchesSha256(targetPath, entry.sha256())) {
                throw new IOException("ModSync verification failed: checksum mismatch for " + fileName);
            }
        }
        log.accept("ModSync verification passed for " + checked + " file(s).");
    }

    private List<ManifestEntry> preferModrinthDownloads(List<ManifestEntry> entries) {
        List<String> sha1Hashes = entries.stream()
                .map(ManifestEntry::sha1)
                .filter(hash -> !isBlank(hash))
                .map(hash -> hash.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (sha1Hashes.isEmpty()) {
            return entries;
        }

        Map<String, String> modrinthUrls = fetchModrinthUrlsBySha1(sha1Hashes);
        if (modrinthUrls.isEmpty()) {
            return entries;
        }

        List<ManifestEntry> resolved = new ArrayList<>(entries.size());
        int preferred = 0;
        for (ManifestEntry entry : entries) {
            String sha1 = safeTrim(entry.sha1()).toLowerCase(Locale.ROOT);
            String modrinthUrl = modrinthUrls.get(sha1);
            if (!sha1.isBlank() && !isBlank(modrinthUrl)) {
                resolved.add(entry.withDownloadUrl(modrinthUrl));
                preferred++;
            } else {
                resolved.add(entry);
            }
        }
        if (preferred > 0) {
            log.accept("ModSync: Modrinth used for " + preferred + " file(s), server fallback for the rest.");
        }
        return resolved;
    }

    private Map<String, String> fetchModrinthUrlsBySha1(List<String> sha1Hashes) {
        try {
            JsonObject payload = new JsonObject();
            JsonArray hashes = new JsonArray();
            for (String hash : sha1Hashes) {
                hashes.add(hash);
            }
            payload.add("hashes", hashes);
            payload.addProperty("algorithm", "sha1");

            HttpRequest request = HttpRequest.newBuilder(MODRINTH_VERSION_FILES_URI)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", MODRINTH_USER_AGENT)
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                log.accept("ModSync: Modrinth lookup failed with HTTP " + response.statusCode() + ".");
                return Map.of();
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            Map<String, String> resolved = new java.util.HashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String sha1 = safeTrim(entry.getKey()).toLowerCase(Locale.ROOT);
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject version = entry.getValue().getAsJsonObject();
                if (!version.has("files") || !version.get("files").isJsonArray()) {
                    continue;
                }
                JsonArray files = version.getAsJsonArray("files");
                String matchedUrl = null;
                for (JsonElement fileElement : files) {
                    if (!fileElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject file = fileElement.getAsJsonObject();
                    if (!file.has("hashes") || !file.get("hashes").isJsonObject() || !file.has("url")) {
                        continue;
                    }
                    JsonObject hashesObject = file.getAsJsonObject("hashes");
                    String fileSha1 = hashesObject.has("sha1") ? safeTrim(hashesObject.get("sha1").getAsString()).toLowerCase(Locale.ROOT) : "";
                    if (!sha1.equals(fileSha1)) {
                        continue;
                    }
                    if (file.has("primary") && file.get("primary").getAsBoolean()) {
                        matchedUrl = file.get("url").getAsString();
                        break;
                    }
                    if (matchedUrl == null) {
                        matchedUrl = file.get("url").getAsString();
                    }
                }
                if (matchedUrl != null) {
                    resolved.put(sha1, matchedUrl);
                }
            }
            return resolved;
        } catch (Exception ex) {
            log.accept("ModSync: Modrinth lookup skipped: " + ex.getMessage());
            return Map.of();
        }
    }

    private int deleteExtraneousFiles(Path gameDir, List<ManifestEntry> entries) throws Exception {
        Map<Path, Set<Path>> expectedByRoot = entries.stream()
                .filter(entry -> entry != null && !isBlank(entry.relativePath()) && !isBlank(entry.category()))
                .collect(Collectors.groupingBy(
                        entry -> resolveClientFolder(gameDir, entry.category()).normalize(),
                        Collectors.mapping(entry -> {
                            try {
                                return safeResolve(resolveClientFolder(gameDir, entry.category()), entry.relativePath());
                            } catch (IOException ex) {
                                throw new IllegalStateException(ex);
                            }
                        }, Collectors.toCollection(HashSet::new))
                ));

        int deleted = 0;
        for (Map.Entry<Path, Set<Path>> rootEntry : expectedByRoot.entrySet()) {
            Path root = rootEntry.getKey();
            Set<Path> expectedFiles = rootEntry.getValue();
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .sorted(Comparator.comparingInt(path -> path.getNameCount()))
                        .toList();
                for (Path file : files) {
                    if (shouldKeepExtraFile(root, file, expectedFiles)) {
                        continue;
                    }
                    Files.deleteIfExists(file);
                    deleted++;
                    log.accept("ModSync removed extra file: " + root.relativize(file));
                }
            }
            deleteEmptyDirectories(root);
        }
        return deleted;
    }

    private boolean shouldKeepExtraFile(Path root, Path file, Set<Path> expectedFiles) {
        if (expectedFiles.contains(file)) {
            return true;
        }
        Path relative = root.relativize(file);
        if (root.endsWith("config")) {
            return true;
        }
        return false;
    }

    private void deleteEmptyDirectories(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> directories = stream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
                    .toList();
            for (Path directory : directories) {
                if (directory.equals(root)) {
                    continue;
                }
                try (Stream<Path> children = Files.list(directory)) {
                    if (children.findAny().isEmpty()) {
                        Files.deleteIfExists(directory);
                    }
                }
            }
        }
    }

    private void emit(
            ProgressListener progressListener,
            long completedBytes,
            long totalBytes,
            int completedFiles,
            int totalFiles,
            String currentFile,
            String message,
            boolean indeterminate
    ) {
        if (progressListener == null) {
            return;
        }
        progressListener.onProgress(new ProgressInfo(
                completedBytes,
                totalBytes,
                completedFiles,
                totalFiles,
                currentFile,
                message,
                indeterminate
        ));
    }

    private List<ManifestEntry> readEntries(Path stateFile) throws IOException {
        String json = Files.readString(stateFile, StandardCharsets.UTF_8);
        return parseEntries(json);
    }

    private List<ManifestEntry> parseEntries(String json) throws IOException {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (root.isJsonArray()) {
                List<ManifestEntry> entries = GSON.fromJson(root, ENTRY_LIST_TYPE);
                return entries != null ? entries : List.of();
            }
            if (root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();
                if (object.has("entries") && object.get("entries").isJsonArray()) {
                    JsonArray entriesArray = object.getAsJsonArray("entries");
                    List<ManifestEntry> entries = GSON.fromJson(entriesArray, ENTRY_LIST_TYPE);
                    return entries != null ? entries : List.of();
                }
            }
            return List.of();
        } catch (Exception ex) {
            throw new IOException("Failed to parse ModSync manifest JSON.", ex);
        }
    }

    private Path findBestStateFile(Path gameDir, String serverAddress) throws IOException {
        Path stateDir = gameDir.resolve("config").resolve("modsync-state");
        if (!Files.isDirectory(stateDir)) {
            return null;
        }
        MinecraftServerStatusFetcher.ServerAddress parsedAddress = MinecraftServerStatusFetcher.parseAddress(serverAddress);
        try (var stream = Files.list(stateDir)) {
            List<Path> files = stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparingLong(this::lastModified).reversed())
                    .toList();
            if (parsedAddress != null) {
                String expectedPrefix = parsedAddress.host() + (parsedAddress.port() == 25565 ? "" : "_" + parsedAddress.port());
                for (Path file : files) {
                    String fileName = file.getFileName().toString();
                    if (fileName.startsWith(expectedPrefix + "-") || fileName.equals(expectedPrefix + ".json")) {
                        return file;
                    }
                }
            }
            return files.isEmpty() ? null : files.get(0);
        }
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return Long.MIN_VALUE;
        }
    }

    private Path resolveClientFolder(Path gameDir, String category) {
        String normalized = safeTrim(category).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MOD", "OPTIONAL_CLIENT" -> gameDir.resolve("mods");
            case "RESOURCEPACK" -> gameDir.resolve("resourcepacks");
            case "SHADERPACK" -> gameDir.resolve("shaderpacks");
            case "CONFIG" -> gameDir.resolve("config");
            default -> gameDir.resolve("mods");
        };
    }

    private Path safeResolve(Path root, String relativePath) throws IOException {
        Path resolved = root.resolve(relativePath).normalize();
        Path normalizedRoot = root.normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IOException("Unsafe ModSync path: " + relativePath);
        }
        return resolved;
    }

    private boolean matchesSha256(Path path, String expectedSha256) throws Exception {
        if (!Files.isRegularFile(path) || isBlank(expectedSha256)) {
            return false;
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        String actual = toHex(digest.digest());
        return actual.equalsIgnoreCase(expectedSha256);
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private boolean isBlank(String value) {
        return safeTrim(value).isBlank();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
