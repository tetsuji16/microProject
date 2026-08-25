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
package com.microproject.transaction;

import java.util.Objects;
import java.util.IdentityHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Per-project revision source and committed-change publication point. */
public final class DomainChangeJournal {
	private final AtomicLong revision = new AtomicLong();
	private final Object writeLock = new Object();
	private final Object legacyEventLock = new Object();
	private final IdentityHashMap<Object, Boolean> recentLegacyEvents = new IdentityHashMap<>();
	private final Object[] legacyEventRing = new Object[256];
	private int legacyEventCursor;
	private final AtomicBoolean legacyBatchQueued = new AtomicBoolean();
	private final CopyOnWriteArrayList<Consumer<DomainChangeSet>> listeners = new CopyOnWriteArrayList<>();
	private final ThreadLocal<Integer> legacySuppressionDepth = ThreadLocal.withInitial(() -> Integer.valueOf(0));

	public long revision() {
		return revision.get();
	}

	public DomainChangeSet commit(DomainChangeSet.Draft draft) {
		return write(() -> commitLocked(draft));
	}

	private DomainChangeSet commitLocked(DomainChangeSet.Draft draft) {
		long next = revision.incrementAndGet();
		DomainChangeSet change = new DomainChangeSet(draft.transactionId(), next, draft.origin(), draft.affectedTasks(),
				draft.affectedFieldIds(), draft.topologyImpact(), draft.scheduleCascade(), draft.dependencyCascade());
		for (Consumer<DomainChangeSet> listener : listeners) {
			try {
				listener.accept(change);
			} catch (Throwable ignored) {
				// A diagnostic/UI listener cannot invalidate an already committed model change.
			}
		}
		return change;
	}

	public DomainChangeSet recordLegacy(DomainChangeSet.Origin origin) {
		return write(() -> legacySuppressionDepth.get().intValue() > 0 ? null
				: commitLocked(DomainChangeSet.Draft.fullInvalidation(origin)));
	}

	/**
	 * Records an event at most once even when the same event instance is delivered to
	 * several view caches. The bounded identity window avoids retaining legacy event
	 * graphs for the lifetime of the project.
	 */
	public DomainChangeSet recordLegacyOnce(DomainChangeSet.Origin origin, Object eventIdentity) {
		return write(() -> {
			if (legacySuppressionDepth.get().intValue() > 0) return null;
			if (eventIdentity == null) return commitLocked(DomainChangeSet.Draft.fullInvalidation(origin));
			synchronized (legacyEventLock) {
				if (recentLegacyEvents.containsKey(eventIdentity)) return null;
				Object expired = legacyEventRing[legacyEventCursor];
				if (expired != null) recentLegacyEvents.remove(expired);
				legacyEventRing[legacyEventCursor] = eventIdentity;
				legacyEventCursor = (legacyEventCursor + 1) % legacyEventRing.length;
				recentLegacyEvents.put(eventIdentity, Boolean.TRUE);
			}
			return commitLocked(DomainChangeSet.Draft.fullInvalidation(origin));
		});
	}

	/** Claims the single project-wide legacy revision flush for the current UI turn. */
	public boolean queueLegacyBatch() {
		return !legacyEventsSuppressed() && legacyBatchQueued.compareAndSet(false, true);
	}

	public boolean hasQueuedLegacyBatch() {
		return legacyBatchQueued.get();
	}

	/** Completes a batch claimed by {@link #queueLegacyBatch()}. */
	public DomainChangeSet flushLegacyBatch() {
		return write(() -> {
			if (!legacyBatchQueued.compareAndSet(true, false)
					|| legacySuppressionDepth.get().intValue() > 0) return null;
			return commitLocked(DomainChangeSet.Draft.fullInvalidation(DomainChangeSet.Origin.LEGACY));
		});
	}

	public Scope suppressLegacyEvents() {
		legacySuppressionDepth.set(Integer.valueOf(legacySuppressionDepth.get().intValue() + 1));
		return new Scope(this);
	}

	public boolean legacyEventsSuppressed() {
		return legacySuppressionDepth.get().intValue() > 0;
	}

	public AutoCloseable subscribe(Consumer<DomainChangeSet> listener) {
		if (listener == null)
			return () -> { };
		listeners.add(listener);
		return () -> listeners.remove(listener);
	}

	/** Serializes every command/undo/redo transition for this project. */
	public <T> T write(Supplier<T> transition) {
		Objects.requireNonNull(transition, "transition");
		synchronized (writeLock) {
			return transition.get();
		}
	}

	/** Reads a committed domain snapshot without overlapping a command transition. */
	public <T> T read(Supplier<T> snapshot) {
		Objects.requireNonNull(snapshot, "snapshot");
		synchronized (writeLock) {
			return snapshot.get();
		}
	}

	private void endSuppression() {
		int depth = legacySuppressionDepth.get().intValue() - 1;
		if (depth <= 0)
			legacySuppressionDepth.remove();
		else
			legacySuppressionDepth.set(Integer.valueOf(depth));
	}

	public static final class Scope implements AutoCloseable {
		private DomainChangeJournal owner;

		private Scope(DomainChangeJournal owner) {
			this.owner = owner;
		}

		@Override
		public void close() {
			if (owner == null)
				return;
			owner.endSuppression();
			owner = null;
		}
	}
}
