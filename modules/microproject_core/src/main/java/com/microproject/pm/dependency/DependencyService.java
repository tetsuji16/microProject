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

import javax.swing.undo.UndoableEdit;
import com.microproject.undo.EditSupport;

import com.microproject.association.InvalidAssociationException;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
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
		dependency.testValid(false); // throws if exception
		connect(dependency,eventSource);
		dependency.setDirty(true);
		return dependency;
	}

	//for deserialization
	public void initDependency(Dependency dependency,HasDependencies predecessor, HasDependencies successor, Object eventSource) throws InvalidAssociationException {
		dependency.setPredecessor(predecessor);
		dependency.setSuccessor(successor);
		if (!dependency.isDisabled()) // allow for calling a second time once invalidated
			dependency.testValid(false); // throws if exception
		connect(dependency,eventSource);
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


	}
	public void setFields(Dependency dependency, long lag, int type,Object eventSource) throws InvalidAssociationException{
		setFields(dependency, lag, DependencyType.Kind.fromCode(type), eventSource);
	}

	public void setFields(Dependency dependency, long lag, DependencyType.Kind type,Object eventSource) throws InvalidAssociationException{

//		if (eventSource != null)
//			dependency.getDocument().getObjectEventManager().fireUpdateEvent(eventSource,dependency);
		long oldLag=dependency.getLag();
		int oldType=dependency.getDependencyType();
		dependency.setLag(lag);
		try {
			dependency.setDependencyKind(type);
		} catch (InvalidAssociationException e) {
			dependency.setLag(oldLag);
			dependency.setDependencyType(oldType);
			throw e;
		}
		dependency.setDirty(true);

		EditSupport undoableEditSupport=getEditSupport(dependency);
		if (undoableEditSupport!=null&&!(eventSource instanceof UndoableEdit)){
			undoableEditSupport.postEdit(new DependencySetFieldsEdit(dependency,oldLag,oldType,eventSource));
		}

	}

	public void update(Dependency dependency, Object eventSource) {
		if (eventSource != null)
			dependency.fireUpdateEvent(eventSource);
		dependency.setDirty(true);
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
		ArrayList<Dependency> newDependencies = new ArrayList<>(Math.max(0, tasks.size() - 1));
		ArrayList<HasDependencies> connectableTasks = new ArrayList<>(tasks.size());
		for (Object task : tasks) {
			if (task instanceof HasDependencies hasDependencies && !ClassUtils.isObjectReadOnly(task))
				connectableTasks.add(hasDependencies);
		}
		// try making new dependencies between all items earlier to all items later, thereby checking all possible circularities
		for (int i = 0; i < connectableTasks.size()-1; i++) {
			HasDependencies pred = connectableTasks.get(i);
			for (int j = i+1; j < connectableTasks.size(); j++) {
				HasDependencies succ = connectableTasks.get(j);
				if (canBeSuccessorCondition != null && !canBeSuccessorCondition.test(succ)) // allow exclusion of certain nodes that we don't want to be successors
					continue;
				if (succ.getPredecessorList().findLeft(pred) != null) // if dependency already exists, skip it
					continue;
				Dependency test = Dependency.getInstance(pred,succ,DependencyType.Kind.FS,0); // make a new one
				test.testValid(false); // test for circularity, throws if bad
				if (j == i+1) // only add sequential ones
					newDependencies.add(test);
			}
		}
		for (Dependency dependency : newDependencies)
			connect(dependency, eventSource);


	}
	/**
	 * Remove all dependencies between all tasks in an array
	 * @param tasks
	 * @param eventSource
	 */
	public void removeAnyDependencies(List<?> tasks, Object eventSource) {
		// Remove dependencies between any two selected tasks (pairwise).
		for (int i = 0; i < tasks.size()-1; i++) {
			HasDependencies pred = writableDependencies(tasks.get(i));
			if (pred == null)
				continue;
			for (int j = i+1; j < tasks.size(); j++) {
				HasDependencies succ = writableDependencies(tasks.get(j));
				if (succ == null)
					continue;
				removeAnyDependencies(pred,succ,eventSource);
			}
		}
		// Issue #266: also remove every dependency incident to a selected task, even when
		// the other endpoint is not selected. Without this, selecting a single task (or a
		// task whose only links point to unselected tasks) removed nothing, so "Unlink" did
		// nothing. Snapshot the incident dependencies first because remove() mutates the lists.
		for (int i = 0; i < tasks.size(); i++) {
			HasDependencies selected = writableDependencies(tasks.get(i));
			if (selected == null)
				continue;
			for (Dependency d : getIncidentDependencies(selected))
				remove(d, eventSource, true);
		}
	}

	private HasDependencies writableDependencies(Object value) {
		if (value instanceof HasDependencies dependencies && !ClassUtils.isObjectReadOnly(value))
			return dependencies;
		return null;
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
		Dependency dependency;
		if (first == null || second == null)
			return;
		if (ClassUtils.isObjectReadOnly(first) || ClassUtils.isObjectReadOnly(second))
			return;
		if ((dependency = (Dependency) first.getPredecessorList().findLeft(second)) != null)
			remove(dependency,eventSource,true);
		if ((dependency = (Dependency) second.getPredecessorList().findLeft(first)) != null)
			remove(dependency,eventSource,true);
		if ((dependency = (Dependency) first.getSuccessorList().findRight(second)) != null)
			remove(dependency,eventSource,true);
		if ((dependency = (Dependency) second.getSuccessorList().findRight(first)) != null)
			remove(dependency,eventSource,true);
	}

	public void remove(Collection<?> dependencyList, Object eventSource) {
		for (Object item : dependencyList) {
			Dependency dependency = (Dependency) item;
			remove(dependency,eventSource,true);
		}
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
