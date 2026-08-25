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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ModelTransactionTest {
	@Test
	void successfulTransactionUsesTheRequiredOrderingAndPublishesOnce() {
		List<String> phases = new ArrayList<>();
		DomainChangeJournal journal = new DomainChangeJournal();
		journal.subscribe(change -> phases.add("publish:" + change.domainRevision()));
		ModelTransaction<String> transaction = ModelTransaction.<String>builder()
				.validate(() -> { phases.add("validate"); return true; })
				.authorize(() -> { phases.add("authorize"); return true; })
				.captureRollback(() -> { phases.add("capture"); return () -> phases.add("rollback"); })
				.apply(() -> { phases.add("apply"); return ModelTransaction.Mutation.changed("value"); })
				.schedule(() -> phases.add("schedule"))
				.invariant(() -> { phases.add("invariant"); return true; })
				.commitUndo(value -> phases.add("undo"))
				.changes(value -> draft())
				.postCommit(value -> phases.add("external"))
				.build();

		ModelTransaction.Result<String> result = transaction.execute(journal);

		assertEquals(ModelTransaction.Status.COMMITTED, result.status());
		assertEquals(1L, result.changeSet().domainRevision());
		assertEquals(List.of("validate", "authorize", "capture", "apply", "schedule", "invariant",
				"undo", "publish:1", "external"), phases);
	}

	@Test
	void applyFailureRollsBackWithoutRevisionUndoOrExternalEffects() {
		AtomicInteger state = new AtomicInteger(7);
		AtomicBoolean undo = new AtomicBoolean();
		AtomicBoolean external = new AtomicBoolean();
		DomainChangeJournal journal = new DomainChangeJournal();
		ModelTransaction<Integer> transaction = ModelTransaction.<Integer>builder()
				.captureRollback(() -> {
					int before = state.get();
					return () -> state.set(before);
				})
				.apply(() -> {
					state.set(9);
					throw new IllegalStateException("apply failed");
				})
				.commitUndo(value -> undo.set(true))
				.changes(value -> draft())
				.postCommit(value -> external.set(true))
				.build();

		ModelTransaction.Result<Integer> result = transaction.execute(journal);

		assertEquals(ModelTransaction.Status.FAILED, result.status());
		assertEquals(7, state.get());
		assertEquals(0L, journal.revision());
		assertEquals(false, undo.get());
		assertEquals(false, external.get());
	}

	@Test
	void noOpDoesNotChangeRevisionOrCreateUndo() {
		AtomicBoolean undo = new AtomicBoolean();
		DomainChangeJournal journal = new DomainChangeJournal();
		ModelTransaction<String> transaction = ModelTransaction.<String>builder()
				.apply(() -> ModelTransaction.Mutation.noOp("same"))
				.commitUndo(value -> undo.set(true))
				.changes(value -> draft())
				.build();

		ModelTransaction.Result<String> result = transaction.execute(journal);

		assertEquals(ModelTransaction.Status.NO_OP, result.status());
		assertEquals(0L, journal.revision());
		assertEquals(false, undo.get());
		assertNull(result.changeSet());
		journal.recordLegacy(DomainChangeSet.Origin.LEGACY);
		assertEquals(1L, journal.revision(), "no-op must release legacy-event suppression");
	}

	@Test
	void legacyEventsRaisedDuringACommandDoNotDoubleCountItsRevision() {
		DomainChangeJournal journal = new DomainChangeJournal();
		ModelTransaction<String> transaction = ModelTransaction.<String>builder()
				.apply(() -> {
					journal.recordLegacy(DomainChangeSet.Origin.LEGACY);
					return ModelTransaction.Mutation.changed("value");
				})
				.changes(value -> draft())
				.build();

		ModelTransaction.Result<String> result = transaction.execute(journal);

		assertEquals(ModelTransaction.Status.COMMITTED, result.status());
		assertEquals(1L, journal.revision());
	}

	@Test
	void rollbackFailureEscalatesToRecoveryRequired() {
		AtomicBoolean recovery = new AtomicBoolean();
		ModelTransaction<String> transaction = ModelTransaction.<String>builder()
				.captureRollback(() -> () -> { throw new IllegalStateException("rollback failed"); })
				.apply(() -> { throw new IllegalStateException("apply failed"); })
				.changes(value -> draft())
				.onRecoveryFailure(failure -> recovery.set(true))
				.build();

		ModelTransaction.Result<String> result = transaction.execute(new DomainChangeJournal());

		assertEquals(ModelTransaction.Status.RECOVERY_REQUIRED, result.status());
		assertEquals(true, recovery.get());
		assertEquals(1, result.failure().getSuppressed().length);
	}

	@Test
	void noOpRollbackFailureIsAttemptedOnlyOnce() {
		AtomicInteger attempts = new AtomicInteger();
		ModelTransaction<String> transaction = ModelTransaction.<String>builder()
				.captureRollback(() -> () -> {
					attempts.incrementAndGet();
					throw new IllegalStateException("rollback failed");
				})
				.apply(() -> ModelTransaction.Mutation.noOp("same"))
				.changes(value -> draft())
				.build();

		ModelTransaction.Result<String> result = transaction.execute(new DomainChangeJournal());

		assertEquals(ModelTransaction.Status.RECOVERY_REQUIRED, result.status());
		assertEquals(1, attempts.get());
	}

	@Test
	void changeDraftFailureHappensBeforeUndoCommitAndRollsBack() {
		AtomicInteger state = new AtomicInteger(1);
		AtomicBoolean undo = new AtomicBoolean();
		DomainChangeJournal journal = new DomainChangeJournal();
		ModelTransaction<Integer> transaction = ModelTransaction.<Integer>builder()
				.captureRollback(() -> { int before = state.get(); return () -> state.set(before); })
				.apply(() -> { state.set(2); return ModelTransaction.Mutation.changed(2); })
				.commitUndo(value -> undo.set(true))
				.changes(value -> { throw new IllegalStateException("draft failed"); })
				.build();

		ModelTransaction.Result<Integer> result = transaction.execute(journal);

		assertEquals(ModelTransaction.Status.FAILED, result.status());
		assertEquals(1, state.get());
		assertEquals(false, undo.get());
		assertEquals(0L, journal.revision());
	}

	@Test
	void postCommitFailureDoesNotRollBackCommittedDomainUndoOrRevision() {
		AtomicInteger state = new AtomicInteger(1);
		AtomicBoolean rolledBack = new AtomicBoolean();
		AtomicBoolean undo = new AtomicBoolean();
		DomainChangeJournal journal = new DomainChangeJournal();
		ModelTransaction<Integer> transaction = ModelTransaction.<Integer>builder()
				.captureRollback(() -> () -> { rolledBack.set(true); state.set(1); })
				.apply(() -> { state.set(2); return ModelTransaction.Mutation.changed(2); })
				.commitUndo(value -> undo.set(true))
				.changes(value -> draft())
				.postCommit(value -> { throw new IllegalStateException("external failed"); })
				.build();

		ModelTransaction.Result<Integer> result = transaction.execute(journal);

		assertEquals(ModelTransaction.Status.COMMITTED, result.status());
		assertEquals(2, state.get());
		assertEquals(false, rolledBack.get());
		assertEquals(true, undo.get());
		assertEquals(1L, journal.revision());
		assertEquals("external failed", result.failure().getMessage());
	}

	@Test
	void failingJournalObserverCannotTurnACommitIntoARollback() {
		AtomicInteger state = new AtomicInteger(1);
		AtomicBoolean rolledBack = new AtomicBoolean();
		DomainChangeJournal journal = new DomainChangeJournal();
		journal.subscribe(change -> { throw new AssertionError("observer failed"); });
		ModelTransaction<Integer> transaction = ModelTransaction.<Integer>builder()
				.captureRollback(() -> () -> { rolledBack.set(true); state.set(1); })
				.apply(() -> { state.set(2); return ModelTransaction.Mutation.changed(2); })
				.changes(value -> draft())
				.build();

		ModelTransaction.Result<Integer> result = transaction.execute(journal);

		assertEquals(ModelTransaction.Status.COMMITTED, result.status());
		assertEquals(2, state.get());
		assertEquals(false, rolledBack.get());
		assertEquals(1L, journal.revision());
	}

	private static DomainChangeSet.Draft draft() {
		return new DomainChangeSet.Draft(UUID.randomUUID(), DomainChangeSet.Origin.COMMAND, Set.of(), Set.of(),
				DomainChangeSet.TopologyImpact.NONE, false, false);
	}
}
