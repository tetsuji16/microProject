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
package com.microproject.ui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

class DefaultFormBuilderTest {
	private static final String COLUMNS = "right:pref, 3dlu, fill:80dlu:grow, 3dlu, p";
	private static final String ROWS = "p, 3dlu, p, 3dlu, p, 3dlu, p";

	@Test
	void appendMovesTwoColumnsPerComponentAndNextLineMovesByGridRows() {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(COLUMNS, ROWS));
		assertEquals(1, builder.getColumn());
		assertEquals(1, builder.getRow());
		builder.append("Name:", new JTextField(10));
		assertEquals(5, builder.getColumn());
		builder.nextLine(2);
		assertEquals(1, builder.getColumn());
		assertEquals(3, builder.getRow());
		builder.nextColumn(2);
		assertEquals(3, builder.getColumn());
		builder.nextLine();
		assertEquals(4, builder.getRow());
		assertEquals(5, builder.getColumnCount());
	}

	@Test
	void separatorSpansTheRestOfTheRowAndEndsTheLine() {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(COLUMNS, ROWS));
		JSeparator separator = builder.addSeparator("Section");
		assertEquals(2, builder.getRow());
		assertEquals(1, builder.getColumn());
		JPanel panel = builder.getPanel();
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();
		Component row = separator.getParent();
		assertSame(panel, row.getParent());
		assertEquals(panel.getWidth(), row.getWidth());
	}

	@Test
	void leadingColumnOffsetShiftsTheCurrentColumn() {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(COLUMNS, ROWS));
		builder.setLeadingColumnOffset(2);
		assertEquals(3, builder.getColumn());
		JLabel label = builder.addLabel("Label");
		assertEquals(5, builder.getColumn());
		assertTrue(builder.getPanel().isAncestorOf(label));
	}

	@Test
	void addSpanningPlacesTheComponentAndMovesLikeAdd() {
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(COLUMNS, ROWS));
		builder.addLabel("Label");
		JTextField field = new JTextField(5);
		builder.addSpanning(field, builder.getColumn(), builder.getRow(), 3);
		assertEquals(5, builder.getColumn());
		JPanel panel = builder.getPanel();
		panel.setSize(400, panel.getPreferredSize().height);
		panel.doLayout();
		assertEquals(400, field.getX() + field.getWidth(), "the field spans to the last column");
	}
}
