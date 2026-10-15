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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.microproject.pm.task.NormalTask;

class TaskSortSpecTest {
	private record Row(int first, int second, String encounter) { }

	@Test
	// Microsoft Project Application.Sort exposes Key1/Key2/Key3 and independent
	// directions; equal-key rows use our documented stable-encounter product rule.
	// https://learn.microsoft.com/en-us/office/vba/api/project.application.sort
	void comparesUpToThreeIndependentKeysAndLeavesExactTiesStable() {
		TaskSortSpec spec = new TaskSortSpec(List.of(
				new TaskSortSpec.Key("first", true),
				new TaskSortSpec.Key("second", false)), true);
		Map<String, NodeSorter> sorters = Map.of(
				"first", sorter((left, right) -> Integer.compare(((Row) left).first(), ((Row) right).first())),
				"second", sorter((left, right) -> Integer.compare(((Row) left).second(), ((Row) right).second())));
		List<Row> rows = new ArrayList<>(List.of(
				new Row(1, 2, "encounter-1"), new Row(1, 2, "encounter-2"),
				new Row(1, 9, "secondary"), new Row(0, 0, "primary")));

		rows.sort(spec.comparator(sorters::get));

		assertEquals(List.of("primary", "secondary", "encounter-1", "encounter-2"),
				rows.stream().map(Row::encounter).toList());
	}

	@Test
	void rejectsMoreThanThreeKeysAndSupportsNoKeyViewState() {
		assertThrows(IllegalArgumentException.class, () -> new TaskSortSpec(List.of(
				new TaskSortSpec.Key("a", true), new TaskSortSpec.Key("b", true),
				new TaskSortSpec.Key("c", true), new TaskSortSpec.Key("d", true)), true));
		assertThrows(IllegalArgumentException.class, () -> new TaskSortSpec(List.of(
				new TaskSortSpec.Key("a", true), new TaskSortSpec.Key("a", false)), true));
		assertEquals(List.of(), TaskSortSpec.outlineDefault().keys());
	}

	@Test
	// Microsoft does not define missing-date placement in the cited sort docs;
	// this verifies the explicit product rule: zero-sentinel dates sort last both ways.
	void missingStartAndFinishDatesStayLastInEitherDirection() {
		NormalTask missing = new NormalTask() {
			@Override public long getStart() { return 0L; }
			@Override public long getEnd() { return 0L; }
		};
		NormalTask dated = new NormalTask() {
			@Override public long getStart() { return 100L; }
			@Override public long getEnd() { return 200L; }
		};
		NodeSorter dateSorter = sorter((left, right) -> Long.compare(((NormalTask) left).getStart(), ((NormalTask) right).getStart()));
		for (boolean ascending : List.of(true, false)) {
			TaskSortSpec spec = new TaskSortSpec(List.of(new TaskSortSpec.Key("Sorter.StartDate", ascending)), true);
			assertEquals(-1, spec.comparator(id -> dateSorter).compare(dated, missing));
		}
		NodeSorter finishSorter = sorter((left, right) -> Long.compare(((NormalTask) left).getEnd(), ((NormalTask) right).getEnd()));
		for (boolean ascending : List.of(true, false)) {
			TaskSortSpec spec = new TaskSortSpec(List.of(new TaskSortSpec.Key("Sorter.FinishDate", ascending)), true);
			assertEquals(-1, spec.comparator(id -> finishSorter).compare(dated, missing));
		}
	}

	@Test
	void emptyTextValuesUseTheirFieldComparatorRatherThanMissingDateRules() {
		NodeSorter nameSorter = sorter((left, right) -> ((Row) left).encounter().compareTo(((Row) right).encounter()));
		Row empty = new Row(0, 0, "");
		Row named = new Row(0, 0, "Alpha");
		assertEquals(-1, new TaskSortSpec(List.of(new TaskSortSpec.Key("Sorter.Name", true)), true)
				.comparator(id -> nameSorter).compare(empty, named));
		assertEquals(1, new TaskSortSpec(List.of(new TaskSortSpec.Key("Sorter.Name", false)), true)
				.comparator(id -> nameSorter).compare(empty, named));
	}

	@Test
	void canonicalCatalogComparatorAppliesDescendingNameAfterEqualStartDates() {
		com.microproject.configuration.Dictionary.getInstance();
		NormalTask earlierName = new NormalTask();
		earlierName.setName("Date sort earlier");
		NormalTask laterName = new NormalTask();
		laterName.setName("Date sort tied earlier");
		TaskSortSpec spec = new TaskSortSpec(List.of(
				new TaskSortSpec.Key("Sorter.StartDate", true),
				new TaskSortSpec.Key("Sorter.Name", false)), true);

		assertEquals(earlierName.getStart(), laterName.getStart(), "fixture must tie on Start Date");
		assertEquals(1, spec.comparator().compare(earlierName, laterName),
			"descending Name must reverse the lexical order after the primary Start Date tie");
	}

	private static NodeSorter sorter(Comparator<Object> comparator) {
		return new NodeSorter() {
			@Override public int compare(Object left, Object right) { return comparator.compare(left, right); }
		};
	}
}
