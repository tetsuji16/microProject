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
import java.util.UUID;

/** Persisted CCPM buffer observations for a project. */
public final class CriticalChainBufferHistory {
	private final List<Point> points = new ArrayList<>();
	private final List<Retraction> retractions = new ArrayList<>();

	/** Visible, non-retracted observations. Mutations go through add/retract only. */
	public List<Point> points() { return List.copyOf(points); }
	/** Audit records for observations deliberately excluded from the visible chart. */
	public List<Retraction> retractions() { return List.copyOf(retractions); }

	public void add(Point point) {
		if (point == null) return;
		if (isRetracted(point.observationId())) return;
		if (!points.isEmpty() && points.get(points.size() - 1).sameObservation(point)) return;
		points.add(point);
		points.sort(Comparator.comparing(Point::observedAt));
		while (points.size() > 10000) points.remove(0);
	}

	/**
	 * Removes a point from the chart without erasing its existence.  The caller
	 * must supply a human-readable reason; restore() is used by Undo/Redo.
	 */
	Point retract(UUID observationId, String reason, String actorId, String actorName, Instant retractedAt) {
		if (observationId == null || reason == null || reason.isBlank()) return null;
		for (int index = 0; index < points.size(); index++) {
			Point point = points.get(index);
			if (!observationId.equals(point.observationId())) continue;
			points.remove(index);
			retractions.add(new Retraction(observationId, retractedAt, actorId, actorName, reason));
			return point;
		}
		return null;
	}

	void restore(Point point, Retraction retraction) {
		if (point == null || retraction == null) return;
		retractions.remove(retraction);
		if (!points.contains(point)) {
			points.add(point);
			points.sort(Comparator.comparing(Point::observedAt));
		}
	}

	/** Restores a persisted audit event; public for the MPOF exchange boundary. */
	public void recordRetraction(Retraction retraction) {
		if (retraction == null || isRetracted(retraction.observationId())) return;
		points.removeIf(point -> retraction.observationId().equals(point.observationId()));
		retractions.add(retraction);
	}

	void replace(List<Point> active, List<Retraction> audit) {
		points.clear();
		retractions.clear();
		if (audit != null) for (Retraction retraction : audit) recordRetraction(retraction);
		if (active != null) for (Point point : active) add(point);
	}

	private boolean isRetracted(UUID observationId) {
		return retractions.stream().anyMatch(value -> value.observationId().equals(observationId));
	}

	public record Point(UUID observationId, Instant observedAt, String actorId, String actorName,
			double progressPercent, double consumptionPercent, String zone, String baselineId) {
		/** Source-compatible constructor for pre-retraction callers and MPOF v1 history. */
		public Point(Instant observedAt, String actorId, String actorName, double progressPercent,
			double consumptionPercent, String zone, String baselineId) {
			this(UUID.randomUUID(), observedAt, actorId, actorName, progressPercent, consumptionPercent, zone, baselineId);
		}
		/** Returns whether two points represent the same measurement, ignoring sampling time. */
		public boolean sameObservation(Point other) {
			return other != null
				&& actorId.equals(other.actorId)
				&& actorName.equals(other.actorName)
				&& Double.compare(progressPercent, other.progressPercent) == 0
				&& Double.compare(consumptionPercent, other.consumptionPercent) == 0
				&& zone.equals(other.zone)
				&& baselineId.equals(other.baselineId);
		}

		public Point {
			if (observationId == null) observationId = UUID.randomUUID();
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

	/** Immutable audit event for an intentionally discarded chart point. */
	public record Retraction(UUID observationId, Instant retractedAt, String actorId, String actorName, String reason) {
		public Retraction {
			if (observationId == null) throw new IllegalArgumentException("observationId is required");
			if (retractedAt == null) retractedAt = Instant.now();
			if (actorId == null || actorId.isBlank()) actorId = "unknown";
			if (actorName == null || actorName.isBlank()) actorName = "unknown";
			if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason is required");
		}
	}
}
