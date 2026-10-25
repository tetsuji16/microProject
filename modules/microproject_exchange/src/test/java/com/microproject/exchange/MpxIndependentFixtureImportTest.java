/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.core.pm.exchange.MspImporter;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkRange;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import net.sf.mpxj.RelationType;
import net.sf.mpxj.TimeUnit;

class MpxIndependentFixtureImportTest {
	private static final String FIXTURE = "/mpxj-v11.5.4/task-links-project98.mpx";
	private static final String CALENDAR_FIXTURE = "/mpxj-v11.5.4/calendar-calendars-project98.mpx";

	@Test
	void pinsProject98CalendarIdentityExpectationsFromMpxj() throws Exception {
		net.sf.mpxj.ProjectFile source = new net.sf.mpxj.mpx.MPXReader().read(calendarFixture());
		assertEquals("Standard", source.getCalendars().getByUniqueID(1).getName());
		assertEquals("Calendar1", source.getCalendars().getByUniqueID(2).getName());
		assertEquals("Calendar2", source.getCalendars().getByUniqueID(3).getName());
		assertEquals("Resource One", source.getCalendars().getByUniqueID(4).getName());
		assertEquals("Resource Two", source.getCalendars().getByUniqueID(5).getName());
	}

	@Test
	void importsProject98StandardCalendarThroughMpoReload() throws Exception {
		Project imported = new MspImporter().importProject(calendarFixture(), "mpx", (progress, label) -> {});
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(imported, saved));
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project reopened = reader.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertNotNull(reopened);
		assertStandardCalendar(reopened.getWorkCalendar());
	}

	@Test
	void importsIndependentProject98MpxTaskLinksThroughMpoReload() throws Exception {
		net.sf.mpxj.ProjectFile raw = new net.sf.mpxj.mpx.MPXReader().read(fixture());
		assertRawLink(raw, 1, 2, RelationType.FINISH_START, 0, TimeUnit.DAYS);
		assertRawLink(raw, 3, 4, RelationType.FINISH_START, 1, TimeUnit.DAYS);
		assertRawLink(raw, 5, 6, RelationType.FINISH_START, 2, TimeUnit.DAYS);
		assertRawLink(raw, 7, 8, RelationType.FINISH_START, 1, TimeUnit.WEEKS);
		assertRawLink(raw, 9, 10, RelationType.FINISH_START, 2, TimeUnit.WEEKS);
		assertRawLink(raw, 11, 12, RelationType.START_FINISH, 2, TimeUnit.DAYS);
		assertRawLink(raw, 13, 14, RelationType.START_START, 2, TimeUnit.DAYS);
		assertRawLink(raw, 15, 16, RelationType.FINISH_FINISH, 2, TimeUnit.DAYS);
		Project imported = new MspImporter().importProject(fixture(), "mpx", (progress, label) -> {});
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(imported, saved));
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project reopened = reader.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertNotNull(reopened);

		assertLink(reopened, 1, 2, DependencyType.Kind.FS, 0);
		assertLink(reopened, 3, 4, DependencyType.Kind.FS, 8);
		assertLink(reopened, 5, 6, DependencyType.Kind.FS, 16);
		assertLink(reopened, 7, 8, DependencyType.Kind.FS, 40);
		assertLink(reopened, 9, 10, DependencyType.Kind.FS, 80);
		assertLink(reopened, 11, 12, DependencyType.Kind.SF, 16);
		assertLink(reopened, 13, 14, DependencyType.Kind.SS, 16);
		assertLink(reopened, 15, 16, DependencyType.Kind.FF, 16);
	}

	private static void assertRawLink(net.sf.mpxj.ProjectFile project, int predecessorId, int successorId,
		RelationType type, double lag, TimeUnit units) {
		net.sf.mpxj.Task predecessor = project.getTaskByID(predecessorId);
		net.sf.mpxj.Task successor = project.getTaskByID(successorId);
		assertEquals("Task 1", predecessor.getName());
		assertEquals("Task 2", successor.getName());
		assertEquals(1, successor.getPredecessors().size());
		net.sf.mpxj.Relation relation = successor.getPredecessors().getFirst();
		assertEquals(predecessor, relation.getTargetTask());
		assertEquals(successor, relation.getSourceTask());
		assertEquals(type, relation.getType());
		assertEquals(units, relation.getLag().getUnits());
		assertEquals(lag, relation.getLag().getDuration());
	}

	private static void assertLink(Project project, int predecessorId, int successorId,
		DependencyType.Kind type, double lagHours) {
		List<Task> sourceTasks = project.getTaskList().stream()
			.filter(task -> "Task 1".equals(task.getName()) || "Task 2".equals(task.getName())).toList();
		assertEquals(16, sourceTasks.size(), "MPXJ TaskLinksTest uses ordered task IDs 1 through 16");
		for (int index = 0; index < sourceTasks.size(); index++)
			assertEquals(index % 2 == 0 ? "Task 1" : "Task 2", sourceTasks.get(index).getName(),
				"MPXJ TaskLinksTest source task ID " + (index + 1));
		Task predecessor = sourceTasks.get(predecessorId - 1);
		Task successor = sourceTasks.get(successorId - 1);
		assertEquals("Task 1", predecessor.getName());
		assertEquals("Task 2", successor.getName());
		List<Dependency> predecessors = successor.getPredecessorList().stream()
			.map(Dependency.class::cast).toList();
		assertEquals(1, predecessors.size());
		Dependency link = predecessors.getFirst();
		assertEquals(predecessor, link.getPredecessor());
		assertEquals(successor, link.getSuccessor());
		assertEquals(type, link.getDependencyKind());
		assertEquals(Math.round(lagHours * 60 * 60 * 1000), link.getLag(),
			"MPXJ v11.5.4 TaskLinksTest expects 8-hour days and 40-hour weeks");
	}

	private static void assertStandardCalendar(WorkCalendar calendar) {
		assertNotNull(calendar);
		assertEquals("Standard", calendar.getName());
		WorkingCalendar standard = (WorkingCalendar) calendar;
		for (int day = 0; day < 7; day++) {
			WorkDay weekDay = standard.getConcreteInstance().getWeekDay(day);
			if (day == 0 || day == 6) {
				assertTrue(weekDay == null || !weekDay.isWorking(), "MPXJ fixture weekend day " + day);
				continue;
			}
			assertNotNull(weekDay, "MPXJ fixture working day " + day);
			List<WorkRange> intervals = weekDay.getWorkingHours().getIntervals().stream()
				.filter(java.util.Objects::nonNull).toList();
			assertEquals(2, intervals.size(), "MPXJ fixture split shift day " + day);
			assertEquals(8 * 60 * 60 * 1000L, intervals.get(0).getStart());
			assertEquals(12 * 60 * 60 * 1000L, intervals.get(0).getEnd());
			assertEquals(13 * 60 * 60 * 1000L, intervals.get(1).getStart());
			assertEquals(17 * 60 * 60 * 1000L, intervals.get(1).getEnd());
		}
	}

	private static InputStream fixture() {
		InputStream input = MpxIndependentFixtureImportTest.class.getResourceAsStream(FIXTURE);
		assertNotNull(input, "Pinned independent MPX fixture");
		return input;
	}

	private static InputStream calendarFixture() {
		InputStream input = MpxIndependentFixtureImportTest.class.getResourceAsStream(CALENDAR_FIXTURE);
		assertNotNull(input, "Pinned independent MPX calendar fixture");
		return input;
	}
}
