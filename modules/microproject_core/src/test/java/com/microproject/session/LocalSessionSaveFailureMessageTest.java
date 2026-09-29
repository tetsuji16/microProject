/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.microproject.port.SaveFailureFeedback;
import com.microproject.strings.Messages;

class LocalSessionSaveFailureMessageTest {
	@Test
	void usesRecoveryFeedbackFromWrappedFailure() {
		IOException failure = new IOException("save failed", new RecoveryFailure());

		assertEquals("Recovery copy: C:/project/recovery.mpo",
				LocalSession.saveFailureMessage(failure));
	}

	@Test
	void usesGenericMessageWhenFailureHasNoRecoveryFeedback() {
		assertEquals(Messages.getString("Message.saveError"),
				LocalSession.saveFailureMessage(new IOException("save failed")));
	}

	private static final class RecoveryFailure extends IOException implements SaveFailureFeedback {
		@Override
		public String userFacingMessage() {
			return "Recovery copy: C:/project/recovery.mpo";
		}
	}
}
