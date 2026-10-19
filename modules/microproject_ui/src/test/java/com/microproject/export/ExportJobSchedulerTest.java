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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class ExportJobSchedulerTest {
	@Test
	void cancelledChooserDoesNotScheduleAJob() {
		AtomicReference<ExportTarget> scheduled = new AtomicReference<>();
		AtomicBoolean cleanedUp = new AtomicBoolean();

		ExportJobScheduler.scheduleIfSelected(Optional.empty(), scheduled::set, () -> cleanedUp.set(true));

		assertFalse(scheduled.get() != null, "cancelled export must not enqueue a job");
		assertEquals(true, cleanedUp.get(), "cancelled export must release the pageable");
	}

	@Test
	void selectedTargetIsScheduled() {
		ExportTarget target = new ExportTarget(new File("report.pdf"), ExportFormat.PDF);
		AtomicReference<ExportTarget> scheduled = new AtomicReference<>();

		ExportJobScheduler.scheduleIfSelected(Optional.of(target), scheduled::set);

		assertEquals(target, scheduled.get());
	}
}
