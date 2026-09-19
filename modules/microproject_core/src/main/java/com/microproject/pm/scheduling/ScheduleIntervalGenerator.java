/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.scheduling;

/** Produces scheduled intervals for a callback. */
@FunctionalInterface
public interface ScheduleIntervalGenerator {
	void consumeIntervals(Object object, IntervalConsumer consumer);
}
