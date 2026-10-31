/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import com.microproject.transaction.ModelTransaction;
import com.microproject.transaction.ProjectMutationState;
import java.util.function.Function;

/**
 * The single capture / event-batch / {@link ModelTransaction} / rollback / Undo skeleton shared by
 * task-set mutations that roll back through {@link ProjectTaskSnapshot}.
 *
 * <p>The process-wide mutation barrier and the project event batch are always released, including
 * when the mutation, the commit or the publication throws (issue #946).</p>
 */
final class ProjectTaskMutation {
	private ProjectTaskMutation() { }

	/** Stage callbacks that differ between commands; everything else is the shared skeleton. */
	interface Operation {
		/** Throws when the project may not be mutated. */
		void authorize();
		/** Applies the command, throwing to trigger rollback. */
		void apply();
		/** Recalculates after {@link #apply()}; defaults to nothing. */
		default void schedule() { }
		/** Verifies post-conditions against the before-snapshot, throwing to trigger rollback. */
		default void checkInvariants(ProjectTaskSnapshot before) { }
	}

	/**
	 * Runs one mutation transactionally and returns its Undo/Redo change.
	 *
	 * @param capture snapshot factory, called for the before and after states
	 * @param rollbackSource event source used when a failed mutation is rolled back
	 * @param undoSource event source used by the returned Undo/Redo restores
	 * @param failureLabel prefix of the message of a staged model-transaction failure
	 */
	static ReversibleModelChange run(Project project, Function<Project, ProjectTaskSnapshot> capture,
			Object rollbackSource, Object undoSource, String failureLabel, Operation operation) {
		ProjectTaskSnapshot[] before = new ProjectTaskSnapshot[1];
		ProjectTaskSnapshot[] after = new ProjectTaskSnapshot[1];
		try (ProjectMutationState.Scope mutation = project.getMutationState().begin()) {
			project.beginEventBatch();
			try {
				ModelTransaction.execute(new ModelTransaction.Steps<ProjectTaskSnapshot, Integer>() {
					@Override public void validate() { }
					@Override public void authorize() { operation.authorize(); }
					@Override public ProjectTaskSnapshot capture() {
						before[0] = capture.apply(project);
						return before[0];
					}
					@Override public ModelTransaction.Mutation<Integer> apply() {
						operation.apply();
						return ModelTransaction.Mutation.changed(0);
					}
					@Override public void schedule() { operation.schedule(); }
					@Override public void checkInvariants() {
						operation.checkInvariants(before[0]);
						after[0] = capture.apply(project);
					}
					@Override public void restore(ProjectTaskSnapshot snapshot) { snapshot.restore(project, rollbackSource); }
					@Override public void commit(Integer count) { }
				});
				Project.PendingEventBatch pending = project.commitEventBatch(mutation, true);
				project.publishEventBatch(pending);
			} catch (ModelTransaction.ModelTransactionException failure) {
				rollbackEvents(project, failure);
				throw new IllegalStateException(failureLabel + " failed during " + failure.failedStage() + ": "
					+ (failure.getCause() == null ? "unknown" : failure.getCause().getMessage()), failure);
			} catch (RuntimeException | Error failure) {
				rollbackEvents(project, failure);
				throw failure;
			}
		}
		return ReversibleModelChange.changed(
			() -> restore(project, before[0], undoSource), () -> restore(project, after[0], undoSource));
	}

	private static void rollbackEvents(Project project, Throwable failure) {
		try { project.endEventBatch(false); }
		catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
	}

	private static void restore(Project project, ProjectTaskSnapshot snapshot, Object source) {
		try (ProjectMutationState.Scope mutation = project.getMutationState().begin()) {
			project.beginEventBatch();
			Project.PendingEventBatch pending;
			try {
				snapshot.restore(project, source);
				project.setDirty(true);
				pending = project.commitEventBatch(mutation, true);
			} catch (RuntimeException | Error failure) {
				rollbackEvents(project, failure);
				throw failure;
			}
			project.publishEventBatch(pending);
		}
	}
}
