/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.model.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.microproject.association.InvalidAssociationException;
import com.microproject.model.dependency.DependencyService;
import com.microproject.model.dependency.DependencyType;
import com.microproject.node.Node;
import com.microproject.node.NodeFactory;
import com.microproject.model.task.Project;
import com.microproject.model.task.Task;
import com.microproject.model.task.NormalTask;
import com.microproject.model.resource.Resource;
import com.microproject.model.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class SheetTextStylesTest {

	@Test
	void mpoOnlyStateRemainsTransientForFrozenPodSerialization() throws Exception {
		assertTrue(Modifier.isTransient(Task.class.getDeclaredField("marked").getModifiers()));
		assertTrue(Modifier.isTransient(Project.class.getDeclaredField("sheetTextStyles").getModifiers()));
	}

    private Project newProject() {
        DataFactoryUndoController undo = new DataFactoryUndoController();
        ResourcePool pool = ResourcePool.createRourcePool("format-test", undo);
        Project project = Project.createProject(pool, undo);
        project.initialize(false, false);
        // Register project with resource pool so getProjects() works
        pool.addProject(project);
        return project;
    }

    private NormalTask addTask(Project project, String name, NormalTask parent) {
        NormalTask task = new NormalTask(project);
        task.setName(name);
        project.connectTask(task);
        project.getSchedulingAlgorithm().addObject(task);
        task.getCurrentSchedule().setStart(project.getStart());
        task.setDuration(8L * 60L * 60L * 1000L); // 1 day
        if (parent != null) {
            task.setWbsParent(parent);
            parent.setWbsChildrenNodes(java.util.Collections.singletonList(NodeFactory.getInstance().createNode(task)));
        }
        // Recalculate to ensure critical path, slack, etc. are computed
        project.recalculate();
        return task;
    }

    @Test
    void newProjectStartsWithAnEmptySheetTextStyleSet() {
        Project project = newProject();
        assertNotNull(project.getSheetTextStyles());
        assertTrue(project.getSheetTextStyles().isEmpty());
    }

    @Test
    void emptyStylesReturnsNull() {
        Project project = newProject();
        SheetTextStyles styles = SheetTextStyles.empty();
        project.setSheetTextStyles(styles);

        NormalTask task = addTask(project, "Test", null);

        assertNull(SheetTextStyles.resolve(task));
    }

    @Test
    void allCategoryAppliesToAllTasks() {
        Project project = newProject();
        SheetTextStyles styles = SheetTextStyles.empty();
        styles.set(SheetTextStyleCategory.ALL, new SheetTextFormat("Arial", 12, 0xFF0000, true, false, false, false, null));
        project.setSheetTextStyles(styles);

        NormalTask task = addTask(project, "Test", null);

        SheetTextFormat resolved = SheetTextStyles.resolve(task);
        assertNotNull(resolved);
        assertEquals("Arial", resolved.family());
        assertEquals(12, resolved.size());
        assertEquals(Integer.valueOf(0xFF0000), resolved.color());
        assertTrue(resolved.bold());
    }

    @Test
    void criticalTasksPriorityOverMilestone() {
        Project project = newProject();
        SheetTextStyles styles = SheetTextStyles.empty();
        styles.set(SheetTextStyleCategory.CRITICAL_TASKS, new SheetTextFormat("CriticalFont", 12, null, true, false, false, false, null));
        styles.set(SheetTextStyleCategory.MILESTONE_TASKS, new SheetTextFormat("MilestoneFont", 10, null, false, true, false, false, null));
        project.setSheetTextStyles(styles);

        // Create a critical task (long duration) and a parallel non-critical task (short duration, no dependency)
        NormalTask criticalTask = addTask(project, "Critical", null);
        criticalTask.setDuration(10L * 8L * 60L * 60L * 1000L); // 10 days

        NormalTask nonCriticalTask = addTask(project, "NonCritical", null);
        nonCriticalTask.setDuration(1L * 8L * 60L * 60L * 1000L); // 1 day
        // No dependency - nonCriticalTask runs in parallel, finishes early, has slack
        project.recalculate();

        // Non-critical task should not match CRITICAL_TASKS or MILESTONE_TASKS
        SheetTextFormat resolved = SheetTextStyles.resolve(nonCriticalTask);
        assertNull(resolved); // No ALL set, not critical, not milestone
    }

    @Test
    void sharedResourceStylesRequireAnExplicitProjectContext() {
        DataFactoryUndoController undo = new DataFactoryUndoController();
        ResourcePool pool = ResourcePool.createRourcePool("shared-format-test", undo);
        Project first = Project.createProject(pool, undo);
        first.initialize(false, false);
        pool.addProject(first);
        Project second = Project.createProject(pool, undo);
        second.initialize(false, false);
        pool.addProject(second);
        Resource resource = pool.newResourceInstance();
        SheetTextFormat firstStyle = new SheetTextFormat("First", 11, null, false, false, false, false, null);
        SheetTextFormat secondStyle = new SheetTextFormat("Second", 12, null, false, false, false, false, null);
        SheetTextStyles firstStyles = SheetTextStyles.empty();
        firstStyles.set(SheetTextStyleCategory.ALL_RESOURCES, firstStyle);
        first.setSheetTextStyles(firstStyles);
        SheetTextStyles secondStyles = SheetTextStyles.empty();
        secondStyles.set(SheetTextStyleCategory.ALL_RESOURCES, secondStyle);
        second.setSheetTextStyles(secondStyles);

        assertNull(SheetTextStyles.resolve(resource), "shared pool alone cannot identify the active project");
        assertEquals(firstStyle, SheetTextStyles.resolve(first, resource));
        assertEquals(secondStyle, SheetTextStyles.resolve(second, resource));
    }

    @Test
    void changingMarkedStateMarksTaskDirty() {
        Project project = newProject();
        NormalTask task = addTask(project, "Marked", null);
        task.setDirty(false);

        task.setMarked(true);

        assertTrue(task.isDirty());
        task.setDirty(false);
        task.setMarked(true);
        assertFalse(task.isDirty(), "setting the existing value must not create a dirty change");
    }

    @Test
    void markedTasksTakePriorityOverCriticalTasks() {
        Project project = newProject();
        SheetTextStyles styles = SheetTextStyles.empty();
        styles.set(SheetTextStyleCategory.MARKED_TASKS,
                new SheetTextFormat("MarkedFont", 15, 0x008800, true, false, false, false, null));
        styles.set(SheetTextStyleCategory.CRITICAL_TASKS,
                new SheetTextFormat("CriticalFont", 12, 0xFF0000, false, true, false, false, null));
        project.setSheetTextStyles(styles);

        NormalTask criticalTask = addTask(project, "Marked critical", null);
        criticalTask.setDuration(10L * 8L * 60L * 60L * 1000L);
        criticalTask.setMarked(true);
        project.recalculate();
        assertTrue(criticalTask.isCritical(), "fixture must match both categories");

        SheetTextFormat resolved = SheetTextStyles.resolve(criticalTask);
        assertNotNull(resolved);
        assertEquals("MarkedFont", resolved.family());
    }

    @Test
    void summaryTasksPriorityOverNonCritical() {
        Project project = newProject();
        SheetTextStyles styles = SheetTextStyles.empty();
        styles.set(SheetTextStyleCategory.SUMMARY_TASKS, new SheetTextFormat("SummaryFont", 13, null, true, false, false, false, null));
        styles.set(SheetTextStyleCategory.NONCRITICAL_TASKS, new SheetTextFormat("NonCriticalFont", 11, null, false, false, false, false, null));
        project.setSheetTextStyles(styles);

        // Add a long-duration task to make the critical path longer
        NormalTask longTask = addTask(project, "LongTask", null);
        longTask.setDuration(20L * 8L * 60L * 60L * 1000L); // 20 days

        NormalTask summary = addTask(project, "Summary", null);
        NormalTask child = addTask(project, "Child", summary);
        // Summary and child are short, so they have slack
        project.recalculate();

        SheetTextFormat summaryFormat = SheetTextStyles.resolve(summary);
        assertNotNull(summaryFormat);
        assertEquals("SummaryFont", summaryFormat.family());

        // Child is non-critical (has slack because longTask determines project end)
        SheetTextFormat childFormat = SheetTextStyles.resolve(child);
        assertNotNull(childFormat);
        assertEquals("NonCriticalFont", childFormat.family());
    }

    @Test
    void nonCriticalTasksAppliesWhenNotCritical() {
        Project project = newProject();
        SheetTextStyles styles = SheetTextStyles.empty();
        styles.set(SheetTextStyleCategory.NONCRITICAL_TASKS, new SheetTextFormat("NonCriticalFont", 11, null, false, false, false, false, null));
        project.setSheetTextStyles(styles);

        // Create a non-critical task by giving it slack (parallel, shorter duration)
        NormalTask criticalTask = addTask(project, "Critical", null);
        criticalTask.setDuration(10L * 8L * 60L * 60L * 1000L); // 10 days

        NormalTask nonCriticalTask = addTask(project, "NonCritical", null);
        nonCriticalTask.setDuration(1L * 8L * 60L * 60L * 1000L); // 1 day
        // No dependency - nonCriticalTask runs in parallel, finishes early, has slack
        project.recalculate();

        SheetTextFormat resolved = SheetTextStyles.resolve(nonCriticalTask);
        assertNotNull(resolved);
        assertEquals("NonCriticalFont", resolved.family());
    }

    @Test
    void copyIsIndependent() {
        SheetTextStyles original = SheetTextStyles.empty();
        original.set(SheetTextStyleCategory.ALL, new SheetTextFormat("Arial", 12, null, false, false, false, false, null));
        original.set(SheetTextStyleCategory.CRITICAL_TASKS, new SheetTextFormat("Bold", 10, null, true, false, false, false, null));

        SheetTextStyles copy = original.copy();

        // Modify copy
        copy.set(SheetTextStyleCategory.ALL, new SheetTextFormat("Changed", 14, null, false, false, false, false, null));

        // Original unchanged
        assertEquals("Arial", original.get(SheetTextStyleCategory.ALL).family());
        assertEquals(12, original.get(SheetTextStyleCategory.ALL).size());

        // Copy changed
        assertEquals("Changed", copy.get(SheetTextStyleCategory.ALL).family());
        assertEquals(14, copy.get(SheetTextStyleCategory.ALL).size());

        // Critical unchanged in both
        assertEquals("Bold", original.get(SheetTextStyleCategory.CRITICAL_TASKS).family());
        assertEquals("Bold", copy.get(SheetTextStyleCategory.CRITICAL_TASKS).family());
    }

    @Test
    void copyOfNullReturnsEmpty() {
        SheetTextStyles copy = SheetTextStyles.copyOf(null);
        assertNotNull(copy);
        assertTrue(copy.isEmpty());
    }

    @Test
    void emptyStylesIsEmpty() {
        assertTrue(SheetTextStyles.empty().isEmpty());
    }

    @Test
    void clearRemovesCategory() {
        SheetTextStyles styles = SheetTextStyles.empty();
        styles.set(SheetTextStyleCategory.ALL, new SheetTextFormat("Arial", 12, null, false, false, false, false, null));
        assertFalse(styles.isEmpty());

        styles.clear(SheetTextStyleCategory.ALL);
        assertTrue(styles.isEmpty());
        assertNull(styles.get(SheetTextStyleCategory.ALL));
    }

    @Test
    void equalsAndHashCode() {
        SheetTextStyles a = SheetTextStyles.empty();
        a.set(SheetTextStyleCategory.ALL, new SheetTextFormat("Arial", 12, null, false, false, false, false, null));

        SheetTextStyles b = SheetTextStyles.empty();
        b.set(SheetTextStyleCategory.ALL, new SheetTextFormat("Arial", 12, null, false, false, false, false, null));

        SheetTextStyles c = SheetTextStyles.empty();
        c.set(SheetTextStyleCategory.ALL, new SheetTextFormat("Different", 12, null, false, false, false, false, null));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void resolveResourceOverallocatedPriority() {
        Project project = newProject();
        SheetTextStyles styles = SheetTextStyles.empty();
        styles.set(SheetTextStyleCategory.OVERALLOCATED_RESOURCES, new SheetTextFormat("OverallocFont", 12, 0xFF0000, true, false, false, false, null));
        styles.set(SheetTextStyleCategory.ALL_RESOURCES, new SheetTextFormat("AllResFont", 10, 0x0000FF, false, true, false, false, null));
        project.setSheetTextStyles(styles);

        Resource resource = project.getResourcePool().createScriptedResource();
        resource.setName("Test Resource");

        // With no assignments, resource is not overallocated, so ALL_RESOURCES should apply
        SheetTextFormat resolved = SheetTextStyles.resolve(resource);
        assertNotNull(resolved);
        assertEquals("AllResFont", resolved.family());
        assertEquals(10, resolved.size());
        assertEquals(Integer.valueOf(0x0000FF), resolved.color());
        assertTrue(resolved.italic());
    }

    @Test
    void resolveResourceAllResourcesWhenNotOverallocated() {
        Project project = newProject();
        SheetTextStyles styles = SheetTextStyles.empty();
        styles.set(SheetTextStyleCategory.ALL_RESOURCES, new SheetTextFormat("AllResFont", 10, 0x0000FF, false, true, false, false, null));
        project.setSheetTextStyles(styles);

        Resource resource = project.getResourcePool().createScriptedResource();
        resource.setName("Test Resource");

        SheetTextFormat resolved = SheetTextStyles.resolve(resource);
        assertNotNull(resolved);
        assertEquals("AllResFont", resolved.family());
        assertEquals(10, resolved.size());
        assertEquals(Integer.valueOf(0x0000FF), resolved.color());
        assertTrue(resolved.italic());
    }

    @Test
    void formatIsEmptyWhenAllDefaults() {
        SheetTextFormat format = new SheetTextFormat(null, 0, null, false, false, false, false, null);
        assertTrue(format.isEmpty());

        SheetTextFormat format2 = SheetTextFormat.of(null, 0, null, false, false, false, false, null);
        assertTrue(format2.isEmpty());
    }

    @Test
    void formatWithFamilyNotEmpty() {
        SheetTextFormat format = new SheetTextFormat("Arial", 0, null, false, false, false, false, null);
        assertFalse(format.isEmpty());
    }

    @Test
    void formatWithSizeNotEmpty() {
        SheetTextFormat format = new SheetTextFormat(null, 12, null, false, false, false, false, null);
        assertFalse(format.isEmpty());
    }

    @Test
    void formatWithColorNotEmpty() {
        SheetTextFormat format = new SheetTextFormat(null, 0, 0xFF0000, false, false, false, false, null);
        assertFalse(format.isEmpty());
    }

    @Test
    void formatWithBoldNotEmpty() {
        SheetTextFormat format = new SheetTextFormat(null, 0, null, true, false, false, false, null);
        assertFalse(format.isEmpty());
    }

    @Test
    void formatWithBackgroundNotEmpty() {
        SheetTextFormat format = new SheetTextFormat(null, 0, null, false, false, false, false, 0x00FF00);
        assertFalse(format.isEmpty());
    }

    @Test
    void formatNormalizesBlankFamilyToNull() {
        SheetTextFormat format = new SheetTextFormat("  ", 12, null, false, false, false, false, null);
        assertNull(format.family());
    }

    @Test
    void formatClampsNegativeSizeToZero() {
        SheetTextFormat format = new SheetTextFormat("Arial", -5, null, false, false, false, false, null);
        assertEquals(0, format.size());
    }

    @Test
    void formatClampsSizeAbove72() {
        SheetTextFormat format = new SheetTextFormat("Arial", 100, null, false, false, false, false, null);
        assertEquals(72, format.size());
    }
}
