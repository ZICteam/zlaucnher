package com.novaevent.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ModSyncManifestFetcher {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_MODSYNC_HTTP_PORT = 8080;
    private static final char HIDDEN_START = '\u2063';
    private static final char HIDDEN_END = '\u2064';
    private static final char HIDDEN_ZERO = '\u200B';
    private static final char HIDDEN_ONE = '\u200C';

    private final HttpClient httpClient;
    private final Consumer<String> log;

    public record LiveManifest(String json, URI sourceUrl, int hiddenPort) {
    }

    public ModSyncManifestFetcher(HttpClient httpClient, Consumer<String> log) {
        this.httpClient = httpClient;
        this.log = log;
    }

    public LiveManifest fetch(String serverAddress) throws Exception {
        MinecraftServerStatusFetcher.ServerAddress parsedAddress = MinecraftServerStatusFetcher.parseAddress(serverAddress);
        if (parsedAddress == null) {
            throw new IOException("Server address is not configured.");
        }

        MinecraftServerStatusFetcher statusFetcher = new MinecraftServerStatusFetcher();
        MinecraftServerStatusFetcher.ServerStatus status = statusFetcher.fetch(serverAddress, 2500, 3500);
        int hiddenPort = extractHiddenHttpPort(status.rawDescription());
        String host = normalizeHost(parsedAddress.host());
        List<URI> candidates = buildManifestCandidateUrls(host, hiddenPort, DEFAULT_MODSYNC_HTTP_PORT);

        Exception lastError = null;
        for (URI candidate : candidates) {
            try {
                log.accept("ModSync manifest candidate: " + candidate);
                HttpRequest request = HttpRequest.newBuilder(candidate)
                        .GET()
                        .timeout(Duration.ofSeconds(8))
                        .header("Accept", "application/json")
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() / 100 != 2) {
                    lastError = new IOException("HTTP " + response.statusCode() + " for " + candidate);
                    continue;
                }
                String body = response.body() == null ? "" : response.body().trim();
                validateManifestPayload(body);
                log.accept("ModSync manifest loaded: " + candidate);
                return new LiveManifest(body, candidate, hiddenPort);
            } catch (Exception ex) {
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw new IOException("Could not load live ModSync manifest from server " + serverAddress + ": " + lastError.getMessage(), lastError);
        }
        throw new IOException("Could not load live ModSync manifest from server " + serverAddress + ".");
    }

    private void validateManifestPayload(String body) throws IOException {
        try {
            JsonElement root = JsonParser.parseString(body);
            JsonArray entries = null;
            if (root.isJsonArray()) {
                entries = root.getAsJsonArray();
            } else if (root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();
                if (object.has("entries") && object.get("entries").isJsonArray()) {
                    entries = object.getAsJsonArray("entries");
                }
            }
            if (entries == null || entries.isEmpty()) {
                throw new IOException("Manifest is empty.");
            }
        } catch (Exception ex) {
            throw new IOException("Manifest response is not valid JSON.", ex);
        }
    }

    private List<URI> buildManifestCandidateUrls(String host, int hiddenPort, int defaultPort) {
        List<URI> candidates = new ArrayList<>();
        if (hiddenPort > 0) {
            candidates.add(URI.create("http://" + host + ":" + hiddenPort + "/manifest"));
        }
        candidates.add(URI.create("http://" + host + ":" + defaultPort + "/manifest"));
        candidates.add(URI.create("https://" + host + "/manifest"));
        candidates.add(URI.create("http://" + host + "/manifest"));
        return candidates;
    }

    private int extractHiddenHttpPort(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        int start = value.indexOf(HIDDEN_START);
        if (start < 0) {
            return -1;
        }
        int end = value.indexOf(HIDDEN_END, start + 1);
        if (end < 0 || end <= start + 1) {
            return -1;
        }
        StringBuilder binary = new StringBuilder();
        for (int i = start + 1; i < end; i++) {
            char current = value.charAt(i);
            if (current == HIDDEN_ZERO) {
                binary.append('0');
            } else if (current == HIDDEN_ONE) {
                binary.append('1');
            }
        }
        if (binary.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(binary.toString(), 2);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String normalizeHost(String rawHost) {
        String host = rawHost == null ? "" : rawHost.trim();
        if (host.indexOf(':') >= 0 && !host.startsWith("[") && !host.endsWith("]")) {
            return "[" + host + "]";
        }
        return host.toLowerCase(Locale.ROOT);
    }
}
