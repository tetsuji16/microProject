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
package com.microproject.pm.graphic.spreadsheet;

import javax.swing.tree.TreeNode;

/**
 * Microsoft Project style outline numbers ("1", "1.2", "1.2.3"): the 1-based position of a row among
 * its siblings, joined with the positions of its ancestors. The number follows the outline, so it
 * changes when tasks are moved, indented or inserted.
 */
public final class OutlineNumbering {
	private OutlineNumbering() {
	}

	/** Outline number of the node, or an empty string for the (invisible) root or a detached node. */
	public static String of(TreeNode node) {
		if (node == null)
			return "";
		StringBuilder number = new StringBuilder();
		TreeNode current = node;
		while (current.getParent() != null) {
			int position = current.getParent().getIndex(current);
			if (position < 0)
				return "";
			if (number.length() > 0)
				number.insert(0, '.');
			number.insert(0, position + 1);
			current = current.getParent();
		}
		return number.toString();
	}

	/** The cell text with the outline number in front of it; unchanged when there is no number. */
	public static String prefix(TreeNode node, String text) {
		String number = of(node);
		return number.isEmpty() ? text : number + " " + text;
	}
}
