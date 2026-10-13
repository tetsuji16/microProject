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
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.DependencyCreationEdit;
import com.microproject.undo.DependencyDeletionEdit;
import com.microproject.undo.DependencySetFieldsEdit;
import javax.swing.undo.AbstractUndoableEdit;
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

		Dependency dependency = Dependency.getInstance(predecessor, successor, dependencyType, lead);
		runDependencyMutation(List.of(predecessorTask, successorTask), eventSource, (context, mutationSource) -> {
			dependency.testValid(false); // throws if exception
			connectInternal(dependency, mutationSource, context);
			dependency.setDirty(true);
			return true;
		});
		return dependency;
	}

	//for deserialization
	public void initDependency(Dependency dependency,HasDependencies predecessor, HasDependencies successor, Object eventSource) throws InvalidAssociationException {
		Task predecessorTask = (Task) predecessor;
		Task successorTask = (Task) successor;
		runDependencyMutation(List.of(predecessorTask, successorTask), eventSource, (context, mutationSource) -> {
			HasDependencies oldPredecessor = dependency.getPredecessor();
			HasDependencies oldSuccessor = dependency.getSuccessor();
			dependency.setPredecessor(predecessor);
			dependency.setSuccessor(successor);
			try {
				if (!dependency.isDisabled()) // allow for calling a second time once invalidated
					dependency.testValid(false); // throws if exception
			} catch (InvalidAssociationException failure) {
				dependency.setPredecessor(oldPredecessor);
				dependency.setSuccessor(oldSuccessor);
				throw failure;
			}
			connectInternal(dependency, mutationSource, context);
			return true;
		});
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
		runDependencyMutation(List.of(predecessor, successor), eventSource, (context, mutationSource) -> {
			return connectInternal(dependency, mutationSource, context);
		});
	}

	private boolean connectInternal(Dependency dependency, Object eventSource, MutationContext context) {
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

		EditSupport undoableEditSupport=getEditSupport(dependency);
		if (undoableEditSupport!=null&&eventSource!=null&&!(eventSource instanceof UndoableEdit)){
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
		Task predecessor = (Task) dependency.getPredecessor();
		Task successor = (Task) dependency.getSuccessor();
		runDependencyMutation(List.of(predecessor, successor), eventSource, (context, mutationSource) -> {
			return removeInternal(dependency, mutationSource, undo, context);
		});
	}

	private boolean removeInternal(Dependency dependency, Object eventSource, boolean undo, MutationContext context) {
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

		EditSupport undoableEditSupport=getEditSupport(dependency);
		if (undo && undoableEditSupport!=null&&!(eventSource instanceof UndoableEdit)){
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
		}
		context.markApplied(dependency);
		if (eventSource != null) dependency.fireUpdateEvent(eventSource);
		dependency.setDirty(true);

		EditSupport undoableEditSupport=getEditSupport(dependency);
		if (undoableEditSupport!=null&&!(eventSource instanceof UndoableEdit)){
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
		try {
			return ProjectMutationState.coordinated(() -> {
				try {
					return runDependencyMutationCoordinated(affectedTasks, eventSource, operation);
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
			Object eventSource, DependencyMutation<T, E> operation) throws E {
		List<Project> participants = affectedProjects(affectedTasks);
		boolean initialized = participants.stream().anyMatch(Project::isInitialized);
		if (participants.isEmpty() || (eventSource == null && !initialized)) {
			return operation.apply(new MutationContext(), eventSource);
		}
		Object mutationSource = eventSource == null ? new LiveDependencyRefreshSource() : eventSource;
		return runProjectCommands(participants, 0, operation, mutationSource, new MutationContext());
	}

	private static final class CheckedDependencyMutationFailure extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private CheckedDependencyMutationFailure(Exception cause) { super(cause); }
	}

	private <T, E extends Exception> T runProjectCommands(List<Project> projects, int index,
			DependencyMutation<T, E> operation, Object mutationSource, MutationContext context) throws E {
		if (index >= projects.size())
			return operation.apply(context, mutationSource);
		Project project = projects.get(index);
		return project.runModelCommand(markApplied -> {
			context.push(project, markApplied);
			try {
				return runProjectCommands(projects, index + 1, operation, mutationSource, context);
			} finally {
				context.pop(project);
			}
		}, value -> Boolean.TRUE.equals(value) && context.changedProjects.containsKey(project), value -> value);
	}

	private List<Project> affectedProjects(List<Task> affectedTasks) {
		Map<Project, Boolean> unique = new IdentityHashMap<>();
		for (Task task : affectedTasks) {
			if (task == null) continue;
			if (task.getProject() != null) unique.put(task.getProject(), Boolean.TRUE);
			if (task.getMasterDocument() instanceof Project project) unique.put(project, Boolean.TRUE);
		}
		return unique.keySet().stream().sorted(Comparator.comparingInt(System::identityHashCode)).toList();
	}

	/** Marks live refresh events as non-user edits so they publish without adding Undo entries. */
	private static final class LiveDependencyRefreshSource extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;
	}

// update the starting and ending sentinels of the project - the sentinels keep track of which
//	tasks have no preds or no successors
	public void updateSentinels(Dependency dependency) {
		Task predecessor = (Task) dependency.getPredecessor();
		Task successor = (Task) dependency.getSuccessor();
		predecessor.updateEndSentinel();
		successor.updateStartSentinel();
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
			for (Dependency dependency : newDependencies)
				connectInternal(dependency, mutationSource, context);
			return !newDependencies.isEmpty();
		});
	}
	/**
	 * Remove all dependencies between all tasks in an array
	 * @param tasks
	 * @param eventSource
	 */
	public void removeAnyDependencies(List<?> tasks, Object eventSource) {
		ProjectMutationState.coordinated(() -> {
			List<Dependency> incident = new ArrayList<>();
			for (Object candidate : tasks) {
				if (candidate instanceof HasDependencies hasDependencies && !ClassUtils.isObjectReadOnly(candidate))
					for (Dependency dependency : getIncidentDependencies(hasDependencies))
						if (!incident.contains(dependency)) incident.add(dependency);
			}
			removeDependenciesInOneCommand(incident, eventSource, true);
		});
	}

	private void removeDependenciesInOneCommand(List<Dependency> dependencies, Object eventSource, boolean undo) {
		if (dependencies.isEmpty()) return;
		Map<Task, Boolean> uniqueEndpoints = new IdentityHashMap<>();
		List<Task> endpoints = new ArrayList<>(dependencies.size() * 2);
		for (Dependency dependency : dependencies) {
			for (Task task : List.of((Task) dependency.getPredecessor(), (Task) dependency.getSuccessor())) {
				if (uniqueEndpoints.put(task, Boolean.TRUE) == null) endpoints.add(task);
			}
		}
		runDependencyMutation(endpoints, eventSource, (context, mutationSource) -> {
			boolean changed = false;
			for (Dependency dependency : dependencies)
				changed |= removeInternal(dependency, mutationSource, undo, context);
			return changed;
		});
	}

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
		ProjectMutationState.coordinated(() -> {
			List<Dependency> dependencies = dependencyList.stream().map(Dependency.class::cast).toList();
			removeDependenciesInOneCommand(dependencies, eventSource, undo);
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
