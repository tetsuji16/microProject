/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.update4j.Configuration;

class ConfigurationGeneratorTest {

    @Test
    void writesSignedManifestForJpackageApplicationJars(@TempDir Path temp) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        Path appDir = Files.createDirectories(temp.resolve("app"));
        Files.writeString(appDir.resolve("micrproject_core.jar"), "core", StandardCharsets.UTF_8);
        Files.writeString(appDir.resolve("micrproject_ui.jar"), "ui", StandardCharsets.UTF_8);
        Path privateKey = temp.resolve("private.pem");
        String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        Files.writeString(privateKey, privatePem, StandardCharsets.UTF_8);
        Path output = temp.resolve("release/configuration.xml");

        ConfigurationGenerator.main(new String[] {
                "--app-dir", appDir.toString(),
                "--output", output.toString(),
                "--private-key", privateKey.toString(),
                "--version", "0.0.23.999",
                "--asset-base", "https://example.test/release/"
        });

        Configuration configuration;
        try (var reader = Files.newBufferedReader(output, StandardCharsets.UTF_8)) {
            configuration = Configuration.read(reader, keyPair.getPublic());
        }
        assertNotNull(configuration);
        assertEquals(2, configuration.getFiles().size());
        assertEquals("micrproject_core.jar", configuration.getFiles().get(0).getPath().getFileName().toString());
        assertEquals("https://example.test/release/microProject-0.0.23.999-micrproject_core.jar",
                configuration.getFiles().get(0).getUri().toString());
    }
}
