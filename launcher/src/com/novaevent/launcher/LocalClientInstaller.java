package com.novaevent.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public final class LocalClientInstaller {
    private final Consumer<String> log;

    public LocalClientInstaller(Consumer<String> log) {
        this.log = log;
    }

    public void migrateLegacyClientIfNeeded(ClientProfile profile, Path projectDir, Path gameDir) throws IOException {
        Path expectedVersionJson = gameDir.resolve("versions")
                .resolve(profile.resolvedVersionId())
                .resolve(profile.resolvedVersionId() + ".json");
        if (Files.exists(expectedVersionJson)) {
            return;
        }

        Path legacyClientDir = projectDir.resolve("client");
        Path legacyVersionJson = legacyClientDir.resolve("versions")
                .resolve(profile.resolvedVersionId())
                .resolve(profile.resolvedVersionId() + ".json");
        if (!Files.exists(legacyVersionJson)) {
            return;
        }

        log.accept("Migrating existing local client into launcher/instances/" + profile.folderName);
        copyRecursively(legacyClientDir, gameDir);
        log.accept("Local client migration completed");
    }

    private void copyRecursively(Path sourceRoot, Path targetRoot) throws IOException {
        try (var stream = Files.walk(sourceRoot)) {
            for (Path source : stream.toList()) {
                Path relative = sourceRoot.relativize(source);
                Path target = targetRoot.resolve(relative);
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
