/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.port;

/** Service-provider contract for format implementations outside core. */
public interface SessionImporterProvider {
	void register(SessionImporterRegistry registry);

	/** Optional typed import/export registrations for the application boundary. */
	default void registerPorts(PortRegistry registry) {
	}
}
