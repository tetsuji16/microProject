/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structurally immutable, versioned row topology for one view cache.
 *
 * <p>The contained {@link GraphicNode} is a transitional legacy handle. The row
 * list and both indexes are immutable after publication, and view row data is
 * never written back to the shared node.</p>
 */
public final class RevisionedProjectionIndex {
	public record Row(ProjectionRowKey key, GraphicNode node) {
	}

	public static final class Snapshot {
		private final long domainRevision;
		private final long topologyRevision;
		private final List<Row> rows;
		private final Map<GraphicNode, Integer> rowsByNode;
		private final Map<ProjectionRowKey, Integer> rowsByKey;

		private Snapshot(long domainRevision, long topologyRevision, List<Row> rows, Map<GraphicNode, Integer> rowsByNode,
				Map<ProjectionRowKey, Integer> rowsByKey) {
			this.domainRevision = domainRevision;
			this.topologyRevision = topologyRevision;
			this.rows = List.copyOf(rows);
			this.rowsByNode = Collections.unmodifiableMap(rowsByNode);
			this.rowsByKey = Collections.unmodifiableMap(rowsByKey);
		}

		public long domainRevision() {
			return domainRevision;
		}

		public long topologyRevision() {
			return topologyRevision;
		}

		public List<Row> rows() {
			return rows;
		}

		public int rowOf(GraphicNode node) {
			Integer row = rowsByNode.get(node);
			return row == null ? -1 : row.intValue();
		}

		public int rowOf(ProjectionRowKey key) {
			Integer row = rowsByKey.get(key);
			return row == null ? -1 : row.intValue();
		}

		public ProjectionRowKey keyAt(int row) {
			return row < 0 || row >= rows.size() ? null : rows.get(row).key();
		}
	}

	private final ProjectionRowKeyResolver keyResolver = new ProjectionRowKeyResolver();
	private volatile Snapshot snapshot = new Snapshot(0L, 0L, List.of(), Map.of(), Map.of());
	private long refreshCount;

	public synchronized Snapshot refresh(List<?> elements) {
		return refresh(elements, snapshot.domainRevision());
	}

	public synchronized Snapshot refresh(List<?> elements, long domainRevision) {
		Snapshot candidate = candidate(elements, domainRevision);
		publish(candidate);
		return candidate;
	}

	synchronized Snapshot candidate(List<?> elements, long domainRevision) {
		refreshCount++;
		List<Row> rows = new ArrayList<>(elements == null ? 0 : elements.size());
		Map<GraphicNode, Integer> byNode = new IdentityHashMap<>();
		Map<ProjectionRowKey, Integer> byKey = new HashMap<>();
		if (elements != null) {
			for (Object value : elements) {
				if (!(value instanceof GraphicNode node))
					continue;
				ProjectionRowKey key = keyResolver.resolve(node);
				int row = rows.size();
				Integer existingRow = byKey.get(key);
				if (existingRow != null) {
					GraphicNode existingNode = rows.get(existingRow.intValue()).node();
					if (!sameProjectedEntity(existingNode, node))
						throw new IllegalStateException("Duplicate durable projection identity: " + key);
					key = duplicateOccurrenceKey(key, row);
				}
				byKey.put(key, Integer.valueOf(row));
				byNode.put(node, Integer.valueOf(row));
				rows.add(new Row(key, node));
			}
		}
		if (sameTopology(snapshot, rows) && snapshot.domainRevision() == domainRevision)
			return snapshot;
		long topologyRevision = sameTopology(snapshot, rows)
				? snapshot.topologyRevision()
				: snapshot.topologyRevision() + 1L;
		return new Snapshot(domainRevision, topologyRevision, rows, byNode, byKey);
	}

	synchronized void publish(Snapshot candidate) {
		if (candidate != null && candidate.domainRevision() >= snapshot.domainRevision()) snapshot = candidate;
	}

	private static boolean sameTopology(Snapshot current, List<Row> candidate) {
		if (current.rows().size() != candidate.size())
			return false;
		for (int row = 0; row < candidate.size(); row++) {
			Row existing = current.rows().get(row);
			Row replacement = candidate.get(row);
			if (!existing.key().equals(replacement.key()) || existing.node() != replacement.node())
				return false;
		}
		return true;
	}

	private static ProjectionRowKey duplicateOccurrenceKey(ProjectionRowKey key, int row) {
		long occurrenceId = row + 1L;
		ProjectionRowKey duplicate = new ProjectionRowKey(key.kind(), key.taskKey(), key.entityId(), occurrenceId);
		return duplicate;
	}

	private static boolean sameProjectedEntity(GraphicNode first, GraphicNode second) {
		if (first == second || first.getNode() == second.getNode())
			return true;
		return first.getNode() != null && second.getNode() != null
				&& first.getNode().getImpl() == second.getNode().getImpl();
	}

	public Snapshot snapshot() {
		return snapshot;
	}

	public synchronized long refreshCount() { return refreshCount; }
}
