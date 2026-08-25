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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Rollback-capable transaction with fixed phase ordering.
 * External effects run only after Undo and ChangeSet commit.
 */
public final class ModelTransaction<T> {
	public enum Status {
		COMMITTED,
		NO_OP,
		VALIDATION_FAILED,
		AUTHORIZATION_FAILED,
		FAILED,
		RECOVERY_REQUIRED
	}

	@FunctionalInterface
	public interface CheckedBooleanSupplier {
		boolean getAsBoolean() throws Exception;
	}

	@FunctionalInterface
	public interface CheckedSupplier<V> {
		V get() throws Exception;
	}

	@FunctionalInterface
	public interface CheckedRunnable {
		void run() throws Exception;
	}

	@FunctionalInterface
	public interface CheckedConsumer<V> {
		void accept(V value) throws Exception;
	}

	public record Mutation<V>(boolean changed, V value) {
		public static <V> Mutation<V> changed(V value) {
			return new Mutation<>(true, value);
		}

		public static <V> Mutation<V> noOp(V value) {
			return new Mutation<>(false, value);
		}
	}

	public record Result<V>(Status status, V value, DomainChangeSet changeSet, Throwable failure) {
		public boolean committed() {
			return status == Status.COMMITTED;
		}
	}

	private final CheckedBooleanSupplier validation;
	private final CheckedBooleanSupplier authorization;
	private final CheckedSupplier<CheckedRunnable> captureRollback;
	private final CheckedSupplier<Mutation<T>> apply;
	private final CheckedRunnable schedule;
	private final CheckedBooleanSupplier invariant;
	private final CheckedBooleanSupplier commitAuthorization;
	private final CheckedConsumer<T> commitUndo;
	private final Function<T, DomainChangeSet.Draft> changes;
	private final CheckedConsumer<T> postCommit;
	private final Consumer<Throwable> recoveryFailure;

	private ModelTransaction(Builder<T> builder) {
		validation = builder.validation;
		authorization = builder.authorization;
		captureRollback = builder.captureRollback;
		apply = Objects.requireNonNull(builder.apply, "apply");
		schedule = builder.schedule;
		invariant = builder.invariant;
		commitAuthorization = builder.commitAuthorization;
		commitUndo = builder.commitUndo;
		changes = Objects.requireNonNull(builder.changes, "changes");
		postCommit = builder.postCommit;
		recoveryFailure = builder.recoveryFailure;
	}

	public Result<T> execute(DomainChangeJournal journal) {
		Objects.requireNonNull(journal, "journal");
		return journal.write(() -> executeLocked(journal));
	}

	private Result<T> executeLocked(DomainChangeJournal journal) {
		CheckedRunnable rollback = () -> { };
		DomainChangeJournal.Scope legacySuppression = null;
		T value = null;
		boolean rollbackAttempted = false;
		try {
			if (!validation.getAsBoolean())
				return new Result<>(Status.VALIDATION_FAILED, null, null, null);
			if (!authorization.getAsBoolean())
				return new Result<>(Status.AUTHORIZATION_FAILED, null, null, null);
			legacySuppression = journal.suppressLegacyEvents();
			rollback = Objects.requireNonNullElseGet(captureRollback.get(), () -> () -> { });
			Mutation<T> mutation = Objects.requireNonNull(apply.get(), "apply result");
			value = mutation.value();
			if (!mutation.changed()) {
				rollbackAttempted = true;
				rollback.run();
				legacySuppression.close();
				legacySuppression = null;
				return new Result<>(Status.NO_OP, value, null, null);
			}
			schedule.run();
			if (!invariant.getAsBoolean())
				throw new IllegalStateException("transaction invariant failed");
			if (!commitAuthorization.getAsBoolean()) {
				rollbackAttempted = true;
				rollback.run();
				legacySuppression.close();
				legacySuppression = null;
				return new Result<>(Status.AUTHORIZATION_FAILED, value, null, null);
			}
			DomainChangeSet.Draft changeDraft = Objects.requireNonNull(changes.apply(value), "change draft");
			commitUndo.accept(value);
			DomainChangeSet change = journal.commit(changeDraft);
			legacySuppression.close();
			legacySuppression = null;
			try {
				postCommit.accept(value);
				return new Result<>(Status.COMMITTED, value, change, null);
			} catch (Throwable externalFailure) {
				// The domain, Undo and revision are already committed. A non-rollbackable
				// external effect must be retried/reported, never answered by creating a
				// second, contradictory rollback transition.
				return new Result<>(Status.COMMITTED, value, change, externalFailure);
			}
		} catch (Throwable failure) {
			Throwable rollbackFailure = rollbackAttempted ? failure : null;
			try {
				if (!rollbackAttempted) rollback.run();
				if (legacySuppression != null)
					legacySuppression.close();
				if (rollbackFailure != null) {
					recoveryFailure.accept(failure);
					return new Result<>(Status.RECOVERY_REQUIRED, value, null, failure);
				}
				return new Result<>(Status.FAILED, value, null, failure);
			} catch (Throwable recoveryError) {
				if (legacySuppression != null)
					legacySuppression.close();
				failure.addSuppressed(recoveryError);
				recoveryFailure.accept(failure);
				return new Result<>(Status.RECOVERY_REQUIRED, value, null, failure);
			}
		}
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	public static final class Builder<T> {
		private CheckedBooleanSupplier validation = () -> true;
		private CheckedBooleanSupplier authorization = () -> true;
		private CheckedSupplier<CheckedRunnable> captureRollback = () -> () -> { };
		private CheckedSupplier<Mutation<T>> apply;
		private CheckedRunnable schedule = () -> { };
		private CheckedBooleanSupplier invariant = () -> true;
		private CheckedBooleanSupplier commitAuthorization = () -> true;
		private CheckedConsumer<T> commitUndo = ignored -> { };
		private Function<T, DomainChangeSet.Draft> changes;
		private CheckedConsumer<T> postCommit = ignored -> { };
		private Consumer<Throwable> recoveryFailure = ignored -> { };

		public Builder<T> validate(CheckedBooleanSupplier step) { validation = step; return this; }
		public Builder<T> authorize(CheckedBooleanSupplier step) { authorization = step; return this; }
		public Builder<T> captureRollback(CheckedSupplier<CheckedRunnable> step) { captureRollback = step; return this; }
		public Builder<T> apply(CheckedSupplier<Mutation<T>> step) { apply = step; return this; }
		public Builder<T> schedule(CheckedRunnable step) { schedule = step; return this; }
		public Builder<T> invariant(CheckedBooleanSupplier step) { invariant = step; return this; }
		public Builder<T> commitAuthorization(CheckedBooleanSupplier step) { commitAuthorization = step; return this; }
		public Builder<T> commitUndo(CheckedConsumer<T> step) { commitUndo = step; return this; }
		public Builder<T> changes(Function<T, DomainChangeSet.Draft> step) { changes = step; return this; }
		public Builder<T> postCommit(CheckedConsumer<T> step) { postCommit = step; return this; }
		public Builder<T> onRecoveryFailure(Consumer<Throwable> step) { recoveryFailure = step; return this; }
		public ModelTransaction<T> build() { return new ModelTransaction<>(this); }
	}
}
