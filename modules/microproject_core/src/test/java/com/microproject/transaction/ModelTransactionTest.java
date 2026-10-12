/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ModelTransactionTest {
	@Test
	void schedulingAndInvariantFailuresRestoreStateAndSkipCommit() {
		for (ModelTransaction.Stage stage : new ModelTransaction.Stage[] {
				ModelTransaction.Stage.SCHEDULE, ModelTransaction.Stage.INVARIANT }) {
			Fixture fixture = new Fixture(stage);
			ModelTransaction.ModelTransactionException failure = assertThrows(
				ModelTransaction.ModelTransactionException.class,
				() -> ModelTransaction.execute(fixture));

			assertEquals(stage, failure.failedStage());
			assertEquals(4, fixture.value);
			assertEquals(0, fixture.commits);
		}
	}

	@Test
	void rollbackFailureRetainsOriginalAndRollbackCauses() {
		Fixture fixture = new Fixture(ModelTransaction.Stage.INVARIANT);
		fixture.failRestore = true;

		ModelTransaction.ModelTransactionException failure = assertThrows(
			ModelTransaction.ModelTransactionException.class,
			() -> ModelTransaction.execute(fixture));

		assertEquals(ModelTransaction.Stage.ROLLBACK, failure.stage());
		assertEquals(ModelTransaction.Stage.INVARIANT, failure.failedStage());
		assertSame(fixture.failure, failure.getCause());
		assertEquals(1, failure.getSuppressed().length);
	}

	@Test
	void commitFailureDoesNotAttemptUnsafeModelRollback() {
		Fixture fixture = new Fixture(null);
		fixture.failCommit = true;

		ModelTransaction.ModelTransactionException failure = assertThrows(
			ModelTransaction.ModelTransactionException.class,
			() -> ModelTransaction.execute(fixture));

		assertEquals(ModelTransaction.Stage.COMMIT, failure.failedStage());
		assertEquals(5, fixture.value, "publication failure is reported after model commit");
		assertEquals(1, fixture.commits);
	}

	@Test
	void fatalErrorsPropagateAfterRestoringCapturedState() {
		Fixture fixture = new Fixture(null);
		fixture.failure = new AssertionError("fatal apply failure");
		fixture.failApply = true;

		AssertionError failure = assertThrows(AssertionError.class,
			() -> ModelTransaction.execute(fixture));

		assertSame(fixture.failure, failure);
		assertEquals(4, fixture.value);
		assertEquals(0, fixture.commits);
	}

	@Test
	void fatalPublicationErrorsPropagateAndKeepCommittedState() {
		Fixture fixture = new Fixture(null);
		AssertionError fatal = new AssertionError("fatal publication failure");
		fixture.failure = fatal;
		fixture.failCommit = true;

		AssertionError thrown = assertThrows(AssertionError.class,
			() -> ModelTransaction.execute(fixture));

		assertSame(fatal, thrown);
		assertEquals(5, fixture.value);
		assertEquals(1, fixture.commits);
	}

	private static final class Fixture implements ModelTransaction.Steps<Integer, Integer> {
		private final ModelTransaction.Stage failingStage;
		private int value = 4;
		private int commits;
		private boolean failRestore;
		private boolean failCommit;
		private boolean failApply;
		private Throwable failure = new IllegalStateException("injected failure");

		Fixture(ModelTransaction.Stage failingStage) {
			this.failingStage = failingStage;
		}

		@Override public void validate() { }
		@Override public void authorize() { }
		@Override public Integer capture() { return value; }
		@Override public ModelTransaction.Mutation<Integer> apply() {
			value++;
			if (failApply) throw (Error) failure;
			return ModelTransaction.Mutation.changed(value);
		}
		@Override public void schedule() throws Exception {
			if (failingStage == ModelTransaction.Stage.SCHEDULE) throw (Exception) failure;
		}
		@Override public void checkInvariants() throws Exception {
			if (failingStage == ModelTransaction.Stage.INVARIANT) throw (Exception) failure;
		}
		@Override public void restore(Integer snapshot) {
			if (failRestore) throw new IllegalStateException("injected rollback failure");
			value = snapshot;
		}
		@Override public void commit(Integer result) {
			commits++;
			if (failCommit) {
				if (failure instanceof Error fatal) throw fatal;
				throw new IllegalStateException("injected publication failure");
			}
		}
	}
}
