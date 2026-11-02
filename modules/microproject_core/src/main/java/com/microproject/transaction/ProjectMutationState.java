/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongFunction;

import com.microproject.util.UiDispatch;

/** Transient per-project barrier and revision for mutations that publish snapshots. */
public final class ProjectMutationState {
	private static final ReentrantLock COORDINATOR = new ReentrantLock(true);
	private static final ThreadLocal<CoordinatorContext> CONTEXT = new ThreadLocal<>();
	/**
	 * How long the Swing event thread may wait for a background holder of the
	 * coordinator (for example recovery job construction). An unbounded wait on
	 * the EDT freezes Gantt/Resource interaction while Task Manager still shows
	 * normal CPU use (issue #946). Background threads keep an unbounded wait so
	 * they do not spuriously fail under load.
	 */
	static final long EDT_COORDINATOR_TIMEOUT_MILLIS = 5_000L;
	private final Deque<Scope> scopes = new ArrayDeque<>();
	private volatile long revision;

	/** Acquires the project mutation barrier. Scopes must be closed in LIFO order. */
	public Scope begin() {
		acquireCoordinator();
		if (CONTEXT.get() == null) CONTEXT.set(new CoordinatorContext());
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
		return COORDINATOR.isHeldByCurrentThread();
	}

	/** Returns whether the current thread holds the process-wide coordinator for any project. */
	public static boolean isCoordinatorHeldByCurrentThread() {
		return COORDINATOR.isHeldByCurrentThread();
	}

	/** Diagnostic probe: whether the coordinator can be obtained within the timeout. */
	public static boolean isCoordinatorAvailable(long timeoutMillis) throws InterruptedException {
		if (!COORDINATOR.tryLock(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)) return false;
		COORDINATOR.unlock();
		return true;
	}

	/** Runs a read or membership operation under the same process-wide coordination boundary. */
	public static <T> T coordinated(java.util.function.Supplier<T> operation) {
		acquireCoordinator();
		if (CONTEXT.get() == null) CONTEXT.set(new CoordinatorContext());
		try {
			return operation.get();
		} finally {
			releaseCoordinator();
		}
	}

	/**
	 * Acquires the process-wide coordinator. On the UI dispatch thread the wait is
	 * bounded so a stuck background holder cannot freeze interaction indefinitely.
	 */
	private static void acquireCoordinator() {
		if (COORDINATOR.isHeldByCurrentThread()) {
			COORDINATOR.lock();
			return;
		}
		if (!UiDispatch.isDispatchThread()) {
			COORDINATOR.lock();
			return;
		}
		try {
			if (!COORDINATOR.tryLock(EDT_COORDINATOR_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
				throw new ProjectMutationBusyException(
						"Model mutation coordinator was busy for more than "
								+ EDT_COORDINATOR_TIMEOUT_MILLIS + " ms on the UI thread");
			}
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new ProjectMutationBusyException(
					"Interrupted while waiting for the model mutation coordinator on the UI thread",
					interrupted);
		}
	}

	public static void coordinated(Runnable operation) {
		coordinated(() -> { operation.run(); return null; });
	}

	/** Defers observable work until every active coordinated mutation releases the barrier. */
	public static void publishAfterAllScopes(Runnable publication) {
		enqueuePublication(java.util.Objects.requireNonNull(publication, "publication"));
	}

	/** Whether a completed batch must wait for other nested project publications. */
	public static boolean shouldDeferPublication() {
		if (!COORDINATOR.isHeldByCurrentThread()) return false;
		CoordinatorContext context = CONTEXT.get();
		return COORDINATOR.getHoldCount() > 1 || context != null && !context.publications.isEmpty();
	}

	/** Advances an independent shared-state revision at most once per coordinated operation. */
	public static long coordinatedRevision(Object owner, java.util.function.LongSupplier advanceRevision) {
		return coordinatedRevision(owner, advanceRevision, ignored -> { });
	}

	public static long coordinatedRevision(Object owner, java.util.function.LongSupplier advanceRevision,
			java.util.function.LongConsumer restoreRevision) {
		if (!COORDINATOR.isHeldByCurrentThread())
			throw new IllegalStateException("A coordinated revision requires the mutation coordinator");
		CoordinatorContext context = CONTEXT.get();
		if (context == null)
			throw new IllegalStateException("A coordinated revision requires an active context");
		Object key = java.util.Objects.requireNonNull(owner, "owner");
		context.revisionRestorers.putIfAbsent(key, java.util.Objects.requireNonNull(restoreRevision, "restoreRevision"));
		return context.revisions.computeIfAbsent(key, ignored -> advanceRevision.getAsLong());
	}

	/** Checkpoint for a coordinated operation that may need to retract deferred work. */
	public static Checkpoint checkpoint() {
		if (!COORDINATOR.isHeldByCurrentThread() || CONTEXT.get() == null)
			throw new IllegalStateException("A transaction checkpoint requires the mutation coordinator");
		CoordinatorContext context = CONTEXT.get();
		return new Checkpoint(context.publications.size(), new java.util.IdentityHashMap<>(context.revisions));
	}

	/** Retracts publications and shared revisions recorded after the checkpoint. */
	public static void rollback(Checkpoint checkpoint) {
		if (!COORDINATOR.isHeldByCurrentThread() || CONTEXT.get() == null)
			throw new IllegalStateException("A transaction rollback requires the mutation coordinator");
		java.util.Objects.requireNonNull(checkpoint, "checkpoint");
		CoordinatorContext context = CONTEXT.get();
		if (checkpoint.publicationCount > context.publications.size())
			throw new IllegalStateException("Checkpoint is newer than the current publications");
		context.publications.subList(checkpoint.publicationCount, context.publications.size()).clear();
		java.util.IdentityHashMap<Object, Long> retained = new java.util.IdentityHashMap<>();
		for (var entry : context.revisions.entrySet()) {
			if (checkpoint.revisions.containsKey(entry.getKey())) retained.put(entry.getKey(), entry.getValue());
			else {
				var restore = context.revisionRestorers.get(entry.getKey());
				if (restore != null) restore.accept(entry.getValue());
			}
		}
		context.revisions.clear();
		context.revisions.putAll(retained);
	}

	/** Snapshot of deferred publications and shared revisions under one coordinator acquisition. */
	public static final class Checkpoint {
		private final int publicationCount;
		private final java.util.IdentityHashMap<Object, Long> revisions;
		private Checkpoint(int publicationCount, java.util.IdentityHashMap<Object, Long> revisions) {
			this.publicationCount = publicationCount;
			this.revisions = revisions;
		}
	}

	private long finish(Scope scope) {
		scope.validateActive();
		scopes.pop();
		if (scope.committed && scope.changed) {
			if (!scopes.isEmpty()) {
				// Nested commit: parent remains responsible for advancing revision.
			} else
				revision++;
		}
		long completedRevision = revision;
		Throwable failure = null;
		try {
			for (LongFunction<Runnable> publication : scope.publications) {
				try {
					Runnable deferred = publication.apply(completedRevision);
					if (deferred != null) enqueuePublication(deferred);
				} catch (Throwable current) {
					failure = combine(failure, current);
				}
			}
		} finally {
			scope.closed = true;
			try {
				releaseCoordinator();
			} catch (Throwable current) {
				failure = combine(failure, current);
			}
		}
		throwIfFailed(failure);
		return completedRevision;
	}

	private static Throwable combine(Throwable failure, Throwable current) {
		if (failure == null) return current;
		failure.addSuppressed(current);
		return failure;
	}

	private static void throwIfFailed(Throwable failure) {
		if (failure instanceof Error error) throw error;
		if (failure instanceof RuntimeException runtime) throw runtime;
		if (failure != null) throw new IllegalStateException("Coordinated mutation completion failed", failure);
	}

	private static void releaseCoordinator() {
		List<Runnable> publications = null;
		if (COORDINATOR.getHoldCount() == 1) {
			CoordinatorContext context = CONTEXT.get();
			if (context != null) publications = List.copyOf(context.publications);
			CONTEXT.remove();
		}
		COORDINATOR.unlock();
		if (publications == null) return;
		Throwable failure = null;
		for (Runnable publication : publications) {
			try { publication.run(); }
			catch (Throwable current) {
				if (failure == null) failure = current; else failure.addSuppressed(current);
			}
		}
		if (failure instanceof Error error) throw error;
		if (failure instanceof RuntimeException runtime) throw runtime;
		if (failure != null) throw new IllegalStateException("Coordinated publication failed", failure);
	}

	private static final class CoordinatorContext {
		private final List<Runnable> publications = new ArrayList<>();
		private final java.util.IdentityHashMap<Object, Long> revisions = new java.util.IdentityHashMap<>();
		private final java.util.IdentityHashMap<Object, java.util.function.LongConsumer> revisionRestorers = new java.util.IdentityHashMap<>();
	}

	private static void enqueuePublication(Runnable publication) {
		CoordinatorContext context = CONTEXT.get();
		if (context == null || !COORDINATOR.isHeldByCurrentThread())
			throw new IllegalStateException("A coordinated publication requires an active mutation scope");
		context.publications.add(publication);
	}

	/** One nested acquisition of the project mutation barrier. */
	public static final class Scope implements AutoCloseable {
		private final ProjectMutationState state;
		private final Thread owner;
		private boolean committed;
		private boolean changed;
		private boolean closed;
		private final List<LongFunction<Runnable>> publications = new ArrayList<>();

		private Scope(ProjectMutationState state, Thread owner) {
			this.state = state;
			this.owner = owner;
		}

		/** Validates that this scope can still commit and close in stack order. */
		public void validateActive() {
			if (closed || Thread.currentThread() != owner)
				throw new IllegalStateException("Project mutation scope is not active on this thread");
			if (state.scopes.peek() != this)
				throw new IllegalStateException("Project mutation scopes must close in LIFO order");
		}

		/** Marks this completed mutation as committed; no-op commits leave the revision unchanged. */
		public void commit(boolean changed) {
			validateActive();
			this.committed = true;
			this.changed = changed;
		}

		/** Registers work to run only after all nested project scopes release the coordinator. */
		public void publishAfterAllScopes(Runnable publication) {
			if (closed || Thread.currentThread() != owner)
				throw new IllegalStateException("Project mutation scope is not active on this thread");
			enqueuePublication(java.util.Objects.requireNonNull(publication, "publication"));
		}

		/** Builds a publication with this scope's exact resulting revision before any scope unlocks. */
		public void afterCommitRevision(LongFunction<Runnable> publication) {
			if (closed || Thread.currentThread() != owner)
				throw new IllegalStateException("Project mutation scope is not active on this thread");
			publications.add(java.util.Objects.requireNonNull(publication, "publication"));
		}

		/** Returns the revision this scope will expose when closed, without releasing the coordinator. */
		public long projectedRevision() {
			if (closed || Thread.currentThread() != owner)
				throw new IllegalStateException("Project mutation scope is not active on this thread");
			return state.scopes.size() == 1 && committed && changed ? state.revision + 1 : state.revision;
		}

		@Override public void close() {
			if (closed) return;
			state.finish(this);
		}

		/** Closes this scope and returns its exact revision before another writer can advance it. */
		public long closeAndGetRevision() {
			if (closed) throw new IllegalStateException("Project mutation scope is already closed");
			return state.finish(this);
		}
	}
}
