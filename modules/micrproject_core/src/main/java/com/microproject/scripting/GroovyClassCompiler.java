package com.microproject.scripting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.Map;

import groovy.lang.GroovyClassLoader;

/** Shared, type-safe boundary for ProjectLibre's generated Groovy classes. */
public final class GroovyClassCompiler {
    private static final int MAX_CACHED_CLASSES = 128;
    private static final Map<String, CompiledClass> CLASSES_BY_SOURCE =
        new LinkedHashMap<>(16, 0.75F, true);

    private GroovyClassCompiler() {
    }

    public static String scriptClassName(String prefix, String definition) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(definition.getBytes(StandardCharsets.UTF_8));
            return prefix + HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
        }
    }

    public static synchronized <T> Class<? extends T> compile(String source, Class<T> expectedType) {
        CompiledClass cached = CLASSES_BY_SOURCE.get(source);
        if (cached != null)
            return cached.type.asSubclass(expectedType);

        GroovyClassLoader loader = new GroovyClassLoader(GroovyClassCompiler.class.getClassLoader());
        try {
            Class<?> compiled = loader.parseClass(source);
            Class<? extends T> typed = compiled.asSubclass(expectedType);
            CLASSES_BY_SOURCE.put(source, new CompiledClass(loader, compiled));
            evictOldestClasses();
            return typed;
        } catch (RuntimeException | Error e) {
            close(loader);
            throw e;
        }
    }

    public static <T> T compileAndInstantiate(String source, Class<T> expectedType)
            throws ReflectiveOperationException {
        return compile(source, expectedType).getDeclaredConstructor().newInstance();
    }

    static synchronized void clearCache() {
        for (CompiledClass compiled : CLASSES_BY_SOURCE.values())
            close(compiled.loader);
        CLASSES_BY_SOURCE.clear();
    }

    static synchronized int cachedClassCount() {
        return CLASSES_BY_SOURCE.size();
    }

    private static void evictOldestClasses() {
        while (CLASSES_BY_SOURCE.size() > MAX_CACHED_CLASSES) {
            Map.Entry<String, CompiledClass> oldest = CLASSES_BY_SOURCE.entrySet().iterator().next();
            CLASSES_BY_SOURCE.remove(oldest.getKey());
            close(oldest.getValue().loader);
        }
    }

    private static void close(GroovyClassLoader loader) {
        loader.clearCache();
        try {
            loader.close();
        } catch (IOException ignored) {
            // Groovy class loaders do not normally own closeable resources.
        }
    }

    private static final class CompiledClass {
        private final GroovyClassLoader loader;
        private final Class<?> type;

        private CompiledClass(GroovyClassLoader loader, Class<?> type) {
            this.loader = loader;
            this.type = type;
        }
    }
}
