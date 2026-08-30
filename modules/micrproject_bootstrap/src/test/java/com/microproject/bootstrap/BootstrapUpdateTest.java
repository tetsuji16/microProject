/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.bootstrap;

import org.update4j.Configuration;
import org.update4j.FileMetadata;
import org.update4j.service.Launcher;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless verification of the secure update4j bootstrap (#338, plan A).
 *
 * <p>These tests prove the security-critical behaviour without forking the real
 * application:
 * <ul>
 *   <li>a correctly signed feed is read and applied, and an outdated local file
 *       is replaced with the verified feed content;</li>
 *   <li>a feed whose signature was tampered with is rejected (null → safe
 *       offline fallback), so a bad actor cannot push unverified files;</li>
 *   <li>a feed whose declared checksum does not match the served file is
 *       rejected at update time;</li>
 *   <li>an unreachable feed yields null so startup never blocks;</li>
 *   <li>the 24h check-interval preference is honoured;</li>
 *   <li>the bundled public key loads as a usable RSA key.</li>
 * </ul>
 *
 * <p>The actual fork-into-the-real-app launch path is covered by manual
 * verification (the bootstrap runs the business application via
 * {@link Configuration#launch()} once updates are applied).
 */
class BootstrapUpdateTest {

    private static final String APP_FILE = "app.jar";

    private HttpServer server;

    @BeforeEach
    void resetRecorder() {
        RecordingLauncher.RAN.set(false);
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static KeyPair generateRsa() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    private HttpServer startServer(Path dir) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            Path file = dir.resolve(path.substring(1));
            if (!Files.exists(file) || Files.isDirectory(file)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] data = Files.readAllBytes(file);
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        });
        s.start();
        this.server = s;
        return s;
    }

    @Test
    void signedFeedIsReadAndOutOfDateFileIsUpdated(@TempDir Path temp) throws Exception {
        KeyPair kp = generateRsa();
        Path serverDir = temp.resolve("server");
        Path localDir = temp.resolve("local");
        Files.createDirectories(serverDir);
        Files.createDirectories(localDir);
        Files.writeString(serverDir.resolve(APP_FILE), "new content v2");
        Files.writeString(localDir.resolve(APP_FILE), "old content v1");

        Configuration config = Configuration.builder()
                .basePath(localDir)
                .baseUri(serverDir.toUri().toString())
                .signer(kp.getPrivate())
                .files(FileMetadata.streamDirectory(serverDir).toList())
                .build();
        try (Writer w = Files.newBufferedWriter(serverDir.resolve("configuration.xml"),
                StandardCharsets.UTF_8)) {
            config.write(w);
        }

        HttpServer s = startServer(serverDir);
        int port = s.getAddress().getPort();
        URI uri = URI.create("http://127.0.0.1:" + port + "/configuration.xml");

        Configuration loaded = MicroProjectUpdater.readConfiguration(uri, kp.getPublic());
        assertNotNull(loaded, "signed feed must load with the correct public key");
        assertEquals(1, loaded.getFiles().size());

        boolean changed = MicroProjectUpdater.applyUpdate(loaded, kp.getPublic());
        assertTrue(changed, "update should report a change when local content is outdated");
        assertEquals("new content v2", Files.readString(localDir.resolve(APP_FILE)));
    }

    @Test
    void tamperedSignatureIsRejectedForSafeFallback(@TempDir Path temp) throws Exception {
        KeyPair kp = generateRsa();
        Path serverDir = temp.resolve("server");
        Path localDir = temp.resolve("local");
        Files.createDirectories(serverDir);
        Files.createDirectories(localDir);
        Files.writeString(serverDir.resolve(APP_FILE), "content");
        Files.writeString(localDir.resolve(APP_FILE), "content");

        Configuration config = Configuration.builder()
                .basePath(localDir)
                .baseUri(serverDir.toUri().toString())
                .signer(kp.getPrivate())
                .files(FileMetadata.streamDirectory(serverDir).toList())
                .build();
        Path configFile = serverDir.resolve("configuration.xml");
        try (Writer w = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            config.write(w);
        }

        // Tamper with the configuration-level signature attribute.
        String xml = Files.readString(configFile, StandardCharsets.UTF_8);
        String tampered = xml.replaceFirst("signature=\"[^\"]*\"",
                "signature=\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\"");
        Files.writeString(configFile, tampered, StandardCharsets.UTF_8);

        HttpServer s = startServer(serverDir);
        int port = s.getAddress().getPort();
        URI uri = URI.create("http://127.0.0.1:" + port + "/configuration.xml");

        Configuration loaded = MicroProjectUpdater.readConfiguration(uri, kp.getPublic());
        assertNull(loaded,
                "a feed with a bad signature must be rejected (null) so the app still launches");
    }

    @Test
    void tamperedFileContentIsRejectedOnUpdate(@TempDir Path temp) throws Exception {
        KeyPair kp = generateRsa();
        Path serverDir = temp.resolve("server");
        Path localDir = temp.resolve("local");
        Files.createDirectories(serverDir);
        Files.createDirectories(localDir);
        Files.writeString(serverDir.resolve(APP_FILE), "good content");
        // Local copy is outdated so update4j actually downloads from the feed.
        Files.writeString(localDir.resolve(APP_FILE), "old v1");

        Configuration config = Configuration.builder()
                .basePath(localDir)
                .baseUri(serverDir.toUri().toString())
                .signer(kp.getPrivate())
                .files(FileMetadata.streamDirectory(serverDir).toList())
                .build();
        try (Writer w = Files.newBufferedWriter(serverDir.resolve("configuration.xml"),
                StandardCharsets.UTF_8)) {
            config.write(w);
        }

        // Swap the served file for tampered content; the signed manifest still
        // declares the good-content checksum, so update() must reject it.
        Files.writeString(serverDir.resolve(APP_FILE), "TAMPERED content");

        HttpServer s = startServer(serverDir);
        int port = s.getAddress().getPort();
        URI uri = URI.create("http://127.0.0.1:" + port + "/configuration.xml");

        Configuration loaded = MicroProjectUpdater.readConfiguration(uri, kp.getPublic());
        assertNotNull(loaded, "config signature is intact, so it still loads");
        boolean changed = MicroProjectUpdater.applyUpdate(loaded, kp.getPublic());
        assertFalse(changed,
                "update must refuse to apply a feed whose served file does not match the signed manifest");
        assertEquals("old v1", Files.readString(localDir.resolve(APP_FILE)),
                "the tampered bytes must never be written to the install directory");
    }

    @Test
    void unreachableFeedReturnsNullForSafeFallback() throws Exception {
        KeyPair kp = generateRsa();
        URI dead = URI.create("https://127.0.0.1:1/configuration.xml");
        Configuration loaded = MicroProjectUpdater.readConfiguration(dead, kp.getPublic());
        assertNull(loaded, "an unreachable feed must yield null so the app still launches");
    }

    @Test
    void missingLocalConfigReturnsNull() throws Exception {
        KeyPair kp = generateRsa();
        Path absent = Files.createTempDirectory("absent").resolve("configuration.xml");
        Configuration loaded = MicroProjectUpdater.readConfiguration(absent.toUri(), kp.getPublic());
        assertNull(loaded);
    }

    @Test
    void checkIntervalPreferenceIsHonoured() throws Exception {
        var prefs = java.util.prefs.Preferences.userRoot().node("com/microproject/bootstrap");
        prefs.putBoolean("checkEnabled", false);
        assertFalse(MicroProjectUpdater.shouldCheckNow(false),
                "disabled preference must suppress the check");
        prefs.putBoolean("checkEnabled", true);
        prefs.putLong("lastCheckMillis", System.currentTimeMillis());
        assertFalse(MicroProjectUpdater.shouldCheckNow(false),
                "a recent check must suppress within the 24h interval");
        assertTrue(MicroProjectUpdater.shouldCheckNow(true),
                "force-check must override the interval");
        // restore a clean state so the test does not leak preferences
        prefs.remove("checkEnabled");
        prefs.remove("lastCheckMillis");
    }

    @Test
    void bundledPublicKeyLoadsAsRsa() {
        PublicKey key = MicroProjectUpdater.loadBundledPublicKey();
        assertNotNull(key, "bundled public.key resource must load");
        assertEquals("RSA", key.getAlgorithm());
    }

    @Test
    void launchOnlyModeIsAnExplicitOfflinePath() {
        assertTrue(MicroProjectUpdater.isLaunchOnly(new String[] {"--launch-only"}),
                "launch-only invocation must be recognized without consulting a feed");
        assertFalse(MicroProjectUpdater.isLaunchOnly(new String[] {"--force-check"}),
                "other flags must not accidentally select offline launch mode");
        assertFalse(MicroProjectUpdater.isLaunchOnly(null),
                "null arguments must remain safe for a launcher wrapper");
    }

    @Test
    void launchInvokesConfiguredLauncherInProcess(@TempDir Path temp) throws Exception {
        KeyPair kp = generateRsa();
        Path dir = Files.createTempDirectory("launch");
        Files.writeString(dir.resolve(APP_FILE), "content");

        Configuration config = Configuration.builder()
                .basePath(dir)
                .baseUri(dir.toUri().toString())
                .signer(kp.getPrivate())
                .files(FileMetadata.streamDirectory(dir).toList())
                .build();

        Launcher recorder = new RecordingLauncher();
        config.launch(recorder);
        assertTrue(RecordingLauncher.RAN.get(),
                "the launch path must execute the supplied launcher");
    }
}
