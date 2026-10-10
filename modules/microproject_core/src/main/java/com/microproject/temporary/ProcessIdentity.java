/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.temporary;

import java.lang.management.ManagementFactory;
import java.time.Instant;

/** Stable process identity for temporary-artifact ownership, including restricted runtimes. */
public record ProcessIdentity(long pid, Instant startedAt) {
	private static final ProcessIdentity CURRENT = new ProcessIdentity(ProcessHandle.current().pid(),
		ProcessHandle.current().info().startInstant().orElseGet(() ->
			Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime())));

	public static ProcessIdentity current() {
		return CURRENT;
	}
}
