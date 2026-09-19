/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class ExportJobSchedulerTest {
	@Test
	void cancelledChooserDoesNotScheduleAJob() {
		AtomicReference<ExportTarget> scheduled = new AtomicReference<>();

		ExportJobScheduler.scheduleIfSelected(Optional.empty(), scheduled::set);

		assertFalse(scheduled.get() != null, "cancelled export must not enqueue a job");
	}

	@Test
	void selectedTargetIsScheduled() {
		ExportTarget target = new ExportTarget(new File("report.pdf"), ExportFormat.PDF);
		AtomicReference<ExportTarget> scheduled = new AtomicReference<>();

		ExportJobScheduler.scheduleIfSelected(Optional.of(target), scheduled::set);

		assertEquals(target, scheduled.get());
	}
}
