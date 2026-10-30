/*
 * MIT License
 * Copyright (c) 2026 microProject
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, subject to inclusion of this notice.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 */
package com.microproject.ui.diagnostics;

import java.awt.EventQueue;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.microproject.transaction.ProjectMutationState;

/** Reports one bounded thread dump per period in which the EDT or the model mutation coordinator is stuck. */
public final class EdtWatchdog implements AutoCloseable {
	private static final Logger LOG = Logger.getLogger(EdtWatchdog.class.getName());
	private static final int MAX_THREADS = 64;
	private static final int MAX_FRAMES = 32;
	private final long timeoutMillis;
	private final Thread monitor;
	private boolean closed;
	private boolean coordinatorReported;

	private EdtWatchdog(long timeoutMillis) {
		this.timeoutMillis = timeoutMillis;
		monitor = new Thread(this::watch, "microproject-edt-watchdog");
		monitor.setDaemon(true);
		monitor.start();
	}

	/** Starts monitoring; close the returned instance during application shutdown. */
	public static EdtWatchdog start() {
		return new EdtWatchdog(10_000);
	}

	static EdtWatchdog start(long timeoutMillis) {
		if (timeoutMillis <= 0) throw new IllegalArgumentException("timeout must be positive");
		return new EdtWatchdog(timeoutMillis);
	}

	private void watch() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				CountDownLatch response = new CountDownLatch(1);
				AtomicBoolean edtHoldsCoordinator = new AtomicBoolean();
				EventQueue.invokeLater(() -> {
					edtHoldsCoordinator.set(ProjectMutationState.isCoordinatorHeldByCurrentThread());
					response.countDown();
				});
				if (!response.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
					synchronized (this) {
						if (closed) return;
						// The response may have arrived immediately after the timeout.
						if (response.getCount() != 0) report("EDT has not responded");
					}
					// Do not flood logs while the same event remains blocked.
					response.await();
				} else {
					checkCoordinator(edtHoldsCoordinator.get());
				}
				Thread.sleep(Math.min(timeoutMillis, 1_000));
			}
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	/** A scope surviving between EDT events, or an unobtainable coordinator, stops model publication. */
	private void checkCoordinator(boolean edtHoldsCoordinator) throws InterruptedException {
		String problem = edtHoldsCoordinator ? "EDT holds the model mutation coordinator between events"
				: ProjectMutationState.isCoordinatorAvailable(timeoutMillis) ? null
				: "Model mutation coordinator is unavailable";
		if (problem == null) {
			coordinatorReported = false;
		} else if (!coordinatorReported) {
			coordinatorReported = true;
			synchronized (this) {
				if (!closed) report(problem);
			}
		}
	}

	private void report(String problem) {
		LOG.log(new WatchdogRecord(threadDump(problem)));
	}

	private static String threadDump(String problem) {
		StringBuilder dump = new StringBuilder(problem).append("; thread dump:\n");
		Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
		// Put the EDT first so it remains visible even when the thread count is large.
		stacks.entrySet().stream().sorted((a, b) -> Boolean.compare(
				isEdt(b.getKey()), isEdt(a.getKey())))
				.limit(MAX_THREADS).forEach(entry -> {
					Thread thread = entry.getKey();
					dump.append(isEdt(thread) ? "AWT-EventQueue" : "Thread")
							.append(" #").append(thread.threadId()).append(' ').append(thread.getState()).append('\n');
					StackTraceElement[] frames = entry.getValue();
					for (int i = 0; i < Math.min(frames.length, MAX_FRAMES); i++) {
						String frame = formatFrame(frames[i]);
						dump.append("  at ").append(frame, 0, Math.min(frame.length(), 500)).append('\n');
					}
					if (frames.length > MAX_FRAMES) dump.append("  ... truncated\n");
				});
		if (stacks.size() > MAX_THREADS) dump.append("... additional threads omitted\n");
		return dump.toString();
	}

	static String formatFrame(StackTraceElement frame) {
		return frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber();
	}

	/** True only for thread dumps produced by this watchdog, not arbitrary WARNING messages. */
	public static boolean isDiagnosticLogRecord(LogRecord record) {
		return record instanceof WatchdogRecord;
	}

	private static final class WatchdogRecord extends LogRecord {
		private static final long serialVersionUID = 1L;

		private WatchdogRecord(String dump) {
			super(Level.WARNING, dump);
			setLoggerName(LOG.getName());
			setSourceClassName(EdtWatchdog.class.getName());
			setSourceMethodName("watch");
		}
	}

	private static boolean isEdt(Thread thread) {
		return thread.getName().matches("AWT-EventQueue-[0-9]+");
	}

	@Override
	public synchronized void close() {
		closed = true;
		monitor.interrupt();
	}
}
