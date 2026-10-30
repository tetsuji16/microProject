/*
 * MIT License
 * Copyright (c) 2026 microProject
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, subject to inclusion of this notice.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 */
package com.microproject.ui.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.microproject.transaction.ProjectMutationState;

class EdtWatchdogTest {
	@Test
	void reportsOneDumpForBlockedEdtAndStopsOnClose() throws Exception {
		Logger logger = Logger.getLogger(EdtWatchdog.class.getName());
		CountDownLatch report = new CountDownLatch(1);
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicInteger reports = new AtomicInteger();
		AtomicReference<String> dump = new AtomicReference<>();
		CountDownLatch releaseNamedThread = new CountDownLatch(1);
		Thread namedThread = new Thread(() -> {
			try { releaseNamedThread.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		}, "secret-C:\\private\\project.mpo");
		namedThread.start();
		Handler handler = new Handler() {
			@Override public void publish(LogRecord record) {
				if (record.getMessage().contains("thread dump")) {
					dump.set(record.getMessage());
					reports.incrementAndGet();
					report.countDown();
				}
			}
			@Override public void flush() { }
			@Override public void close() { }
		};
		logger.addHandler(handler);
		try {
			EventQueue.invokeLater(() -> {
				entered.countDown();
				try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			});
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			try (EdtWatchdog watchdog = EdtWatchdog.start(150)) {
				assertTrue(report.await(5, TimeUnit.SECONDS));
				assertTrue(dump.get().contains("AWT-EventQueue #"));
				assertTrue(dump.get().contains("Thread #" + namedThread.threadId()));
				assertFalse(dump.get().contains("secret-C:"));
				Thread.sleep(400);
				assertEquals(1, reports.get());
			}
			release.countDown();
			EventQueue.invokeAndWait(() -> { });
			Thread.sleep(350);
			assertEquals(1, reports.get());
		} finally {
			release.countDown();
			releaseNamedThread.countDown();
			namedThread.join(5000);
			logger.removeHandler(handler);
		}
	}

	@Test
	void reportsCoordinatorHeldByAnotherThreadOnce() throws Exception {
		assertCoordinatorReport("Model mutation coordinator is unavailable", false);
	}

	@Test
	void reportsCoordinatorLeakedByEdtOnce() throws Exception {
		assertCoordinatorReport("EDT holds the model mutation coordinator", true);
	}

	private static void assertCoordinatorReport(String expected, boolean onEdt) throws Exception {
		Logger logger = Logger.getLogger(EdtWatchdog.class.getName());
		CountDownLatch report = new CountDownLatch(1);
		AtomicInteger reports = new AtomicInteger();
		Handler handler = new Handler() {
			@Override public void publish(LogRecord record) {
				if (record.getMessage().contains(expected)) {
					reports.incrementAndGet();
					report.countDown();
				}
			}
			@Override public void flush() { }
			@Override public void close() { }
		};
		ProjectMutationState state = new ProjectMutationState();
		AtomicReference<ProjectMutationState.Scope> leaked = new AtomicReference<>();
		CountDownLatch held = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		Thread holder = new Thread(() -> {
			try (ProjectMutationState.Scope scope = state.begin()) {
				held.countDown();
				release.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "coordinator-holder");
		logger.addHandler(handler);
		try {
			if (onEdt) EventQueue.invokeAndWait(() -> leaked.set(state.begin()));
			else {
				holder.start();
				assertTrue(held.await(5, TimeUnit.SECONDS));
			}
			try (EdtWatchdog watchdog = EdtWatchdog.start(150)) {
				assertTrue(report.await(5, TimeUnit.SECONDS));
				Thread.sleep(500);
				assertEquals(1, reports.get());
			}
		} finally {
			release.countDown();
			if (onEdt) EventQueue.invokeAndWait(() -> { if (leaked.get() != null) leaked.get().close(); });
			else holder.join(5000);
			logger.removeHandler(handler);
		}
	}

	@Test
	void frameOmitsSensitiveFileName() {
		StackTraceElement frame = new StackTraceElement("example.Worker", "run",
				"C:\\private\\project.mpo", 42);
		assertEquals("example.Worker.run:42", EdtWatchdog.formatFrame(frame));
	}

	@Test
	void responsiveEdtDoesNotReport() throws Exception {
		Logger logger = Logger.getLogger(EdtWatchdog.class.getName());
		AtomicInteger reports = new AtomicInteger();
		Handler handler = new Handler() {
			@Override public void publish(LogRecord record) { reports.incrementAndGet(); }
			@Override public void flush() { }
			@Override public void close() { }
		};
		logger.addHandler(handler);
		try (EdtWatchdog watchdog = EdtWatchdog.start(250)) {
			EventQueue.invokeAndWait(() -> { });
			Thread.sleep(600);
			assertEquals(0, reports.get());
		} finally {
			logger.removeHandler(handler);
		}
	}
}
