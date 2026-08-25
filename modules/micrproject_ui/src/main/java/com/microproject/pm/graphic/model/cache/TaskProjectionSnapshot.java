/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.Task;

/** Value-only render projection; it never retains Task, Node, or GraphicNode. */
public final class TaskProjectionSnapshot {
	public record Interval(long start, long end) { }
	public record Bar(String formatId, int layer, int row, List<Interval> intervals, List<Double> progressRatios,
			int startRgb, int middleRgb, int endRgb, int progressRgb, boolean progressVisible) {
		public Bar {
			intervals = List.copyOf(intervals);
			progressRatios = List.copyOf(progressRatios);
		}
	}
	public record GanttRow(List<Bar> bars, String annotation, String fontFamily, int fontSize, int fontStyle,
			boolean fontStrikethrough, int fontRgb, boolean horizontalLine) {
		public GanttRow { bars = List.copyOf(bars); }
		public static GanttRow empty() { return new GanttRow(List.of(), "", null, 0, 0, false, 0, false); }
	}
	public record Edge(ProjectionRowKey predecessor, ProjectionRowKey successor, int type, long lag,
			boolean disabled, boolean crossProject) { }
	public record Row(ProjectionRowKey key, boolean voidRow, boolean summary,
			boolean milestone, boolean schedule, boolean assignment, boolean external,
			boolean subproject, boolean collapsed, long start, long end, long completed, double percentComplete,
			List<Interval> intervals) {
		public Row {
			intervals = List.copyOf(intervals);
		}
	}

	private final long domainRevision;
	private final long topologyRevision;
	private final long renderRevision;
	private final long projectStatusDate;
	private final List<Row> rows;
	private final List<Edge> edges;
	private final Map<ProjectionRowKey, Integer> rowByKey;
	private final Map<ProjectionRowKey, GanttRow> ganttRows;
	private final Map<Edge, List<String>> edgeFormatIds;

	TaskProjectionSnapshot(long domainRevision, long topologyRevision, long projectStatusDate, List<Row> rows) {
		this(domainRevision, topologyRevision, projectStatusDate, rows, List.of());
	}

	TaskProjectionSnapshot(long domainRevision, long topologyRevision, long projectStatusDate,
			List<Row> rows, List<Edge> edges) {
		this(domainRevision, topologyRevision, 0L, projectStatusDate, rows, edges, Map.of(), Map.of());
	}

	private TaskProjectionSnapshot(long domainRevision, long topologyRevision, long renderRevision, long projectStatusDate,
			List<Row> rows, List<Edge> edges, Map<ProjectionRowKey, GanttRow> ganttRows,
			Map<Edge, List<String>> edgeFormatIds) {
		this.domainRevision = domainRevision;
		this.topologyRevision = topologyRevision;
		this.renderRevision = renderRevision;
		this.projectStatusDate = projectStatusDate;
		this.rows = List.copyOf(rows);
		this.edges = List.copyOf(edges);
		Map<ProjectionRowKey, Integer> index = new HashMap<>();
		for (int row = 0; row < rows.size(); row++) index.put(rows.get(row).key(), row);
		rowByKey = Map.copyOf(index);
		this.ganttRows = Map.copyOf(ganttRows);
		Map<Edge, List<String>> immutableFormats = new HashMap<>();
		edgeFormatIds.forEach((edge, ids) -> immutableFormats.put(edge, List.copyOf(ids)));
		this.edgeFormatIds = Map.copyOf(immutableFormats);
	}

	public TaskProjectionSnapshot withGanttValues(long renderRevision, Map<ProjectionRowKey, GanttRow> rows,
			Map<Edge, List<String>> edgeFormats) {
		return new TaskProjectionSnapshot(domainRevision, topologyRevision, renderRevision, projectStatusDate,
				this.rows, edges, rows, edgeFormats);
	}

	public static TaskProjectionSnapshot empty() {
		return new TaskProjectionSnapshot(0L, 0L, 0L, List.of(), List.of());
	}

	static Optional<TaskProjectionSnapshot> capture(Project project, RevisionedProjectionIndex.Snapshot topology,
			Predicate<GraphicNode> collapsedResolver) {
		return project.getDomainChangeJournal().read(
				() -> captureLocked(project, topology, collapsedResolver));
	}

	private static Optional<TaskProjectionSnapshot> captureLocked(Project project,
			RevisionedProjectionIndex.Snapshot topology, Predicate<GraphicNode> collapsedResolver) {
		long expected = topology.domainRevision();
		if (project.getDomainChangeJournal().revision() != expected) return Optional.empty();
		List<Row> values = new ArrayList<>(topology.rows().size());
		Map<ProjectTaskKey, List<ProjectionRowKey>> visibleTaskKeys = new HashMap<>();
		for (RevisionedProjectionIndex.Row projected : topology.rows()) {
			GraphicNode node = projected.node();
			Object impl = node.getNode() == null ? null : node.getNode().getImpl();
			if (impl instanceof Task task)
				ProjectTaskKey.from(task).ifPresent(key -> visibleTaskKeys
						.computeIfAbsent(key, ignored -> new ArrayList<>()).add(projected.key()));
			List<Interval> intervals = java.util.Collections.synchronizedList(new ArrayList<>());
			if (!node.isVoid()) node.consumeIntervals(value -> intervals.add(new Interval(value.getStart(), value.getEnd())));
			boolean milestone = impl instanceof Task task && task.isMilestone();
			boolean external = impl instanceof Task task && task.isExternal();
			boolean subproject = impl instanceof Task task && task.isSubproject();
			double percentComplete = impl instanceof com.microproject.pm.scheduling.Schedule schedule
					? schedule.getPercentComplete() : 0.0d;
			values.add(new Row(projected.key(), node.isVoid(), node.isSummary(), milestone,
					node.isSchedule(), node.isAssignment(), external,
					subproject, collapsedResolver.test(node), node.getStart(), node.getEnd(), node.getCompleted(), percentComplete,
					List.copyOf(intervals)));
		}
		List<Edge> edges = new ArrayList<>();
		Set<Dependency> captured = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		for (RevisionedProjectionIndex.Row projected : topology.rows()) {
			Object impl = projected.node().getNode() == null ? null : projected.node().getNode().getImpl();
			if (!(impl instanceof Task task)) continue;
			for (java.util.Iterator<?> iterator = task.getSuccessorList().iterator(); iterator.hasNext();) {
				Dependency dependency = (Dependency) iterator.next();
				if (!captured.add(dependency) || !(dependency.getPredecessor() instanceof Task predecessor)
						|| !(dependency.getSuccessor() instanceof Task successor)) continue;
				List<ProjectionRowKey> predecessorRows = ProjectTaskKey.from(predecessor)
						.map(visibleTaskKeys::get).orElse(List.of());
				List<ProjectionRowKey> successorRows = ProjectTaskKey.from(successor)
						.map(visibleTaskKeys::get).orElse(List.of());
				if (!predecessorRows.isEmpty() && !successorRows.isEmpty())
					edges.add(new Edge(predecessorRows.get(0), successorRows.get(0), dependency.getDependencyType(),
							dependency.getLag(), dependency.isDisabled(), dependency.isCrossProject()));
			}
		}
		if (project.getDomainChangeJournal().revision() != expected) return Optional.empty();
		return Optional.of(new TaskProjectionSnapshot(expected, topology.topologyRevision(), project.getStatusDate(), values, edges));
	}

	public long domainRevision() { return domainRevision; }
	public long topologyRevision() { return topologyRevision; }
	public long renderRevision() { return renderRevision; }
	public long projectStatusDate() { return projectStatusDate; }
	public List<Row> rows() { return rows; }
	public List<Edge> edges() { return edges; }
	public GanttRow ganttRow(ProjectionRowKey key) { return ganttRows.getOrDefault(key, GanttRow.empty()); }
	public List<String> edgeFormatIds(Edge edge) { return edgeFormatIds.getOrDefault(edge, List.of()); }
	public Row rowAt(int row) { return row < 0 || row >= rows.size() ? null : rows.get(row); }
	public int rowOf(ProjectionRowKey key) { return rowByKey.getOrDefault(key, -1); }
}
