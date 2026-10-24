/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.graphic.spreadsheet.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

import com.microproject.util.FlatUiSupport;

/** Microsoft Project "Header Wrapping": long headings wrap by default and grow the header row. */
class SpreadSheetColumnHeaderWrapTest {
	private static JTable table(String heading, int width) {
		JTable table = new JTable(new DefaultTableModel(new Object[][] { { "x" } }, new Object[] { heading }));
		FlatUiSupport.applySpreadsheetTableStyle(table);
		table.getColumnModel().getColumn(0).setWidth(width);
		table.getColumnModel().getColumn(0).setPreferredWidth(width);
		return table;
	}

	private static void assertWrapsFully(String heading, int width) {
		JTable table = table(heading, width);
		SpreadSheetColumnHeaderRenderer renderer = new SpreadSheetColumnHeaderRenderer();
		JLabel single = (JLabel) new SpreadSheetColumnHeaderRenderer()
				.getTableCellRendererComponent(table(heading, 2000), heading, false, false, -1, 0);
		int singleHeight = single.getPreferredSize().height;

		JLabel wrapped = (JLabel) renderer.getTableCellRendererComponent(table, heading, false, false, -1, 0);
		List<String> lines = renderer.getWrappedLines();
		assertTrue(lines.size() > 1, "expected several lines for " + heading + " but got " + lines);
		assertEquals(heading.replace(" ", ""), String.join("", lines).replace(" ", ""), "all text must be kept");
		assertFalse(String.join("", lines).contains("…"), "no ellipsis when wrapping");
		Dimension size = wrapped.getPreferredSize();
		assertTrue(size.height > singleHeight, "preferred height " + size.height + " must exceed " + singleHeight);
	}

	@Test
	void japaneseAndLatinHeadingsWrapOntoSeveralLinesWithTheFullText() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			assertWrapsFully("タスクモードのスケジュール", 60);
			assertWrapsFully("Resource Names Total", 70);
			assertWrapsFully("Supercalifragilisticexpialidocious", 60);
		});
	}

	@Test
	void latinHeadingsPreferBreakingAtSpaces() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JTable table = table("Resource Names", 90);
			SpreadSheetColumnHeaderRenderer renderer = new SpreadSheetColumnHeaderRenderer();
			renderer.getTableCellRendererComponent(table, "Resource Names", false, false, -1, 0);
			assertEquals(List.of("Resource", "Names"), renderer.getWrappedLines());
		});
	}

	@Test
	void headingThatFitsStaysOnOneLine() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheetColumnHeaderRenderer renderer = new SpreadSheetColumnHeaderRenderer();
			renderer.getTableCellRendererComponent(table("Name", 400), "Name", false, false, -1, 0);
			assertEquals(List.of("Name"), renderer.getWrappedLines());
		});
	}

	@Test
	void activeColumnKeepsTheClosedOutlineBorderOnTheWrappedComponent() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JTable table = table("Resource Names Total", 70);
			table.getColumnModel().getSelectionModel().setSelectionInterval(0, 0);
			SpreadSheetColumnHeaderRenderer renderer = new SpreadSheetColumnHeaderRenderer();
			JLabel component = (JLabel) renderer.getTableCellRendererComponent(table, "Resource Names Total", true, false, -1, 0);
			assertTrue(renderer.getWrappedLines().size() > 1);
			LineBorder border = assertInstanceOf(LineBorder.class, component.getBorder());
			assertEquals(FlatUiSupport.spreadsheetActiveCellBorderColor(), border.getLineColor());
		});
	}
}
