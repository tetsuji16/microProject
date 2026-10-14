/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.graphic.gantt.GanttBarIntervalGenerator;
import com.microproject.pm.scheduling.ScheduleInterval;

class GraphicNodeIntervalTraversalTest {
	@Test
	void cachedIntervalsKeepEncounterOrderForConsumptionAndContainment() {
		Node node = NodeFactory.getInstance().createNode(new Object());
		GraphicNode graphicNode = new GraphicNode(node, 1);
		graphicNode.setScheduleCaching(true);
		ScheduleInterval first = new ScheduleInterval(10L, 20L);
		ScheduleInterval second = new ScheduleInterval(30L, 40L);
		graphicNode.intervals.add(first);
		graphicNode.intervals.add(second);

		List<ScheduleInterval> consumed = new ArrayList<>();
		new GanttBarIntervalGenerator().consumeIntervals(graphicNode, consumed::add);

		assertEquals(List.of(first, second), consumed);
		assertSame(first, graphicNode.contains(15d, 0d, 0d, null));
		assertSame(second, graphicNode.contains(40d, 0d, 0d, null));
	}

	@Test
	void capturedScheduleIntervalsDoNotAliasMutableModelIntervals() {
		GraphicNode graphicNode = new GraphicNode(NodeFactory.getInstance().createNode(new Object()), 1);
		graphicNode.setScheduleCaching(true);
		ScheduleInterval source = new ScheduleInterval(10L, 20L);
		GraphicNode.CacheIntervalConsumer consumer = new GraphicNode.CacheIntervalConsumer();
		consumer.initCache(graphicNode, graphicNode.intervals);
		consumer.consumeInterval(source);
		ScheduleInterval captured = graphicNode.intervals.getFirst();
		source.setMaximumEnd(99L);

		assertNotSame(source, captured);
		assertEquals(10L, captured.getStart());
		assertEquals(20L, captured.getEnd());
		assertEquals(10L, graphicNode.getStart());
		assertEquals(20L, graphicNode.getEnd());
		assertNotEquals(source.getMaximumEnd(), captured.getMaximumEnd());
	}
}
