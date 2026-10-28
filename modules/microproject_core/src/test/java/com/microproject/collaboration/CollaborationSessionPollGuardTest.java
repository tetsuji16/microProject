/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** Issue #946: one failed poll must not cancel collaboration heartbeats for the rest of the session. */
class CollaborationSessionPollGuardTest {
	@Test
	void failedPollIsContainedSoTheTimerKeepsRunning() throws Exception {
		AtomicInteger polls = new AtomicInteger();
		CountDownLatch thirdPoll = new CountDownLatch(3);
		Timer timer = new Timer("collaboration-poll-guard-test", true);
		try {
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override public void run() {
					CollaborationSession.runGuardedPoll(() -> {
						int poll = polls.incrementAndGet();
						thirdPoll.countDown();
						if (poll == 1)
							throw new IllegalStateException("transient metadata failure");
					});
				}
			}, 0L, 10L);
			assertTrue(thirdPoll.await(5, TimeUnit.SECONDS), "polling must continue after a failed poll");
		} finally {
			timer.cancel();
		}
		assertTrue(polls.get() >= 3);
	}

	@Test
	void ordinaryErrorsAreContainedButJvmErrorsPropagate() {
		assertDoesNotThrow(() -> CollaborationSession.runGuardedPoll(() -> {
			throw new AssertionError("non-fatal error");
		}));
		OutOfMemoryError fatal = new OutOfMemoryError("fatal");
		assertEquals(fatal, assertThrows(OutOfMemoryError.class,
			() -> CollaborationSession.runGuardedPoll(() -> { throw fatal; })));
	}
}
