/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.transaction;

import java.util.Objects;

/**
 * Executes one in-memory model mutation with a captured rollback state.
 *
 * <p>The caller owns serialization (for example, a per-project write lock).
 * Validation and authorization run before state capture; failures during apply,
 * scheduling, or invariant checks restore that state before returning. The
 * commit callback is reached only after those stages succeed. A commit callback
 * must itself publish atomically because external effects cannot be undone by
 * restoring the model snapshot. If publication throws, the transaction reports
 * a COMMIT-stage failure and leaves the already-applied model in place.</p>
 */
public final class ModelTransaction {
	private ModelTransaction() {
	}

	public enum Stage {
		VALIDATE,
		AUTHORIZE,
		CAPTURE,
		APPLY,
		SCHEDULE,
		INVARIANT,
		COMMIT,
		ROLLBACK
	}

	/** A changed mutation and its typed result. */
	public record Mutation<R>(boolean changed, R value) {
		public static <R> Mutation<R> changed(R value) {
			return new Mutation<>(true, value);
		}

		public static <R> Mutation<R> unchanged(R value) {
			return new Mutation<>(false, value);
		}
	}

	/** Transaction callbacks. The snapshot must contain every mutable value the mutation can affect. */
	public interface Steps<S, R> {
		void validate() throws Exception;

		void authorize() throws Exception;

		S capture() throws Exception;

		Mutation<R> apply() throws Exception;

		void schedule() throws Exception;

		void checkInvariants() throws Exception;

		void restore(S snapshot) throws Exception;

		void commit(R result) throws Exception;
	}

	/** Runs the transaction in the documented order and returns the mutation result. */
	public static <S, R> Mutation<R> execute(Steps<S, R> steps) throws ModelTransactionException {
		Objects.requireNonNull(steps, "steps");
		run(Stage.VALIDATE, steps::validate);
		run(Stage.AUTHORIZE, steps::authorize);

		final S snapshot;
		try {
			snapshot = steps.capture();
		} catch (Throwable failure) {
			if (failure instanceof Error fatal)
				throw fatal;
			throw new ModelTransactionException(Stage.CAPTURE, failure, null);
		}

		Mutation<R> mutation;
		try {
			mutation = Objects.requireNonNull(steps.apply(), "apply result");
			if (!mutation.changed())
				return mutation;
			run(Stage.SCHEDULE, steps::schedule);
			run(Stage.INVARIANT, steps::checkInvariants);
		} catch (ModelTransactionException failure) {
			throw rollback(steps, snapshot, failure);
		} catch (Throwable failure) {
			throw rollback(steps, snapshot, new ModelTransactionException(Stage.APPLY, failure, null));
		}

		// Publication may already have reached listeners when a callback fails;
		// no generic snapshot restore can safely retract those external effects.
		try {
			steps.commit(mutation.value());
		} catch (Throwable failure) {
			if (failure instanceof Error fatal)
				throw fatal;
			throw new ModelTransactionException(Stage.COMMIT, failure, null);
		}
		return mutation;
	}

	private static void run(Stage stage, CheckedAction action) throws ModelTransactionException {
		try {
			action.run();
		} catch (Throwable failure) {
			if (failure instanceof Error fatal)
				throw fatal;
			throw new ModelTransactionException(stage, failure, null);
		}
	}

	private static <S, R> ModelTransactionException rollback(Steps<S, R> steps, S snapshot,
			ModelTransactionException failure) {
		if (failure.getCause() instanceof Error fatal) {
			try {
				steps.restore(snapshot);
			} catch (Throwable rollbackFailure) {
				fatal.addSuppressed(rollbackFailure);
			}
			throw fatal;
		}
		try {
			steps.restore(snapshot);
			return failure;
		} catch (Throwable rollbackFailure) {
			return new ModelTransactionException(Stage.ROLLBACK, failure.getCause(), rollbackFailure, failure.stage());
		}
	}

	@FunctionalInterface
	private interface CheckedAction {
		void run() throws Exception;
	}

	/** Identifies the failed stage. A rollback failure retains both causes. */
	public static final class ModelTransactionException extends Exception {
		private static final long serialVersionUID = 1L;
		private final Stage stage;
		private final Stage failedStage;

		private ModelTransactionException(Stage stage, Throwable cause, Throwable rollbackFailure) {
			this(stage, cause, rollbackFailure, stage);
		}

		private ModelTransactionException(Stage stage, Throwable cause, Throwable rollbackFailure, Stage failedStage) {
			super("Model transaction failed during " + (stage == Stage.ROLLBACK ? failedStage + " and rollback" : stage), cause);
			this.stage = stage;
			this.failedStage = failedStage;
			if (rollbackFailure != null)
				addSuppressed(rollbackFailure);
		}

		/** Returns ROLLBACK when restoration failed, otherwise the original failing stage. */
		public Stage stage() {
			return stage;
		}

		/** Returns the stage that first failed, even when rollback also failed. */
		public Stage failedStage() {
			return failedStage;
		}
	}
}
