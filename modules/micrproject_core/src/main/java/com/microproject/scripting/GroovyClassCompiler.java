/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
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
