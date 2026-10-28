/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.link_routing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.DependencyType;

class DefaultGanttLinkRoutingTest {
	@Test
	void routesAllDependencyKindsFromSelectedBarEdgesToTheirTargetEdges() {
		for (DependencyType.Kind kind : DependencyType.Kind.values()) {
			DefaultGanttLinkRouting routing = new DefaultGanttLinkRouting();
			routing.setVerticalArrow(false);
			GeneralPath path = new GeneralPath();
			routing.routePath(path, 20, 10, 80, 50, 50, 56, 44, kind);

			List<Point> points = points(path);
			assertEquals(new Point(20, 10), points.getFirst(), kind + " source endpoint");
			assertEquals(new Point(80, 50), points.getLast(), kind + " target endpoint");
			assertEquals(20, routing.getFirstX(), kind + " arrow source x");
			assertEquals(80, routing.getLastX(), kind + " arrow target x");
		}
	}

	@Test
	void finishToStartUsesTargetBarFloorForNearbyVerticalArrow() {
		DefaultGanttLinkRouting routing = new DefaultGanttLinkRouting();
		GeneralPath path = new GeneralPath();
		routing.routePath(path, 40, 10, 30, 50, 50, 56, 44, DependencyType.FS);

		assertEquals(List.of(new Point(40, 10), new Point(45, 10), new Point(45, 44)), points(path));
		assertEquals(45, routing.getLastX());
		assertEquals(44, routing.getLastY());
	}

	private static List<Point> points(GeneralPath path) {
		List<Point> points = new ArrayList<>();
		float[] coordinates = new float[6];
		for (PathIterator iterator = path.getPathIterator(null); !iterator.isDone(); iterator.next()) {
			int segment = iterator.currentSegment(coordinates);
			if (segment == PathIterator.SEG_MOVETO || segment == PathIterator.SEG_LINETO) {
				points.add(new Point(Math.round(coordinates[0]), Math.round(coordinates[1])));
			}
		}
		return points;
	}

	private record Point(int x, int y) { }
}
