/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.core.configuration;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/** Verifies that the compatibility engine has an explicit, non-ambiguous entry point. */
class LegacyConfigurationBoundaryTest {
    @Test
    void legacyFacadeIsStableAndDistinctFromApplicationEngine() {
        assertSame(LegacyConfiguration.getInstance(), LegacyConfiguration.getInstance());
        assertNotSame(LegacyConfiguration.class, com.microproject.configuration.Configuration.class);
        assertSame(LegacyConfiguration.getInstance().getDictionary(),
                LegacyConfiguration.getInstance().getDictionary());
    }
}
