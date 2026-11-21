/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.format;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.microproject.pm.resource.TeamPlannerService;

/**
 * Container for text styles by category, used in sheet views.
 * <p>
 * Stores an EnumMap of category to format. Provides resolution logic for tasks
 * and resources with Microsoft Project-compatible priority ordering.
 */
public final class SheetTextStyles implements Serializable {

    private static final long serialVersionUID = 1L;

    private final EnumMap<SheetTextStyleCategory, SheetTextFormat> formats;

    private SheetTextStyles(EnumMap<SheetTextStyleCategory, SheetTextFormat> formats) {
        this.formats = formats;
    }

    /**
     * Creates an empty text styles container.
     */
    public static SheetTextStyles empty() {
        return new SheetTextStyles(new EnumMap<>(SheetTextStyleCategory.class));
    }

    /**
     * Returns the format for the given category, or null if not set.
     */
    public SheetTextFormat get(SheetTextStyleCategory category) {
        return formats.get(category);
    }

    /**
     * Sets the format for the given category.
     * If format is null or empty, the category is removed.
     */
    public void set(SheetTextStyleCategory category, SheetTextFormat format) {
        if (format == null || format.isEmpty()) {
            formats.remove(category);
        } else {
            formats.put(category, format);
        }
    }

    /**
     * Clears the format for the given category.
     */
    public void clear(SheetTextStyleCategory category) {
        formats.remove(category);
    }

    /**
     * Returns a deep copy of this SheetTextStyles.
     */
    public SheetTextStyles copy() {
        EnumMap<SheetTextStyleCategory, SheetTextFormat> copy = new EnumMap<>(SheetTextStyleCategory.class);
        formats.forEach(copy::put);
        return new SheetTextStyles(copy);
    }

    /**
     * Returns true if no formats are set.
     */
    public boolean isEmpty() {
        return formats.isEmpty();
    }

    /**
     * Resolves the text format for a task based on its attributes.
     * Priority order (highest first):
     * MARKED_TASKS > CRITICAL_TASKS > MILESTONE_TASKS > SUMMARY_TASKS > NONCRITICAL_TASKS > ALL
     *
     * @param task the task to resolve format for
     * @return the format for the highest-priority matching category, or null if none set
     */
    public SheetTextFormat resolve(com.microproject.pm.task.Task task) {
        if (task == null) {
            return formats.get(SheetTextStyleCategory.ALL);
        }

        // Priority order: MARKED_TASKS > CRITICAL_TASKS > MILESTONE_TASKS > SUMMARY_TASKS > NONCRITICAL_TASKS > ALL
        if (task.isMarked() && formats.containsKey(SheetTextStyleCategory.MARKED_TASKS)) {
            return formats.get(SheetTextStyleCategory.MARKED_TASKS);
        }
        if (task.isCritical() && formats.containsKey(SheetTextStyleCategory.CRITICAL_TASKS)) {
            return formats.get(SheetTextStyleCategory.CRITICAL_TASKS);
        }
        if (task.isMilestone() && formats.containsKey(SheetTextStyleCategory.MILESTONE_TASKS)) {
            return formats.get(SheetTextStyleCategory.MILESTONE_TASKS);
        }
        if (task.isSummary() && formats.containsKey(SheetTextStyleCategory.SUMMARY_TASKS)) {
            return formats.get(SheetTextStyleCategory.SUMMARY_TASKS);
        }
        if (formats.containsKey(SheetTextStyleCategory.NONCRITICAL_TASKS)) {
            return formats.get(SheetTextStyleCategory.NONCRITICAL_TASKS);
        }
        return formats.get(SheetTextStyleCategory.ALL);
    }

    /**
     * Resolves the text format for a resource based on its attributes.
     * Priority order: OVERALLOCATED_RESOURCES > ALL_RESOURCES > ALL
     *
     * @param resource the resource to resolve format for
     * @return the format for the highest-priority matching category, or null if none set
     */
    public SheetTextFormat resolve(com.microproject.pm.resource.Resource resource) {
    		if (resource == null) {
    			return formats.get(SheetTextStyleCategory.ALL);
    		}

    		// Priority: OVERALLOCATED_RESOURCES > ALL_RESOURCES > ALL
    		if (TeamPlannerService.isOverallocated(resource) && formats.containsKey(SheetTextStyleCategory.OVERALLOCATED_RESOURCES)) {
    			return formats.get(SheetTextStyleCategory.OVERALLOCATED_RESOURCES);
    		}
    		if (formats.containsKey(SheetTextStyleCategory.ALL_RESOURCES)) {
    			return formats.get(SheetTextStyleCategory.ALL_RESOURCES);
    		}
    		return formats.get(SheetTextStyleCategory.ALL);
    	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SheetTextStyles that)) return false;
        return Objects.equals(formats, that.formats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formats);
    }

    @Override
    public String toString() {
        return "SheetTextStyles{" + formats + "}";
    }
}