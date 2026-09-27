/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class JobAlertDispatchTest {
	@Test
	void waitingDispatchReturnsTheDialogResultAfterRunningOnTheEdt() {
		AtomicBoolean ranOnEdt = new AtomicBoolean();

		String result = Job.dispatchAlert(() -> {
			ranOnEdt.set(SwingUtilities.isEventDispatchThread());
			return "accepted";
		}, true, "default");

		assertEquals("accepted", result);
		assertTrue(ranOnEdt.get());
	}

	@Test
	void nonWaitingDispatchReturnsTheDefaultAndStillSchedulesOnTheEdt() throws InterruptedException {
		CountDownLatch dispatched = new CountDownLatch(1);
		AtomicBoolean ranOnEdt = new AtomicBoolean();

		String result = Job.dispatchAlert(() -> {
			ranOnEdt.set(SwingUtilities.isEventDispatchThread());
			dispatched.countDown();
			return "later";
		}, false, "default");

		assertEquals("default", result);
		assertTrue(dispatched.await(2, TimeUnit.SECONDS));
		assertTrue(ranOnEdt.get());
		assertFalse(SwingUtilities.isEventDispatchThread());
	}
}
