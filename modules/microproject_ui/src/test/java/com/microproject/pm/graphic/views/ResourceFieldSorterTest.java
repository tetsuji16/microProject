package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;

class ResourceFieldSorterTest {
	private static final Comparator<Object> NULLS_FIRST_NATURAL = Comparator
			.nullsFirst((Comparator<Object>) (a, b) -> ((String) a).compareTo((String) b));
	// Each entry is {sort key, identity label}.
	private static final String[][] ROWS = { { "b", "1" }, { null, "2" }, { "a", "3" }, { "b", "4" }, { null, "5" } };

	private static String key(Object resource) {
		return ((String[]) resource)[0];
	}

	private static List<String> sorted(boolean ascending) {
		ResourceView.ResourceFieldSorter sorter = new ResourceView.ResourceFieldSorter(
				(l, r) -> NULLS_FIRST_NATURAL.compare(key(l), key(r)), ascending);
		List<Node> nodes = new ArrayList<>();
		for (String[] row : ROWS) nodes.add(NodeFactory.getInstance().createNode(row));
		nodes.sort(sorter); // List.sort is stable, so ties must keep their model order
		return nodes.stream().map(n -> ((String[]) n.getImpl())[1]).toList();
	}

	@Test
	void ascendingPutsEmptyValuesFirstAndKeepsTiesInModelOrder() {
		assertEquals(Arrays.asList("2", "5", "3", "1", "4"), sorted(true));
	}

	@Test
	void descendingReversesOnlyTheKeyOrderAndKeepsTiesInModelOrder() {
		assertEquals(Arrays.asList("1", "4", "3", "2", "5"), sorted(false));
	}
}
