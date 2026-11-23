/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.graphic.model.transform.NodeCacheTransformer;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

class NodeCacheTransformerTest {

    @Test
    void projectSummaryTaskNotShownWhenFlagOff() {
        Project project = createTestProject();
        ReferenceNodeModelCache refCache = createRefCache(project);

        NodeCacheTransformer transformer = new NodeCacheTransformer("test", refCache, null);
        transformer.setShowProjectSummaryTask(false);

        VisibleNodes view = new VisibleNodes("test", transformer);
        VisibleDependencies deps = new VisibleDependencies("test-dependencies");
        view.setVisibleDependencies(deps);
        deps.setVisibleNodes(view);
        refCache.updateVisibleElements(view);

        List<GraphicNode> nodes = view.getElements();
        // Should not have project summary row at index 0
        assertFalse(nodes.isEmpty());
        // First node should be a task, not the project
        assertFalse(nodes.get(0).getNode().getImpl() instanceof Project);
    }

    @Test
    void projectSummaryTaskShownWhenFlagOn() {
        Project project = createTestProject();
        ReferenceNodeModelCache refCache = createRefCache(project);

        NodeCacheTransformer transformer = new NodeCacheTransformer("test", refCache, null);
        transformer.setShowProjectSummaryTask(true);

        VisibleNodes view = new VisibleNodes("test", transformer);
        VisibleDependencies deps = new VisibleDependencies("test-dependencies");
        view.setVisibleDependencies(deps);
        deps.setVisibleNodes(view);
        refCache.updateVisibleElements(view);

        List<GraphicNode> nodes = view.getElements();
        // Should have project summary row at index 0
        assertFalse(nodes.isEmpty());
        GraphicNode firstNode = nodes.get(0);
        assertTrue(firstNode.getNode().getImpl() instanceof Project, "First node should be Project");
        assertEquals(0, firstNode.getLevel(), "Project summary row should have level 0");
        assertTrue(firstNode.isSummary(), "Project summary row should be marked as summary");
        assertTrue(firstNode.isComposite(), "Project summary row should be marked as composite");
    }

    @Test
    void levelOffsetIncreasedWhenProjectSummaryTaskShown() {
        Project project = createTestProject();
        ReferenceNodeModelCache refCache = createRefCache(project);

        NodeCacheTransformer transformer = new NodeCacheTransformer("test", refCache, null);
        transformer.setShowProjectSummaryTask(true);

        VisibleNodes view = new VisibleNodes("test", transformer);
        VisibleDependencies deps = new VisibleDependencies("test-dependencies");
        view.setVisibleDependencies(deps);
        deps.setVisibleNodes(view);
        refCache.updateVisibleElements(view);

        List<GraphicNode> nodes = view.getElements();
        // Find a task node (should be at level 1 now instead of 0)
        boolean foundTask = false;
        for (GraphicNode node : nodes) {
            if (node.getNode().getImpl() instanceof NormalTask) {
                foundTask = true;
                assertEquals(1, node.getLevel(), "Task levels should be offset by 1");
            }
        }
        assertTrue(foundTask, "Should have at least one task node");
    }

    @Test
    void projectSummaryTaskEndDateMatchesProjectEnd() {
        Project project = createTestProject();
        ReferenceNodeModelCache refCache = createRefCache(project);

        NodeCacheTransformer transformer = new NodeCacheTransformer("test", refCache, null);
        transformer.setShowProjectSummaryTask(true);

        VisibleNodes view = new VisibleNodes("test", transformer);
        VisibleDependencies deps = new VisibleDependencies("test-dependencies");
        view.setVisibleDependencies(deps);
        deps.setVisibleNodes(view);
        refCache.updateVisibleElements(view);

        List<GraphicNode> nodes = view.getElements();
        GraphicNode projectSummaryNode = nodes.get(0);
        long projectEnd = project.getEnd();
        long summaryEnd = projectSummaryNode.getEnd();
        assertEquals(projectEnd, summaryEnd, "Project summary row end should match project end");
    }

    private Project createTestProject() {
        DataFactoryUndoController undo = new DataFactoryUndoController();
        ResourcePool pool = ResourcePool.createRourcePool("test", undo);
        Project project = Project.createProject(pool, undo);
        // Add a couple of tasks
        NormalTask task1 = project.newNormalTaskInstance();
        task1.setName("Task 1");
        NormalTask task2 = project.newNormalTaskInstance();
        task2.setName("Task 2");
        // Add tasks to all outlines so they appear in the hierarchy
        project.getTaskOutlines().addToAll(task1, null);
        project.getTaskOutlines().addToAll(task2, null);
        project.recalculate();
        return project;
    }

    private ReferenceNodeModelCache createRefCache(Project project) {
        ReferenceNodeModelCache refCache = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
        refCache.update();
        return refCache;
    }
}