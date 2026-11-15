/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.chart;

import java.util.ArrayList;
import java.util.List;

import com.microproject.pm.graphic.chart.ResourceGraphSettings.GraphType;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.Item;

/**
 * The series of one Resource Graph for one {@link GraphType}, computed from the per-interval totals of
 * the filtered resources and of the selected tasks. The computation is independent of Swing and of
 * the schedule, so each Graph option can be verified on plain arrays.
 */
public final class ResourceGraphData {
	/** Milliseconds in one hour: work is held in milliseconds. */
	static final double HOUR = 3_600_000D;

	/** The unit of the plotted values. */
	public enum ValueKind {
		/** Fractions of one full-time resource, shown as percentages. */
		UNITS,
		/** Hours of work. */
		HOURS,
		MONEY
	}

	/** One plotted series; {@code y} is as drawn (possibly stacked), {@code raw} the value shown as a label. */
	public record Series(Item item, double[] x, double[] y, double[] raw) {
		public boolean isEmpty() {
			return x.length == 0;
		}
	}

	private final GraphType type;
	private final ValueKind kind;
	private final List<Series> series;
	private final double[] availabilityX;
	private final double[] availabilityY;

	private ResourceGraphData(GraphType type, ValueKind kind, List<Series> series, double[] availabilityX, double[] availabilityY) {
		this.type = type;
		this.kind = kind;
		this.series = List.copyOf(series);
		this.availabilityX = availabilityX;
		this.availabilityY = availabilityY;
	}

	public GraphType getType() { return type; }
	public ValueKind getKind() { return kind; }

	/** The series in drawing order, back to front. */
	public List<Series> getSeries() { return series; }

	public Series getSeries(Item item) {
		for (Series candidate : series)
			if (candidate.item() == item)
				return candidate;
		return null;
	}

	/** The availability (capacity) line as stepped points; empty when the graph has none. */
	public double[] getAvailabilityX() { return availabilityX; }
	public double[] getAvailabilityY() { return availabilityY; }

	/**
	 * Computes the series of a graph.
	 *
	 * @param dates start of each timescale interval
	 * @param total work (milliseconds) of the filtered resources per interval
	 * @param selected work of the selected tasks per interval
	 * @param availability work availability (milliseconds) per interval
	 * @param totalCost cost of the filtered resources per interval
	 * @param selectedCost cost of the selected tasks per interval
	 * @param unitScale milliseconds of work one full-time unit supplies in an interval
	 * @param stackSelected draw the selected portion on top of the rest instead of overlaid
	 */
	public static ResourceGraphData compute(GraphType type, long[] dates, double[] total, double[] selected, double[] availability,
			double[] totalCost, double[] selectedCost, double unitScale, boolean stackSelected) {
		int n = dates == null ? 0 : dates.length;
		double scale = unitScale > 0D ? unitScale : 1D;
		double[] t = fit(total, n);
		double[] s = fit(selected, n);
		double[] a = fit(availability, n);
		double[] tc = fit(totalCost, n);
		double[] sc = fit(selectedCost, n);
		double[] excess = new double[n];
		double[] excessSelected = new double[n];
		for (int i = 0; i < n; i++) {
			excess[i] = Math.max(0D, t[i] - a[i]);
			excessSelected[i] = Math.min(excess[i], s[i]);
		}

		double[] filtered;
		double[] resource;
		double[] overFiltered = new double[n];
		double[] overResource = new double[n];
		double[] line = null;
		ValueKind kind = ValueKind.UNITS;
		boolean stackable = true;
		switch (type == null ? GraphType.PEAK_UNITS : type) {
			case WORK -> {
				kind = ValueKind.HOURS;
				filtered = divide(t, HOUR);
				resource = divide(s, HOUR);
				overFiltered = divide(excess, HOUR);
				overResource = divide(excessSelected, HOUR);
				line = divide(a, HOUR);
			}
			case CUMULATIVE_WORK -> {
				kind = ValueKind.HOURS;
				filtered = cumulative(divide(t, HOUR));
				resource = cumulative(divide(s, HOUR));
				overFiltered = cumulative(divide(excess, HOUR));
				overResource = cumulative(divide(excessSelected, HOUR));
				line = cumulative(divide(a, HOUR));
			}
			case OVERALLOCATION -> {
				kind = ValueKind.HOURS;
				filtered = new double[n];
				resource = new double[n];
				overFiltered = divide(excess, HOUR);
				overResource = divide(excessSelected, HOUR);
			}
			case PERCENT_ALLOCATION -> {
				filtered = ratio(t, a, scale);
				resource = ratio(s, a, scale);
				double[] ratioTotal = filtered;
				for (int i = 0; i < n; i++) {
					overFiltered[i] = Math.max(0D, ratioTotal[i] - 1D);
					overResource[i] = Math.min(overFiltered[i], resource[i]);
				}
				line = new double[n];
				for (int i = 0; i < n; i++)
					line[i] = a[i] > 0D ? 1D : 0D;
			}
			case REMAINING_AVAILABILITY -> {
				kind = ValueKind.HOURS;
				stackable = false;
				filtered = new double[n];
				resource = new double[n];
				for (int i = 0; i < n; i++) {
					filtered[i] = Math.max(0D, a[i] - t[i]) / HOUR;
					resource[i] = Math.max(0D, a[i] - s[i]) / HOUR;
				}
			}
			case COST -> {
				kind = ValueKind.MONEY;
				filtered = tc;
				resource = sc;
			}
			case CUMULATIVE_COST -> {
				kind = ValueKind.MONEY;
				filtered = cumulative(tc);
				resource = cumulative(sc);
			}
			case WORK_AVAILABILITY -> {
				kind = ValueKind.HOURS;
				stackable = false;
				filtered = divide(a, HOUR);
				resource = new double[n];
			}
			case UNIT_AVAILABILITY -> {
				stackable = false;
				filtered = divide(a, scale);
				resource = new double[n];
			}
			default -> { // PEAK_UNITS: the units each interval needs, against the units available
				filtered = divide(t, scale);
				resource = divide(s, scale);
				overFiltered = divide(excess, scale);
				overResource = divide(excessSelected, scale);
				line = divide(a, scale);
			}
		}

		double[] x = new double[n];
		for (int i = 0; i < n; i++)
			x[i] = dates[i];

		List<Series> list = new ArrayList<>(4);
		boolean showsAllocated = type != GraphType.OVERALLOCATION;
		if (showsAllocated) {
			double[] plottedFiltered = filtered;
			double[] plottedResource = resource;
			if (stackable && stackSelected) {
				plottedResource = filtered;
				plottedFiltered = new double[n];
				for (int i = 0; i < n; i++)
					plottedFiltered[i] = Math.max(0D, filtered[i] - resource[i]);
				list.add(new Series(Item.ALLOCATED_RESOURCE, x, plottedResource, resource));
				list.add(new Series(Item.ALLOCATED_FILTERED, x, plottedFiltered, filtered));
			} else {
				list.add(new Series(Item.ALLOCATED_FILTERED, x, plottedFiltered, filtered));
				list.add(new Series(Item.ALLOCATED_RESOURCE, x, plottedResource, resource));
			}
		}
		boolean showsOver = type == GraphType.PEAK_UNITS || type == GraphType.WORK || type == GraphType.CUMULATIVE_WORK
			|| type == GraphType.OVERALLOCATION || type == GraphType.PERCENT_ALLOCATION || type == null;
		if (showsOver) {
			list.add(positiveOnly(Item.OVERALLOCATED_FILTERED, x, overFiltered));
			list.add(positiveOnly(Item.OVERALLOCATED_RESOURCE, x, overResource));
		}

		double[] lineX = new double[0];
		double[] lineY = new double[0];
		if (line != null && n > 0) {
			lineX = new double[2 * n];
			lineY = new double[2 * n];
			double previous = 0D;
			for (int i = 0; i < n; i++) {
				lineX[2 * i] = x[i];
				lineY[2 * i] = previous;
				previous = line[i];
				lineX[2 * i + 1] = x[i];
				lineY[2 * i + 1] = previous;
			}
		}
		return new ResourceGraphData(type == null ? GraphType.PEAK_UNITS : type, kind, list, lineX, lineY);
	}

	/** Keeps the intervals whose value is above zero, so an item without any stays empty. */
	private static Series positiveOnly(Item item, double[] x, double[] values) {
		int count = 0;
		for (double value : values)
			if (value > 0D)
				count++;
		double[] px = new double[count];
		double[] py = new double[count];
		int at = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i] > 0D) {
				px[at] = x[i];
				py[at++] = values[i];
			}
		}
		return new Series(item, px, py, py);
	}

	private static double[] fit(double[] values, int n) {
		double[] result = new double[n];
		if (values != null)
			System.arraycopy(values, 0, result, 0, Math.min(n, values.length));
		return result;
	}

	private static double[] divide(double[] values, double by) {
		double[] result = new double[values.length];
		for (int i = 0; i < values.length; i++)
			result[i] = values[i] / by;
		return result;
	}

	private static double[] cumulative(double[] values) {
		double[] result = new double[values.length];
		double sum = 0D;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
			result[i] = sum;
		}
		return result;
	}

	/** Allocation as a fraction of the availability; with no availability, as a fraction of one full-time unit. */
	private static double[] ratio(double[] work, double[] availability, double unitScale) {
		double[] result = new double[work.length];
		for (int i = 0; i < work.length; i++)
			result[i] = availability[i] > 0D ? work[i] / availability[i] : work[i] / unitScale;
		return result;
	}
}
