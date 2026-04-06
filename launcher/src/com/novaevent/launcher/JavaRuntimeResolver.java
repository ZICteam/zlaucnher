package com.novaevent.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JavaRuntimeResolver {
    private JavaRuntimeResolver() {
    }

    public static String resolve(Path projectDir, LauncherConfig config, ClientProfile profile) {
        String runtime = profile != null && profile.javaRuntime != null ? profile.javaRuntime.trim().toLowerCase() : "auto";

        if ("java17".equals(runtime)) {
            String bundled = resolveBundled(projectDir, "java-17");
            if (bundled != null) {
                return bundled;
            }
        } else if ("java21".equals(runtime)) {
            String bundled = resolveBundled(projectDir, "java-21");
            if (bundled != null) {
                return bundled;
            }
        } else if ("java25".equals(runtime)) {
            String bundled = resolveBundled(projectDir, "java-25");
            if (bundled != null) {
                return bundled;
            }
        } else if ("custom".equals(runtime)) {
            if (config.javaPath != null && !config.javaPath.isBlank()) {
                return config.javaPath;
            }
        } else {
            String bundled = resolveBundled(projectDir, selectAutoRuntime(profile));
            if (bundled != null) {
                return bundled;
            }
        }

        if (config.javaPath != null && !config.javaPath.isBlank()) {
            return config.javaPath;
        }

        Path javaBinary = Path.of(System.getProperty("java.home"), "bin", executableName("java"));
        if (Files.exists(javaBinary)) {
            return javaBinary.toAbsolutePath().toString();
        }

        return executableName("java");
    }

    public static String resolveBundled(Path projectDir, String runtimeFolder) {
        Path root = projectDir.resolve("launcher").resolve("runtime").resolve(runtimeFolder);
        for (Path candidate : bundledCandidates(root)) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return null;
    }

    private static String selectAutoRuntime(ClientProfile profile) {
        if (profile != null && profile.minecraftVersion != null) {
            if (profile.minecraftVersion.startsWith("1.20.5")
                    || profile.minecraftVersion.startsWith("1.20.6")
                    || profile.minecraftVersion.startsWith("1.21")) {
                return "java-21";
            }
        }
        return "java-17";
    }

    private static List<Path> bundledCandidates(Path root) {
        List<Path> candidates = new ArrayList<>();
        candidates.add(root.resolve("bin").resolve(executableName("java")));
        candidates.add(root.resolve("Home").resolve("bin").resolve(executableName("java")));
        candidates.add(root.resolve("Contents").resolve("Home").resolve("bin").resolve(executableName("java")));
        return candidates;
    }

    private static String executableName(String baseName) {
        return isWindows() ? baseName + ".exe" : baseName;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
