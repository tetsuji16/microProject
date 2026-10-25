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
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;

class PlannerIndependentFixtureImportTest {
	private static final String FIXTURE = "/mpxj-v11.5.4/planner-sample.planner";

	@Test
	void importsPlannerV2SampleSemanticsThroughMpoReload() throws Exception {
		// Independent literal expectations from the pinned Planner v2 XML fixture.
		String xml = new String(fixture().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(xml.contains("mrproject-version=\"2\""));
		assertTrue(xml.contains("<task id=\"1\" name=\"Task One\""));
		assertTrue(xml.contains("percent-complete=\"0\""));
		assertTrue(xml.contains("<predecessor id=\"1\" predecessor-id=\"1\" type=\"FS\"/>"));
		assertTrue(xml.contains("<resource id=\"1\" name=\"Test Resource\" short-name=\"\" type=\"1\" units=\"0\""));
		assertTrue(xml.contains("<allocations/>"));
		assertTrue(xml.contains("<default-week mon=\"0\" tue=\"0\" wed=\"0\" thu=\"0\" fri=\"0\" sat=\"1\" sun=\"1\"/>"));
		Project imported = new MspImporter().importProject(fixture(), "planner", (progress, label) -> {});
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(imported, saved));
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project reopened = reader.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertNotNull(reopened);
		List<Task> tasks = reopened.getTaskList();
		assertEquals(List.of("Task One", "Task Two"), tasks.stream().map(Task::getName).toList());
		assertEquals(0.0, tasks.get(0).getPercentComplete());
		assertEquals(0.0, tasks.get(1).getPercentComplete());
		assertEquals(1, tasks.get(1).getPredecessorList().size());
		assertEquals(tasks.get(0), ((com.microproject.pm.dependency.Dependency) tasks.get(1).getPredecessorList().get(0)).getPredecessor());
		assertEquals("Default", reopened.getWorkCalendar().getName());
		assertDefaultCalendarWeek(reopened.getWorkCalendar());
		List<Resource> resources = reopened.getResourcePool().getResourceList();
		assertEquals(1, resources.size());
		assertEquals("Test Resource", resources.get(0).getName());
		assertEquals(1.0, resources.get(0).getMaximumUnits());
	}

	private static void assertDefaultCalendarWeek(WorkCalendar calendar) {
		WorkingCalendar working = (WorkingCalendar) calendar;
		for (int day = 0; day < 7; day++) {
			WorkDay workDay = working.getConcreteInstance().getWeekDay(day);
			if (day == 0 || day == 6) {
				assertTrue(workDay == null || !workDay.isWorking(), "Planner default-week day " + day);
			} else {
				assertNotNull(workDay, "Planner default-week day " + day);
				assertTrue(workDay.isWorking(), "Planner default-week day " + day);
			}
		}
	}

	private static InputStream fixture() {
		InputStream input = PlannerIndependentFixtureImportTest.class.getResourceAsStream(FIXTURE);
		assertNotNull(input, "Pinned GNOME Planner v2 XML fixture");
		return input;
	}
}
