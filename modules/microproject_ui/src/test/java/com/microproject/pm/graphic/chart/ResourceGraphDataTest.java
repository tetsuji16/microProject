/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.chart;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.IntervalXYDataset;
import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.availability.Availability;
import com.microproject.pm.graphic.chart.ResourceGraphData.ValueKind;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.GraphType;
import com.microproject.pm.graphic.chart.ResourceGraphSettings.Item;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.calendar.WorkRangeException;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;
import com.microproject.timescale.TimeIterator;
import com.microproject.timescale.TimeScale;

/** The series each Graph option of the Resource Graph Format tab plots, on three intervals of plain numbers. */
class ResourceGraphDataTest {
	@Test
	void peakUnitsUseTheHighestSimultaneousContourUnitsInsideEachPeriod() {
		List<ResourceGraphData.AllocationInterval> intervals = List.of(
			new ResourceGraphData.AllocationInterval(0, 6, 0.75D),
			new ResourceGraphData.AllocationInterval(4, 10, 0.5D),
			new ResourceGraphData.AllocationInterval(10, 14, 1D));
		assertArrayEquals(new double[] { 1.25D, 1D },
			ResourceGraphData.peakUnits(new long[] { 0, 10 }, new long[] { 10, 20 }, intervals), DELTA);
		ResourceGraphData graph = ResourceGraphData.compute(GraphType.PEAK_UNITS, new long[] { 0, 10 },
			new double[] { 0, 0 }, new double[] { 0, 0 }, new double[] { 10, 10 }, null, null, 10, false,
			new double[] { 1.25D, 1D }, new double[] { 0.5D, 0.25D });
		assertY(new double[] { 1.25D, 1D }, graph, Item.ALLOCATED_FILTERED);
	}

	@Test
	void peakOverallocationComparesLoadWithCapacityAtTheSameInstantNotThePeriodAverage() {
		List<ResourceGraphData.AllocationInterval> loads = List.of(
			new ResourceGraphData.AllocationInterval(5, 10, 1D),
			new ResourceGraphData.AllocationInterval(10, 15, 1D));
		List<ResourceGraphData.AllocationInterval> capacity = List.of(
			new ResourceGraphData.AllocationInterval(0, 5, 0.5D),
			new ResourceGraphData.AllocationInterval(5, 10, 1D),
			new ResourceGraphData.AllocationInterval(10, 15, 0.5D),
			new ResourceGraphData.AllocationInterval(15, 20, 1D));
		double[] overload = ResourceGraphData.peakOverallocatedUnits(new long[] { 0, 10 }, new long[] { 10, 20 }, loads, capacity);
		assertArrayEquals(new double[] { 0D, 0.5D }, overload, DELTA);
		ResourceGraphData graph = ResourceGraphData.compute(GraphType.PEAK_UNITS, new long[] { 0, 10 },
			new double[] { 0D, 0D }, new double[] { 0D, 0D }, new double[] { 7.5D, 7.5D }, null, null, 10D,
			false, new double[] { 1D, 1D }, new double[] { 0D, 0D }, overload);
		assertY(new double[] { 0D, 0.5D }, graph, Item.OVERALLOCATED_FILTERED);
	}

	@Test
	void finalPeakPeriodUsesTheNextRealMonthBoundaryAcrossMarchClockChangeDate() {
		TimeScale monthly = new TimeScale();
		monthly.setCalendarField1(Calendar.MONTH);
		monthly.setNumber1(1);
		monthly.setCalendarField2(Calendar.YEAR);
		monthly.setNumber2(1);
		monthly.setPattern1("MMM");
		monthly.setPattern2("yyyy");
		TimeZone previousZone = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
			GregorianCalendar local = new GregorianCalendar(TimeZone.getDefault());
			local.clear();
			local.set(2024, Calendar.FEBRUARY, 15, 0, 0);
			long start = local.getTimeInMillis();
			local.set(2024, Calendar.APRIL, 10, 0, 0);
			long end = local.getTimeInMillis();
			long[] periodEnds = ChartModel.periodEnds(new TimeIterator(start, end, monthly, -1L), 3);
			assertArrayEquals(new long[] {
				DateTime.calendarInstance(2024, Calendar.MARCH, 1).getTimeInMillis(),
				DateTime.calendarInstance(2024, Calendar.APRIL, 1).getTimeInMillis(),
				DateTime.calendarInstance(2024, Calendar.MAY, 1).getTimeInMillis()
			}, periodEnds);
		} finally {
			TimeZone.setDefault(previousZone);
		}
	}

	@Test
	void projectAssignmentsFlowThroughChartModelIntoPeakSeries() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-graph-peak-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		ResourceImpl resource = (ResourceImpl) pool.createScriptedResource();
		resource.setName("Peak test resource");
		for (int i = 0; i < 2; i++) {
			NormalTask task = project.createScriptedTask();
			task.setName("Peak task " + i);
			task.setDuration(CalendarOption.getInstance().getMillisPerDay());
			AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		}
		NormalTask unassigned = project.createScriptedTask();
		unassigned.setName("Unassigned task must not contribute a dummy allocation");
		unassigned.setDuration(CalendarOption.getInstance().getMillisPerDay());
		project.recalculate();
		ChartModel model = new ChartModel(new CoordinatesConverter(project));
		model.computeResourceGraph(project, null, List.of(resource), new ResourceGraphSettings(), false);
		ResourceGraphData.Series plotted = model.getResourceGraph().getSeries(Item.ALLOCATED_FILTERED);
		assertNotNull(plotted);
		assertEquals(2D, java.util.Arrays.stream(plotted.y()).max().orElseThrow(), DELTA,
			"the Resource Graph peak must include both simultaneous project assignments");
		assertEquals(2D, java.util.Arrays.stream(ChartModel.exactPeakUnits(project.getTaskOutlineIterator(), null,
			new long[] { project.getStart() }, new long[] { project.getEnd() })).max().orElseThrow(), DELTA,
			"an unassigned task's dummy assignment must not inflate an all-resources peak");
		XYPlot rendered = ResourceGraphChart.build(model.getResourceGraph(), new ResourceGraphSettings()).getXYPlot();
		IntervalXYDataset bars = (IntervalXYDataset) rendered.getDataset(datasetIndex(rendered, Item.ALLOCATED_FILTERED));
		double plottedPeak = 0D;
		for (int i = 0; i < bars.getItemCount(0); i++)
			plottedPeak = Math.max(plottedPeak, bars.getEndYValue(0, i));
		assertEquals(2D, plottedPeak, DELTA, "the chart renderer must receive the exact Peak Units result");
	}

	@Test
	void projectAssignmentAtFullAvailabilityIsNotOverallocatedByThePeriodsLowerAverage() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-graph-availability-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		ResourceImpl resource = (ResourceImpl) pool.createScriptedResource();
		resource.setName("Changing availability");
		resource.setMaximumUnits(0.5D);
		NormalTask task = project.createScriptedTask();
		task.setName("Assignment during full availability");
		task.setDuration(CalendarOption.getInstance().getMillisPerDay());
		var assignment = AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		assignment.setDelay(3L * CalendarOption.getInstance().getMillisPerDay());
		project.recalculate();
		long switchTime = assignment.getStart();
		Availability highAvailability = (Availability) resource.getAvailabilityTable().newValueObject(switchTime);
		highAvailability.setMaximumUnits(1D);
		project.recalculate();
		assertEquals(switchTime, assignment.getStart(), "fixture assignment must start after the availability change");
		long periodEnd = resource.getEffectiveWorkCalendar().add(task.getEnd(),
			CalendarOption.getInstance().getMillisPerDay(), true);
		double[] overload = ChartModel.exactPeakOverallocatedUnits(project, List.of(resource),
			new long[] { project.getStart() }, new long[] { periodEnd });
		assertArrayEquals(new double[] { 0D }, overload, DELTA,
			"availability changes earlier in the period must not make a fully covered assignment appear overallocated");
	}

	@Test
	void assignmentsOnNonoverlappingShiftsAndAcrossAHolidayDoNotCreateFalsePeaks() throws WorkRangeException {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-graph-shift-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		long holidayDate = DateTime.nextDay(DateTime.dayFloor(project.getStart()) + 2L * com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_DAY);
		WorkingCalendar early = shiftCalendar(project, 8, 12, holidayDate);
		WorkingCalendar late = shiftCalendar(project, 13, 17, holidayDate);
		Resource earlyResource = pool.createScriptedResource();
		earlyResource.setName("Early shift");
		earlyResource.setWorkCalendar(early);
		Resource lateResource = pool.createScriptedResource();
		lateResource.setName("Late shift");
		lateResource.setWorkCalendar(late);
		for (Resource resource : List.of(earlyResource, lateResource)) {
			NormalTask task = project.createScriptedTask();
			task.setName(resource.getName() + " assignment");
			task.setWorkCalendar(resource.getWorkCalendar());
			task.setDuration((long) (40D * H));
			AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);
		}
		project.recalculate();
		ChartModel model = new ChartModel(new CoordinatesConverter(project));
		model.computeResourceGraph(project, null, List.of(earlyResource, lateResource), new ResourceGraphSettings(), false);
		ResourceGraphData.Series peak = model.getResourceGraph().getSeries(Item.ALLOCATED_FILTERED);
		assertEquals(1D, java.util.Arrays.stream(peak.y()).max().orElseThrow(), DELTA,
			"adjacent shifts must not overlap in the midday gap, overnight gaps, or the holiday");
	}

	private static WorkingCalendar shiftCalendar(Project project, int startHour, int endHour, long holidayDate)
			throws WorkRangeException {
		WorkingCalendar calendar = WorkingCalendar.getInstanceBasedOn(project.getWorkCalendar());
		WorkingHours hours = new WorkingHours();
		hours.setInterval(0, WorkingHours.hourTime(startHour), WorkingHours.hourTime(endHour));
		WorkDay weekdays = new WorkDay();
		weekdays.setWorkingHours(hours);
		for (int day = Calendar.MONDAY - 1; day <= Calendar.FRIDAY - 1; day++)
			calendar.setWeekDay(day, weekdays.clone());
		calendar.addOrReplaceException(new WorkDay(holidayDate, holidayDate, "Resource Graph test holiday"));
		calendar.invalidate();
		return calendar;
	}
	private static final double H = 3_600_000D;
	private static final double DELTA = 1e-9;
	private static final long[] DATES = { 0L, 1L, 2L };
	private static final double[] TOTAL = { 8 * H, 12 * H, 4 * H };
	private static final double[] SELECTED = { 4 * H, 6 * H, 0D };
	private static final double[] AVAILABILITY = { 8 * H, 8 * H, 8 * H };
	private static final double[] COST = { 100D, 200D, 50D };
	private static final double[] SELECTED_COST = { 40D, 60D, 0D };
	private static final double UNIT = 8 * H;

	private static ResourceGraphData compute(GraphType type, boolean stack) {
		return ResourceGraphData.compute(type, DATES, TOTAL, SELECTED, AVAILABILITY, COST, SELECTED_COST, UNIT, stack);
	}

	private static void assertY(double[] expected, ResourceGraphData data, Item item) {
		ResourceGraphData.Series series = data.getSeries(item);
		assertNotNull(series, item.name());
		assertArrayEquals(expected, series.y(), DELTA, item.name());
	}

	@Test
	void peakUnitsPlotsTheUnitsEachIntervalNeedsAgainstTheAvailableUnits() {
		ResourceGraphData data = compute(GraphType.PEAK_UNITS, false);
		assertEquals(ValueKind.UNITS, data.getKind());
		assertY(new double[] { 1D, 1.5D, 0.5D }, data, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 0.5D, 0.75D, 0D }, data, Item.ALLOCATED_RESOURCE);
		assertY(new double[] { 0D, 0.5D, 0D }, data, Item.OVERALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 0, 1, 2 }, data.getSeries(Item.OVERALLOCATED_FILTERED).x(), DELTA);
		assertY(new double[] { 0D, 0.5D, 0D }, data, Item.OVERALLOCATED_RESOURCE);
		assertArrayEquals(new double[] { 0, 1, 1, 1, 1, 1 }, data.getAvailabilityY(), DELTA);
		assertArrayEquals(new double[] { 0, 0, 1, 1, 2, 2 }, data.getAvailabilityX(), DELTA);
	}

	@Test
	void workPlotsHoursPerInterval() {
		ResourceGraphData data = compute(GraphType.WORK, false);
		assertEquals(ValueKind.HOURS, data.getKind());
		assertY(new double[] { 8, 12, 4 }, data, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 4, 6, 0 }, data, Item.ALLOCATED_RESOURCE);
		assertY(new double[] { 0, 4, 0 }, data, Item.OVERALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 0, 8, 8, 8, 8, 8 }, data.getAvailabilityY(), DELTA);
	}

	@Test
	void cumulativeWorkAddsUpTheIntervals() {
		ResourceGraphData data = compute(GraphType.CUMULATIVE_WORK, false);
		assertEquals(ValueKind.HOURS, data.getKind());
		assertY(new double[] { 8, 20, 24 }, data, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 4, 10, 10 }, data, Item.ALLOCATED_RESOURCE);
		assertY(new double[] { 0, 4, 4 }, data, Item.OVERALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 0, 8, 8, 16, 16, 24 }, data.getAvailabilityY(), DELTA);
	}

	@Test
	void overallocationPlotsOnlyTheWorkAboveTheAvailability() {
		ResourceGraphData data = compute(GraphType.OVERALLOCATION, false);
		assertEquals(ValueKind.HOURS, data.getKind());
		assertNull(data.getSeries(Item.ALLOCATED_FILTERED));
		assertNull(data.getSeries(Item.ALLOCATED_RESOURCE));
		assertY(new double[] { 0, 4, 0 }, data, Item.OVERALLOCATED_FILTERED);
		assertY(new double[] { 0, 4, 0 }, data, Item.OVERALLOCATED_RESOURCE);
		assertEquals(0, data.getAvailabilityX().length);
	}

	@Test
	void percentAllocationIsAFractionOfTheAvailability() {
		ResourceGraphData data = compute(GraphType.PERCENT_ALLOCATION, false);
		assertEquals(ValueKind.UNITS, data.getKind());
		assertY(new double[] { 1D, 1.5D, 0.5D }, data, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 0.5D, 0.75D, 0D }, data, Item.ALLOCATED_RESOURCE);
		assertY(new double[] { 0D, 0.5D, 0D }, data, Item.OVERALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 0, 1, 1, 1, 1, 1 }, data.getAvailabilityY(), DELTA);
	}

	@Test
	void percentAllocationWithoutAvailabilityFallsBackToOneFullTimeUnit() {
		ResourceGraphData data = ResourceGraphData.compute(GraphType.PERCENT_ALLOCATION, new long[] { 0L }, new double[] { 4 * H },
			new double[0], new double[] { 0D }, null, null, UNIT, false);
		assertY(new double[] { 0.5D }, data, Item.ALLOCATED_FILTERED);
		assertEquals(0, data.getSeries(Item.OVERALLOCATED_FILTERED).x().length);
	}

	@Test
	void remainingAvailabilityIsWhatTheAssignedWorkLeavesFree() {
		ResourceGraphData data = compute(GraphType.REMAINING_AVAILABILITY, false);
		assertEquals(ValueKind.HOURS, data.getKind());
		assertY(new double[] { 0, 0, 4 }, data, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 4, 2, 8 }, data, Item.ALLOCATED_RESOURCE);
		assertNull(data.getSeries(Item.OVERALLOCATED_FILTERED));
	}

	@Test
	void costAndCumulativeCostPlotMoney() {
		ResourceGraphData cost = compute(GraphType.COST, false);
		assertEquals(ValueKind.MONEY, cost.getKind());
		assertY(new double[] { 100, 200, 50 }, cost, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 40, 60, 0 }, cost, Item.ALLOCATED_RESOURCE);
		ResourceGraphData cumulative = compute(GraphType.CUMULATIVE_COST, false);
		assertEquals(ValueKind.MONEY, cumulative.getKind());
		assertY(new double[] { 100, 300, 350 }, cumulative, Item.ALLOCATED_FILTERED);
		assertY(new double[] { 40, 100, 100 }, cumulative, Item.ALLOCATED_RESOURCE);
	}

	@Test
	void workAvailabilityAndUnitAvailabilityPlotTheCapacity() {
		ResourceGraphData work = compute(GraphType.WORK_AVAILABILITY, false);
		assertEquals(ValueKind.HOURS, work.getKind());
		assertY(new double[] { 8, 8, 8 }, work, Item.ALLOCATED_FILTERED);
		ResourceGraphData units = compute(GraphType.UNIT_AVAILABILITY, false);
		assertEquals(ValueKind.UNITS, units.getKind());
		assertY(new double[] { 1, 1, 1 }, units, Item.ALLOCATED_FILTERED);
	}

	@Test
	void stackingDrawsTheSelectedPortionOnTopOfTheRest() {
		ResourceGraphData data = compute(GraphType.WORK, true);
		ResourceGraphData.Series resource = data.getSeries(Item.ALLOCATED_RESOURCE);
		assertArrayEquals(new double[] { 8, 12, 4 }, resource.y(), DELTA);
		assertArrayEquals(new double[] { 4, 6, 0 }, resource.raw(), DELTA);
		assertY(new double[] { 4, 6, 4 }, data, Item.ALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 8, 12, 4 }, data.getSeries(Item.ALLOCATED_FILTERED).raw(), DELTA);
		// the selected portion spans from the rest up to the total, independent of the other item
		assertArrayEquals(new double[] { 4, 6, 4 }, resource.base(), DELTA);
		assertNull(data.getSeries(Item.ALLOCATED_FILTERED).base());
	}

	@Test
	void stackedItemsHonorTheirShowAsShape() {
		ResourceGraphData data = compute(GraphType.WORK, true);
		ResourceGraphSettings settings = new ResourceGraphSettings();
		XYPlot bars = ResourceGraphChart.build(data, settings).getXYPlot();
		int index = datasetIndex(bars, Item.ALLOCATED_RESOURCE);
		assertTrue(bars.getRenderer(index) instanceof XYBarRenderer);
		assertTrue(bars.getDataset(index) instanceof IntervalXYDataset);
		ResourceGraphSettings.BarStyle style = settings.getStyle(Item.ALLOCATED_RESOURCE);
		settings.setStyle(Item.ALLOCATED_RESOURCE, new ResourceGraphSettings.BarStyle(ResourceGraphSettings.ShowAs.LINE, style.rgb(), style.pattern()));
		XYPlot line = ResourceGraphChart.build(data, settings).getXYPlot();
		index = datasetIndex(line, Item.ALLOCATED_RESOURCE);
		assertTrue(!(line.getRenderer(index) instanceof XYBarRenderer), "a stacked item shown as a line must not draw bars");
		assertEquals(12D, line.getDataset(index).getYValue(0, 1), DELTA);
		assertTrue(index > datasetIndex(line, Item.ALLOCATED_FILTERED), "the descending stacked line must be drawn after the filtered fill");
	}

	@Test
	void barOverlapControlsPerPeriodBarWidths() {
		ResourceGraphData data = compute(GraphType.WORK, false);
		ResourceGraphSettings settings = new ResourceGraphSettings();
		XYPlot plot = ResourceGraphChart.build(data, settings).getXYPlot();
		IntervalXYDataset overlap = (IntervalXYDataset) plot.getDataset(datasetIndex(plot, Item.ALLOCATED_RESOURCE));
		assertEquals(0D, overlap.getStartXValue(0, 0), DELTA);
		assertEquals(1D, overlap.getEndXValue(0, 0), DELTA);
		settings.setPercentBarOverlap(0);
		plot = ResourceGraphChart.build(data, settings).getXYPlot();
		IntervalXYDataset sideBySide = (IntervalXYDataset) plot.getDataset(datasetIndex(plot, Item.ALLOCATED_RESOURCE));
		assertEquals(0.5D, sideBySide.getStartXValue(0, 0), DELTA);
		assertEquals(1D, sideBySide.getEndXValue(0, 0), DELTA);
	}

	private static int datasetIndex(XYPlot plot, Item item) {
		for (int i = 0; i < plot.getDatasetCount(); i++)
			if (plot.getDataset(i) != null && item.name().equals(plot.getDataset(i).getSeriesKey(0)))
				return i;
		throw new AssertionError(item + " has no dataset");
	}

	@Test
	void separateOverloadsStayApartAndKeepTheirIntervalBoundaries() {
		ResourceGraphData data = ResourceGraphData.compute(GraphType.WORK, new long[] { 0L, 1L, 2L, 3L, 4L },
			new double[] { 12 * H, 8 * H, 12 * H, 8 * H, 8 * H }, new double[5], new double[] { 8 * H, 8 * H, 8 * H, 8 * H, 8 * H },
			null, null, UNIT, false);
		assertY(new double[] { 4, 0, 4, 0, 0 }, data, Item.OVERALLOCATED_FILTERED);
		assertArrayEquals(new double[] { 0, 1, 2, 3, 4 }, data.getSeries(Item.OVERALLOCATED_FILTERED).x(), DELTA);
	}

	@Test
	void finalIntervalEndRepeatsThePreviousWidthOrOneDay() {
		assertEquals(3D, ResourceGraphChart.intervalEnd(new double[] { 0, 1, 2 }, 2), DELTA);
		assertEquals(1D, ResourceGraphChart.intervalEnd(new double[] { 0, 1, 2 }, 0), DELTA);
		assertEquals(86_400_000D, ResourceGraphChart.intervalEnd(new double[] { 0 }, 0), DELTA);
	}

	@Test
	void everyGraphTypeComputesOnEmptyAndMissingInput() {
		for (GraphType type : GraphType.values()) {
			ResourceGraphData empty = ResourceGraphData.compute(type, new long[0], null, null, null, null, null, 0D, false);
			assertEquals(type, empty.getType());
			for (ResourceGraphData.Series series : empty.getSeries())
				assertTrue(series.isEmpty(), type + " " + series.item());
			assertEquals(0, empty.getAvailabilityX().length, type.name());
			assertEquals(GraphType.PEAK_UNITS,
				ResourceGraphData.compute(null, DATES, TOTAL, SELECTED, AVAILABILITY, COST, SELECTED_COST, UNIT, false).getType());
		}
	}
}
