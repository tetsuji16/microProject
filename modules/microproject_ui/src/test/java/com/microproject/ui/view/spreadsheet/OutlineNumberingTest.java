/*
 * Copyright (c) 2026 microProject
 * Licensed under the MIT License.
 */
package com.microproject.ui.view.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.tree.DefaultMutableTreeNode;

import org.junit.jupiter.api.Test;

class OutlineNumberingTest {
	@Test
	void numbersFollowSiblingPositionsAndNesting() {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
		DefaultMutableTreeNode first = new DefaultMutableTreeNode("a");
		DefaultMutableTreeNode second = new DefaultMutableTreeNode("b");
		DefaultMutableTreeNode child = new DefaultMutableTreeNode("c");
		DefaultMutableTreeNode grandchild = new DefaultMutableTreeNode("d");
		root.add(first);
		root.add(second);
		second.add(new DefaultMutableTreeNode("x"));
		second.add(child);
		child.add(grandchild);

		assertEquals("", OutlineNumbering.of(root));
		assertEquals("1", OutlineNumbering.of(first));
		assertEquals("2.2", OutlineNumbering.of(child));
		assertEquals("2.2.1", OutlineNumbering.of(grandchild));
		assertEquals("2.2 Task", OutlineNumbering.prefix(child, "Task"));
		assertEquals("Task", OutlineNumbering.prefix(root, "Task"));
		assertEquals("", OutlineNumbering.of(null));
	}

	@Test
	void numbersChangeWhenRowsMove() {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
		DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
		DefaultMutableTreeNode b = new DefaultMutableTreeNode("b");
		root.add(a);
		root.add(b);
		root.insert(new DefaultMutableTreeNode("new"), 0);
		assertEquals("3", OutlineNumbering.of(b));
	}
}
