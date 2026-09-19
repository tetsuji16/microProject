/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.scheduling;

/** Callback used while iterating scheduled intervals. */
@FunctionalInterface
public interface IntervalConsumer extends Cloneable {
	void consumeInterval(ScheduleInterval interval);
}
