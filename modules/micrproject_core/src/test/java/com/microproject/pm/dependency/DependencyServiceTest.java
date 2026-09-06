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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.microproject.association.InvalidAssociationException;
import com.microproject.options.CalendarOption;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.Node;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.undo.DataFactoryUndoController;

class DependencyServiceTest {
	@Test
	void unknownDependencyTypeFailsExplicitlyWhenFormatted() {
		assertThrows(IllegalArgumentException.class, () -> DependencyType.toLongString(99));
	}

	@Test
	void newDependencyCreatesFsZeroLagLink() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(0L, dependency.getLag());
	}

	@Test
	void kindApiRoundTripsThroughThePersistedIntegerBoundary() throws InvalidAssociationException {
		Project project = createProject("kind-api-project");
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(
				predecessor, successor, DependencyType.Kind.SS, 0L, this);
		assertEquals(DependencyType.Kind.SS, dependency.getDependencyKind());
		assertEquals(DependencyType.Kind.SS.code(), dependency.getDependencyType());

		dependency.setDependencyKind(DependencyType.Kind.FF);
		assertEquals(DependencyType.Kind.FF, dependency.getDependencyKind());
		assertEquals(DependencyType.Kind.FF.code(), dependency.getDependencyType());
	}

	@Test
	void crossProjectLinksSupportEveryMicrosoftProjectDependencyType() throws InvalidAssociationException {
		Project predecessorProject = createProject("predecessor-project");
		Project successorProject = createProject("successor-project");
		NormalTask predecessor = new NormalTask(predecessorProject);
		NormalTask successor = new NormalTask(successorProject);
		predecessorProject.connectTask(predecessor);
		successorProject.connectTask(successor);

		for (int type : new int[] { DependencyType.FS, DependencyType.SS, DependencyType.FF, DependencyType.SF }) {
			Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, type, 0L, this);
			assertEquals(type, dependency.getDependencyType());
			assertSame(predecessor, dependency.getPredecessor());
			assertSame(successor, dependency.getSuccessor());
			DependencyService.getInstance().remove(dependency, this, false);
		}
	}

	@Test
	void rejectsACycleThatCrossesProjectBoundariesWithoutAddingAPartialLink() throws InvalidAssociationException {
		Project firstProject = createProject("first-project");
		Project secondProject = createProject("second-project");
		NormalTask first = new NormalTask(firstProject);
		NormalTask second = new NormalTask(secondProject);
		firstProject.connectTask(first);
		secondProject.connectTask(second);
		DependencyService.getInstance().newDependency(first, second, DependencyType.FS, 0L, this);

		assertThrows(InvalidAssociationException.class,
				() -> DependencyService.getInstance().newDependency(second, first, DependencyType.FS, 0L, this));
		assertEquals(1, first.getSuccessorList().size());
		assertEquals(1, second.getPredecessorList().size());
		assertEquals(0, first.getPredecessorList().size());
		assertEquals(0, second.getSuccessorList().size());
	}

	@Test
	void dependencyDataObjectHasStableIdentityAndName() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor,
				DependencyType.FS, 0L, this);

		assertEquals(predecessor.getUniqueId() + "." + successor.getUniqueId(), dependency.getName());
		assertNotEquals(0L, dependency.getUniqueId());
		dependency.setName("link");
		assertEquals("link", dependency.getName());
		dependency.setUniqueId(42L);
		assertEquals(42L, dependency.getUniqueId());
	}

	@Test
	void newDependencyRejectsSelfLink() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		assertThrows(InvalidAssociationException.class, () -> DependencyService.getInstance().newDependency(task, task, DependencyType.FS, 0L, this));
	}

	@Test
	void newDependencyRejectsExternalSourceTask() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		predecessor.setExternal(true);

		assertThrows(InvalidAssociationException.class,
				() -> DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this));
	}

	@Test
	void newDependencyAllowsWritableSubprojectTask() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		SubprojectTask predecessor = new SubprojectTask(project, true);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		assertEquals(predecessor, dependency.getPredecessor());
		assertEquals(successor, dependency.getSuccessor());
	}

	@Test
	void newDependencyRejectsClosedSubprojectTask() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		SubprojectTask predecessor = new SubprojectTask(project, false);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		assertThrows(InvalidAssociationException.class,
				() -> DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this));
	}

	@Test
	void resizingSuccessorFromEndKeepsDependencyAlignedStart() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		long day = CalendarOption.getInstance().getMillisPerDay();
		predecessor.setEnd(predecessor.getEffectiveWorkCalendar().add(predecessor.getStart(), day, false));
		successor.setEnd(successor.getEffectiveWorkCalendar().add(successor.getStart(), day, false));
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		long successorStartBeforeResize = successor.getStart();
		long successorEndBeforeResize = successor.getEnd();
		long successorEndAfterResize = CalendarOption.getInstance().makeValidEnd(
				successor.getEffectiveWorkCalendar().add(successorEndBeforeResize, day, false), true);

		ScheduleService.getInstance().setInterval(this, successor, successorStartBeforeResize, successorEndAfterResize,
				new ScheduleInterval(successorStartBeforeResize, successorEndBeforeResize), undoController.getEditSupport());

		assertEquals(successorStartBeforeResize, successor.getStart());
		assertEquals(successorEndAfterResize, successor.getEnd());
	}

	@Test
	void setFieldsRollsBackLagWhenDependencyTypeChangeIsRejected() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		NormalTask child = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		project.connectTask(child);
		child.setWbsParent(successor);
		successor.setWbsChildrenNodes(Collections.singletonList((Node) NodeFactory.getInstance().createNode(child)));

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor,
				DependencyType.FS, 0L, this);

		assertThrows(InvalidAssociationException.class,
				() -> DependencyService.getInstance().setFields(dependency, 2L * CalendarOption.getInstance().getMillisPerDay(),
						DependencyType.FF, this));

		assertEquals(0L, dependency.getLag());
		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertSame(predecessor, dependency.getPredecessor());
		assertSame(successor, dependency.getSuccessor());
	}

	@Test
	void connectListSkipsReadOnlyPredecessorTasks() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask readOnly = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(readOnly);
		project.connectTask(successor);
		readOnly.setExternal(true);

		DependencyService.getInstance().connect(Arrays.asList(readOnly, successor), this, null);

		assertFalse(readOnly.getSuccessorList().iterator().hasNext());
		assertFalse(successor.getPredecessorList().iterator().hasNext());
	}

	@Test
	void connectListSkipsReadonlyItemsButStillLinksEditablePairs() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask readOnly = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(readOnly);
		project.connectTask(successor);
		readOnly.setExternal(true);

		DependencyService.getInstance().connect(Arrays.asList(predecessor, readOnly, successor), this, null);

		assertTrue(predecessor.getSuccessorList().iterator().hasNext());
		assertTrue(successor.getPredecessorList().iterator().hasNext());
	}

	@Test
	void removeAnyDependenciesSkipsReadOnlyTasks() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		project.setReadOnly(true);

		DependencyService.getInstance().removeAnyDependencies(Arrays.asList(predecessor, successor), this);

		assertTrue(predecessor.getSuccessorList().iterator().hasNext());
		assertTrue(successor.getPredecessorList().iterator().hasNext());
	}

	@Test
	void removeAnyDependenciesUnlinksIncidentLinkWhenOnlyOneEndpointIsSelected() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		DependencyService.getInstance().removeAnyDependencies(Arrays.asList(successor), this);

		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
		assertFalse(successor.getPredecessorList().iterator().hasNext());
	}

	@Test
	void removeAnyDependenciesSkipsReadonlyItemsButStillUnlinksEditablePairs() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask readOnly = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(readOnly);
		project.connectTask(successor);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		readOnly.setExternal(true);

		DependencyService.getInstance().removeAnyDependencies(Arrays.asList(predecessor, readOnly, successor), this);

		assertFalse(predecessor.getSuccessorList().iterator().hasNext());
		assertFalse(successor.getPredecessorList().iterator().hasNext());
	}

	@Test
	void incidentDependencySnapshotSupportsRemovingOnlyTheChosenLink() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("unlink-choice-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask selected = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(selected);
		project.connectTask(successor);
		Dependency incoming = DependencyService.getInstance().newDependency(predecessor, selected, DependencyType.FS, 0L, this);
		Dependency outgoing = DependencyService.getInstance().newDependency(selected, successor, DependencyType.SS, 0L, this);

		assertEquals(Arrays.asList(incoming, outgoing), DependencyService.getInstance().getIncidentDependencies(selected));
		DependencyService.getInstance().remove(incoming, this, true);
		assertEquals(0, selected.getPredecessorList().size());
		assertEquals(1, selected.getSuccessorList().size());
		assertSame(outgoing, selected.getSuccessorList().iterator().next());
	}

	private static Project createProject(String name) {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool(name, undoController);
		return Project.createProject(resourcePool, undoController);
	}

	private static final class SubprojectTask extends NormalTask implements SubProj {
		private final boolean writable;

		private SubprojectTask(Project project, boolean writable) {
			super(project);
			this.writable = writable;
		}

		public Project getSubproject() {
			return null;
		}

		public boolean isSubproject() {
			return true;
		}

		public boolean isSubprojectOpen() {
			return writable;
		}

		public boolean isValidAndOpen() {
			return writable;
		}

		public boolean isWritable() {
			return writable;
		}

		public long getSubprojectUniqueId() {
			return 0L;
		}

		public void setFetching(boolean b) {
		}

		public boolean isValid() {
			return writable;
		}

		public void setSubprojectFieldValues(java.util.Map subprojectFieldValues) {
		}

		public void setSubprojectUniqueId(long subprojectId) {
		}

		public void setSchedulesFromSubprojectFieldValues() {
		}
	}
}
