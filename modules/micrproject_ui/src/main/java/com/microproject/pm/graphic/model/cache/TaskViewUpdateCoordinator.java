/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.BooleanSupplier;

import javax.swing.SwingUtilities;

/** Installs one latest snapshot before delivering any cache notifications. */
final class TaskViewUpdateCoordinator implements AutoCloseable {
	private final LongSupplier currentRevision;
	private final LongConsumer installer;
	private final Consumer<Runnable> dispatcher;
	private final BooleanSupplier mustDefer;
	private final List<Runnable> notifications = new ArrayList<>();
	private long requestedRevision;
	private boolean queued;
	private boolean closed;

	TaskViewUpdateCoordinator(LongSupplier currentRevision, LongConsumer installer) {
		this(currentRevision, installer, SwingUtilities::invokeLater, () -> false);
	}

	TaskViewUpdateCoordinator(LongSupplier currentRevision, LongConsumer installer, Consumer<Runnable> dispatcher) {
		this(currentRevision, installer, dispatcher, () -> false);
	}

	TaskViewUpdateCoordinator(LongSupplier currentRevision, LongConsumer installer, Consumer<Runnable> dispatcher,
			BooleanSupplier mustDefer) {
		this.currentRevision = Objects.requireNonNull(currentRevision, "currentRevision");
		this.installer = Objects.requireNonNull(installer, "installer");
		this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
		this.mustDefer = Objects.requireNonNull(mustDefer, "mustDefer");
	}

	void requestRevision(long revision) {
		boolean flushNow;
		synchronized (this) {
			if (closed) return;
			requestedRevision = Math.max(requestedRevision, revision);
			flushNow = SwingUtilities.isEventDispatchThread();
			if (!queued) {
				queued = true;
				if (!flushNow) dispatcher.accept(this::flush);
			}
		}
		if (flushNow) flush();
	}

	void afterInstall(Runnable notification) {
		boolean flushNow;
		synchronized (this) {
			if (closed) return;
			notifications.add(Objects.requireNonNull(notification, "notification"));
			flushNow = SwingUtilities.isEventDispatchThread() && !queued && !mustDefer.getAsBoolean();
			if (flushNow) queued = true;
			else queue();
		}
		if (flushNow) flush();
	}

	private void queue() {
		if (queued)
			return;
		queued = true;
		dispatcher.accept(this::flush);
	}

	private void flush() {
		List<Runnable> pending;
		long revision;
		synchronized (this) {
			if (!queued) return;
			if (closed) {
				queued = false;
				notifications.clear();
				return;
			}
			revision = Math.max(requestedRevision, currentRevision.getAsLong());
			requestedRevision = revision;
			pending = List.copyOf(notifications);
			notifications.clear();
			queued = false;
		}
		installer.accept(revision);
		for (Runnable notification : pending)
			notification.run();
	}

	@Override
	public synchronized void close() {
		closed = true;
		notifications.clear();
	}
}
