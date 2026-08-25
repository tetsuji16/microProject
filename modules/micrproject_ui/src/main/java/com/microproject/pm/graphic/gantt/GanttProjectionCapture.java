/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.gantt;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.BarStyle;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.GanttBarFormatOverrides;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.model.cache.RevisionedProjectionIndex;
import com.microproject.pm.graphic.model.cache.TaskProjectionSnapshot;
import com.microproject.pm.graphic.model.cache.TaskProjectionSnapshot.Bar;
import com.microproject.pm.graphic.model.cache.TaskProjectionSnapshot.GanttRow;
import com.microproject.pm.graphic.model.cache.TaskProjectionSnapshot.Interval;
import com.microproject.pm.scheduling.IntervalConsumer;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleIntervalGenerator;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.util.DateFieldSupport;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.GanttColorPalette;
import com.microproject.util.GanttProgress;

/** Converts the live task model to the primitive values consumed by Gantt paint and hit testing. */
public final class GanttProjectionCapture {
	public record Options(BarStyles styles, String annotationFieldId, String formatViewName,
			GanttColorPalette palette, boolean assignmentRowsVisible) { }

	private GanttProjectionCapture() { }

	public static TaskProjectionSnapshot capture(Project project, RevisionedProjectionIndex.Snapshot topology,
			TaskProjectionSnapshot base, NodeModel model, Options options, long renderRevision,
			Iterable<GraphicDependency> dependencies) {
		if (options == null || options.styles() == null || options.palette() == null) return base;
		Map<ProjectionRowKey, GanttRow> rows = new HashMap<>();
		Set<Long> criticalChainIds = criticalChainIds(project);
		for (RevisionedProjectionIndex.Row projected : topology.rows()) {
			GraphicNode graphic = projected.node();
			Node node = graphic.getNode();
			Object value = node == null ? null : node.getImpl();
			List<Bar> bars = new ArrayList<>();
			options.styles().apply(value, candidate -> addBar(bars, (BarFormat)candidate, graphic, node, value,
					model, project, options, criticalChainIds));
			boolean horizontalLine = applies(options.styles(), value, false, false, false, true);
			rows.put(projected.key(), new GanttRow(bars, annotation(graphic, node, value, model, options),
					value instanceof Task task ? task.getFontFamily() : null,
					value instanceof Task task ? task.getFontSize() : 0,
					value instanceof Task task ? (task.isFontBold() ? Font.BOLD : Font.PLAIN)
							| (task.isFontItalic() ? Font.ITALIC : Font.PLAIN) : Font.PLAIN,
					value instanceof Task task && task.isFontStrikethrough(),
					value instanceof Task task && task.getFontColor() != null
							? task.getFontColor() : FlatUiSupport.tableForeground().getRGB(), horizontalLine));
		}
		Map<TaskProjectionSnapshot.Edge, List<String>> edgeFormats = new HashMap<>();
		if (dependencies != null) for (GraphicDependency dependency : dependencies) {
			List<String> ids = new ArrayList<>();
			options.styles().apply(dependency, value -> ids.add(((BarFormat)value).getId()), true, false, false, false);
			for (TaskProjectionSnapshot.Edge edge : base.edges())
				if (matches(topology, dependency, edge)) edgeFormats.put(edge, List.copyOf(ids));
		}
		return base.withGanttValues(renderRevision, rows, edgeFormats);
	}

	private static boolean applies(BarStyles styles, Object value, boolean link, boolean annotation,
			boolean calendar, boolean horizontalGrid) {
		boolean[] found = { false };
		styles.apply(value, ignored -> found[0] = true, link, annotation, calendar, horizontalGrid);
		return found[0];
	}

	static boolean hasHorizontalLine(BarStyles styles, Object value) {
		return applies(styles, value, false, false, false, true);
	}

	private static boolean matches(RevisionedProjectionIndex.Snapshot topology, GraphicDependency dependency,
			TaskProjectionSnapshot.Edge edge) {
		int predecessor = topology.rowOf(dependency.getPredecessor());
		int successor = topology.rowOf(dependency.getSuccessor());
		if (predecessor < 0 || successor < 0) return false;
		ProjectionRowKey predecessorKey = topology.keyAt(predecessor);
		ProjectionRowKey successorKey = topology.keyAt(successor);
		return edge.predecessor().taskKey() != null && edge.successor().taskKey() != null
				&& edge.predecessor().taskKey().equals(predecessorKey.taskKey())
				&& edge.successor().taskKey().equals(successorKey.taskKey())
				&& edge.type() == dependency.getType();
	}

	private static void addBar(List<Bar> result, BarFormat format, GraphicNode graphic, Node node, Object value,
			NodeModel model, Project project, Options options, Set<Long> criticalChainIds) {
		if (format == null || suppressBar(value, graphic.isSummary(), format, options.assignmentRowsVisible())) return;
		List<ScheduleInterval> generated = new ArrayList<>();
		ScheduleIntervalGenerator generator = format.getScheduleIntervalGenerator();
		if (generator == null) {
			Field from = format.getFromField();
			Field to = format.getToField();
			Object start = from == null ? null : from.getValue(node, model, null);
			Object end = to == null ? null : to.getValue(node, model, null);
			if (start instanceof Date s && end instanceof Date e)
				generated.add(new ScheduleInterval(s.getTime(), e.getTime()));
		} else {
			generator.consumeIntervals(graphic, (IntervalConsumer)generated::add);
		}
		ScheduleInterval planned = generated.isEmpty() ? null
				: new ScheduleInterval(generated.get(0).getStart(), generated.get(generated.size() - 1).getEnd());
		List<ScheduleInterval> display = GanttBarSupport.displayIntervals(format, generated, planned);
		if (display.isEmpty()) return;
		Schedule schedule = value instanceof Schedule candidate ? candidate : null;
		boolean baseline = GanttBarSupport.isBaselineBarFormat(format);
		Color middle = displayedMiddleColor(value, schedule, baseline, options, criticalChainIds);
		GanttBarFormatOverrides.BarFormat individual = value instanceof Task task
				&& GanttBarSupport.isIndividuallyFormattable(format)
				? project.getGanttBarFormatOverrides().get(options.formatViewName(), task.getUniqueId())
				: GanttBarFormatOverrides.BarFormat.automatic();
		int middleRgb = individual.getMiddleRgb() == null ? rgb(middle) : individual.getMiddleRgb();
		Color displayedMiddle = new Color(middleRgb);
		Color accent = options.palette().getAccentColor(format, displayedMiddle, value);
		Color endpoint = GanttBarSupport.shouldUseUniformEndpointColor(format) ? displayedMiddle : accent;
		int startRgb = individual.getStartRgb() == null ? rgb(endpoint) : individual.getStartRgb();
		int endRgb = individual.getEndRgb() == null ? rgb(endpoint) : individual.getEndRgb();
		double ratio = GanttProgress.ratioForObject(value);
		boolean progressVisible = format.isMain() && GanttProgress.hasVisibleProgress(value)
				&& ("Bar.task".equals(format.getId()) || "Bar.critical".equals(format.getId())
						|| "Bar.summary".equals(format.getId()) || "Bar.assignment".equals(format.getId()));
		result.add(new Bar(format.getId(), format.getLayer(), format.getRow(),
				display.stream().map(i -> new Interval(i.getStart(), i.getEnd())).toList(),
				GanttBarSupport.progressRatiosForIntervals(display, ratio), startRgb, middleRgb, endRgb,
				rgb(options.palette().getProgressFillColor(displayedMiddle)), progressVisible));
	}

	static Color displayedMiddleColor(Object value, Schedule schedule, boolean baseline, Options options,
			Set<Long> criticalChainIds) {
		Color middle = baseline ? options.palette().getBaselineBarColor()
				: options.palette().getStatusColor(schedule, value);
		return !baseline && value instanceof Task task
				&& (task.isCritical() || criticalChainIds.contains(task.getUniqueId()))
				? options.palette().getCriticalTaskColor() : middle;
	}

	private static String annotation(GraphicNode graphic, Node node, Object value, NodeModel model, Options options) {
		if (Gantt.ANNOTATION_FIELD_HIDDEN.equals(options.annotationFieldId())
				|| suppressAnnotation(value, graphic.isSummary(), options.assignmentRowsVisible())) return "";
		List<Field> fields = new ArrayList<>();
		if (options.annotationFieldId() != null) {
			Field field = Configuration.getFieldFromId(options.annotationFieldId());
			if (field != null) fields.add(field);
		} else {
			options.styles().apply(value, candidate -> {
				Field field = ((BarFormat)candidate).getField();
				if (field != null && !fields.contains(field)) fields.add(field);
			}, false, true, false, false);
		}
		for (Field field : fields) {
			Object fieldValue = field.getValue(node, model, null);
			if (fieldValue == null && value != null) fieldValue = field.getValue(value, null);
			String text = DateFieldSupport.annotationTextFor(fieldValue, field);
			if (text != null && !text.isBlank()) return text;
		}
		return "";
	}

	private static boolean suppressBar(Object value, boolean summary, BarFormat format, boolean assignments) {
		return assignments && value instanceof NormalTask task && !summary && task.hasRealAssignments()
				&& ("Bar.task".equals(format.getId()) || "Bar.critical".equals(format.getId()));
	}

	private static boolean suppressAnnotation(Object value, boolean summary, boolean assignments) {
		return assignments && value instanceof NormalTask task && !summary && task.hasRealAssignments();
	}

	static Set<Long> criticalChainIds(Project project) {
		if (!CriticalChainDisplayState.isVisible(project)) return Set.of();
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.findSettings(project);
		CriticalChainService.Baseline baseline = service.findBaseline(project);
		if (settings == null || !settings.isEnabled() || baseline == null) return Set.of();
		CriticalChainService.Analysis analysis = service.findAnalysis(project);
		return new HashSet<>(analysis == null ? baseline.criticalTaskIds() : analysis.criticalTaskIds());
	}

	private static int rgb(Color color) { return color == null ? 0 : color.getRGB(); }
}
