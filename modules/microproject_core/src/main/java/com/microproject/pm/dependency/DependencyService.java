/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.dependency;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.Collection;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Comparator;
import java.util.Map;

import javax.swing.undo.UndoableEdit;
import com.microproject.undo.EditSupport;

import com.microproject.association.InvalidAssociationException;
import com.microproject.association.Association;
import com.microproject.association.AssociationList;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.DependencyCreationEdit;
import com.microproject.undo.DependencyDeletionEdit;
import com.microproject.undo.DependencySetFieldsEdit;
import com.microproject.util.ClassUtils;

/**
 * Manages the creation and deleting of dependencies as well as events
 */
public class DependencyService {
	private static DependencyService instance = null;
	public static DependencyService getInstance() {
		if (instance == null)
			instance = new DependencyService();
		return instance;
	}

	public Dependency newDependency(HasDependencies predecessor, HasDependencies successor, int dependencyType, long lead, Object eventSource) throws InvalidAssociationException {
		return newDependency(predecessor, successor, DependencyType.Kind.fromCode(dependencyType), lead, eventSource);
	}

	public Dependency newDependency(HasDependencies predecessor, HasDependencies successor,
			DependencyType.Kind dependencyType, long lead, Object eventSource) throws InvalidAssociationException {
		return newDependency(predecessor, successor, dependencyType, lead, eventSource, null);
	}

	public Dependency newDependency(Object predecessor, Object successor, DependencyType.Kind dependencyType,
			long lead, Object eventSource, Project undoProject) throws InvalidAssociationException {
		java.util.Objects.requireNonNull(dependencyType, "dependencyType");
		if (predecessor == successor)
			throw new InvalidAssociationException(Messages.getString("Message.cantLinkToSelf"));
		Task predecessorTask = (Task)predecessor;
		if (predecessorTask.isExternal())
			throw new InvalidAssociationException(Messages.getString("Message.cantLinkToExternal"));
		if (predecessorTask.isSubproject() && !((SubProj)predecessorTask).isWritable())
			throw new InvalidAssociationException(Messages.getString("Message.cantLinkToClosedSubproject"));
		Task successorTask = (Task)successor;
		if (successorTask.isExternal())
			throw new InvalidAssociationException(Messages.getString("Message.cantLinkToExternal"));
		if (successorTask.isSubproject() && !((SubProj)successorTask).isWritable())
			throw new InvalidAssociationException(Messages.getString("Message.cantLinkToClosedSubproject"));
		if ((predecessorTask.getOwningProject() != null && predecessorTask.getOwningProject().isReadOnly())
				|| (successorTask.getOwningProject() != null && successorTask.getOwningProject().isReadOnly()))
			throw new InvalidAssociationException(Messages.getString("Message.cantLinkToReadOnly"));

		Dependency dependency = Dependency.getInstance((HasDependencies) predecessor, (HasDependencies) successor,
				dependencyType, lead);
		DependencyGraphSnapshot snapshot = new DependencyGraphSnapshot(List.of(dependency), List.of(predecessorTask, successorTask));
		runDependencyMutation(List.of(predecessorTask, successorTask), eventSource, (context, mutationSource) -> {
			try {
				dependency.testValid(false); // throws if exception
				connectInternal(dependency, mutationSource, undoProject, context);
				dependency.setDirty(true);
				return true;
			} catch (RuntimeException | Error failure) {
				snapshot.restore();
				throw failure;
			}
		}, true);
		return dependency;
	}

	//for deserialization
	public void initDependency(Dependency dependency,HasDependencies predecessor, HasDependencies successor, Object eventSource) throws InvalidAssociationException {
		Task predecessorTask = (Task) predecessor;
		Task successorTask = (Task) successor;
		DependencyGraphSnapshot snapshot = new DependencyGraphSnapshot(List.of(dependency), List.of(predecessorTask, successorTask));
		runDependencyMutation(List.of(predecessorTask, successorTask), eventSource, (context, mutationSource) -> {
			HasDependencies oldPredecessor = dependency.getPredecessor();
			HasDependencies oldSuccessor = dependency.getSuccessor();
			try {
				dependency.setPredecessor(predecessor);
				dependency.setSuccessor(successor);
				if (!dependency.isDisabled()) // allow for calling a second time once invalidated
					dependency.testValid(false); // throws if exception
			} catch (InvalidAssociationException failure) {
				dependency.setPredecessor(oldPredecessor);
				dependency.setSuccessor(oldSuccessor);
				throw failure;
			} catch (RuntimeException | Error failure) {
				snapshot.restore();
				dependency.setPredecessor(oldPredecessor);
				dependency.setSuccessor(oldSuccessor);
				throw failure;
			}
			try {
				connectInternal(dependency, mutationSource, context);
				return true;
			} catch (RuntimeException | Error failure) {
				snapshot.restore();
				dependency.setPredecessor(oldPredecessor);
				dependency.setSuccessor(oldSuccessor);
				throw failure;
			}
		}, true);
	}

	public void addStartSentinelDependency(HasDependencies sentinel, HasDependencies successor) {
		Dependency dependency = Dependency.getInstance(sentinel, successor, DependencyType.Kind.SS,0);
		sentinel.getSuccessorList().add(dependency);
	}
	public void addEndSentinelDependency(HasDependencies sentinel, HasDependencies predecessor) {
		Dependency dependency = Dependency.getInstance(predecessor, sentinel, DependencyType.Kind.FS,0);
		sentinel.getPredecessorList().add(dependency);
	}

	public boolean removeEndSentinel(HasDependencies sentinel, HasDependencies task) {
		Dependency dependency;
		dependency = (Dependency) sentinel.getPredecessorList().findLeft(task);
		if (dependency != null) {
			sentinel.getPredecessorList().remove(dependency);
			return true;
		}
		return false;
	}
	public boolean removeStartSentinel(HasDependencies sentinel, HasDependencies task) {
		Dependency dependency;
		dependency = (Dependency) sentinel.getSuccessorList().findRight(task);
		if (dependency != null) {
			sentinel.getSuccessorList().remove(dependency);
			return true;
		}
		return false;
	}

	public void connect(Dependency dependency, Object eventSource) {
		Task predecessor = (Task) dependency.getPredecessor();
		Task successor = (Task) dependency.getSuccessor();
		DependencyGraphSnapshot snapshot = new DependencyGraphSnapshot(List.of(dependency), List.of(predecessor, successor));
		runDependencyMutation(List.of(predecessor, successor), eventSource, (context, mutationSource) -> {
			try {
				return connectInternal(dependency, mutationSource, context);
			} catch (RuntimeException | Error failure) {
				snapshot.restore();
				throw failure;
			}
		}, true);
	}

	/** Resolves every dependency touching an unloaded external task as one open-time mutation. */
	public Runnable resolveExternalTask(Task externalTask, Task realTask, Object eventSource) {
		return resolveExternalTask(externalTask, realTask, eventSource, false);
	}

	/** Resolves an external task and removes its placeholder from its owner and root projections atomically. */
	public Runnable resolveExternalTaskAndRemovePlaceholder(Task externalTask, Task realTask, Object eventSource) {
		return resolveExternalTask(externalTask, realTask, eventSource, true);
	}

	private Runnable resolveExternalTask(Task externalTask, Task realTask, Object eventSource, boolean removePlaceholder) {
		if (externalTask == null || realTask == null || externalTask == realTask) return () -> {};
		List<Dependency> dependencies = getIncidentDependencies(externalTask);
		List<Task> endpoints = new ArrayList<>();
		Map<Task, Boolean> unique = new IdentityHashMap<>();
		for (Task endpoint : List.of(externalTask, realTask))
			if (unique.put(endpoint, Boolean.TRUE) == null) endpoints.add(endpoint);
		for (Dependency dependency : dependencies) {
			for (Task endpoint : List.of((Task) dependency.getPredecessor(), (Task) dependency.getSuccessor(), realTask))
				if (unique.put(endpoint, Boolean.TRUE) == null) endpoints.add(endpoint);
		}
		DependencyRebindSnapshot snapshot = new DependencyRebindSnapshot(dependencies, endpoints);
		List<Dependency> invalid = new ArrayList<>();
		List<Project> removalProjects = new ArrayList<>(2);
		if (removePlaceholder) {
			Project owner = externalTask.getProject();
			Project root = externalTask.liesInSubproject() ? externalTask.getRootProject() : null;
			if (owner != null) removalProjects.add(owner);
			if (root != null && root != owner) removalProjects.add(root);
		}
		List<Runnable> removalRollbacks = new ArrayList<>();
		runDependencyMutation(endpoints, eventSource,
			(context, mutationSource) -> {
				try {
					for (Dependency dependency : dependencies) {
						boolean predecessor = dependency.getPredecessor() == externalTask;
						if (!predecessor && dependency.getSuccessor() != externalTask) continue;
						context.markApplied(dependency);
						context.markAffectedProjects(endpoints);
						dependency.fireDeleteEvent(mutationSource);
						if (predecessor) {
							externalTask.getSuccessorList().remove(dependency);
							dependency.setPredecessor(realTask);
						} else {
							externalTask.getPredecessorList().remove(dependency);
							dependency.setSuccessor(realTask);
						}
						if (predecessor) realTask.getSuccessorList().add(dependency);
						else realTask.getPredecessorList().add(dependency);
						try {
							dependency.testValid(false);
						} catch (InvalidAssociationException failure) {
							dependency.setDisabled(true);
							invalid.add(dependency);
						}
						refreshDependencySentinels(dependency);
						dependency.fireCreateEvent(mutationSource);
					}
					if (removePlaceholder) {
						for (Project project : removalProjects) {
							context.markProjectChanged(project);
							removalRollbacks.add(project.removeExternalForResolution(externalTask));
						}
					}
					return !dependencies.isEmpty() || !removalProjects.isEmpty();
				} catch (RuntimeException | Error failure) {
					for (int i = removalRollbacks.size() - 1; i >= 0; i--) {
						try { removalRollbacks.get(i).run(); }
						catch (RuntimeException | Error rollbackFailure) { failure.addSuppressed(rollbackFailure); }
					}
					snapshot.restore();
					for (Task endpoint : endpoints) refreshTaskSentinels(endpoint);
					throw failure;
				}
			}, true, removalProjects);
		for (Dependency dependency : invalid)
			warnCircularCrossProjectLinkMessage(dependency.getPredecessor(), dependency.getSuccessor());
		return () -> {
			for (int i = removalRollbacks.size() - 1; i >= 0; i--) removalRollbacks.get(i).run();
			snapshot.restore();
			for (Task endpoint : endpoints) refreshTaskSentinels(endpoint);
		};
	}

	private boolean connectInternal(Dependency dependency, Object eventSource, MutationContext context) {
		return connectInternal(dependency, eventSource, null, context);
	}

	private boolean connectInternal(Dependency dependency, Object eventSource, Project undoProject,
			MutationContext context) {
		if (dependency.getPredecessor().getSuccessorList().findRight(dependency.getSuccessor()) == dependency
				&& dependency.getSuccessor().getPredecessorList().findLeft(dependency.getPredecessor()) == dependency)
			return false;
		context.markApplied(dependency);
		dependency.getPredecessor().getSuccessorList().add(dependency);
		dependency.getSuccessor().getPredecessorList().add(dependency);
		updateSentinels(dependency);
		if (eventSource != null) {
			dependency.fireCreateEvent(eventSource);
		}
		dependency.setDirty(true);

		EditSupport undoableEditSupport=undoProject == null ? getEditSupport(dependency) : getEditSupport(undoProject);
		if (undoableEditSupport!=null&&eventSource!=null&&shouldRecordUndo(eventSource)){
			undoableEditSupport.postEdit(new DependencyCreationEdit(dependency,eventSource));
		}
		return true;
	}

	public void fireTaskPredecessors(Collection<?> list) {
		for (Object taskValue : list) {
			Task task = (Task) taskValue;
			for (Object dependencyValue : task.getPredecessorList()) {
				((Dependency) dependencyValue).fireCreateEvent(this);
			}
		}
	}

	public void remove(Dependency dependency, Object eventSource,boolean undo) {
		remove(dependency, eventSource, undo, null);
	}

	/** Removes a dependency and optionally records its Undo edit in the command's project. */
	public void remove(Dependency dependency, Object eventSource, boolean undo, Project undoProject) {
		Task predecessor = (Task) dependency.getPredecessor();
		Task successor = (Task) dependency.getSuccessor();
		DependencyGraphSnapshot snapshot = new DependencyGraphSnapshot(List.of(dependency), List.of(predecessor, successor));
		runDependencyMutation(List.of(predecessor, successor), eventSource, (context, mutationSource) -> {
			try {
				return removeInternal(dependency, mutationSource, undo, undoProject, context);
			} catch (RuntimeException | Error failure) {
				snapshot.restore();
				throw failure;
			}
		}, true);
	}

	private boolean removeInternal(Dependency dependency, Object eventSource, boolean undo, MutationContext context) {
		return removeInternal(dependency, eventSource, undo, null, context);
	}

	private boolean removeInternal(Dependency dependency, Object eventSource, boolean undo, Project undoProject,
			MutationContext context) {
		boolean connected = dependency.getPredecessor().getSuccessorList().findRight(dependency.getSuccessor()) == dependency
				|| dependency.getSuccessor().getPredecessorList().findLeft(dependency.getPredecessor()) == dependency;
		if (!connected) return false;
		context.markApplied(dependency);
		dependency.setDirty(true); //for setGroupDirty()
		dependency.getPredecessor().getSuccessorList().remove(dependency);
		dependency.getSuccessor().getPredecessorList().remove(dependency);
		updateSentinels(dependency);

		if (eventSource != null)
			dependency.fireDeleteEvent(eventSource);

		EditSupport undoableEditSupport=undoProject == null ? getEditSupport(dependency) : getEditSupport(undoProject);
		if (undo && undoableEditSupport!=null&&shouldRecordUndo(eventSource)){
			undoableEditSupport.postEdit(new DependencyDeletionEdit(dependency,eventSource));
		}
		return true;


	}
	public void setFields(Dependency dependency, long lag, int type,Object eventSource) throws InvalidAssociationException{
		setFields(dependency, lag, DependencyType.Kind.fromCode(type), eventSource);
	}

	public void setFields(Dependency dependency, long lag, DependencyType.Kind type,Object eventSource) throws InvalidAssociationException{
		Task predecessor = (Task) dependency.getPredecessor();
		Task successor = (Task) dependency.getSuccessor();
		runDependencyMutation(List.of(predecessor, successor), eventSource, (context, mutationSource) -> {
			return setFieldsInternal(dependency, lag, type, mutationSource, context);
		});
	}

	private boolean setFieldsInternal(Dependency dependency, long lag, DependencyType.Kind type,
			Object eventSource, MutationContext context) throws InvalidAssociationException {
		long oldLag=dependency.getLag();
		int oldType=dependency.getDependencyType();
		if (oldLag == lag && oldType == type.code()) return false;
		dependency.setLag(lag);
		try {
			dependency.setDependencyKind(type);
		} catch (InvalidAssociationException e) {
			dependency.setLag(oldLag);
			dependency.setDependencyType(oldType);
			throw e;
		} catch (RuntimeException | Error failure) {
			dependency.setLag(oldLag);
			dependency.setDependencyType(oldType);
			throw failure;
		}
		context.markApplied(dependency);
		if (eventSource != null) dependency.fireUpdateEvent(eventSource);
		dependency.setDirty(true);

		EditSupport undoableEditSupport=getEditSupport(dependency);
		if (undoableEditSupport!=null&&shouldRecordUndo(eventSource)){
			undoableEditSupport.postEdit(new DependencySetFieldsEdit(dependency,oldLag,oldType,eventSource));
		}
		return true;

	}

	public void update(Dependency dependency, Object eventSource) {
		Task predecessor = (Task) dependency.getPredecessor();
		Task successor = (Task) dependency.getSuccessor();
		runDependencyMutation(List.of(predecessor, successor), eventSource, (context, mutationSource) -> {
			context.markApplied(dependency);
			updateInternal(dependency, mutationSource);
			return true;
		});
	}

	private void updateInternal(Dependency dependency, Object eventSource) {
		if (eventSource != null)
			dependency.fireUpdateEvent(eventSource);
		dependency.setDirty(true);
	}

	private static boolean shouldRecordUndo(Object eventSource) {
		return !(eventSource instanceof UndoableEdit) && eventSource != LiveDependencyRefreshSource.INSTANCE;
	}

	@FunctionalInterface
	private interface DependencyMutation<T, E extends Exception> {
		T apply(MutationContext context, Object mutationSource) throws E;
	}

	private final class MutationContext {
		private final Map<Project, Boolean> changedProjects = new IdentityHashMap<>();
		private final Map<Project, Runnable> activeMarkAppliedCallbacks = new IdentityHashMap<>();

		private void markApplied(Dependency dependency) {
			for (Project project : affectedProjects(List.of((Task) dependency.getPredecessor(),
				(Task) dependency.getSuccessor()))) {
				changedProjects.put(project, Boolean.TRUE);
				Runnable markApplied = activeMarkAppliedCallbacks.get(project);
				if (markApplied != null) markApplied.run();
			}
		}

		private void markAffectedProjects(List<Task> tasks) {
			for (Project project : affectedProjects(tasks)) {
				changedProjects.put(project, Boolean.TRUE);
				Runnable markApplied = activeMarkAppliedCallbacks.get(project);
				if (markApplied != null) markApplied.run();
			}
		}

		private void markProjectChanged(Project project) {
			changedProjects.put(project, Boolean.TRUE);
			Runnable markApplied = activeMarkAppliedCallbacks.get(project);
			if (markApplied != null) markApplied.run();
		}

		private void push(Project project, Runnable callback) {
			activeMarkAppliedCallbacks.put(project, callback);
		}

		private void pop(Project project) {
			activeMarkAppliedCallbacks.remove(project);
		}
	}

	/**
	 * Runs dependency writes through each affected project's publication boundary.
	 * Cross-project dependency events stay with the successor's document and Undo
	 * edits stay with each predecessor's project.
	 */
	private <T, E extends Exception> T runDependencyMutation(List<Task> affectedTasks,
			Object eventSource, DependencyMutation<T, E> operation) throws E {
		return runDependencyMutation(affectedTasks, eventSource, operation, false);
	}

	private <T, E extends Exception> T runDependencyMutation(List<Task> affectedTasks,
		Object eventSource, DependencyMutation<T, E> operation, boolean atomicFailure) throws E {
		return runDependencyMutation(affectedTasks, eventSource, operation, atomicFailure, List.of());
	}

	private <T, E extends Exception> T runDependencyMutation(List<Task> affectedTasks,
		Object eventSource, DependencyMutation<T, E> operation, boolean atomicFailure,
		List<Project> additionalProjects) throws E {
		try {
			return ProjectMutationState.coordinated(() -> {
				try {
					return runDependencyMutationCoordinated(affectedTasks, eventSource, operation, atomicFailure,
						additionalProjects);
				} catch (RuntimeException | Error failure) {
					throw failure;
				} catch (Exception failure) {
					throw new CheckedDependencyMutationFailure(failure);
				}
			});
		} catch (CheckedDependencyMutationFailure failure) {
			@SuppressWarnings("unchecked") E typedFailure = (E) failure.getCause();
			throw typedFailure;
		}
	}

	private <T, E extends Exception> T runDependencyMutationCoordinated(List<Task> affectedTasks,
		Object eventSource, DependencyMutation<T, E> operation, boolean atomicFailure,
		List<Project> additionalProjects) throws E {
		List<Project> participants = affectedProjects(affectedTasks, additionalProjects);
		boolean initialized = participants.stream().anyMatch(Project::isInitialized);
		if (participants.isEmpty() || (eventSource == null && !initialized)) {
			return operation.apply(new MutationContext(), eventSource);
		}
		Object mutationSource = eventSource == null ? LiveDependencyRefreshSource.INSTANCE : eventSource;
		return runProjectCommands(participants, 0, operation, mutationSource, new MutationContext(), atomicFailure);
	}

	private static final class CheckedDependencyMutationFailure extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private CheckedDependencyMutationFailure(Exception cause) { super(cause); }
	}

	private <T, E extends Exception> T runProjectCommands(List<Project> projects, int index,
		DependencyMutation<T, E> operation, Object mutationSource, MutationContext context,
		boolean atomicFailure) throws E {
		if (index >= projects.size())
			return operation.apply(context, mutationSource);
		Project project = projects.get(index);
		Project.ModelCommand<T, E> command = markApplied -> {
			context.push(project, markApplied);
			try {
				return runProjectCommands(projects, index + 1, operation, mutationSource, context, atomicFailure);
			} finally {
				context.pop(project);
			}
		};
		if (atomicFailure)
			return project.runAtomicModelCommand(command,
				value -> Boolean.TRUE.equals(value) && context.changedProjects.containsKey(project), value -> value);
		return project.runModelCommand(command,
			value -> Boolean.TRUE.equals(value) && context.changedProjects.containsKey(project), value -> value);
	}

	private List<Project> affectedProjects(List<Task> affectedTasks) {
		return affectedProjects(affectedTasks, List.of());
	}

	private List<Project> affectedProjects(List<Task> affectedTasks, List<Project> additionalProjects) {
		Map<Project, Boolean> unique = new IdentityHashMap<>();
		for (Task task : affectedTasks) {
			if (task == null) continue;
			if (task.getProject() != null) unique.put(task.getProject(), Boolean.TRUE);
			if (task.getMasterDocument() instanceof Project project) unique.put(project, Boolean.TRUE);
		}
		for (Project project : additionalProjects)
			if (project != null) unique.put(project, Boolean.TRUE);
		return unique.keySet().stream().sorted(Comparator.comparingInt(System::identityHashCode)).toList();
	}

	/** Neutral source marker for live refresh events that must not create user Undo entries. */
	private enum LiveDependencyRefreshSource { INSTANCE }

// update the starting and ending sentinels of the project - the sentinels keep track of which
//	tasks have no preds or no successors
	public void updateSentinels(Dependency dependency) {
		Task predecessor = (Task) dependency.getPredecessor();
		Task successor = (Task) dependency.getSuccessor();
		predecessor.updateEndSentinel();
		successor.updateStartSentinel();
	}

	private static void refreshDependencySentinels(Dependency dependency) {
		Task predecessor = (Task) dependency.getPredecessor();
		Task successor = (Task) dependency.getSuccessor();
		refreshTaskEndSentinel(predecessor);
		refreshTaskStartSentinel(successor);
	}

	private static void refreshTaskSentinels(Task task) {
		refreshTaskEndSentinel(task);
		refreshTaskStartSentinel(task);
	}

	private static void refreshTaskEndSentinel(Task task) {
		if (task.getProject() != null && task.getProject().getSchedulingAlgorithm() != null)
			task.updateEndSentinel();
	}

	private static void refreshTaskStartSentinel(Task task) {
		if (task.getProject() != null && task.getProject().getSchedulingAlgorithm() != null)
			task.updateStartSentinel();
	}

	/**
	 * Connect tasks sequentially.
	 * Circularities will be tested, and an exception thrown if any circularity would occur
	 *
	 * @param tasks
	 * @param eventSource
	 * @throws InvalidAssociationException
	 */
	public void connect(List<?> tasks, Object eventSource, Predicate<Object> canBeSuccessorCondition) throws InvalidAssociationException {
		connect(tasks, eventSource, canBeSuccessorCondition, null);
	}

	/** Connects tasks sequentially and records creation edits in the command's project when supplied. */
	public void connect(List<?> tasks, Object eventSource, Predicate<Object> canBeSuccessorCondition,
			Project undoProject) throws InvalidAssociationException {
		ArrayList<HasDependencies> connectableTasks = new ArrayList<>(tasks.size());
		for (Object task : tasks) {
			if (task instanceof HasDependencies hasDependencies && !ClassUtils.isObjectReadOnly(task))
				connectableTasks.add(hasDependencies);
		}
		if (connectableTasks.size() < 2) return;
		Task predecessor = (Task) connectableTasks.get(0);
		Task successor = (Task) connectableTasks.get(connectableTasks.size() - 1);
		runDependencyMutation(connectableTasks.stream().map(Task.class::cast).toList(), eventSource,
				(context, mutationSource) -> {
			ArrayList<Dependency> newDependencies = new ArrayList<>(Math.max(0, connectableTasks.size() - 1));
			// try making dependencies between earlier and later items to check all possible circularities
			for (int i = 0; i < connectableTasks.size()-1; i++) {
				HasDependencies pred = connectableTasks.get(i);
				for (int j = i+1; j < connectableTasks.size(); j++) {
					HasDependencies succ = connectableTasks.get(j);
					if (canBeSuccessorCondition != null && !canBeSuccessorCondition.test(succ))
						continue;
					if (succ.getPredecessorList().findLeft(pred) != null)
						continue;
					Dependency test = Dependency.getInstance(pred, succ, DependencyType.Kind.FS, 0);
					test.testValid(false);
					if (j == i+1) newDependencies.add(test);
				}
			}
			DependencyGraphSnapshot snapshot = new DependencyGraphSnapshot(newDependencies,
				connectableTasks.stream().map(Task.class::cast).toList());
			try {
				for (Dependency dependency : newDependencies)
					connectInternal(dependency, mutationSource, undoProject, context);
				return !newDependencies.isEmpty();
			} catch (RuntimeException | Error failure) {
				snapshot.restore();
				throw failure;
			}
		}, true);
	}
	/**
	 * Remove all dependencies between all tasks in an array
	 * @param tasks
	 * @param eventSource
	 */
	public void removeAnyDependencies(List<?> tasks, Object eventSource) {
		removeAnyDependencies(tasks, eventSource, null);
	}

	/** Removes dependencies incident to the tasks and records Undo in the command's project when supplied. */
	public void removeAnyDependencies(List<?> tasks, Object eventSource, Project undoProject) {
		ProjectMutationState.coordinated(() -> {
			List<Dependency> incident = new ArrayList<>();
			for (Object candidate : tasks) {
				if (candidate instanceof HasDependencies hasDependencies && !ClassUtils.isObjectReadOnly(candidate))
					for (Dependency dependency : getIncidentDependencies(hasDependencies))
						if (!incident.contains(dependency)) incident.add(dependency);
			}
			removeDependenciesInOneCommand(incident, eventSource, true, undoProject);
		});
	}

	private void removeDependenciesInOneCommand(List<Dependency> dependencies, Object eventSource, boolean undo) {
		removeDependenciesInOneCommand(dependencies, eventSource, undo, null);
	}

	private void removeDependenciesInOneCommand(List<Dependency> dependencies, Object eventSource, boolean undo,
			Project undoProject) {
		if (dependencies.isEmpty()) return;
		Map<Task, Boolean> uniqueEndpoints = new IdentityHashMap<>();
		List<Task> endpoints = new ArrayList<>(dependencies.size() * 2);
		for (Dependency dependency : dependencies) {
			for (Task task : List.of((Task) dependency.getPredecessor(), (Task) dependency.getSuccessor())) {
				if (uniqueEndpoints.put(task, Boolean.TRUE) == null) endpoints.add(task);
			}
		}
		DependencyGraphSnapshot snapshot = new DependencyGraphSnapshot(dependencies, endpoints);
		runDependencyMutation(endpoints, eventSource, (context, mutationSource) -> {
			try {
				boolean changed = false;
				for (Dependency dependency : dependencies)
					changed |= removeInternal(dependency, mutationSource, undo, undoProject, context);
				return changed;
			} catch (RuntimeException | Error failure) {
				snapshot.restore();
				throw failure;
			}
		}, true);
	}

	/** Restores dependency adjacency and sentinel lists if an atomic bulk unlink fails mid-command. */
	private static final class DependencyGraphSnapshot {
		private final Map<AssociationList, List<Association>> lists = new IdentityHashMap<>();
		private final Map<Dependency, Boolean> dirtyStates = new IdentityHashMap<>();

		private DependencyGraphSnapshot(List<Dependency> dependencies, List<Task> endpoints) {
			for (Task task : endpoints) {
				snapshot(task.getPredecessorList());
				snapshot(task.getSuccessorList());
			}
			List<Association> adjacent = lists.values().stream().flatMap(List::stream).toList();
			for (Association association : adjacent) {
				if (association instanceof Dependency dependency) {
					snapshot(dependency.getPredecessor().getPredecessorList());
					snapshot(dependency.getPredecessor().getSuccessorList());
					snapshot(dependency.getSuccessor().getPredecessorList());
					snapshot(dependency.getSuccessor().getSuccessorList());
				}
			}
			for (Dependency dependency : dependencies) dirtyStates.put(dependency, dependency.isDirty());
		}

		private void snapshot(AssociationList list) {
			if (lists.containsKey(list)) return;
			lists.put(list, List.copyOf(list));
		}

		private void restore() {
			IdentityHashMap<Association, Boolean> candidates = new IdentityHashMap<>();
			for (Map.Entry<AssociationList, List<Association>> entry : lists.entrySet()) {
				for (Association association : entry.getValue()) candidates.put(association, Boolean.TRUE);
				for (Association association : entry.getKey()) candidates.put(association, Boolean.TRUE);
			}
			for (Association association : candidates.keySet()) {
				if (!(association instanceof Dependency dependency)) continue;
				AssociationList predecessorList = dependency.getPredecessor().getSuccessorList();
				AssociationList successorList = dependency.getSuccessor().getPredecessorList();
				if (!lists.containsKey(predecessorList)) predecessorList.remove(dependency);
				if (!lists.containsKey(successorList)) successorList.remove(dependency);
			}
			for (Map.Entry<AssociationList, List<Association>> entry : lists.entrySet()) {
				entry.getKey().clear();
				entry.getKey().addAll(entry.getValue());
			}
			for (Map.Entry<Dependency, Boolean> entry : dirtyStates.entrySet())
				entry.getKey().setDirty(entry.getValue());
		}
	}

	private static final class DependencyRebindSnapshot {
		private final DependencyGraphSnapshot graph;
		private final Map<Dependency, EndpointState> endpoints = new IdentityHashMap<>();
		private DependencyRebindSnapshot(List<Dependency> dependencies, List<Task> tasks) {
			graph = new DependencyGraphSnapshot(dependencies, tasks);
			for (Dependency dependency : dependencies)
				endpoints.put(dependency, new EndpointState(dependency.getPredecessor(), dependency.getSuccessor(),
					dependency.isDisabled(), dependency.isDirty()));
		}
		private void restore() {
			for (Map.Entry<Dependency, EndpointState> entry : endpoints.entrySet()) {
				entry.getKey().setPredecessor(entry.getValue().predecessor());
				entry.getKey().setSuccessor(entry.getValue().successor());
				entry.getKey().setDisabled(entry.getValue().disabled());
			}
			graph.restore();
			for (Map.Entry<Dependency, EndpointState> entry : endpoints.entrySet())
				entry.getKey().setDirty(entry.getValue().dirty());
		}
	}
	private record EndpointState(HasDependencies predecessor, HasDependencies successor, boolean disabled, boolean dirty) { }

	/**
	 * Remove the dependencies between two tasks. Kept as a pairwise helper for
	 * hierarchy changes; the list command owns the outer transaction when present.
	 */
	private void removeAnyDependenciesInternal(HasDependencies first, HasDependencies second, Object eventSource) {
		List<Dependency> dependencies = new ArrayList<>(4);
		Dependency dependency;
		if (first == null || second == null)
			return;
		if (ClassUtils.isObjectReadOnly(first) || ClassUtils.isObjectReadOnly(second))
			return;
		if ((dependency = (Dependency) first.getPredecessorList().findLeft(second)) != null) dependencies.add(dependency);
		if ((dependency = (Dependency) second.getPredecessorList().findLeft(first)) != null) dependencies.add(dependency);
		if ((dependency = (Dependency) first.getSuccessorList().findRight(second)) != null) dependencies.add(dependency);
		if ((dependency = (Dependency) second.getSuccessorList().findRight(first)) != null) dependencies.add(dependency);
		removeDependenciesInOneCommand(dependencies, eventSource, true);
	}

	/** Returns a stable snapshot of every dependency touching the supplied task. */
	public java.util.List<Dependency> getIncidentDependencies(HasDependencies task) {
		java.util.List<Dependency> incident = new java.util.ArrayList<Dependency>(
				task == null ? 0 : task.getPredecessorList().size() + task.getSuccessorList().size());
		if (task == null) return incident;
		for (Object candidate : task.getPredecessorList())
			incident.add((Dependency) candidate);
		for (Object candidate : task.getSuccessorList())
			incident.add((Dependency) candidate);
		return incident;
	}
	public void removeAnyDependencies(HasDependencies first, HasDependencies second, Object eventSource) {
		ProjectMutationState.coordinated(() -> removeAnyDependenciesInternal(first, second, eventSource));
	}

	public void remove(Collection<?> dependencyList, Object eventSource) {
		remove(dependencyList, eventSource, true);
	}

	public void remove(Collection<?> dependencyList, Object eventSource, boolean undo) {
		remove(dependencyList, eventSource, undo, null);
	}

	/** Removes dependencies and records all deletion edits in the command's project when supplied. */
	public void remove(Collection<?> dependencyList, Object eventSource, boolean undo, Project undoProject) {
		ProjectMutationState.coordinated(() -> {
			List<Dependency> dependencies = dependencyList.stream().map(Dependency.class::cast).toList();
			removeDependenciesInOneCommand(dependencies, eventSource, undo, undoProject);
		});
	}

	//fix
	public <T> void remove(Collection<? extends T> dependencyList, Collection<? super T> toRemove) {
		for (T dependency : dependencyList)
			toRemove.add(dependency);
	}




	//undo
	public EditSupport getEditSupport(Dependency dependency) {
		if (dependency.getPredecessor()==null)
			return null;
		else {
			DataFactoryUndoController c = ((Task)dependency.getPredecessor()).getProject().getUndoController();
			if (c == null)
				return null;
			return c.getEditSupport();
		}
	}

	private EditSupport getEditSupport(Project project) {
		DataFactoryUndoController controller = project == null ? null : project.getUndoController();
		return controller == null ? null : controller.getEditSupport();
	}

	public static String getCircularCrossProjectLinkMessage(Object predecessor, Object successor) {
		return MessageFormat.format(Messages.getString("Message.crossProjectCircularDependency.mf"),new Object[] {predecessor,successor});
	}
	/**
	 * Warn that a cross project link is disabled. This is invoked later to give time for the gantt to redraw first
	 * @param predecessor
	 * @param successor
	 */
	public static void warnCircularCrossProjectLinkMessage(final Object predecessor, final Object successor) {
		DependencyWarningNotifications.warnCircularLink(getCircularCrossProjectLinkMessage(predecessor, successor));
	}
}
