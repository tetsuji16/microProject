/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import com.microproject.port.PortRegistry;

/**
 * Service-provider hook used by format modules to contribute importer
 * factories without coupling the core session to concrete exchange classes.
 */
public interface FileImporterProvider {
	void register(ImporterRegistry registry);

	/** Optional typed application boundary; legacy providers need no change. */
	default void registerPorts(PortRegistry registry) {
	}
}
