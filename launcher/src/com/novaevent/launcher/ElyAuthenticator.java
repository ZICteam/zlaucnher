package com.novaevent.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public final class ElyAuthenticator {
    private static final URI AUTH_ENDPOINT = URI.create("https://authserver.ely.by/auth/authenticate");
    private static final URI REFRESH_ENDPOINT = URI.create("https://authserver.ely.by/auth/refresh");
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;

    public ElyAuthenticator(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public ElySession authenticate(String username, char[] password, String clientToken) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();
        JsonObject agent = new JsonObject();
        agent.addProperty("name", "Minecraft");
        agent.addProperty("version", 1);
        payload.add("agent", agent);
        payload.addProperty("username", username);
        payload.addProperty("password", new String(password));
        payload.addProperty("clientToken", normalizeClientToken(clientToken));
        payload.addProperty("requestUser", true);
        return sendSessionRequest(AUTH_ENDPOINT, payload, "authenticate");
    }

    public ElySession refresh(String accessToken, String clientToken) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();
        payload.addProperty("accessToken", accessToken);
        payload.addProperty("clientToken", normalizeClientToken(clientToken));
        payload.addProperty("requestUser", true);
        return sendSessionRequest(REFRESH_ENDPOINT, payload, "refresh");
    }

    private ElySession sendSessionRequest(URI endpoint, JsonObject payload, String action) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Ely.by " + action + " failed with HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
        if (json == null || !json.has("accessToken") || !json.has("selectedProfile")) {
            throw new IOException("Unexpected Ely.by " + action + " response.");
        }

        JsonObject selectedProfile = json.getAsJsonObject("selectedProfile");
        return new ElySession(
                json.get("accessToken").getAsString(),
                json.has("clientToken") ? json.get("clientToken").getAsString() : normalizeClientToken(payload.get("clientToken").getAsString()),
                selectedProfile.get("id").getAsString(),
                selectedProfile.get("name").getAsString(),
                false
        );
    }

    private String normalizeClientToken(String clientToken) {
        return clientToken != null && !clientToken.isBlank() ? clientToken : UUID.randomUUID().toString();
    }
}
