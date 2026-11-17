/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

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
	/** One constant-units segment from an assignment's timephased contour. */
	static record AllocationInterval(long start, long end, double units, Object group) {
		AllocationInterval(long start, long end, double units) { this(start, end, units, null); }
	}
	private record UnitChange(long time, double delta) { }
	private record OverallocationChange(long time, Object group, double loadDelta, double selectedDelta, double availabilityDelta) { }
	private record GroupLoad(double load, double selected, double capacity) { }
	static record PeakOverallocated(double[] total, double[] selected) { }

	/** Highest simultaneous units in each half-open display period. */
	static double[] peakUnits(long[] starts, long[] ends, List<AllocationInterval> intervals) {
		List<UnitChange> events = new ArrayList<>(intervals.size() * 2);
		for (AllocationInterval interval : intervals) {
			if (interval.end() > interval.start() && interval.units() > 0D) {
				events.add(new UnitChange(interval.start(), interval.units()));
				events.add(new UnitChange(interval.end(), -interval.units()));
			}
		}
		return peakFromEvents(starts, ends, events);
	}

	/** Highest positive load above actual availability at any instant in each display period. */
	static double[] peakOverallocatedUnits(long[] starts, long[] ends, List<AllocationInterval> allocations,
			List<AllocationInterval> availability) {
		return peakOverallocatedUnits(starts, ends, allocations, List.of(), availability).total();
	}

	/** Computes total overload and selected-task overload from the same simultaneous load/capacity sweep. */
	static PeakOverallocated peakOverallocatedUnits(long[] starts, long[] ends, List<AllocationInterval> allocations,
			List<AllocationInterval> selectedAllocations, List<AllocationInterval> availability) {
		List<OverallocationChange> events = new ArrayList<>((allocations.size() + selectedAllocations.size() + availability.size()) * 2);
		addOverallocationEvents(events, allocations, 1);
		addOverallocationEvents(events, selectedAllocations, 2);
		addOverallocationEvents(events, availability, 3);
		events.sort(Comparator.comparingLong(OverallocationChange::time));
		List<Long> times = new ArrayList<>();
		List<Double> totalAtChange = new ArrayList<>();
		List<Double> selectedAtChange = new ArrayList<>();
		java.util.Map<Object, GroupLoad> current = new java.util.HashMap<>();
		double total = 0D;
		double selectedTotal = 0D;
		for (int i = 0; i < events.size();) {
			long time = events.get(i).time();
			java.util.Map<Object, double[]> deltas = new java.util.HashMap<>();
			while (i < events.size() && events.get(i).time() == time) {
				OverallocationChange event = events.get(i++);
				double[] delta = deltas.computeIfAbsent(event.group(), ignored -> new double[3]);
				delta[0] += event.loadDelta();
				delta[1] += event.selectedDelta();
				delta[2] += event.availabilityDelta();
			}
			for (var entry : deltas.entrySet()) {
				Object group = entry.getKey();
				GroupLoad before = current.getOrDefault(group, new GroupLoad(0D, 0D, 0D));
				double beforeExcess = Math.max(0D, before.load() - before.capacity());
				total -= beforeExcess;
				selectedTotal -= Math.min(beforeExcess, Math.max(0D, before.selected()));
				double[] delta = entry.getValue();
				GroupLoad after = new GroupLoad(before.load() + delta[0], before.selected() + delta[1], before.capacity() + delta[2]);
				current.put(group, after);
				double afterExcess = Math.max(0D, after.load() - after.capacity());
				total += afterExcess;
				selectedTotal += Math.min(afterExcess, Math.max(0D, after.selected()));
			}
			times.add(time);
			totalAtChange.add(total);
			selectedAtChange.add(selectedTotal);
		}
		double[] totalPeaks = new double[starts.length];
		double[] selectedPeaks = new double[starts.length];
		for (int p = 0; p < starts.length; p++) {
			int first = upperBoundTimes(times, starts[p]);
			int previous = first - 1;
			totalPeaks[p] = previous < 0 ? 0D : totalAtChange.get(previous);
			selectedPeaks[p] = previous < 0 ? 0D : selectedAtChange.get(previous);
			for (int i = first; i < times.size() && times.get(i) < ends[p]; i++) {
				totalPeaks[p] = Math.max(totalPeaks[p], totalAtChange.get(i));
				selectedPeaks[p] = Math.max(selectedPeaks[p], selectedAtChange.get(i));
			}
		}
		return new PeakOverallocated(totalPeaks, selectedPeaks);
	}

	private static void addOverallocationEvents(List<OverallocationChange> events, List<AllocationInterval> intervals, int kind) {
		for (AllocationInterval interval : intervals) {
			if (interval.end() > interval.start() && interval.units() > 0D) {
				double units = interval.units();
				double loadDelta = kind == 1 ? units : 0D;
				double selectedDelta = kind == 2 ? units : 0D;
				double availabilityDelta = kind == 3 ? units : 0D;
				events.add(new OverallocationChange(interval.start(), interval.group(), loadDelta, selectedDelta, availabilityDelta));
				events.add(new OverallocationChange(interval.end(), interval.group(), -loadDelta, -selectedDelta, -availabilityDelta));
			}
		}
	}

	private static int upperBoundTimes(List<Long> changes, long time) {
		int low = 0;
		int high = changes.size();
		while (low < high) {
			int middle = (low + high) >>> 1;
			if (changes.get(middle) <= time) low = middle + 1;
			else high = middle;
		}
		return low;
	}

	private static double[] peakFromEvents(long[] starts, long[] ends, List<UnitChange> events) {
		events.sort(Comparator.comparingLong(UnitChange::time));
		List<UnitChange> changes = new ArrayList<>(events.size());
		for (int i = 0; i < events.size();) {
			long time = events.get(i).time();
			double delta = 0D;
			do {
				delta += events.get(i++).delta();
			} while (i < events.size() && events.get(i).time() == time);
			changes.add(new UnitChange(time, delta));
		}
		double[] cumulative = new double[changes.size()];
		double total = 0D;
		for (int i = 0; i < changes.size(); i++)
			cumulative[i] = total += changes.get(i).delta();

		double[] peaks = new double[starts.length];
		for (int p = 0; p < starts.length; p++) {
			int first = upperBound(changes, starts[p]); // Changes at start are active immediately after the boundary.
			double current = first == 0 ? 0D : cumulative[first - 1];
			peaks[p] = Math.max(0D, current);
			for (int i = first; i < changes.size() && changes.get(i).time() < ends[p]; i++) {
				current += changes.get(i).delta();
				peaks[p] = Math.max(peaks[p], current);
			}
		}
		return peaks;
	}

	private static int upperBound(List<UnitChange> changes, long time) {
		int low = 0;
		int high = changes.size();
		while (low < high) {
			int middle = (low + high) >>> 1;
			if (changes.get(middle).time() <= time) low = middle + 1;
			else high = middle;
		}
		return low;
	}

	/** The unit of the plotted values. */
	public enum ValueKind {
		/** Fractions of one full-time resource, shown as percentages. */
		UNITS,
		/** Hours of work. */
		HOURS,
		MONEY
	}

	/**
	 * One plotted series; {@code y} is as drawn (possibly stacked), {@code raw} the value shown as a label.
	 * A non-null {@code base} is the lower bound of each bar of a stacked item (the bar spans {@code base} to {@code y}).
	 */
	public record Series(Item item, double[] x, double[] y, double[] raw, double[] base) {
		public Series(Item item, double[] x, double[] y, double[] raw) {
			this(item, x, y, raw, null);
		}

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
		return compute(type, dates, total, selected, availability, totalCost, selectedCost, unitScale, stackSelected, null, null);
	}

	/** Computes graph data with exact peak allocation fractions when supplied. */
	static ResourceGraphData compute(GraphType type, long[] dates, double[] total, double[] selected, double[] availability,
			double[] totalCost, double[] selectedCost, double unitScale, boolean stackSelected, double[] peakTotal, double[] peakSelected) {
		return compute(type, dates, total, selected, availability, totalCost, selectedCost, unitScale, stackSelected,
			peakTotal, peakSelected, null);
	}

	/** Computes graph data with exact peak allocation and overload values when supplied. */
	static ResourceGraphData compute(GraphType type, long[] dates, double[] total, double[] selected, double[] availability,
			double[] totalCost, double[] selectedCost, double unitScale, boolean stackSelected, double[] peakTotal,
			double[] peakSelected, double[] peakOverallocated) {
		return compute(type, dates, total, selected, availability, totalCost, selectedCost, unitScale, stackSelected,
			peakTotal, peakSelected, peakOverallocated, null);
	}

	/** Computes graph data with exact total and selected overload values when supplied. */
	static ResourceGraphData compute(GraphType type, long[] dates, double[] total, double[] selected, double[] availability,
			double[] totalCost, double[] selectedCost, double unitScale, boolean stackSelected, double[] peakTotal,
			double[] peakSelected, double[] peakOverallocated, double[] peakSelectedOverallocated) {
		int n = dates == null ? 0 : dates.length;
		double scale = unitScale > 0D ? unitScale : 1D;
		double[] t = fit(total, n);
		double[] s = fit(selected, n);
		double[] a = fit(availability, n);
		double[] tc = fit(totalCost, n);
		double[] sc = fit(selectedCost, n);
		double[] exactOverallocated = peakOverallocated == null ? null : fit(peakOverallocated, n);
		double[] exactSelectedOverallocated = peakSelectedOverallocated == null ? null : fit(peakSelectedOverallocated, n);
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
				filtered = peakTotal == null ? divide(t, scale) : fit(peakTotal, n);
				resource = peakSelected == null ? divide(s, scale) : fit(peakSelected, n);
				overFiltered = new double[n];
				overResource = new double[n];
				for (int i = 0; i < n; i++) {
					double over = exactOverallocated == null
						? Math.max(0D, filtered[i] - a[i] / scale)
						: Math.max(0D, exactOverallocated[i]);
					overFiltered[i] = over;
					overResource[i] = exactSelectedOverallocated == null ? Math.min(over, resource[i])
						: Math.max(0D, exactSelectedOverallocated[i]);
				}
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
				list.add(new Series(Item.ALLOCATED_RESOURCE, x, plottedResource, resource, plottedFiltered));
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

	/**
	 * Keeps every interval when at least one value is above zero, so separate overloads stay separated by
	 * zero-valued intervals; an item without any overload stays empty.
	 */
	private static Series positiveOnly(Item item, double[] x, double[] values) {
		boolean any = false;
		for (double value : values)
			if (value > 0D)
				any = true;
		if (!any)
			return new Series(item, new double[0], new double[0], new double[0]);
		return new Series(item, x, values, values);
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
