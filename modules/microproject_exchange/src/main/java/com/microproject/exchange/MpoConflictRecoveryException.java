/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.microproject.collaboration.OperationLog;

/** Reports a rejected concurrent MPOF save and names its durable recovery copy. */
public final class MpoConflictRecoveryException extends IOException {
	private static final long serialVersionUID = 1L;
	private final Path recoveryCopy;
	private final List<OperationLog.Conflict> conflicts;

	public MpoConflictRecoveryException(Path recoveryCopy, List<OperationLog.Conflict> conflicts) {
		this("Concurrent MPOF changes conflict. The shared archive was left untouched; a recovery copy was saved to ",
			recoveryCopy, conflicts, null);
	}

	private MpoConflictRecoveryException(String message, Path recoveryCopy, List<OperationLog.Conflict> conflicts,
			Throwable cause) {
		super(message + Objects.requireNonNull(recoveryCopy, "recoveryCopy"), cause);
		this.recoveryCopy = recoveryCopy;
		this.conflicts = List.copyOf(Objects.requireNonNull(conflicts, "conflicts"));
	}

	public static MpoConflictRecoveryException lockUnavailable(Path recoveryCopy, IOException cause) {
		return new MpoConflictRecoveryException(
				"Could not acquire the MPOF transaction lock. The shared archive was left untouched; local edits were saved to ",
				recoveryCopy, List.of(), cause);
	}

	public Path recoveryCopy() { return recoveryCopy; }
	public List<OperationLog.Conflict> conflicts() { return conflicts; }
}
