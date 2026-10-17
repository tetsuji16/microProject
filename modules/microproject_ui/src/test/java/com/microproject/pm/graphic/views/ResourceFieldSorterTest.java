package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;

class ResourceFieldSorterTest {
	/** Row = {text, number, date, identity}; null cells model empty values. */
	private static final Object[][] ROWS = {
		{ "b", 10, new Date(3000), "1" },
		{ null, 2, null, "2" },
		{ "a", 10, new Date(1000), "3" },
		{ "b", 2, new Date(2000), "4" },
		{ null, null, new Date(1000), "5" } };

	@SuppressWarnings("unchecked")
	private static Comparator<Object> column(int index) {
		return Comparator.comparing(row -> (Comparable<Object>) ((Object[]) row)[index],
			Comparator.nullsFirst(Comparator.naturalOrder()));
	}

	private static List<String> sorted(ResourceView.ResourceFieldSorter sorter) {
		List<Node> nodes = new ArrayList<>();
		for (Object[] row : ROWS) nodes.add(NodeFactory.getInstance().createNode(row));
		nodes.sort(sorter); // List.sort is stable, so ties must keep their model order
		return nodes.stream().map(node -> (String) ((Object[]) node.getImpl())[3]).toList();
	}

	private static ResourceView.ResourceFieldSorter keys(Object... columnAndDirection) {
		List<ResourceView.ResourceFieldSorter.Key> keys = new ArrayList<>();
		for (int index = 0; index < columnAndDirection.length; index += 2)
			keys.add(new ResourceView.ResourceFieldSorter.Key(column((Integer) columnAndDirection[index]),
				(Boolean) columnAndDirection[index + 1]));
		return new ResourceView.ResourceFieldSorter(keys);
	}

	@Test
	void textAscendingPutsEmptyFirstAndKeepsTiesInModelOrder() {
		assertEquals(Arrays.asList("2", "5", "3", "1", "4"), sorted(keys(0, true)));
	}

	@Test
	void textDescendingReversesKeyOrderAndKeepsTiesInModelOrder() {
		assertEquals(Arrays.asList("1", "4", "3", "2", "5"), sorted(keys(0, false)));
	}

	@Test
	void numericKeyHandlesEmptyAndEqualValuesInBothDirections() {
		assertEquals(Arrays.asList("5", "2", "4", "1", "3"), sorted(keys(1, true)));
		assertEquals(Arrays.asList("1", "3", "2", "4", "5"), sorted(keys(1, false)));
	}

	@Test
	void dateKeyOrdersEmptyAndEqualDates() {
		assertEquals(Arrays.asList("2", "3", "5", "4", "1"), sorted(keys(2, true)));
		assertEquals(Arrays.asList("1", "4", "3", "5", "2"), sorted(keys(2, false)));
	}

	@Test
	void secondaryKeyOnlyBreaksTiesOfThePrimaryKey() {
		// text ascending, then number descending: ties on "b" and on empty are resolved by the second key.
		assertEquals(Arrays.asList("2", "5", "3", "1", "4"), sorted(keys(0, true, 1, false)));
		// text descending, then number ascending.
		assertEquals(Arrays.asList("4", "1", "3", "5", "2"), sorted(keys(0, false, 1, true)));
	}

	@Test
	void thirdKeyResolvesRemainingTiesAndDirectionsAreIndependent() {
		// number descending, then text ascending, then date descending.
		assertEquals(Arrays.asList("3", "1", "2", "4", "5"), sorted(keys(1, false, 0, true, 2, false)));
	}
}
