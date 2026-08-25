/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.model.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.Task;

/** Keeps synthetic group identity stable without using localized display text. */
final class SyntheticGroupIdentityRegistry {
	private record Entry(String id, int level, String parentId, Set<Object> members) { }
	private final List<Entry> entries = new ArrayList<>();
	private final Set<String> usedThisGeneration = new HashSet<>();
	private long nextId = 1L;

	void beginGeneration() { usedThisGeneration.clear(); }

	String resolve(int level, String parentId, List<GraphicNode> nodes) {
		Set<Object> members = memberTokens(nodes);
		Entry best = null;
		int bestOverlap = 0;
		for (Entry entry : entries) {
			if (entry.level() != level || !java.util.Objects.equals(entry.parentId(), parentId)
					|| usedThisGeneration.contains(entry.id())) continue;
			int overlap = overlap(entry.members(), members);
			if (overlap > bestOverlap) {
				best = entry;
				bestOverlap = overlap;
			}
		}
		if (best == null) {
			best = new Entry("group-" + nextId++, level, parentId, new HashSet<>());
			entries.add(best);
		}
		best.members().addAll(members);
		usedThisGeneration.add(best.id());
		return best.id();
	}

	private static Set<Object> memberTokens(List<GraphicNode> nodes) {
		Set<Object> result = new HashSet<>();
		for (GraphicNode node : nodes) {
			Object impl = node == null || node.getNode() == null ? null : node.getNode().getImpl();
			if (impl instanceof Task task)
				result.add(ProjectTaskKey.from(task).map(Object.class::cast).orElse(task));
			else if (impl != null)
				result.add(impl);
		}
		return result;
	}

	private static int overlap(Set<Object> first, Set<Object> second) {
		int count = 0;
		for (Object value : second) if (first.contains(value)) count++;
		return count;
	}
}
