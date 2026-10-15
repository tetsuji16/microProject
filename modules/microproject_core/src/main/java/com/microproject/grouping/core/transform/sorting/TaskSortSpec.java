/*
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
 */
package com.microproject.grouping.core.transform.sorting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.transform.TransformList;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.Task;

/** Immutable task-view sort contract. Keys are evaluated in order; equal rows
 * retain their encounter order through the stable list sort used by callers. */
public record TaskSortSpec(List<Key> keys, boolean keepOutline) {
	public static final int MAX_KEYS = 3;

	public TaskSortSpec {
		keys = List.copyOf(Objects.requireNonNull(keys, "keys"));
		if (keys.size() > MAX_KEYS) throw new IllegalArgumentException("At most three sort keys are supported");
		if (keys.stream().map(Key::fieldId).distinct().count() != keys.size())
			throw new IllegalArgumentException("A sort field can be selected only once");
	}

	public record Key(String fieldId, boolean ascending) {
		public Key {
			if (fieldId == null || fieldId.isBlank()) throw new IllegalArgumentException("A sort field is required");
		}
	}

	public static TaskSortSpec outlineDefault() { return new TaskSortSpec(List.of(), true); }

	/** Resolve configured task fields through the shared sorter registry. */
	public Comparator<Object> comparator() {
		return comparator(id -> {
			Object configured = TransformList.getInstance("user_sorters").getTransform(id);
			if (!(configured instanceof NodeSorter nodeSorter))
				throw new IllegalArgumentException("Invalid task sort field: " + id);
			return nodeSorter;
		});
	}

	/** Missing task dates sort last in either direction. Other empty values use
	 * their configured field comparator; stable encounter order resolves ties. */
	public Comparator<Object> comparator(Function<String, NodeSorter> sorterResolver) {
		List<NodeSorter> sorters = new ArrayList<>(keys.size());
		for (Key key : keys) sorters.add(Objects.requireNonNull(sorterResolver.apply(key.fieldId()), "Unknown sort field: " + key.fieldId()));
		return (left, right) -> {
			for (int index = 0; index < keys.size(); index++) {
				Key key = keys.get(index);
				int missing = missingDateCompare(key.fieldId(), left, right);
				if (missing != Integer.MIN_VALUE && missing != 0) return missing;
				int result = Integer.signum(sorters.get(index).compare(left, right));
				if (result != 0) return key.ascending() ? result : -result;
			}
			return 0;
		};
	}

	private static int missingDateCompare(String fieldId, Object left, Object right) {
		if (!"Sorter.StartDate".equals(fieldId) && !"Sorter.FinishDate".equals(fieldId)) return Integer.MIN_VALUE;
		boolean leftMissing = missingDate(left, fieldId);
		boolean rightMissing = missingDate(right, fieldId);
		if (leftMissing == rightMissing) return 0;
		return leftMissing ? 1 : -1;
	}

	private static boolean missingDate(Object value, String fieldId) {
		Object impl = value instanceof Node node ? node.getImpl() : value;
		if (impl instanceof Task task)
			return "Sorter.StartDate".equals(fieldId) ? task.getStart() == 0L : task.getEnd() == 0L;
		if (impl instanceof Assignment assignment)
			return "Sorter.StartDate".equals(fieldId) ? assignment.getStart() == 0L : assignment.getEnd() == 0L;
		return false;
	}
}
