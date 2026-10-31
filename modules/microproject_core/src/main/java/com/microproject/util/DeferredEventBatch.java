/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Nested deferred-event state machine shared by the project event managers: batch depth, the
 * rollback-only flag and the ordered deferred list.
 *
 * <p>Every method is guarded by this object's monitor. No listener is invoked here; the owner
 * dispatches the {@link Detached} facts outside of the monitor, so the thread that publishes is the
 * one that calls the manager's {@code publishBatch}.</p>
 *
 * @param <E> deferred fact type kept while a batch is open
 */
public final class DeferredEventBatch<E> {
	private final List<E> deferred = new ArrayList<>();
	private final String noBatchMessage;
	private final String invalidCheckpointMessage;
	private int depth;
	private boolean rollbackOnly;

	/** The ordered facts of one completed outermost batch and whether they may be published. */
	public record Detached<F>(List<F> events, boolean publish) { }

	public DeferredEventBatch(String noBatchMessage, String invalidCheckpointMessage) {
		this.noBatchMessage = Objects.requireNonNull(noBatchMessage, "noBatchMessage");
		this.invalidCheckpointMessage = Objects.requireNonNull(invalidCheckpointMessage, "invalidCheckpointMessage");
	}

	public synchronized void begin() {
		depth++;
	}

	public synchronized int pendingCount() {
		return deferred.size();
	}

	/** Discards facts added after a checkpoint without affecting the enclosing batch. */
	public synchronized void discardAfter(int checkpoint) {
		if (checkpoint < 0 || checkpoint > deferred.size())
			throw new IllegalArgumentException(invalidCheckpointMessage);
		deferred.subList(checkpoint, deferred.size()).clear();
	}

	/** Whether a deferred fact at or after {@code start} matches. */
	public synchronized boolean anyFrom(int start, Predicate<? super E> match) {
		for (int index = Math.max(0, start); index < deferred.size(); index++)
			if (match.test(deferred.get(index))) return true;
		return false;
	}

	/** Records a fact when a batch is open; the fact is created while holding the monitor. */
	public synchronized boolean tryDefer(Supplier<? extends E> fact) {
		if (depth <= 0) return false;
		deferred.add(fact.get());
		return true;
	}

	/**
	 * Closes one batch level. Returns {@code null} for a nested level; for the outermost level it
	 * returns the facts, converted under the monitor, and resets the state machine.
	 */
	public synchronized <F> Detached<F> detach(boolean publish, Function<? super E, ? extends F> convert) {
		if (depth == 0) throw new IllegalStateException(noBatchMessage);
		if (!publish) rollbackOnly = true;
		if (--depth != 0) return null;
		List<F> events = new ArrayList<>(deferred.size());
		for (E fact : deferred) events.add(convert.apply(fact));
		deferred.clear();
		boolean publishable = !rollbackOnly;
		rollbackOnly = false;
		return new Detached<>(List.copyOf(events), publishable);
	}
}
