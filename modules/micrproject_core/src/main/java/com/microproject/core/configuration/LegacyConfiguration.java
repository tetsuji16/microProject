/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.core.configuration;

import java.io.OutputStream;

import com.microproject.core.dictionary.Dictionary;

/**
 * Explicit boundary for the JAXB-based compatibility configuration engine.
 *
 * <p>The application configuration engine lives in
 * {@code com.microproject.configuration.Configuration}.  Keeping this
 * adapter name explicit prevents new core callers from accidentally importing
 * the wrong singleton while preserving the legacy JAXB API and data model.</p>
 */
@Deprecated(forRemoval = false)
public final class LegacyConfiguration {
    private static final LegacyConfiguration INSTANCE = new LegacyConfiguration(Configuration.getInstance());

    private final Configuration delegate;

    private LegacyConfiguration(Configuration delegate) {
        this.delegate = delegate;
    }

    public static LegacyConfiguration getInstance() {
        return INSTANCE;
    }

    public Dictionary getDictionary() {
        return delegate.getDictionary();
    }

    public void register(String file, Class<?>... classesToBeBound) {
        delegate.register(file, classesToBeBound);
    }

    public void load() {
        delegate.load();
    }

    public Object load(String resourceName, Class<?>... classesToBeBound) {
        return delegate.load(resourceName, classesToBeBound);
    }

    public void save(String resourceName, Object configuration, Class<?> configurationClass) {
        delegate.save(resourceName, configuration, configurationClass);
    }

    public void dump(Object object, OutputStream out) {
        Configuration.dump(object, out);
    }
}
