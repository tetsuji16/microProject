/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import java.util.ArrayList;
import java.util.List;

/** Thread-safe ordered listener storage with stable dispatch snapshots. */
public final class ListenerRegistry<L> {
	private final Object lock = new Object();
	private final List<L> listeners = new ArrayList<>();

	public void add(L listener) {
		if (listener == null)
			return;
		synchronized (lock) {
			listeners.add(listener);
		}
	}

	/** Removes the most recently registered listener equal to {@code listener}. */
	public void remove(L listener) {
		if (listener == null)
			return;
		synchronized (lock) {
			int index = listeners.lastIndexOf(listener);
			if (index >= 0)
				listeners.remove(index);
		}
	}

	/** Returns an immutable registration-order snapshot for one event dispatch. */
	public List<L> snapshot() {
		synchronized (lock) {
			return List.copyOf(listeners);
		}
	}
}
