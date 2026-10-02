/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Immutable row/key lookup snapshot for one visible view projection. */
public final class RevisionedProjectionIndex {
	private final long topologyRevision;
	private final List<GraphicNode> nodes;
	private final List<ProjectionRowKey> rowKeys;
	private final Map<ProjectionRowKey, Integer> rowsByKey;
	private final IdentityHashMap<GraphicNode, List<Integer>> rowsByNode;

	private RevisionedProjectionIndex(long topologyRevision, List<GraphicNode> nodes,
			List<ProjectionRowKey> rowKeys, Map<ProjectionRowKey, Integer> rowsByKey,
			IdentityHashMap<GraphicNode, List<Integer>> rowsByNode) {
		this.topologyRevision = topologyRevision;
		this.nodes = List.copyOf(nodes);
		this.rowKeys = List.copyOf(rowKeys);
		this.rowsByKey = Map.copyOf(rowsByKey);
		this.rowsByNode = new IdentityHashMap<>();
		rowsByNode.forEach((node, rows) -> this.rowsByNode.put(node, List.copyOf(rows)));
	}

	public static RevisionedProjectionIndex empty() {
		return create(List.of(), null);
	}

	public static RevisionedProjectionIndex create(List<GraphicNode> projectedNodes,
			RevisionedProjectionIndex previous) {
		if (projectedNodes == null)
			throw new IllegalArgumentException("projectedNodes must not be null");
		List<GraphicNode> snapshot = List.copyOf(projectedNodes);
		Map<Object, Integer> occurrences = new HashMap<>();
		List<ProjectionRowKey> keys = new ArrayList<>(snapshot.size());
		Map<ProjectionRowKey, Integer> keyRows = new HashMap<>(snapshot.size());
		IdentityHashMap<GraphicNode, List<Integer>> nodeRows = new IdentityHashMap<>();
		for (int row = 0; row < snapshot.size(); row++) {
			GraphicNode graphicNode = snapshot.get(row);
			Object occurrenceIdentity = occurrenceIdentity(graphicNode);
			int occurrence = occurrences.merge(occurrenceIdentity, 1, Integer::sum) - 1;
			ProjectionRowKey key = ProjectionRowKey.forNode(graphicNode, occurrence);
			keys.add(key);
			keyRows.putIfAbsent(key, row);
			nodeRows.computeIfAbsent(graphicNode, ignored -> new ArrayList<>()).add(row);
		}
		boolean sameTopology = previous != null && previous.rowKeys.equals(keys);
		long revision = previous == null ? 0L
				: previous.topologyRevision + (sameTopology ? 0L : 1L);
		return new RevisionedProjectionIndex(revision, snapshot, keys, keyRows, nodeRows);
	}

	private static Object occurrenceIdentity(GraphicNode graphicNode) {
		ProjectionRowKey key = ProjectionRowKey.forNode(graphicNode, 0);
		return key instanceof ProjectionRowKey.TaskRow taskRow ? taskRow.taskKey()
				: ((ProjectionRowKey.SyntheticRow) key).node();
	}

	public long topologyRevision() {
		return topologyRevision;
	}

	public int size() {
		return nodes.size();
	}

	public GraphicNode nodeAt(int row) {
		return nodes.get(row);
	}

	public ProjectionRowKey keyAt(int row) {
		return rowKeys.get(row);
	}

	public int rowForKey(ProjectionRowKey key) {
		return rowsByKey.getOrDefault(key, -1);
	}

	/** Returns the first row for a node; use {@link #rowsForNode(GraphicNode)} for duplicate occurrences. */
	public int rowForNode(GraphicNode node) {
		List<Integer> rows = rowsByNode.get(node);
		return rows == null || rows.isEmpty() ? -1 : rows.getFirst();
	}

	public List<Integer> rowsForNode(GraphicNode node) {
		return rowsByNode.getOrDefault(node, List.of());
	}

	public List<GraphicNode> nodes() {
		return nodes;
	}

	public List<ProjectionRowKey> rowKeys() {
		return rowKeys;
	}

	public Map<ProjectionRowKey, Integer> rowsByKey() {
		return rowsByKey;
	}

}
