/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.io.IOException;

/**
 * Central compatibility policy for the MPOF container version.
 *
 * <p>Minor revisions are additive and remain readable inside a major version.
 * A future incompatible revision adds its major version here, together with its
 * reader/migration, rather than scattering version checks through the importer.
 */
final class MpoFormatVersion {
	static final MpoFormatVersion CURRENT = new MpoFormatVersion(1, 0);
	private final int major;
	private final int minor;

	private MpoFormatVersion(int major, int minor) {
		this.major = major;
		this.minor = minor;
	}

	static MpoFormatVersion parse(String value) throws IOException {
		if (value == null || !value.matches("[0-9]{1,4}\\.[0-9]{1,4}"))
			throw new IOException("Invalid MPOF formatVersion: " + value);
		String[] parts = value.split("\\.", -1);
		return new MpoFormatVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
	}

	boolean isReadableBy(MpoFormatVersion reader) {
		return major == reader.major;
	}

	int major() { return major; }

	@Override public String toString() { return major + "." + minor; }
}
