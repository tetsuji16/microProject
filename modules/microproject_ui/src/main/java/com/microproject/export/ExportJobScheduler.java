/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.util.Optional;
import java.util.function.Consumer;

final class ExportJobScheduler {
	private ExportJobScheduler() { }

	static void scheduleIfSelected(Optional<ExportTarget> target, Consumer<ExportTarget> scheduler) {
		target.ifPresent(scheduler);
	}

	static void scheduleIfSelected(Optional<ExportTarget> target, Consumer<ExportTarget> scheduler, Runnable onCancelled) {
		if (target.isPresent()) scheduler.accept(target.get());
		else onCancelled.run();
	}
}
