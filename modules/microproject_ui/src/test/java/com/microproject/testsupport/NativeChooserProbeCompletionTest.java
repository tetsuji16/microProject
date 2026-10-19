/*
 * Copyright (c) 2026 microProject
 * SPDX-License-Identifier: MIT
 */
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

class NativeChooserProbeCompletionTest {
	private static final Instant DEADLINE = Instant.parse("2026-10-15T16:25:52.257Z");
	private static final String TIMELY_READY = "nativeChooserFirstReadyObservedUtc=2026-10-15T16:25:52.110Z hwnd=0x1\n"
		+ "nativeChooserProbe completedUtc=2026-10-15T16:25:52.143Z reason=ready ready=True\n";
	private static final String LATE_READY = "nativeChooserFirstReadyObservedUtc=2026-10-15T16:25:52.300Z hwnd=0x1\n"
		+ "nativeChooserProbe completedUtc=2026-10-15T16:25:52.310Z reason=ready ready=True\n";
	private static final String TIMELY_READY_LATE_EXIT = "nativeChooserFirstReadyObservedUtc=2026-10-15T16:25:52.110Z hwnd=0x1\n"
		+ "nativeChooserProbe completedUtc=2026-10-15T16:25:52.500Z reason=ready ready=True\n";
	private static final String PARTIAL_READY = "nativeChooserFirstReadyObservedUtc=2026-10-15T16:25:52.110Z hwnd=0x1\n";
	private static final String READY_WITHOUT_STAGE_TIMESTAMP =
		"nativeChooserProbe completedUtc=2026-10-15T16:25:52.143Z reason=ready ready=True\n";

	@Test
	void acceptsReadinessObservedBeforeDeadlineWhenProbeExitIsDelayed() throws Exception {
		FakeProcess process = new FakeProcess(0, false, true);
		Supplier<String> output = () -> TIMELY_READY;
		var result = NativeChooserProbeCompletion.await(process, output, System.nanoTime() + TimeUnit.SECONDS.toNanos(1), DEADLINE,
			Duration.ofMillis(300), Duration.ofMillis(25));

		assertEquals(NativeChooserProbeCompletion.Status.COMPLETE, result.status());
		assertEquals(0, result.exitCode());
	}

	@Test
	void acceptsAuthoritativeReadyResultWhenStageSamplerMissedTheTransition() throws Exception {
		FakeProcess process = new FakeProcess(0, false, true);
		var result = NativeChooserProbeCompletion.await(process, () -> READY_WITHOUT_STAGE_TIMESTAMP,
			System.nanoTime() + TimeUnit.SECONDS.toNanos(1), DEADLINE,
			Duration.ofMillis(300), Duration.ofMillis(25));

		assertEquals(NativeChooserProbeCompletion.Status.COMPLETE, result.status());
		assertEquals(0, result.exitCode());
	}

	@Test
	void observerStartupDelayDoesNotConsumeTheChooserReadinessDeadline() throws Exception {
		FakeProcess preparation = new FakeProcess(0, false);
		Instant deadlineIfStartupWereIncluded = Instant.now().plusMillis(100);
		java.util.concurrent.atomic.AtomicInteger reads = new java.util.concurrent.atomic.AtomicInteger();
		NativeChooserProbeCompletion.awaitPrepared(preparation, () -> {
			if (reads.getAndIncrement() == 0) return "nativeChooserProbe processStartedUtc=2026-10-15T16:25:40Z\\n";
			try {
				Thread.sleep(200);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				throw new AssertionError(interrupted);
			}
			return "nativeChooserProbe preparedUtc=2026-10-15T16:25:45Z\\n";
		}, Duration.ofSeconds(1));

		FakeProcess readiness = new FakeProcess(0, true);
		Instant chooserStart = Instant.now();
		Instant chooserDeadlineUtc = chooserStart.plusMillis(100);
		Instant firstReady = chooserStart.plusMillis(10);
		Instant completed = chooserStart.plusMillis(20);
		assertTrue(firstReady.isAfter(deadlineIfStartupWereIncluded));
		assertFalse(firstReady.isAfter(chooserDeadlineUtc));
		String readyOutput = "nativeChooserFirstReadyObservedUtc=" + firstReady + " hwnd=0x1\\n"
			+ "nativeChooserProbe completedUtc=" + completed + " reason=ready ready=True\\n";
		var result = NativeChooserProbeCompletion.await(readiness, () -> readyOutput,
			System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100), chooserDeadlineUtc,
			Duration.ofMillis(300), Duration.ofMillis(25));

		assertEquals(NativeChooserProbeCompletion.Status.COMPLETE, result.status());
		assertEquals(0, result.exitCode());
	}

	@Test
	void drainsForCompletionWhenFirstReadyIsTimelyButSnapshotMarkerIsNotYetSerialized() throws Exception {
		FakeProcess process = new FakeProcess(0, true);
		Queue<String> outputs = new ArrayDeque<>();
		outputs.add(PARTIAL_READY);
		outputs.add(TIMELY_READY_LATE_EXIT);
		var result = NativeChooserProbeCompletion.await(process, outputs::remove,
			System.nanoTime() - 1, DEADLINE, Duration.ofMillis(300), Duration.ofMillis(25));

		assertEquals(NativeChooserProbeCompletion.Status.COMPLETE, result.status());
		assertEquals(0, result.exitCode());
	}

	@Test
	void rejectsReadinessFirstObservedAfterDeadlineEvenWhenProbeExits() throws Exception {
		FakeProcess process = new FakeProcess(0, false, true);
		var result = NativeChooserProbeCompletion.await(process, () -> LATE_READY,
			System.nanoTime() + TimeUnit.SECONDS.toNanos(1), DEADLINE,
			Duration.ofMillis(300), Duration.ofMillis(25));

		assertEquals(NativeChooserProbeCompletion.Status.NOT_READY_BY_DEADLINE, result.status());
	}

	@Test
	void acceptsTimelyReadinessEvenWhenProbeCompletionMarkerIsWrittenDuringBoundedDrain() throws Exception {
		FakeProcess process = new FakeProcess(0, false, true);
		var result = NativeChooserProbeCompletion.await(process, () -> TIMELY_READY_LATE_EXIT,
			System.nanoTime() + TimeUnit.SECONDS.toNanos(1), DEADLINE, Duration.ofMillis(300), Duration.ofMillis(25));

		assertEquals(NativeChooserProbeCompletion.Status.COMPLETE, result.status());
	}

	@Test
	void reportsIncompleteSerializationSeparatelyFromReadinessTimeout() throws Exception {
		FakeProcess process = new FakeProcess(0, false, false);
		var result = NativeChooserProbeCompletion.await(process, () -> TIMELY_READY,
			System.nanoTime() + TimeUnit.SECONDS.toNanos(1), DEADLINE,
			Duration.ofMillis(1), Duration.ofMillis(1));

		assertEquals(NativeChooserProbeCompletion.Status.READY_BUT_INCOMPLETE, result.status());
	}

	@Test
	void keepsDeadlineAndPartialOutputWithoutCompletedReadyMarkerAsNotReady() throws Exception {
		FakeProcess deadlineProcess = new FakeProcess(0, true);
		var deadlineResult = NativeChooserProbeCompletion.await(deadlineProcess,
			() -> "nativeChooserProbe completedUtc=2026-10-15T16:25:52.200Z reason=deadline ready=False\n",
			System.nanoTime() + TimeUnit.SECONDS.toNanos(1), DEADLINE,
			Duration.ofMillis(300), Duration.ofMillis(25));
		FakeProcess partialProcess = new FakeProcess(0, false, false, false);
		var partialResult = NativeChooserProbeCompletion.await(partialProcess, () -> PARTIAL_READY,
			System.nanoTime() + TimeUnit.SECONDS.toNanos(1), DEADLINE,
			Duration.ofMillis(1), Duration.ofMillis(1));

		assertEquals(NativeChooserProbeCompletion.Status.NOT_READY_BY_DEADLINE, deadlineResult.status());
		assertEquals(NativeChooserProbeCompletion.Status.READY_BUT_INCOMPLETE, partialResult.status());
	}

	@Test
	void keepsExitFailureDistinctEvenWhenReadyWasObserved() throws Exception {
		FakeProcess process = new FakeProcess(7, false, true);
		var result = NativeChooserProbeCompletion.await(process, () -> TIMELY_READY,
			System.nanoTime() + TimeUnit.SECONDS.toNanos(1), DEADLINE,
			Duration.ofMillis(300), Duration.ofMillis(25));

		assertEquals(NativeChooserProbeCompletion.Status.PROCESS_FAILED, result.status());
		assertEquals(7, result.exitCode());
	}

	private static final class FakeProcess extends Process {
		private final Queue<Boolean> waits = new ArrayDeque<>();
		private final int exitCode;
		private boolean exited;

		FakeProcess(int exitCode, boolean... waitResults) {
			this.exitCode = exitCode;
			for (boolean result : waitResults) waits.add(result);
		}

		@Override public boolean waitFor(long timeout, TimeUnit unit) {
			boolean result = !waits.isEmpty() && waits.remove();
			exited |= result;
			return result;
		}
		@Override public int exitValue() {
			if (!exited) throw new IllegalThreadStateException();
			return exitCode;
		}
		@Override public int waitFor() { exited = true; return exitCode; }
		@Override public boolean isAlive() { return !exited; }
		@Override public void destroy() { exited = true; }
		@Override public Process destroyForcibly() { exited = true; return this; }
		@Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
		@Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[0]); }
		@Override public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
	}
}
