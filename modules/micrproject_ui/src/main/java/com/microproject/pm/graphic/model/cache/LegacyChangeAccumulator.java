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

import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import com.microproject.transaction.DomainChangeJournal;
import com.microproject.transaction.DomainChangeSet;

/** Coalesces command-external legacy model events into one revision per EDT turn. */
final class LegacyChangeAccumulator implements AutoCloseable {
	private final DomainChangeJournal journal;
	private final Consumer<Runnable> dispatcher;
	private volatile boolean closed;

	LegacyChangeAccumulator(DomainChangeJournal journal) {
		this(journal, SwingUtilities::invokeLater);
	}

	LegacyChangeAccumulator(DomainChangeJournal journal, Consumer<Runnable> dispatcher) {
		this.journal = Objects.requireNonNull(journal, "journal");
		this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
	}

	void record() {
		if (closed || !journal.queueLegacyBatch())
			return;
		dispatcher.accept(journal::flushLegacyBatch);
	}

	/** Records an already-applied off-EDT mutation before Swing publication is queued. */
	void recordImmediately(Object eventIdentity) {
		if (closed || journal.legacyEventsSuppressed()) return;
		journal.recordLegacyOnce(DomainChangeSet.Origin.LEGACY, eventIdentity);
	}

	boolean isPending() { return journal.hasQueuedLegacyBatch(); }
	void flushNowIfPending() {
		if (SwingUtilities.isEventDispatchThread())
			journal.flushLegacyBatch();
	}

	@Override
	public void close() {
		closed = true;
	}
}
