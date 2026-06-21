package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.undo.DataFactoryUndoController;

class ProjectScheduleBehaviorTest {
	@Test
	void moveIntervalUpdatesProjectSpan() {
		Project project = createProject();
		long start = project.getStart();
		long end = project.getEffectiveWorkCalendar().add(start, 2L * day(), false);
		project.setStart(start);
		project.setEnd(end);
		ScheduleInterval oldInterval = new ScheduleInterval(start, end);

		long newStart = project.getEffectiveWorkCalendar().add(start, day(), false);
		long newEnd = project.getEffectiveWorkCalendar().add(end, day(), false);

		project.moveInterval(this, newStart, newEnd, oldInterval, false);

		assertEquals(newStart, project.getStart(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
		assertEquals(newEnd, project.getEnd(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
	}

	@Test
	void backupDetailRoundTripsProjectSpan() {
		Project project = createProject();
		long originalStart = project.getStart();
		long originalEnd = project.getEffectiveWorkCalendar().add(originalStart, 3L * day(), false);
		project.setStart(originalStart);
		project.setEnd(originalEnd);
		Object backup = project.backupDetail();

		assertNotNull(backup);

		long movedStart = project.getEffectiveWorkCalendar().add(originalStart, day(), false);
		long movedEnd = project.getEffectiveWorkCalendar().add(originalEnd, day(), false);
		project.setStart(movedStart);
		project.setEnd(movedEnd);

		project.restoreDetail(this, backup, false);

		assertEquals(originalStart, project.getStart(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
		assertEquals(originalEnd, project.getEnd(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
	}

	@Test
	void setDirtyUpdatesProjectFlag() {
		Project project = createProject();

		project.setDirty(true);

		assertTrue(project.isDirty());
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}
}
