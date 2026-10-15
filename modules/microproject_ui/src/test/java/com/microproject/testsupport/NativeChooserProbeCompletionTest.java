package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		assertEquals(NativeChooserProbeCompletion.Status.NOT_READY_BY_DEADLINE, partialResult.status());
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
