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
		super("Concurrent MPOF changes conflict. The shared archive was left untouched; a recovery copy was saved to "
				+ Objects.requireNonNull(recoveryCopy, "recoveryCopy"));
		this.recoveryCopy = recoveryCopy;
		this.conflicts = List.copyOf(Objects.requireNonNull(conflicts, "conflicts"));
	}

	public Path recoveryCopy() { return recoveryCopy; }
	public List<OperationLog.Conflict> conflicts() { return conflicts; }
}
