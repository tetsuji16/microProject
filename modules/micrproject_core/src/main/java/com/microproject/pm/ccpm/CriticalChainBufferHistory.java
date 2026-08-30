/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.ccpm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Persisted CCPM buffer observations for a project. */
public final class CriticalChainBufferHistory {
	private final List<Point> points = new ArrayList<>();

	public List<Point> points() { return points; }

	public void add(Point point) {
		if (point == null) return;
		if (!points.isEmpty() && points.get(points.size() - 1).equals(point)) return;
		points.add(point);
		points.sort(Comparator.comparing(Point::observedAt));
		while (points.size() > 10000) points.remove(0);
	}

	public record Point(Instant observedAt, String actorId, String actorName,
			double progressPercent, double consumptionPercent, String zone, String baselineId) {
		public Point {
			if (observedAt == null) throw new IllegalArgumentException("observedAt is required");
			if (actorId == null || actorId.isBlank()) actorId = "unknown";
			if (actorName == null || actorName.isBlank()) actorName = "unknown";
			if (!Double.isFinite(progressPercent) || progressPercent < 0 || progressPercent > 100)
				throw new IllegalArgumentException("progressPercent must be 0..100");
			if (!Double.isFinite(consumptionPercent) || consumptionPercent < 0 || consumptionPercent > 100)
				throw new IllegalArgumentException("consumptionPercent must be 0..100");
			if (zone == null || zone.isBlank()) zone = "UNKNOWN";
			if (baselineId == null) baselineId = "";
		}
	}
}
