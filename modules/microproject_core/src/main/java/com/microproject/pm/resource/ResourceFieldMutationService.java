/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.microproject.field.FieldParseException;
import com.microproject.pm.task.Project;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.undo.EditSupport;

/** Coordinates published resource-field edits across every project using the resource pool. */
public final class ResourceFieldMutationService {
	private static final ResourceFieldMutationService INSTANCE = new ResourceFieldMutationService();

	private ResourceFieldMutationService() { }

	public static ResourceFieldMutationService getInstance() { return INSTANCE; }

	/** Applies the existing node-model field/Undo operation inside the resource's publication boundary. */
	public void execute(ResourceImpl resource, EditSupport undoSupport, Supplier<Object> valueReader,
		FieldMutation mutation, Object eventSource) throws FieldParseException {
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(valueReader, "valueReader");
		Objects.requireNonNull(mutation, "mutation");
		try {
			ProjectMutationState.coordinated(() -> {
				EditSupport.Activation undoActivation = undoSupport == null ? null : undoSupport.beginCommand();
				EditSupport.Group undoGroup = undoActivation == null ? null : undoActivation.group();
				EditSupport.Checkpoint undoCheckpoint = undoActivation == null ? null
					: undoSupport.checkpoint(undoGroup);
				AtomicBoolean applied = new AtomicBoolean();
				try {
					executeCoordinated(resource, valueReader, mutation, eventSource, applied);
				} catch (FieldParseException failure) {
					throw new FieldParseRuntime(failure);
				} finally {
					if (undoActivation != null) {
						try {
							if (applied.get()) undoSupport.reserveCommit(undoGroup);
							else undoSupport.discardSince(undoGroup, undoCheckpoint);
						} finally {
							undoActivation.close();
							ProjectMutationState.publishAfterAllScopes(
								() -> undoSupport.completePublication(undoGroup));
						}
					}
				}
			});
		} catch (FieldParseRuntime failure) {
			throw (FieldParseException) failure.getCause();
		}
	}

	private void executeCoordinated(ResourceImpl resource, Supplier<Object> valueReader,
		FieldMutation mutation, Object eventSource, AtomicBoolean applied) throws FieldParseException {
		ResourcePool pool = resource.getResourcePool();
		if (pool == null) {
			Object before = valueReader.get();
			try {
				mutation.apply();
			} finally {
				if (!Objects.deepEquals(before, valueReader.get())) applied.set(true);
			}
			return;
		}
		List<Project> projects = identityList(pool.getProjects());
		Object before = valueReader.get();
		if (projects.isEmpty()) {
			try {
				executePoolOnly(pool, resource, before, valueReader, mutation, eventSource, applied);
			} catch (FieldParseRuntime failure) {
				throw (FieldParseException) failure.getCause();
			}
			return;
		}
		List<Project.AdditionalObjectEventManager> participants = List.of(
			new Project.AdditionalObjectEventManager(pool.getObjectEventManager(), pool,
				eventSource == null ? resource : eventSource, List.of(resource)));
		List<Runnable> markApplied = new ArrayList<>(projects.size());
		int initialPoolFacts = pool.getObjectEventManager().pendingEventCount();
		try {
			runProjectCommands(projects, participants, 0, markApplied, () -> {
				try {
					mutation.apply();
				} catch (FieldParseException | RuntimeException | Error failure) {
					if (!Objects.deepEquals(before, valueReader.get())) {
						applied.set(true);
						markAllApplied(markApplied);
						refreshResourceIfNeeded(pool, resource, eventSource, initialPoolFacts);
						recalculate(projects);
					}
					throw failure;
				}
				boolean changed = !Objects.deepEquals(before, valueReader.get());
				if (changed) {
					applied.set(true);
					markAllApplied(markApplied);
					refreshResourceIfNeeded(pool, resource, eventSource, initialPoolFacts);
					recalculate(projects);
				}
				return changed;
			});
		} catch (FieldParseException failure) {
			throw failure;
		}
	}

	private void executePoolOnly(ResourcePool pool, ResourceImpl resource, Object before,
		Supplier<Object> valueReader, FieldMutation mutation, Object eventSource, AtomicBoolean applied)
			throws FieldParseException {
		ProjectMutationState.coordinated(() -> {
			var manager = pool.getObjectEventManager();
			int initialFacts = manager.pendingEventCount();
			manager.beginBatch();
			boolean changed = false;
			Throwable failure = null;
			try {
				mutation.apply();
				changed = !Objects.deepEquals(before, valueReader.get());
			} catch (FieldParseException | RuntimeException | Error current) {
				changed = !Objects.deepEquals(before, valueReader.get());
				failure = current;
			}
			try {
				if (changed) applied.set(true);
				if (changed) refreshResourceIfNeeded(pool, resource, eventSource, initialFacts);
				var batch = manager.detachBatch(changed);
				if (changed && batch != null && batch.isPublishable() && batch.hasEvents()) {
					long revision = pool.advanceCoordinationRevision();
					ProjectMutationState.publishAfterAllScopes(() -> manager.publishBatch(batch, revision));
				}
			} catch (Throwable current) {
				if (failure == null) failure = current; else failure.addSuppressed(current);
			}
			if (failure instanceof FieldParseException parse) throw new FieldParseRuntime(parse);
			if (failure instanceof Error error) throw error;
			if (failure instanceof RuntimeException runtime) throw runtime;
		});
	}

	private boolean runProjectCommands(List<Project> projects,
		List<Project.AdditionalObjectEventManager> participants, int index, List<Runnable> markApplied,
		CommandOperation operation) throws FieldParseException {
		if (index == projects.size()) return operation.run();
		return projects.get(index).runModelCommand(applied -> {
			markApplied.add(applied);
			return runProjectCommands(projects, participants, index + 1, markApplied, operation);
		}, Boolean.TRUE::equals, changed -> changed, participants);
	}

	private static void markAllApplied(List<Runnable> callbacks) {
		for (Runnable callback : callbacks) callback.run();
	}

	private static void recalculate(List<Project> projects) {
		for (Project project : projects) project.recalculate();
	}

	private static void refreshResourceIfNeeded(ResourcePool pool, ResourceImpl resource, Object source,
		int initialFacts) {
		if (!pool.getObjectEventManager().hasPendingEventFor(resource, initialFacts))
			pool.fireUpdateEvent(source == null ? resource : source, resource);
	}

	private static List<Project> identityList(List<Project> source) {
		List<Project> result = new ArrayList<>(source.size());
		for (Project project : source)
			if (project != null && result.stream().noneMatch(existing -> existing == project)) result.add(project);
		return result;
	}

	@FunctionalInterface
	public interface FieldMutation {
		void apply() throws FieldParseException;
	}

	@FunctionalInterface
	private interface CommandOperation {
		boolean run() throws FieldParseException;
	}

	private static final class FieldParseRuntime extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private FieldParseRuntime(FieldParseException cause) { super(cause); }
	}
}
