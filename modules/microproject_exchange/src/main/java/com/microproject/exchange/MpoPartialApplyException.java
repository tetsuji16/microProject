/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import com.microproject.port.SaveFailureFeedback;
import com.microproject.strings.Messages;

/**
 * Signals the only non-atomic edge of the MPOF transaction: the archive has
 * been replaced, but the open in-memory document was edited during the save or
 * applying the validated operation plan to it failed.  Those edits are not in
 * the archive, and the document stays unsaved.  The operation-state plan is intentionally
 * not committed in this case.  Retrying the same export re-reads the replaced
 * archive and applies the plan again, making recovery explicit instead of
 * silently claiming success.
 */
public final class MpoPartialApplyException extends IOException implements SaveFailureFeedback {
	private final Path archive;

	public MpoPartialApplyException(Path archive, Throwable cause) {
		super("MPOF archive was replaced but the open project differs from it; save again", cause);
		this.archive = Objects.requireNonNull(archive, "archive");
	}

	public Path archive() {
		return archive;
	}

	@Override
	public String userFacingMessage() {
		return Messages.getString("Message.saveRetryRequired");
	}
}
