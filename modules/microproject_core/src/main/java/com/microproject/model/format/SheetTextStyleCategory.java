/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.model.format;

/**
 * Categories of sheet text styles, matching Microsoft Project's "Text Styles" dialog.
 * The declaration order defines the default resolution priority when multiple categories
 * apply to the same element: later entries override earlier ones.
 * {@link SheetTextStyles#resolve(Task)} and {@link SheetTextStyles#resolve(Resource)}
 * implement the explicit priority rules.
 */
public enum SheetTextStyleCategory {
    /** Base style for all tasks and resources when no specific category matches. */
    ALL,
    /** Non-critical tasks (slack > 0). */
    NONCRITICAL_TASKS,
    /** Critical tasks (slack == 0). */
    CRITICAL_TASKS,
    /** Milestone tasks (duration == 0). */
    MILESTONE_TASKS,
    /** Summary tasks (have children). */
    SUMMARY_TASKS,
    /** Tasks with the MSP-compatible Marked task field set to Yes. */
    MARKED_TASKS,
    /** Row and column header text. */
    ROW_AND_COLUMN_TITLES,
    /** Base style for all resources. */
    ALL_RESOURCES,
    /** Overallocated resources. */
    OVERALLOCATED_RESOURCES;
}
