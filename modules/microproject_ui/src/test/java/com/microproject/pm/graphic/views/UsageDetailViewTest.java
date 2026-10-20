package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.pm.task.Task;

class UsageDetailViewTest {
	@Test
	void distributionFieldsUseAssignmentFieldsWhenConfiguredArrayIsMissingOrEmpty() {
		Field work = field("Field.work");
		Field cost = field("Field.cost");

		SpreadSheetFieldArray missing = UsageDetailView.resolveDistributionFields(null, List.of(work, cost));
		SpreadSheetFieldArray empty = UsageDetailView.resolveDistributionFields(new SpreadSheetFieldArray(), List.of(work, cost));

		assertEquals(List.of(work, cost), missing);
		assertEquals(List.of(work, cost), empty);
	}

	@Test
	void configuredDistributionFieldsRemainPreferred() {
		Field configuredField = field("Field.work");
		Field fallbackField = field("Field.cost");
		SpreadSheetFieldArray configured = new SpreadSheetFieldArray();
		configured.add(configuredField);

		SpreadSheetFieldArray resolved = UsageDetailView.resolveDistributionFields(configured, List.of(fallbackField));

		assertSame(configured, resolved);
		assertEquals(List.of(configuredField), resolved);
	}

	@Test
	void selectedTaskZoomRangeIsEmptyWithoutUsableTaskDates() {
		assertNull(SelectedTasksTimescaleZoom.resolve(List.of()));
		assertNull(SelectedTasksTimescaleZoom.resolve(List.of(new Object())));
		Task undated = task(0, 0);
		Task inverted = task(20, 10);
		assertNull(SelectedTasksTimescaleZoom.resolve(List.of(undated, inverted)));
	}

	@Test
	void selectedTaskZoomRangeUsesSingleTaskDatesInEitherUsageView() {
		Task selected = task(100, 200);
		for (boolean taskUsage : List.of(true, false)) {
			SelectedTasksTimescaleZoom.DateRange range = SelectedTasksTimescaleZoom.resolve(List.of(selected));
			assertEquals(new SelectedTasksTimescaleZoom.DateRange(100, 200), range, "taskUsage=" + taskUsage);
		}
	}

	@Test
	void selectedTaskZoomRangeSpansMultipleTasksAndIgnoresUndatedTasks() {
		Task later = task(500, 700);
		Task earlier = task(100, 300);
		Task undated = task(0, 0);
		assertEquals(new SelectedTasksTimescaleZoom.DateRange(100, 700),
				SelectedTasksTimescaleZoom.resolve(List.of(later, undated, earlier)));
	}

	@Test
	void selectedZoomRangeCoversAssignmentRowsAndResourceRowsWithoutMutatingTheModel() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("usage-zoom-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Engineer");
		NormalTask first = projectTask(project, "First");
		NormalTask second = projectTask(project, "Second");
		Assignment a1 = AssignmentService.getInstance().newAssignment(first, resource, 1D, 0L, this);
		Assignment a2 = AssignmentService.getInstance().newAssignment(second, resource, 1D, 0L, this);
		a1.setStart(1_000_000L);
		a1.setEnd(2_000_000L);
		a2.setStart(5_000_000L);
		a2.setEnd(9_000_000L);
		long s1 = a1.getStart(), e1 = a1.getEnd(), s2 = a2.getStart(), e2 = a2.getEnd();
		long taskStart = first.getStart(), taskEnd = first.getEnd();

		assertEquals(new SelectedTasksTimescaleZoom.DateRange(s1, e1), SelectedTasksTimescaleZoom.resolve(List.of(a1)));
		assertEquals(new SelectedTasksTimescaleZoom.DateRange(Math.min(s1, s2), Math.max(e1, e2)),
				SelectedTasksTimescaleZoom.resolve(List.of(a2, a1)));
		assertEquals(new SelectedTasksTimescaleZoom.DateRange(Math.min(s1, s2), Math.max(e1, e2)),
				SelectedTasksTimescaleZoom.resolve(List.of(resource)));
		assertEquals(new SelectedTasksTimescaleZoom.DateRange(taskStart, taskEnd),
				SelectedTasksTimescaleZoom.resolve(List.of(first)));
		assertNull(SelectedTasksTimescaleZoom.resolve(List.of(pool.newResourceInstance())));
		assertEquals(List.of(s1, e1, s2, e2, taskStart, taskEnd),
				List.of(a1.getStart(), a1.getEnd(), a2.getStart(), a2.getEnd(), first.getStart(), first.getEnd()));
	}

	private static NormalTask projectTask(Project project, String name) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(java.util.concurrent.TimeUnit.DAYS.toMillis(1));
		return task;
	}

	private static Task task(long start, long end) {
		Task task = new NormalTask();
		task.getCurrentSchedule().setStart(start);
		task.getCurrentSchedule().setEnd(end);
		return task;
	}

	private static Field field(String id) {
		Field field = new Field();
		field.setId(id);
		return field;
	}
}
