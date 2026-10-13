/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.transaction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;

/** Transient per-project barrier and revision for mutations that publish snapshots. */
public final class ProjectMutationState {
	private final ReentrantLock barrier = new ReentrantLock(true);
	private final Deque<Scope> scopes = new ArrayDeque<>();
	private volatile long revision;

	/** Acquires the project mutation barrier. Scopes must be closed in LIFO order. */
	public Scope begin() {
		barrier.lock();
		Scope scope = new Scope(this, Thread.currentThread());
		scopes.push(scope);
		return scope;
	}

	/** Returns the latest committed revision for this project. */
	public long revision() {
		return revision;
	}

	/** Returns whether the current thread already owns a project mutation scope. */
	public boolean isHeldByCurrentThread() {
		return barrier.isHeldByCurrentThread();
	}

	private long finish(Scope scope) {
		if (Thread.currentThread() != scope.owner)
			throw new IllegalStateException("Project mutation scope must close on its owning thread");
		if (scopes.peek() != scope)
			throw new IllegalStateException("Project mutation scopes must close in LIFO order");
		scopes.pop();
		Scope parent = scopes.peek();
		if (scope.committed && scope.changed) {
			if (parent != null)
				parent.changed = true;
			else
				revision++;
		}
		long completedRevision = revision;
		barrier.unlock();
		return completedRevision;
	}

	public static final class Scope implements AutoCloseable {
		private final ProjectMutationState state;
		private final Thread owner;
		private boolean committed;
		private boolean changed;
		private boolean closed;

		private Scope(ProjectMutationState state, Thread owner) {
			this.state = state;
			this.owner = owner;
		}

		/** Marks this completed mutation as committed; no-op commits leave the revision unchanged. */
		public void commit(boolean changed) {
			if (closed || Thread.currentThread() != owner)
				throw new IllegalStateException("Project mutation scope is not active on this thread");
			this.committed = true;
			this.changed = changed;
		}

		@Override public void close() {
			if (closed) return;
			state.finish(this);
			closed = true;
		}

		/** Closes this scope and returns its exact revision before another writer can advance it. */
		public long closeAndGetRevision() {
			if (closed) throw new IllegalStateException("Project mutation scope is already closed");
			long completedRevision = state.finish(this);
			closed = true;
			return completedRevision;
		}
	}
}
