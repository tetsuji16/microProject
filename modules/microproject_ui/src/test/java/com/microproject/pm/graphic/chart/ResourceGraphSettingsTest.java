/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.chart;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.junit.jupiter.api.Test;

import com.microproject.dialog.CalendarViewSettings.TextStyle;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.BarStyle;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.GraphType;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.Item;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.Pattern;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.ShowAs;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/** Persistence of the Resource Graph Format settings and the Previous/Next Resource navigation. */
class ResourceGraphSettingsTest {
	private static ChartInfo newGraph() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("resource-graph", undo), undo);
		project.initialize(false, false);
		ChartInfo chartInfo = new ChartInfo();
		chartInfo.setMode(ChartMode.RESOURCE_GRAPH);
		chartInfo.setProject(project);
		chartInfo.setCoord(new CoordinatesConverter(project));
		chartInfo.getModel().computeHistogram(project, List.of(), List.of(), chartInfo.getTraces());
		chartInfo.setChartPanel(new TimeChartPanel(chartInfo));
		return chartInfo;
	}

	private static ResourceGraphSettings customised() {
		ResourceGraphSettings s = new ResourceGraphSettings();
		s.setGraphType(GraphType.CUMULATIVE_COST);
		s.setStyle(Item.OVERALLOCATED_FILTERED, new BarStyle(ShowAs.STEP_LINE, 0x123456, Pattern.CROSS_HATCH));
		s.setStyle(Item.ALLOCATED_RESOURCE, new BarStyle(ShowAs.AREA, 0xABCDEF, Pattern.DOTS));
		s.setShowValues(true);
		s.setShowAvailabilityLine(false);
		s.setHorizontalGridlines(false);
		s.setVerticalGridlines(true);
		s.setTextStyle(new TextStyle(14, true));
		return s;
	}

	@Test
	void settingsRoundTripThroughTheirTextBlob() {
		ResourceGraphSettings source = customised();
		ResourceGraphSettings copy = new ResourceGraphSettings();
		copy.restoreSettings(source.saveSettings());
		assertEquals(GraphType.CUMULATIVE_COST, copy.getGraphType());
		assertEquals(new BarStyle(ShowAs.STEP_LINE, 0x123456, Pattern.CROSS_HATCH), copy.getStyle(Item.OVERALLOCATED_FILTERED));
		assertEquals(new BarStyle(ShowAs.AREA, 0xABCDEF, Pattern.DOTS), copy.getStyle(Item.ALLOCATED_RESOURCE));
		assertEquals(Item.ALLOCATED_FILTERED.defaultStyle(), copy.getStyle(Item.ALLOCATED_FILTERED));
		assertTrue(copy.isShowValues());
		assertFalse(copy.isShowAvailabilityLine());
		assertFalse(copy.isHorizontalGridlines());
		assertTrue(copy.isVerticalGridlines());
		assertEquals(new TextStyle(14, true), copy.getTextStyle());
		assertEquals(source.saveSettings(), copy.saveSettings());
	}

	@Test
	void missingOrMalformedStateGivesTheDefaults() {
		ResourceGraphSettings defaults = new ResourceGraphSettings();
		assertEquals("", defaults.saveSettings());
		for (String state : new String[] { null, "", "   ", "\u0000garbage", "graph=NOPE\nstyle.ALLOCATED_FILTERED=x,y\ntext=a,b" }) {
			ResourceGraphSettings s = customised();
			assertDoesNotThrow(() -> s.restoreSettings(state));
			assertEquals(GraphType.PEAK_UNITS, s.getGraphType(), String.valueOf(state));
			assertEquals(Item.ALLOCATED_FILTERED.defaultStyle(), s.getStyle(Item.ALLOCATED_FILTERED));
			assertFalse(s.isShowValues());
			assertTrue(s.isShowAvailabilityLine());
			assertTrue(s.isHorizontalGridlines());
			assertEquals(TextStyle.DEFAULT, s.getTextStyle());
		}
	}

	@Test
	void barStylesPersistWithTheViewWorkspace() {
		ChartInfo source = newGraph();
		source.getResourceGraphSettings().restoreSettings(customised().saveSettings());
		ChartInfo.Workspace saved = (ChartInfo.Workspace) source.createWorkspace(0);

		ChartInfo target = newGraph();
		target.restoreWorkspace(saved, 0);
		assertEquals(customised().saveSettings(), target.getResourceGraphSettings().saveSettings());
		assertEquals(GraphType.CUMULATIVE_COST, target.getResourceGraphSettings().getGraphType());
	}

	@Test
	void workspaceWrittenBeforeTheSettingsExistedStillRestores() {
		ChartInfo chartInfo = newGraph();
		chartInfo.getResourceGraphSettings().setGraphType(GraphType.COST);
		ChartInfo.Workspace old = new ChartInfo.Workspace();
		assertDoesNotThrow(() -> chartInfo.restoreWorkspace(old, 0));
		assertEquals(GraphType.PEAK_UNITS, chartInfo.getResourceGraphSettings().getGraphType());
		assertEquals(new ResourceGraphSettings().saveSettings(), chartInfo.getResourceGraphSettings().saveSettings());
	}

	@Test
	void previousAndNextResourceWalkTheResourceList() {
		ChartInfo chartInfo = newGraph();
		ChartLegend legend = new ChartLegend(chartInfo);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
		for (String name : new String[] { "A", "B", "C" })
			root.add(new DefaultMutableTreeNode(name));
		JTree tree = new JTree(new DefaultTreeModel(root));
		tree.setRootVisible(false);
		legend.tree = tree;
		chartInfo.setChartLegend(legend);

		assertTrue(chartInfo.hasAdjacentResource(1));
		assertTrue(chartInfo.selectAdjacentResource(1));
		assertEquals(0, tree.getMinSelectionRow());
		assertFalse(chartInfo.hasAdjacentResource(-1));
		assertFalse(chartInfo.selectAdjacentResource(-1));
		assertEquals(0, tree.getMinSelectionRow());
		assertTrue(chartInfo.selectAdjacentResource(1));
		assertTrue(chartInfo.selectAdjacentResource(1));
		assertEquals(2, tree.getMinSelectionRow());
		assertFalse(chartInfo.hasAdjacentResource(1));
		assertFalse(chartInfo.selectAdjacentResource(1));
		assertTrue(chartInfo.selectAdjacentResource(-1));
		assertEquals(1, tree.getMinSelectionRow());
	}

	@Test
	void previousResourceWithNothingSelectedStartsFromTheLastResource() {
		ChartInfo chartInfo = newGraph();
		ChartLegend legend = new ChartLegend(chartInfo);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
		root.add(new DefaultMutableTreeNode("A"));
		root.add(new DefaultMutableTreeNode("B"));
		JTree tree = new JTree(new DefaultTreeModel(root));
		tree.setRootVisible(false);
		legend.tree = tree;
		assertTrue(legend.selectAdjacentResource(-1));
		assertEquals(1, tree.getMinSelectionRow());
		assertFalse(new ChartLegend(chartInfo).hasAdjacentResource(1));
	}
}
