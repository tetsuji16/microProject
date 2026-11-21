/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.format;

/**
 * Text style categories for sheet text styles, matching Microsoft Project's
 * Text Styles dialog categories.
 * <p>
 * Priority order (highest first): MARKED_TASKS > CRITICAL_TASKS > MILESTONE_TASKS >
 * SUMMARY_TASKS > NONCRITICAL_TASKS > ALL. For resources: OVERALLOCATED_RESOURCES > ALL_RESOURCES.
 */
public enum SheetTextStyleCategory {
    /** All tasks and resources - lowest priority */
    ALL,
    /** Non-critical tasks (tasks with no special attributes) */
    NONCRITICAL_TASKS,
    /** Critical tasks (on the critical path) */
    CRITICAL_TASKS,
    /** Milestone tasks */
    MILESTONE_TASKS,
    /** Summary tasks */
    SUMMARY_TASKS,
    /** Marked tasks (flagged) - highest priority for tasks */
    MARKED_TASKS,
    /** Row and column titles (headers) */
    ROW_AND_COLUMN_TITLES,
    /** All resources */
    ALL_RESOURCES,
    /** Overallocated resources - higher priority than ALL_RESOURCES */
    OVERALLOCATED_RESOURCES
}