/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.ccpm;

import java.util.ArrayList;
import java.util.List;

/** Transient, document-scoped fever-chart observations for the active CCPM plan. */
public final class CriticalChainBufferHistory {
	private final List<Point> points = new ArrayList<>();

	public List<Point> points() { return points; }

	public record Point(double progressPercent, double consumptionPercent) { }
}
