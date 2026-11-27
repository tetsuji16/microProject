/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.microproject.node.Node;
import com.microproject.model.task.Task;

/** Select All covers task rows only; the trailing blank rows are never selected. */
class SpreadSheetVoidRowSelectionTest {
	@Test
	void selectAllOnEmptyProjectSelectsNothing() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture()) {
			assertTrue(fx.rowCount() > 0, "the empty project still shows blank rows");
			AtomicReference<List<Node>> nodes = new AtomicReference<>();
			AtomicInteger rows = new AtomicInteger(-1);
			HeadlessDocumentFixture.onEdt(() -> {
				var sheet = fx.sheet();
				sheet.selectAll();
				nodes.set(sheet.getSelectedNodes());
				rows.set(sheet.getSelectedRowCount());
			});
			assertTrue(nodes.get().isEmpty(), "selected nodes: " + nodes.get().size());
			assertEquals(0, rows.get());
		}
	}

	@Test
	void selectAllSelectsOnlyTaskRows() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First", "Second", "Third")) {
			AtomicReference<List<Node>> nodes = new AtomicReference<>();
			AtomicInteger rows = new AtomicInteger(-1);
			HeadlessDocumentFixture.onEdt(() -> {
				var sheet = fx.sheet();
				sheet.selectAll();
				nodes.set(sheet.getSelectedNodes());
				rows.set(sheet.getSelectedRowCount());
			});
			assertEquals(3, rows.get());
			assertEquals(3, nodes.get().size());
			for (Node node : nodes.get()) {
				assertFalse(node.isVoid());
				assertInstanceOf(Task.class, node.getImpl());
			}
		}
	}
}
