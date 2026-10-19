package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.pm.task.NormalTask;
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
