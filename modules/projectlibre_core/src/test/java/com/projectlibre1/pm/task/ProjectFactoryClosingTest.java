package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ProjectFactoryClosingTest {
	@Test
	void closeCallbacksRunOnlyAfterTheProjectLeavesTheClosingState() {
		ProjectFactory factory = ProjectFactory.createInstance();
		long projectId = 4242L;
		List<String> events = new ArrayList<>();

		factory.addClosingProject(projectId);
		factory.runAfterProjectClosed(projectId, () -> events.add("first"));
		factory.runAfterProjectClosed(projectId, () -> events.add("second"));

		assertTrue(factory.isProjectClosing(projectId));
		assertTrue(events.isEmpty());

		factory.completeProjectClosing(projectId);

		assertFalse(factory.isProjectClosing(projectId));
		assertEquals(List.of("first", "second"), events);
	}

	@Test
	void closeCallbackRunsImmediatelyWhenNoCloseIsPending() {
		ProjectFactory factory = ProjectFactory.createInstance();
		List<String> events = new ArrayList<>();

		factory.runAfterProjectClosed(5252L, () -> events.add("now"));

		assertEquals(List.of("now"), events);
	}

	@Test
	void completingABranchClearsEveryClosingIdBeforeCallbacksRun() {
		ProjectFactory factory = ProjectFactory.createInstance();
		Set<Long> ids = Set.of(6101L, 6102L);
		List<Boolean> childStillClosing = new ArrayList<>();
		factory.addClosingProjects(ids);
		factory.runAfterProjectClosed(6101L,
				() -> childStillClosing.add(factory.isProjectClosing(6102L)));

		factory.completeProjectClosings(ids);

		assertFalse(factory.isProjectClosing(6101L));
		assertFalse(factory.isProjectClosing(6102L));
		assertEquals(List.of(false), childStillClosing);
	}
}
