/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.bootstrap;

import org.update4j.Configuration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.prefs.Preferences;

/**
 * microProject auto-update bootstrap launcher (#338, plan A — update4j).
 *
 * <p>Entry point for the installed application. On start it loads a remote (or
 * local, for testing) update4j configuration that enumerates the application
 * files published as GitHub Releases assets, verifies the configuration
 * signature and each file's checksum against the bundled RSA public key, applies
 * any available update, and then launches the business application via
 * {@link Configuration#launch()}.
 *
 * <p>Because updates are applied to the install directory <em>before</em> the
 * business application is launched, the Windows file-lock problem (a running
 * exe/jar cannot be overwritten) is avoided: the update happens on the next
 * start, driven by this small bootstrap JVM.
 *
 * <p>If the configuration cannot be reached (offline / private network) or fails
 * signature verification, the bootstrap <em>never</em> blocks startup: it falls
 * back to launching the already-installed application exactly as it is. A machine
 * that cannot verify or fetch an update can still always run what it has.
 *
 * <p>Usage:
 * <pre>
 *   java -jar microproject-bootstrap.jar [config-uri] [--launch-only] [--force-check]
 * </pre>
 * <ul>
 *   <li>{@code config-uri} — override the default GitHub Releases feed URI
 *       ({@code file://...} for local/test).</li>
 *   <li>{@code --launch-only} — skip the update step, just launch the installed app.</li>
 *   <li>{@code --force-check} — ignore the 24h check-interval preference.</li>
 * </ul>
 */
public final class MicroProjectUpdater {

    /** Default update feed: signed configuration.xml published with each GitHub Release. */
    static final URI DEFAULT_CONFIG_URI = URI.create(
            "https://github.com/tetsuji16/ProjectLibre/releases/latest/download/configuration.xml");

    /** Preferences node holding the auto-update settings (mirrors the plan A key table). */
    private static final String PREFS_NODE = "com/microproject/bootstrap";
    private static final String KEY_CHECK_ENABLED = "checkEnabled";
    private static final String KEY_LAST_CHECK_MILLIS = "lastCheckMillis";
    private static final long CHECK_INTERVAL_MILLIS = 24L * 60 * 60 * 1000; // 24h

    private MicroProjectUpdater() {
    }

    public static void main(String[] args) throws IOException {
        URI configUri = DEFAULT_CONFIG_URI;
        boolean launchOnly = false;
        boolean forceCheck = false;
        for (String arg : args) {
            if ("--launch-only".equals(arg)) {
                launchOnly = true;
            } else if ("--force-check".equals(arg)) {
                forceCheck = true;
            } else if (!arg.isEmpty() && !arg.startsWith("-")) {
                configUri = URI.create(arg);
            }
        }

        PublicKey publicKey = loadBundledPublicKey();

        Configuration config = readConfiguration(configUri, publicKey);
        if (config == null) {
            // Unreachable or unverifiable feed: run the installed app as-is.
            System.err.println("update4j: configuration unavailable at " + configUri
                    + "; launching installed application without update.");
            launchInstalledApp();
            return;
        }

        if (!launchOnly && shouldCheckNow(forceCheck)) {
            try {
                applyUpdate(config, publicKey);
            } catch (Exception e) {
                // Verification/network failure must never prevent startup.
                System.err.println("update4j: update skipped (" + e + "); "
                        + "launching installed application.");
            } finally {
                recordCheckTime();
            }
        }

        config.launch();
    }

    /**
     * Reads and verifies the update configuration.
     *
     * @param uri       feed location ({@code file://} for local/test, {@code https://} for releases)
     * @param publicKey trusted verification key, or {@code null} when no key is
     *                  bundled (development builds) — in which case the
     *                  configuration is read <em>without</em> verification and the
     *                  caller must skip applying any update.
     * @return the configuration, or {@code null} if it cannot be read or fails
     *         signature verification (so the caller can fall back to a launch).
     */
    @SuppressWarnings("deprecation") // URI.toURL() is the simplest cross-scheme stream opener here
    static Configuration readConfiguration(URI uri, PublicKey publicKey) {
        try {
            Reader in;
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                if (!Files.exists(Paths.get(uri))) {
                    return null;
                }
                in = Files.newBufferedReader(Paths.get(uri), StandardCharsets.UTF_8);
            } else {
                in = new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8);
            }
            try (Reader r = in) {
                return publicKey != null
                        ? Configuration.read(r, publicKey)
                        : Configuration.read(r);
            }
        } catch (Exception e) {
            // IOException, signature mismatch, unreachable host, etc.
            System.err.println("update4j: failed to read configuration from " + uri + ": " + e);
            return null;
        }
    }

    /**
     * Applies an available update, verifying signatures/checksums when a trusted
     * key is available. No-op (returns {@code false}) when verification is
     * impossible.
     */
    static boolean applyUpdate(Configuration config, PublicKey publicKey) throws Exception {
        if (publicKey == null) {
            // Cannot trust downloaded files without a verification key.
            return false;
        }
        if (!config.requiresUpdate()) {
            return false;
        }
        return config.update(publicKey);
    }

    // ---- preferences / check interval -------------------------------------

    static boolean shouldCheckNow(boolean force) {
        if (force) {
            return true;
        }
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        if (!prefs.getBoolean(KEY_CHECK_ENABLED, true)) {
            return false;
        }
        long last = prefs.getLong(KEY_LAST_CHECK_MILLIS, 0L);
        return System.currentTimeMillis() - last >= CHECK_INTERVAL_MILLIS;
    }

    static void recordCheckTime() {
        try {
            Preferences.userRoot().node(PREFS_NODE)
                    .putLong(KEY_LAST_CHECK_MILLIS, System.currentTimeMillis());
        } catch (Exception ignored) {
            // Preferences are best-effort; never fail startup over them.
        }
    }

    // ---- key loading ------------------------------------------------------

    /**
     * Loads the bundled verification public key (PEM, RSA). Returns {@code null}
     * when no key resource is present (development builds that do not sign their
     * feed). Production builds MUST ship the public key matching the release
     * signing private key, otherwise updates are never applied.
     */
    static PublicKey loadBundledPublicKey() {
        try (var in = MicroProjectUpdater.class.getResourceAsStream(
                "/com/microproject/bootstrap/public.key")) {
            if (in == null) {
                return null;
            }
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem
                    .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            System.err.println("update4j: could not load bundled public key: " + e);
            return null;
        }
    }

    // ---- offline fallback -------------------------------------------------

    /**
     * Launches the already-installed application directly when no verified
     * configuration is available. Mirrors the classpath/entry point the packaged
     * application would use.
     */
    static void launchInstalledApp() {
        String javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("microproject.classpath", "lib/*");
        String mainClass = System.getProperty("microproject.mainClass",
                "com.microproject.main.Main");
        ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", classpath, mainClass);
        pb.inheritIO();
        try {
            pb.start().waitFor();
        } catch (Exception e) {
            System.err.println("update4j: fallback launch failed: " + e);
            System.exit(1);
        }
    }
}
