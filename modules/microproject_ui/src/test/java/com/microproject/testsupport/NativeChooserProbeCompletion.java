/*
 * Copyright (c) 2026 microProject
 * SPDX-License-Identifier: MIT
 */
package com.microproject.testsupport;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Applies the chooser readiness deadline independently from probe serialization and process exit. */
public final class NativeChooserProbeCompletion {
	private static final Pattern READY_OBSERVED = Pattern.compile("nativeChooserFirstReadyObservedUtc=([^\\s]+)");
	private static final Pattern PREPARED = Pattern.compile("nativeChooserProbe preparedUtc=([^\\s]+)");
	private static final Pattern COMPLETED_READY = Pattern.compile(
		"nativeChooserProbe completedUtc=([^\\s]+) reason=ready ready=True", Pattern.CASE_INSENSITIVE);

	public enum Status { COMPLETE, NOT_READY_BY_DEADLINE, READY_BUT_INCOMPLETE, PROCESS_FAILED }

	public record Result(Status status, String output, int exitCode) {}

	private NativeChooserProbeCompletion() {}

	/** Waits for PowerShell and the Win32 observer to initialize before a user command starts its SLA. */
	public static String awaitPrepared(Process process, Supplier<String> readOutput, Duration startupTimeout)
			throws IOException, InterruptedException {
		long deadline = System.nanoTime() + startupTimeout.toNanos();
		while (true) {
			String output = readOutput.get();
			if (PREPARED.matcher(output).find()) return output;
			if (!process.isAlive())
				throw new IOException("Native chooser observer exited before prepared handshake (exit="
					+ process.exitValue() + "); output=" + output);
			long remaining = deadline - System.nanoTime();
			if (remaining <= 0)
				throw new IOException("Native chooser observer did not prepare within " + startupTimeout
					+ "; output=" + readOutput.get());
			TimeUnit.NANOSECONDS.sleep(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(20)));
		}
	}

	public static Result await(Process process, Supplier<String> readOutput, long deadlineNanos, Instant deadlineUtc,
			Duration readinessDrain, Duration diagnosticDrain) throws IOException, InterruptedException {
		long remainingNanos = deadlineNanos - System.nanoTime();
		if (remainingNanos > 0 && process.waitFor(remainingNanos, TimeUnit.NANOSECONDS))
			return classify(process, readOutput.get(), deadlineUtc);

		String output = readOutput.get();
		if (firstReadyWasObservedBy(output, deadlineUtc)) {
			if (process.waitFor(readinessDrain.toNanos(), TimeUnit.NANOSECONDS))
				return classify(process, readOutput.get(), deadlineUtc);
			return new Result(Status.READY_BUT_INCOMPLETE, readOutput.get(), -1);
		}
		if (process.waitFor(diagnosticDrain.toNanos(), TimeUnit.NANOSECONDS))
			return classify(process, readOutput.get(), deadlineUtc);
		output = readOutput.get();
		if (firstReadyWasObservedBy(output, deadlineUtc)) {
			if (process.waitFor(readinessDrain.toNanos(), TimeUnit.NANOSECONDS))
				return classify(process, readOutput.get(), deadlineUtc);
			return new Result(Status.READY_BUT_INCOMPLETE, readOutput.get(), -1);
		}
		return new Result(Status.NOT_READY_BY_DEADLINE, output, -1);
	}

	private static Result classify(Process process, String output, Instant deadlineUtc) {
		int exitCode = process.exitValue();
		if (exitCode != 0) return new Result(Status.PROCESS_FAILED, output, exitCode);
		return new Result(completeReadyWasObservedBy(output, deadlineUtc) ? Status.COMPLETE : Status.NOT_READY_BY_DEADLINE,
			output, exitCode);
	}

	private static boolean firstReadyWasObservedBy(String output, Instant deadlineUtc) {
		Instant firstReady = timestamp(READY_OBSERVED, output);
		return firstReady != null && !firstReady.isAfter(deadlineUtc);
	}

	private static boolean completeReadyWasObservedBy(String output, Instant deadlineUtc) {
		return firstReadyWasObservedBy(output, deadlineUtc) && timestamp(COMPLETED_READY, output) != null;
	}

	private static Instant timestamp(Pattern pattern, String output) {
		Matcher matcher = pattern.matcher(output);
		if (!matcher.find()) return null;
		try {
			return Instant.parse(matcher.group(1));
		} catch (RuntimeException malformed) {
			return null;
		}
	}
}
