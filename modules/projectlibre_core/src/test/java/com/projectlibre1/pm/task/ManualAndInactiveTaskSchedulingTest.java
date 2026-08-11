package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.dependency.DependencyService;
import com.projectlibre1.pm.dependency.DependencyType;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.undo.DataFactoryUndoController;

class ManualAndInactiveTaskSchedulingTest {
	@Test
	void manuallyScheduledDatesSurviveFullRecalculation() {
		Project project = project();
		NormalTask task = task(project, "Draft");
		long start = project.getWorkCalendar().add(project.getStart(), day() * 5L, false);
		long finish = project.getWorkCalendar().add(start, day() * 2L, false);

		task.setManualDates(start, finish);
		project.recalculate();

		assertTrue(task.isManuallyScheduled());
		assertEquals(start, task.getStart());
		assertEquals(finish, task.getEnd());
	}

	@Test
	void inactivePredecessorDoesNotPushSuccessor() throws Exception {
		Project project = project();
		NormalTask inactive = task(project, "Alternative");
		NormalTask successor = task(project, "Committed");
		inactive.setDuration(day() * 10L);
		successor.setDuration(day());
		DependencyService.getInstance().newDependency(inactive, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		long linkedStart = successor.getStart();

		inactive.setInactiveTask(true);
		project.recalculate();

		assertTrue(inactive.isInactiveTask());
		assertTrue(successor.getStart() <= linkedStart);
		inactive.setInactiveTask(false);
		assertFalse(inactive.isInactiveTask());
	}

	private Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("manual-test", undo), undo);
		project.initialize(false, false);
		return project;
	}

	private NormalTask task(Project project, String name) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(day());
		return task;
	}

	private long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}
}
