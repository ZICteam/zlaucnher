package com.novaevent.launcher;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record ElySession(String accessToken, String clientToken, String uuid, String username, boolean offline) {
    public static ElySession offline(String username) {
        String normalized = username == null ? "" : username.trim();
        String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + normalized).getBytes(StandardCharsets.UTF_8))
                .toString()
                .replace("-", "");
        return new ElySession("offline-token", "", offlineUuid, normalized, true);
    }

    public boolean matchesUsername(String candidate) {
        return Objects.equals(username == null ? "" : username.trim(), candidate == null ? "" : candidate.trim());
    }
}
