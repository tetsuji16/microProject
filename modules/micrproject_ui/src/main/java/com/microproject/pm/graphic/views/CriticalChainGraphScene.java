/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microproject.dialog.UsabilityStrings;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

/**
 * Immutable, UI-neutral projection of a CCPM analysis for the network graph.
 *
 * <p>This is deliberately separate from Swing painting.  It is the extension
 * point for future selection, filtering, zoom, accessibility navigation and
 * exports: those features consume the same stable node keys and geometry
 * instead of rebuilding a second interpretation of the critical chain.</p>
 */
public final class CriticalChainGraphScene {
	static final int NODE_WIDTH = 170;
	static final int NODE_HEIGHT = 46;
	private static final int HORIZONTAL_GAP = 70;
	private static final int VERTICAL_GAP = 28;
	private static final int MARGIN = 20;
	private static final DecimalFormat BUFFER_DAYS = new DecimalFormat("0.0");

	public enum NodeKind { TASK, PROJECT_BUFFER, FEEDING_BUFFER }
	public enum EdgeKind { DEPENDENCY, RESOURCE_CONSTRAINT, BUFFER_PROTECTION }

	/** Immutable rectangular hit target in canvas coordinates. */
	public record Bounds(int x, int y, int width, int height) {
		public boolean contains(int pointX, int pointY) {
			return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
		}
	}

	/** A stable graph item. Keys are task:<uniqueId>, project-buffer, or feeding-buffer:<taskId>. */
	public record Node(String key, NodeKind kind, String title, String detail, Bounds bounds,
		CriticalChainService.Buffer buffer) { }
	public record Edge(String sourceKey, String targetKey, EdgeKind kind) { }

	private final Dimension preferredSize;
	private final List<Node> nodes;
	private final List<Edge> edges;
	private final Map<String, Node> nodesByKey;

	private CriticalChainGraphScene(Dimension preferredSize, List<Node> nodes, List<Edge> edges) {
		this.preferredSize = new Dimension(preferredSize);
		this.nodes = List.copyOf(nodes);
		this.edges = List.copyOf(edges);
		Map<String, Node> index = new LinkedHashMap<>(Math.max(4, nodes.size() * 4 / 3 + 1));
		for (Node node : nodes) index.put(node.key(), node);
		this.nodesByKey = Map.copyOf(index);
	}

	public Dimension preferredSize() { return new Dimension(preferredSize); }
	public List<Node> nodes() { return nodes; }
	public List<Edge> edges() { return edges; }
	public Node node(String key) { return nodesByKey.get(key); }
	public Node nodeAt(int x, int y) {
		for (Node node : nodes) if (node.bounds().contains(x, y)) return node;
		return null;
	}

	public static CriticalChainGraphScene from(Project project, CriticalChainService.Analysis analysis) {
		if (analysis == null || analysis.criticalTaskIds().isEmpty()) {
			return new CriticalChainGraphScene(new Dimension(760, 420), List.of(), List.of());
		}
		Map<Long, String> taskNames = taskNames(project, analysis);
		Map<Long, Integer> columns = columns(analysis.criticalTaskIds(), analysis.graphEdges());
		Map<Integer, Integer> nextRows = new LinkedHashMap<>();
		List<Node> nodes = new ArrayList<>(analysis.criticalTaskIds().size() + analysis.feedingBuffers().size() + 1);
		for (Long id : analysis.criticalTaskIds()) {
			int column = columns.getOrDefault(id, 0);
			int row = nextRows.merge(Integer.valueOf(column), Integer.valueOf(1), Integer::sum).intValue() - 1;
			nodes.add(new Node(taskKey(id.longValue()), NodeKind.TASK,
				taskNames.getOrDefault(id, UsabilityStrings.text("common.task") + " " + id), "#" + id,
				new Bounds(MARGIN + column * (NODE_WIDTH + HORIZONTAL_GAP),
					MARGIN + row * (NODE_HEIGHT + VERTICAL_GAP), NODE_WIDTH, NODE_HEIGHT), null));
		}
		int maxColumn = columns.values().stream().mapToInt(Integer::intValue).max().orElse(0);
		int bufferX = MARGIN + (maxColumn + 1) * (NODE_WIDTH + HORIZONTAL_GAP);
		nodes.add(bufferNode("project-buffer", NodeKind.PROJECT_BUFFER, UsabilityStrings.text("ccpm.projectBuffer"),
			analysis.projectBuffer(), bufferX, MARGIN));
		int feederRow = 1;
		for (Map.Entry<Long, CriticalChainService.Buffer> entry : analysis.feedingBuffers().entrySet()) {
			nodes.add(bufferNode(feedingBufferKey(entry.getKey().longValue()), NodeKind.FEEDING_BUFFER,
				UsabilityStrings.text("ccpm.feedingBuffer") + " #" + entry.getKey(), entry.getValue(), bufferX,
				MARGIN + feederRow++ * (NODE_HEIGHT + VERTICAL_GAP)));
		}
		List<Edge> edges = new ArrayList<>(analysis.graphEdges().size() + analysis.feedingBuffers().size() + 1);
		for (CriticalChainService.ChainEdge edge : analysis.graphEdges()) {
			edges.add(new Edge(taskKey(edge.predecessorTaskId()), taskKey(edge.successorTaskId()),
				edge.kind() == CriticalChainService.ChainEdge.Kind.RESOURCE_CONSTRAINT ? EdgeKind.RESOURCE_CONSTRAINT : EdgeKind.DEPENDENCY));
		}
		for (Long targetTaskId : analysis.feedingBuffers().keySet()) {
			edges.add(new Edge(feedingBufferKey(targetTaskId.longValue()), taskKey(targetTaskId.longValue()), EdgeKind.BUFFER_PROTECTION));
		}
		for (Long terminalTaskId : terminalTaskIds(analysis.criticalTaskIds(), analysis.graphEdges())) {
			edges.add(new Edge(taskKey(terminalTaskId.longValue()), "project-buffer", EdgeKind.BUFFER_PROTECTION));
		}
		int maxRows = Math.max(nextRows.values().stream().mapToInt(Integer::intValue).max().orElse(1), feederRow);
		return new CriticalChainGraphScene(new Dimension(
			Math.max(760, 2 * MARGIN + (maxColumn + 2) * (NODE_WIDTH + HORIZONTAL_GAP)),
			Math.max(420, 2 * MARGIN + maxRows * (NODE_HEIGHT + VERTICAL_GAP))), nodes, edges);
	}

	private static Node bufferNode(String key, NodeKind kind, String title, CriticalChainService.Buffer buffer, int x, int y) {
		CriticalChainService.Buffer safeBuffer = buffer == null
			? new CriticalChainService.Buffer(0L, 0L, 0L, 0D, CriticalChainService.BufferStatus.GREEN) : buffer;
		long plannedDays = Math.max(0L, safeBuffer.plannedMillis()) / (24L * 60L * 60L * 1000L);
		return new Node(key, kind, title, MessageFormat.format(UsabilityStrings.text("ccpm.bufferDays"),
			BUFFER_DAYS.format(plannedDays), safeBuffer.status()), new Bounds(x, y, NODE_WIDTH, NODE_HEIGHT), safeBuffer);
	}

	private static Map<Long, String> taskNames(Project project, CriticalChainService.Analysis analysis) {
		Map<Long, String> result = new LinkedHashMap<>(Math.max(4, analysis.criticalTaskIds().size() * 4 / 3 + 1));
		if (project == null) return result;
		for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next();
			result.put(Long.valueOf(task.getUniqueId()), task.getName());
		}
		return result;
	}

	private static Map<Long, Integer> columns(List<Long> ids, List<CriticalChainService.ChainEdge> edges) {
		Map<Long, Integer> result = new LinkedHashMap<>(Math.max(4, ids.size() * 4 / 3 + 1));
		for (Long id : ids) result.put(id, Integer.valueOf(0));
		boolean changed;
		int rounds = 0;
		do {
			changed = false;
			for (CriticalChainService.ChainEdge edge : edges) {
				if (!result.containsKey(edge.predecessorTaskId()) || !result.containsKey(edge.successorTaskId())) continue;
				int candidate = result.get(edge.predecessorTaskId()).intValue() + 1;
				if (candidate > result.get(edge.successorTaskId()).intValue()) {
					result.put(edge.successorTaskId(), Integer.valueOf(candidate));
					changed = true;
				}
			}
		} while (changed && ++rounds <= ids.size());
		return result;
	}

	private static Set<Long> terminalTaskIds(List<Long> ids, List<CriticalChainService.ChainEdge> edges) {
		Set<Long> result = new LinkedHashSet<>(ids);
		for (CriticalChainService.ChainEdge edge : edges) result.remove(Long.valueOf(edge.predecessorTaskId()));
		return result;
	}

	private static String taskKey(long taskId) { return "task:" + taskId; }
	private static String feedingBufferKey(long taskId) { return "feeding-buffer:" + taskId; }
}
