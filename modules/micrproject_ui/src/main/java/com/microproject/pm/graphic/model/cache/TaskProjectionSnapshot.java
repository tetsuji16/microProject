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
import java.util.function.ToIntFunction;
import java.util.function.Predicate;

import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

/** Value-only render projection; it never retains Task, Node, or GraphicNode. */
public final class TaskProjectionSnapshot {
	public record Interval(long start, long end) { }
	public record Row(ProjectionRowKey key, int level, boolean voidRow, boolean group, boolean summary,
			boolean milestone, boolean readOnly, boolean schedule, boolean assignment, boolean external,
			boolean subproject, boolean collapsed, long start, long end, long completed, double percentComplete, String label,
			List<Interval> intervals) {
		public Row {
			intervals = List.copyOf(intervals);
		}
	}

	private final long domainRevision;
	private final long topologyRevision;
	private final long projectStatusDate;
	private final List<Row> rows;
	private final Map<ProjectionRowKey, Integer> rowByKey;

	TaskProjectionSnapshot(long domainRevision, long topologyRevision, long projectStatusDate, List<Row> rows) {
		this.domainRevision = domainRevision;
		this.topologyRevision = topologyRevision;
		this.projectStatusDate = projectStatusDate;
		this.rows = List.copyOf(rows);
		Map<ProjectionRowKey, Integer> index = new HashMap<>();
		for (int row = 0; row < rows.size(); row++) index.put(rows.get(row).key(), row);
		rowByKey = Map.copyOf(index);
	}

	public static TaskProjectionSnapshot empty() {
		return new TaskProjectionSnapshot(0L, 0L, 0L, List.of());
	}

	static Optional<TaskProjectionSnapshot> capture(Project project, RevisionedProjectionIndex.Snapshot topology,
			ToIntFunction<GraphicNode> levelResolver, Predicate<GraphicNode> collapsedResolver) {
		return project.getDomainChangeJournal().read(
				() -> captureLocked(project, topology, levelResolver, collapsedResolver));
	}

	private static Optional<TaskProjectionSnapshot> captureLocked(Project project,
			RevisionedProjectionIndex.Snapshot topology, ToIntFunction<GraphicNode> levelResolver,
			Predicate<GraphicNode> collapsedResolver) {
		long expected = topology.domainRevision();
		if (project.getDomainChangeJournal().revision() != expected) return Optional.empty();
		List<Row> values = new ArrayList<>(topology.rows().size());
		for (RevisionedProjectionIndex.Row projected : topology.rows()) {
			GraphicNode node = projected.node();
			Object impl = node.getNode() == null ? null : node.getNode().getImpl();
			List<Interval> intervals = java.util.Collections.synchronizedList(new ArrayList<>());
			if (!node.isVoid()) node.consumeIntervals(value -> intervals.add(new Interval(value.getStart(), value.getEnd())));
			boolean milestone = impl instanceof Task task && task.isMilestone();
			boolean readOnly = impl instanceof Task task && task.isReadOnly();
			boolean external = impl instanceof Task task && task.isExternal();
			boolean subproject = impl instanceof Task task && task.isSubproject();
			double percentComplete = impl instanceof com.microproject.pm.scheduling.Schedule schedule
					? schedule.getPercentComplete() : 0.0d;
			values.add(new Row(projected.key(), levelResolver.applyAsInt(node), node.isVoid(), node.isGroup(),
					node.isSummary(), milestone, readOnly, node.isSchedule(), node.isAssignment(), external,
					subproject, collapsedResolver.test(node), node.getStart(), node.getEnd(), node.getCompleted(), percentComplete,
					impl == null ? "" : String.valueOf(impl), List.copyOf(intervals)));
		}
		if (project.getDomainChangeJournal().revision() != expected) return Optional.empty();
		return Optional.of(new TaskProjectionSnapshot(expected, topology.topologyRevision(), project.getStatusDate(), values));
	}

	public long domainRevision() { return domainRevision; }
	public long topologyRevision() { return topologyRevision; }
	public long projectStatusDate() { return projectStatusDate; }
	public List<Row> rows() { return rows; }
	public Row rowAt(int row) { return row < 0 || row >= rows.size() ? null : rows.get(row); }
	public int rowOf(ProjectionRowKey key) { return rowByKey.getOrDefault(key, -1); }
}
