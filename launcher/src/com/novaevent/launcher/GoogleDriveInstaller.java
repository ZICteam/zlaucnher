package com.novaevent.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class GoogleDriveInstaller {
    private static final Pattern FILE_ID_PATTERN = Pattern.compile("/file/d/([^/]+)");
    private static final Pattern CONFIRM_PATTERN = Pattern.compile("confirm=([0-9A-Za-z_\\-]+)");
    private static final String BUNDLE_MARKER = ".bundle-source.txt";

    private final HttpClient httpClient;
    private final Consumer<String> log;

    public interface ProgressListener {
        void onProgress(ProgressInfo info);
    }

    public record ProgressInfo(String stage, long completedBytes, long totalBytes, String message, boolean indeterminate) {
    }

    public static final class TransferControl {
        private final Object lock = new Object();
        private volatile boolean cancelled;
        private volatile boolean paused;

        public void pause() {
            paused = true;
        }

        public void resume() {
            synchronized (lock) {
                paused = false;
                lock.notifyAll();
            }
        }

        public void cancel() {
            synchronized (lock) {
                cancelled = true;
                paused = false;
                lock.notifyAll();
            }
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public boolean isPaused() {
            return paused;
        }

        public void awaitIfPaused() throws InterruptedException, IOException {
            synchronized (lock) {
                while (paused && !cancelled) {
                    lock.wait(250L);
                }
            }
            if (cancelled) {
                throw new IOException("Download cancelled.");
            }
        }
    }

    public GoogleDriveInstaller(HttpClient httpClient, Consumer<String> log) {
        this.httpClient = httpClient;
        this.log = log;
    }

    public void installBundle(String rawUrl, Path projectDir, Path gameDir) throws Exception {
        installBundle(rawUrl, projectDir, gameDir, null, null);
    }

    public void installBundle(String rawUrl, Path projectDir, Path gameDir, ProgressListener progressListener, TransferControl transferControl) throws Exception {
        String fileId = extractFileId(rawUrl);
        if (fileId == null || fileId.isBlank()) {
            throw new IOException("Google Drive link is invalid. Paste a share link or direct download link.");
        }

        Path downloadDir = projectDir.resolve("launcher").resolve("downloads");
        Files.createDirectories(downloadDir);
        Path archivePath = downloadDir.resolve("client-bundle.zip");

        emit(progressListener, "download", 0, -1, "Preparing Google Drive download", true);
        downloadGoogleDriveFile(fileId, archivePath, progressListener, transferControl);
        extractZip(archivePath, gameDir, progressListener, transferControl);
        Files.writeString(gameDir.resolve(BUNDLE_MARKER), fileId, StandardCharsets.UTF_8);
    }

    public boolean isBundleInstallRequired(String rawUrl, Path gameDir) {
        try {
            String fileId = extractFileId(rawUrl);
            if (fileId == null || fileId.isBlank()) {
                return true;
            }
            Path markerPath = gameDir.resolve(BUNDLE_MARKER);
            if (!Files.exists(markerPath)) {
                return true;
            }
            String installedFileId = Files.readString(markerPath, StandardCharsets.UTF_8).trim();
            if (!fileId.equals(installedFileId)) {
                return true;
            }
            return !Files.exists(gameDir.resolve("versions")) || !Files.exists(gameDir.resolve("libraries"));
        } catch (Exception ignored) {
            return true;
        }
    }

    private void downloadGoogleDriveFile(String fileId, Path archivePath, ProgressListener progressListener, TransferControl transferControl) throws Exception {
        String initialUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
        HttpResponse<byte[]> initial = sendBytes(initialUrl, null);

        String contentType = initial.headers().firstValue("content-type").orElse("");
        if (!contentType.contains("text/html")) {
            Files.write(archivePath, initial.body());
            emit(progressListener, "download", initial.body().length, initial.body().length, "Downloaded client bundle from Google Drive", false);
            log.accept("Downloaded client bundle from Google Drive");
            return;
        }

        String html = new String(initial.body(), StandardCharsets.UTF_8);
        Matcher matcher = CONFIRM_PATTERN.matcher(html);
        String confirm = matcher.find() ? matcher.group(1) : "t";

        String cookieHeader = initial.headers().allValues("set-cookie").stream()
                .map(value -> value.split(";", 2)[0])
                .reduce((a, b) -> a + "; " + b)
                .orElse("");

        String confirmUrl = "https://drive.usercontent.google.com/download?id=" + fileId
                + "&export=download&confirm=" + confirm;
        HttpResponse<InputStream> confirmed = sendStream(confirmUrl, cookieHeader.isBlank() ? null : cookieHeader);
        if (confirmed.statusCode() / 100 != 2) {
            throw new IOException("Google Drive download failed with HTTP " + confirmed.statusCode());
        }

        long totalBytes = confirmed.headers().firstValueAsLong("content-length").orElse(-1L);
        emit(progressListener, "download", 0, totalBytes, "Downloading client bundle from Google Drive", totalBytes <= 0);
        try (InputStream input = confirmed.body();
             OutputStream output = Files.newOutputStream(archivePath)) {
            copyWithProgress(input, output, progressListener, transferControl, "download", totalBytes, "Downloading client bundle from Google Drive");
        }
        emit(progressListener, "download", totalBytes > 0 ? totalBytes : Files.size(archivePath), totalBytes, "Downloaded client bundle from Google Drive", false);
        log.accept("Downloaded client bundle from Google Drive");
    }

    private void extractZip(Path archivePath, Path targetDir, ProgressListener progressListener, TransferControl transferControl) throws Exception {
        Files.createDirectories(targetDir);
        emit(progressListener, "extract", 0, -1, "Extracting client bundle", true);
        log.accept("Extracting client bundle");

        try (InputStream input = Files.newInputStream(archivePath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (transferControl != null) {
                    transferControl.awaitIfPaused();
                    if (transferControl.isCancelled()) {
                        throw new IOException("Download cancelled.");
                    }
                }
                if (entry.isDirectory()) {
                    continue;
                }

                Path out = safeResolve(targetDir, entry.getName());
                Files.createDirectories(out.getParent());
                try (OutputStream output = Files.newOutputStream(out)) {
                    copyWithProgress(zip, output, progressListener, transferControl, "extract", -1, "Extracting: " + entry.getName());
                }
            }
        }

        emit(progressListener, "extract", 1, 1, "Client bundle extracted", false);
        log.accept("Client bundle extracted");
    }

    private void copyWithProgress(
            InputStream input,
            OutputStream output,
            ProgressListener progressListener,
            TransferControl transferControl,
            String stage,
            long totalBytes,
            String message
    ) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        long completed = 0L;
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (transferControl != null) {
                transferControl.awaitIfPaused();
                if (transferControl.isCancelled()) {
                    throw new IOException("Download cancelled.");
                }
            }
            output.write(buffer, 0, read);
            completed += read;
            emit(progressListener, stage, completed, totalBytes, message, totalBytes <= 0);
        }
    }

    private void emit(ProgressListener listener, String stage, long completedBytes, long totalBytes, String message, boolean indeterminate) {
        if (listener != null) {
            listener.onProgress(new ProgressInfo(stage, completedBytes, totalBytes, message, indeterminate));
        }
    }

    private Path safeResolve(Path targetDir, String entryName) throws IOException {
        Path normalized = targetDir.resolve(entryName).normalize();
        if (!normalized.startsWith(targetDir.normalize())) {
            throw new IOException("Unsafe archive entry: " + entryName);
        }
        return normalized;
    }

    private HttpResponse<byte[]> sendBytes(String url, String cookieHeader) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", "Mozilla/5.0");
        if (cookieHeader != null && !cookieHeader.isBlank()) {
            builder.header("Cookie", cookieHeader);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<InputStream> sendStream(String url, String cookieHeader) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "Mozilla/5.0");
        if (cookieHeader != null && !cookieHeader.isBlank()) {
            builder.header("Cookie", cookieHeader);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
    }

    private String extractFileId(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        Matcher matcher = FILE_ID_PATTERN.matcher(rawUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }

        URI uri = URI.create(rawUrl);
        String query = uri.getRawQuery();
        if (query == null) {
            return null;
        }

        Map<String, String> params = parseQuery(query);
        return params.get("id");
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            String[] pieces = pair.split("=", 2);
            String key = URLDecoder.decode(pieces[0], StandardCharsets.UTF_8);
            String value = pieces.length > 1 ? URLDecoder.decode(pieces[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }
}
