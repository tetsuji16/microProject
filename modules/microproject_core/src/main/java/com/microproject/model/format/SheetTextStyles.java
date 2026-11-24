/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.model.format;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.microproject.document.Document;
import com.microproject.model.task.Task;
import com.microproject.model.task.Project;
import com.microproject.model.resource.Resource;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.resource.TeamPlannerService;

/**
 * Container for per-category sheet text styles.
 * Persisted in MPO sidecar (key {@code sheetTextStyles}); not part of POD serialization.
 */
public final class SheetTextStyles {
    private final EnumMap<SheetTextStyleCategory, SheetTextFormat> formats;

    private SheetTextStyles(EnumMap<SheetTextStyleCategory, SheetTextFormat> formats) {
        this.formats = formats;
    }

    /** Creates an empty style set. */
    public static SheetTextStyles empty() {
        return new SheetTextStyles(new EnumMap<>(SheetTextStyleCategory.class));
    }

    /** Creates a deep copy of the given styles. */
    public static SheetTextStyles copyOf(SheetTextStyles other) {
        if (other == null) return empty();
        EnumMap<SheetTextStyleCategory, SheetTextFormat> copy = new EnumMap<>(SheetTextStyleCategory.class);
        other.formats.forEach(copy::put);
        return new SheetTextStyles(copy);
    }

    /** Returns the format for {@code category}, or {@code null} if not set. */
    public SheetTextFormat get(SheetTextStyleCategory category) {
        return formats.get(category);
    }

    /** Sets the format for {@code category}; {@code null} clears it. */
    public void set(SheetTextStyleCategory category, SheetTextFormat format) {
        if (format == null || format.isEmpty()) {
            formats.remove(category);
        } else {
            formats.put(category, format);
        }
    }

    /** Removes any format for {@code category}. */
    public void clear(SheetTextStyleCategory category) {
        formats.remove(category);
    }

    /** Returns an independent copy. */
    public SheetTextStyles copy() {
        return copyOf(this);
    }

    /** True when no category has a format set. */
    public boolean isEmpty() {
        return formats.isEmpty();
    }

    /**
     * Resolves the applicable sheet text format for a task.
     * Priority (highest first): MARKED_TASKS > CRITICAL_TASKS > MILESTONE_TASKS >
     * SUMMARY_TASKS > NONCRITICAL_TASKS > ALL.
     * Returns {@code null} if no format is set for any matching category.
     */
    public static SheetTextFormat resolve(Task task) {
        if (task == null) return null;
        Project project = (Project) task.getDocument();
        SheetTextStyles styles = project.getSheetTextStyles();
        if (styles == null || styles.isEmpty()) return null;

        if (task.isMarked()) {
            SheetTextFormat f = styles.get(SheetTextStyleCategory.MARKED_TASKS);
            if (f != null) return f;
        }
        // CRITICAL_TASKS
        if (task.isCritical()) {
            SheetTextFormat f = styles.get(SheetTextStyleCategory.CRITICAL_TASKS);
            if (f != null) return f;
        }
        // MILESTONE_TASKS
        if (task.isMilestone()) {
            SheetTextFormat f = styles.get(SheetTextStyleCategory.MILESTONE_TASKS);
            if (f != null) return f;
        }
        // SUMMARY_TASKS
        if (task.isWbsParent()) {
            SheetTextFormat f = styles.get(SheetTextStyleCategory.SUMMARY_TASKS);
            if (f != null) return f;
        }
        // NONCRITICAL_TASKS
        if (!task.isCritical()) {
            SheetTextFormat f = styles.get(SheetTextStyleCategory.NONCRITICAL_TASKS);
            if (f != null) return f;
        }
        // ALL
        return styles.get(SheetTextStyleCategory.ALL);
    }

    /**
     * Resolves the applicable sheet text format for a resource.
     * Priority: OVERALLOCATED_RESOURCES > ALL_RESOURCES.
     * Returns {@code null} if no format is set for any matching category.
     */
    public static SheetTextFormat resolve(Resource resource) {
        if (resource == null) return null;
        Document doc = resource.getDocument();
        Project project = null;
        if (doc instanceof Project) {
            project = (Project) doc;
        } else if (doc instanceof ResourcePool) {
            List<Project> projects = ((ResourcePool) doc).getProjects();
            if (!projects.isEmpty()) {
                project = projects.get(0);
            }
        }
        if (project == null) return null;
        SheetTextStyles styles = project.getSheetTextStyles();
        if (styles == null || styles.isEmpty()) return null;

        if (TeamPlannerService.isOverallocated(resource)) {
            SheetTextFormat f = styles.get(SheetTextStyleCategory.OVERALLOCATED_RESOURCES);
            if (f != null) return f;
        }
        return styles.get(SheetTextStyleCategory.ALL_RESOURCES);
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
}
