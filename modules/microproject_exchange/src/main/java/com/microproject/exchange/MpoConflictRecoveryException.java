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
import com.microproject.port.SaveFailureFeedback;
import com.microproject.strings.Messages;

/** Reports a rejected concurrent MPOF save and names its durable recovery copy. */
public final class MpoConflictRecoveryException extends IOException implements SaveFailureFeedback {
	private static final long serialVersionUID = 1L;
	private final Path recoveryCopy;
	private final List<OperationLog.Conflict> conflicts;
	private final boolean lockUnavailable;

	public MpoConflictRecoveryException(Path recoveryCopy, List<OperationLog.Conflict> conflicts) {
		this("Concurrent MPOF changes conflict. The shared archive was left untouched; a recovery copy was saved to ",
			recoveryCopy, conflicts, null, false);
	}

	private MpoConflictRecoveryException(String message, Path recoveryCopy, List<OperationLog.Conflict> conflicts,
			Throwable cause, boolean lockUnavailable) {
		super(message + Objects.requireNonNull(recoveryCopy, "recoveryCopy"), cause);
		this.recoveryCopy = recoveryCopy;
		this.conflicts = List.copyOf(Objects.requireNonNull(conflicts, "conflicts"));
		this.lockUnavailable = lockUnavailable;
	}

	public static MpoConflictRecoveryException lockUnavailable(Path recoveryCopy, IOException cause) {
		return new MpoConflictRecoveryException(
				"Could not acquire the MPOF transaction lock. The shared archive was left untouched; local edits were saved to ",
				recoveryCopy, List.of(), cause, true);
	}

	public Path recoveryCopy() { return recoveryCopy; }
	public List<OperationLog.Conflict> conflicts() { return conflicts; }
	@Override public String userFacingMessage() {
		String key = lockUnavailable ? "Message.saveLockRecovery" : "Message.saveConflictRecovery";
		return Messages.getString(key) + " " + recoveryCopy;
	}
}
