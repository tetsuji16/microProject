/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.view.network;

import com.microproject.pm.graphic.network.PertManualPositions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.grouping.core.Node;
import com.microproject.ui.view.model.cache.GraphicNode;
import com.microproject.ui.view.model.cache.NodeModelCacheFactory;
import com.microproject.ui.view.model.cache.ReferenceNodeModelCache;
import com.microproject.ui.view.pert.PertChart;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class NetworkManualPositionUndoTest {
	@Test
	void moveUndoRedoRefreshesLayoutAndLayoutNowIsUndoable() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("network-manual-undo", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("Manual position task");
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		PertChart[] chartHolder = new PertChart[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				PertChart chart = new PertChart(project, "Network");
				chart.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "pert"));
				chart.setCache(NodeModelCacheFactory.getInstance().createFilteredCache(reference, "Network", null));
				chart.getCache().update();
				chartHolder[0] = chart;
			});
			PertChart chart = chartHolder[0];
			GraphicNode node = (GraphicNode) chart.getCache().getGraphicNode((Node) project.getTaskModel().search(task));
			PertManualPositions positions = project.getOrCreateTransientDocumentState(
				PertManualPositions.class, PertManualPositions::new);
			PertManualPositions.Position moved = new PertManualPositions.Position(1100, 700);
			positions.putPosition(task.getUniqueId(), moved);
			chart.relayout();
			NetworkBoxMoveEdit move = new NetworkBoxMoveEdit(chart, task.getUniqueId(), null, moved, this);
			move.undo();
			assertEquals(null, positions.getPosition(task.getUniqueId()), "Undo should restore automatic placement");
			assertTrue(node.getPertCenter().getX() < moved.x(), "Undo should refresh the displayed automatic position");
			assertTrue(project.needsSaving(), "Undo of a position edit must leave the project saveable");
			move.redo();
			assertEquals(moved, positions.getPosition(task.getUniqueId()));
			assertEquals(moved.x(), node.getPertCenter().getX(), 0.01);

			Map<Long, PertManualPositions.Position> beforeLayout = Map.copyOf(positions.getPositions());
			positions.clear();
			NetworkLayoutNowEdit layoutNow = new NetworkLayoutNowEdit(chart, beforeLayout);
			layoutNow.undo();
			assertEquals(beforeLayout, positions.getPositions());
			layoutNow.redo();
			assertTrue(positions.isEmpty(), "Redo Layout Now should clear manual overrides");
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				if (chartHolder[0] != null) chartHolder[0].cleanUp();
			});
			reference.close();
		}
	}
}
