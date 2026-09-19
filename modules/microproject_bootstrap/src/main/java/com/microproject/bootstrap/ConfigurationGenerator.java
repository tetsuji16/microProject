/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.bootstrap;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

import org.update4j.Configuration;
import org.update4j.FileMetadata;

/** Generates the signed update4j manifest used by the release workflow. */
public final class ConfigurationGenerator {
    private ConfigurationGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Path appDir = requiredPath(args, "--app-dir");
        Path output = requiredPath(args, "--output");
        Path privateKeyFile = requiredPath(args, "--private-key");
        String version = requiredValue(args, "--version");
        String assetBase = requiredValue(args, "--asset-base");

        Path libDir = appDir;
        if (!Files.isDirectory(libDir)) {
            throw new IllegalArgumentException("Missing application directory: " + libDir);
        }
        List<FileMetadata.Reference> files = new ArrayList<>();
        try (var stream = Files.list(libDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        String assetName = "microProject-" + version + "-" + path.getFileName();
                        files.add(FileMetadata.readFrom(path)
                                .path(appDir.relativize(path))
                                .uri(assetBase.endsWith("/") ? assetBase + assetName : assetBase + "/" + assetName));
                    });
        }
        if (files.isEmpty()) throw new IllegalArgumentException("No application jars found in " + libDir);

        Configuration config = Configuration.builder()
                .basePath(appDir)
                .baseUri(assetBase)
                .signer(readPrivateKey(privateKeyFile))
                .files(files)
                .build();
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            config.write(writer);
        }
    }

    private static Path requiredPath(String[] args, String name) {
        return Path.of(requiredValue(args, name));
    }

    private static String requiredValue(String[] args, String name) {
        if (args != null) {
            for (int i = 0; i + 1 < args.length; i++) {
                if (name.equals(args[i])) return args[i + 1];
            }
        }
        throw new IllegalArgumentException("Missing argument: " + name);
    }

    private static PrivateKey readPrivateKey(Path file) throws Exception {
        String pem = Files.readString(file, StandardCharsets.UTF_8);
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}
