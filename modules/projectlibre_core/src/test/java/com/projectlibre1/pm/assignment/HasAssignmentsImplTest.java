package com.projectlibre1.pm.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.resource.ResourceImpl;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.time.HasStartAndEnd;
import com.projectlibre1.undo.DataFactoryUndoController;

class HasAssignmentsImplTest {
	@Test
	void addRemoveAndFindAssignmentByTaskAndResource() {
		Project project = createProject();
		NormalTask task = createTask(project);
		Assignment assignment = firstAssignment(task);
		ResourceImpl resource = (ResourceImpl) assignment.getResource();

		HasAssignmentsImpl hasAssignments = new HasAssignmentsImpl();
		hasAssignments.addAssignment(assignment);

		assertSame(assignment, hasAssignments.findAssignment(task));
		assertSame(assignment, hasAssignments.findAssignment(resource));

		hasAssignments.removeAssignment(assignment);

		assertNull(hasAssignments.findAssignment(task));
		assertNull(hasAssignments.findAssignment(resource));
	}

	@Test
	void cloneWithTaskRebindsAssignmentsToClonedTask() {
		Project project = createProject();
		NormalTask originalTask = createTask(project);
		NormalTask clonedTask = createTask(project);
		Assignment assignment = firstAssignment(originalTask);

		HasAssignmentsImpl hasAssignments = new HasAssignmentsImpl();
		hasAssignments.addAssignment(assignment);

		HasAssignmentsImpl cloned = (HasAssignmentsImpl) hasAssignments.cloneWithTask(clonedTask);
		Assignment clonedAssignment = (Assignment) cloned.getAssignments().getFirst();

		assertSame(clonedTask, clonedAssignment.getTask());
		assertSame(assignment.getResource(), clonedAssignment.getResource());
	}

	@Test
	void deepCloneWithTaskUsesTheSameRebindingPath() {
		Project project = createProject();
		NormalTask originalTask = createTask(project);
		NormalTask clonedTask = createTask(project);
		Assignment assignment = firstAssignment(originalTask);

		HasAssignmentsImpl hasAssignments = new HasAssignmentsImpl();
		hasAssignments.addAssignment(assignment);

		HasAssignmentsImpl cloned = (HasAssignmentsImpl) hasAssignments.deepCloneWithTask(clonedTask);
		Assignment clonedAssignment = (Assignment) cloned.getAssignments().getFirst();

		assertSame(clonedTask, clonedAssignment.getTask());
		assertSame(assignment.getResource(), clonedAssignment.getResource());
	}

	@Test
	void forEachWorkingIntervalProducesAssignmentWindowForSingleAssignment() {
		Project project = createProject();
		NormalTask task = createTask(project);
		long duration = 2L * CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(duration);

		HasAssignmentsImpl hasAssignments = new HasAssignmentsImpl();
		hasAssignments.addAssignment(firstAssignment(task));

		List<long[]> intervals = new ArrayList<long[]>();
		hasAssignments.forEachWorkingInterval(new Consumer<Object>() { public void accept(Object arg0) {
				HasStartAndEnd interval = (HasStartAndEnd) arg0;
				intervals.add(new long[] { interval.getStart(), interval.getEnd() });
			}
		}, false, task.getEffectiveWorkCalendar());

		assertEquals(1, intervals.size());
		assertEquals(task.getStart(), intervals.get(0)[0]);
		assertEquals(duration,
				task.getEffectiveWorkCalendar().compare(intervals.get(0)[1], intervals.get(0)[0], false));
		assertEquals(duration, hasAssignments.calcActiveAssignmentDuration(task.getEffectiveWorkCalendar()));
		assertTrue(hasAssignments.work(task.getStart(), task.getEnd()) > 0);
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}

	private Assignment firstAssignment(NormalTask task) {
		Iterator iterator = task.getAssignments().iterator();
		return (Assignment) iterator.next();
	}
}
