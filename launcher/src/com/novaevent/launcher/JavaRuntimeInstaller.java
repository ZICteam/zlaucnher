package com.novaevent.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.nio.file.attribute.PosixFilePermission;

public final class JavaRuntimeInstaller {
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private final Consumer<String> log;

    public JavaRuntimeInstaller(HttpClient httpClient, Consumer<String> log) {
        this.httpClient = httpClient;
        this.log = log;
    }

    public void ensureRuntimeForProfile(Path projectDir, LauncherConfig config, ClientProfile profile) throws Exception {
        String runtime = profile != null && profile.javaRuntime != null ? profile.javaRuntime.trim().toLowerCase() : "auto";
        String runtimeFolder = switch (runtime) {
            case "java17" -> "java-17";
            case "java21" -> "java-21";
            case "java25" -> "java-25";
            case "auto" -> resolveAutoRuntime(profile);
            default -> null;
        };

        if (runtimeFolder == null) {
            return;
        }

        if (JavaRuntimeResolver.resolveBundled(projectDir, runtimeFolder) != null) {
            return;
        }

        int version = switch (runtimeFolder) {
            case "java-21" -> 21;
            case "java-25" -> 25;
            default -> 17;
        };
        installRuntime(projectDir, version, runtimeFolder);
    }

    private void installRuntime(Path projectDir, int version, String runtimeFolder) throws Exception {
        log.accept("Downloading bundled Java " + version + " from Eclipse Adoptium");

        Path downloadsDir = projectDir.resolve("launcher").resolve("downloads");
        Files.createDirectories(downloadsDir);
        Path runtimeRoot = projectDir.resolve("launcher").resolve("runtime").resolve(runtimeFolder);
        Path tempRoot = projectDir.resolve("launcher").resolve("runtime").resolve("." + runtimeFolder + "-tmp");

        deleteDirectory(tempRoot);
        Files.createDirectories(tempRoot);

        RuntimeAsset asset = fetchLatestRuntimeAsset(version);
        Path archivePath = downloadsDir.resolve(asset.fileName());
        downloadFile(asset.link(), archivePath);
        extractArchive(archivePath, tempRoot, asset.fileName());

        Path extractedRoot = findRuntimeRoot(tempRoot);
        if (extractedRoot == null) {
            throw new IOException("Bundled Java archive was downloaded but no java binary was found after extraction.");
        }

        deleteDirectory(runtimeRoot);
        Files.createDirectories(runtimeRoot.getParent());
        Files.move(extractedRoot, runtimeRoot, StandardCopyOption.REPLACE_EXISTING);
        ensureExecutableBits(runtimeRoot);
        deleteDirectory(tempRoot);

        log.accept("Bundled Java " + version + " is ready: " + runtimeRoot);
    }

    private RuntimeAsset fetchLatestRuntimeAsset(int version) throws Exception {
        String url = "https://api.adoptium.net/v3/assets/latest/" + version + "/hotspot"
                + "?release_type=ga"
                + "&os=" + mapOs()
                + "&architecture=" + mapArch()
                + "&image_type=jre"
                + "&heap_size=normal"
                + "&vendor=eclipse";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", LauncherMetadata.USER_AGENT)
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Could not query Adoptium API: HTTP " + response.statusCode());
        }

        JsonArray assets = GSON.fromJson(response.body(), JsonArray.class);
        if (assets == null || assets.isEmpty()) {
            throw new IOException("No compatible Adoptium runtime found for Java " + version + ".");
        }

        JsonObject binary = assets.get(0).getAsJsonObject().getAsJsonObject("binary");
        JsonObject pkg = binary.getAsJsonObject("package");
        return new RuntimeAsset(pkg.get("link").getAsString(), pkg.get("name").getAsString());
    }

    private void downloadFile(String url, Path target) throws Exception {
        Files.createDirectories(target.getParent());
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", LauncherMetadata.USER_AGENT)
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Could not download bundled Java: HTTP " + response.statusCode());
        }
        try (InputStream input = response.body()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void extractArchive(Path archivePath, Path targetDir, String fileName) throws Exception {
        log.accept("Extracting bundled Java runtime");
        if (fileName.endsWith(".zip")) {
            extractZip(archivePath, targetDir);
            return;
        }
        if (fileName.endsWith(".tar.gz")) {
            extractTarGz(archivePath, targetDir);
            return;
        }
        throw new IOException("Unsupported Java runtime archive: " + fileName);
    }

    private void extractZip(Path archivePath, Path targetDir) throws Exception {
        try (InputStream input = Files.newInputStream(archivePath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path out = safeResolve(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                    continue;
                }
                Files.createDirectories(out.getParent());
                Files.copy(zip, out, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void extractTarGz(Path archivePath, Path targetDir) throws Exception {
        try (InputStream fileIn = Files.newInputStream(archivePath);
             InputStream gzipIn = new GZIPInputStream(fileIn)) {
            byte[] header = new byte[512];
            while (true) {
                int read = gzipIn.readNBytes(header, 0, 512);
                if (read == 0 || isTarEndBlock(header)) {
                    break;
                }
                if (read < 512) {
                    throw new IOException("Corrupted tar archive.");
                }

                String entryName = readTarString(header, 0, 100);
                if (entryName.isBlank()) {
                    break;
                }
                long size = readTarOctal(header, 124, 12);
                int typeFlag = header[156];
                Path out = safeResolve(targetDir, entryName);

                if (typeFlag == '5') {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(new LimitedInputStream(gzipIn, size), out, StandardCopyOption.REPLACE_EXISTING);
                    skipPadding(gzipIn, size);
                    continue;
                }

                if (size > 0) {
                    gzipIn.skipNBytes(size);
                    skipPadding(gzipIn, size);
                }
            }
        }
    }

    private void skipPadding(InputStream input, long size) throws IOException {
        long padding = (512 - (size % 512)) % 512;
        if (padding > 0) {
            input.skipNBytes(padding);
        }
    }

    private boolean isTarEndBlock(byte[] header) {
        for (byte value : header) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private String readTarString(byte[] header, int offset, int length) {
        int end = offset;
        while (end < offset + length && header[end] != 0) {
            end++;
        }
        return new String(header, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    private long readTarOctal(byte[] header, int offset, int length) {
        String value = readTarString(header, offset, length).replace("\u0000", "").trim();
        if (value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value, 8);
    }

    private Path safeResolve(Path targetDir, String entryName) throws IOException {
        Path normalized = targetDir.resolve(entryName).normalize();
        if (!normalized.startsWith(targetDir.normalize())) {
            throw new IOException("Unsafe archive entry: " + entryName);
        }
        return normalized;
    }

    private Path findRuntimeRoot(Path extractedDir) throws IOException {
        try (var stream = Files.walk(extractedDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(isWindows() ? "java.exe" : "java"))
                    .map(path -> {
                        if (path.toString().contains("Contents/Home/bin")) {
                            return path.getParent().getParent().getParent();
                        }
                        return path.getParent().getParent();
                    })
                    .findFirst()
                    .orElse(null);
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException ignored) {
                }
            });
        }
    }

    private void ensureExecutableBits(Path runtimeRoot) {
        if (isWindows()) {
            return;
        }
        Path[] candidates = new Path[]{
                runtimeRoot.resolve("bin").resolve("java"),
                runtimeRoot.resolve("bin").resolve("jspawnhelper"),
                runtimeRoot.resolve("Home").resolve("bin").resolve("java"),
                runtimeRoot.resolve("Home").resolve("lib").resolve("jspawnhelper"),
                runtimeRoot.resolve("Contents").resolve("Home").resolve("bin").resolve("java"),
                runtimeRoot.resolve("Contents").resolve("Home").resolve("lib").resolve("jspawnhelper")
        };
        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) {
                continue;
            }
            try {
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(candidate);
                permissions.add(PosixFilePermission.OWNER_EXECUTE);
                permissions.add(PosixFilePermission.GROUP_EXECUTE);
                permissions.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(candidate, permissions);
            } catch (Exception ignored) {
            }
        }
    }

    private String resolveAutoRuntime(ClientProfile profile) {
        if (profile != null && profile.minecraftVersion != null) {
            if (profile.minecraftVersion.startsWith("1.20.5")
                    || profile.minecraftVersion.startsWith("1.20.6")
                    || profile.minecraftVersion.startsWith("1.21")) {
                return "java-21";
            }
        }
        return "java-17";
    }

    private String mapOs() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac")) {
            return "mac";
        }
        return "linux";
    }

    private String mapArch() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "aarch64";
        }
        return "x64";
    }

    private boolean isWindows() {
        return mapOs().equals("windows");
    }

    private record RuntimeAsset(String link, String fileName) {
    }

    private static final class LimitedInputStream extends InputStream {
        private final InputStream delegate;
        private long remaining;

        private LimitedInputStream(InputStream delegate, long remaining) {
            this.delegate = delegate;
            this.remaining = remaining;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = delegate.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int read = delegate.read(b, off, (int) Math.min(len, remaining));
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }
    }
}
