/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Signals the only non-atomic edge of the MPOF transaction: the archive has
 * been replaced, but applying its validated operation plan to the already
 * open in-memory document failed.  The operation-state plan is intentionally
 * not committed in this case.  Retrying the same export re-reads the replaced
 * archive and applies the plan again, making recovery explicit instead of
 * silently claiming success.
 */
public final class MpoPartialApplyException extends IOException {
	private final Path archive;

	public MpoPartialApplyException(Path archive, Throwable cause) {
		super("MPOF archive was committed but the in-memory merge was not applied; retry the operation", cause);
		this.archive = Objects.requireNonNull(archive, "archive");
	}

	public Path archive() {
		return archive;
	}
}
