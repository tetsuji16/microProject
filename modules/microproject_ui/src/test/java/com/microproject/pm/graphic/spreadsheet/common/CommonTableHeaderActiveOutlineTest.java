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
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.DefaultListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.renderer.SpreadSheetColumnHeaderRenderer;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetSelectionModel;
import com.microproject.util.FlatUiSupport;

/**
 * The active column header outline must survive every repaint sequence at fractional UI
 * scales, where neighbouring header cells share a device pixel (#860, #895).
 */
class CommonTableHeaderActiveOutlineTest {
	private static final int ACTIVE_COLUMN = 1;
	private static final int[] WIDTHS = { 243, 44, 60 };
	private static final int HEIGHT = 52;

	@Test
	void activeOutlineStaysClosedAfterEveryRepaintSequenceAtFractionalScales() throws Exception {
		String[] failure = new String[1];
		com.formdev.flatlaf.FlatLightLaf.setup();
		com.microproject.ui.theme.MicroProjectTheme.installLight();
		SwingUtilities.invokeAndWait(() -> {
			HeaderSheet sheet = new HeaderSheet();
			CommonTableHeader header = (CommonTableHeader) sheet.getTableHeader();
			header.setSize(WIDTHS[0] + WIDTHS[1] + WIDTHS[2], HEIGHT);
			for (double scale : new double[] { 1.0, 1.25, 1.5, 1.75 }) {
				Rectangle active = header.getHeaderRect(ACTIVE_COLUMN);
				Rectangle left = header.getHeaderRect(ACTIVE_COLUMN - 1);
				Rectangle right = header.getHeaderRect(ACTIVE_COLUMN + 1);
				String[][] sequences = {
					{ "full" },
					{ "full", "neighbour-right" },
					{ "full", "neighbour-left" },
					{ "full", "neighbour-right", "neighbour-left" },
					{ "full", "active" } };
				for (String[] sequence : sequences) {
					BufferedImage image = new BufferedImage((int) Math.ceil(header.getWidth() * scale) + 4,
						(int) Math.ceil(HEIGHT * scale) + 4, BufferedImage.TYPE_INT_RGB);
					Graphics2D clear = image.createGraphics();
					clear.setColor(Color.WHITE);
					clear.fillRect(0, 0, image.getWidth(), image.getHeight());
					clear.dispose();
					for (String step : sequence) {
						Graphics2D g = image.createGraphics();
						g.scale(scale, scale);
						switch (step) {
							case "neighbour-right" -> g.clip(right);
							case "neighbour-left" -> g.clip(left);
							case "active" -> g.clip(active);
							default -> { }
						}
						header.paint(g);
						g.dispose();
					}
					String open = findOpenEdge(image, active, scale);
					if (open != null && failure[0] == null)
						failure[0] = "scale=" + scale + " sequence=" + String.join(">", sequence) + ": " + open;
				}
			}
		});
		assertNull(failure[0], "active header outline is open");
	}

	/** Returns a description of the first edge without any outline-coloured device pixel, or null. */
	private static String findOpenEdge(BufferedImage image, Rectangle cell, double scale) {
		Color outline = FlatUiSupport.spreadsheetActiveCellBorderColor();
		Color fill = FlatUiSupport.spreadsheetHeaderSelectedBackground();
		int x0 = (int) Math.floor(cell.x * scale);
		int x1 = (int) Math.ceil((cell.x + cell.width) * scale) - 1;
		int y0 = (int) Math.floor(cell.y * scale);
		int y1 = (int) Math.ceil((cell.y + cell.height) * scale) - 1;
		int band = 2;
		for (int y = y0 + 3; y < y1 - 3; y++) {
			if (!hasOutline(image, x0 - 1, y, x0 + band - 1, y, outline, fill))
				return "left edge open at y=" + y + " row=" + hexRow(image, y, x0 - 1, x0 + 3);
			if (!hasOutline(image, x1 - band + 1, y, x1 + 1, y, outline, fill))
				return "right edge open at y=" + y + " row=" + hexRow(image, y, x1 - 3, x1 + 1);
		}
		for (int x = x0 + 3; x < x1 - 3; x++) {
			if (!hasOutline(image, x, y0, x, y0 + band - 1, outline, fill))
				return "top edge open at x=" + x;
			if (!hasOutline(image, x, y1 - band + 1, x, y1, outline, fill))
				return "bottom edge open at x=" + x;
		}
		return null;
	}

	private static boolean hasOutline(BufferedImage image, int xa, int ya, int xb, int yb, Color outline, Color fill) {
		for (int y = ya; y <= yb; y++)
			for (int x = xa; x <= xb; x++) {
				if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight())
					continue;
				Color pixel = new Color(image.getRGB(x, y));
				if (distance(pixel, outline) < distance(pixel, fill))
					return true;
			}
		return false;
	}

	private static String hexRow(BufferedImage image, int y, int xa, int xb) {
		StringBuilder hex = new StringBuilder();
		for (int x = Math.max(0, xa); x <= xb && x < image.getWidth(); x++)
			hex.append(Integer.toHexString(image.getRGB(x, y) & 0xFFFFFF)).append(' ');
		return hex.toString().trim();
	}

	private static int distance(Color a, Color b) {
		int r = a.getRed() - b.getRed();
		int g = a.getGreen() - b.getGreen();
		int bl = a.getBlue() - b.getBlue();
		return r * r + g * g + bl * bl;
	}

	private static final class HeaderSheet extends CommonSpreadSheet {
		private HeaderSheet() {
			for (int i = 0; i < WIDTHS.length; i++) {
				TableColumn column = new TableColumn(i, WIDTHS[i]);
				column.setHeaderValue("Col" + i);
				column.setHeaderRenderer(new SpreadSheetColumnHeaderRenderer());
				getColumnModel().addColumn(column);
			}
			setTableHeader(new CommonTableHeader(getColumnModel()));
			selection = new SpreadSheetSelectionModel(this);
			selection.setRowSelection(new DefaultListSelectionModel());
			selection.setColumnSelection(new DefaultListSelectionModel());
			setSelectionModel(selection.getRowSelection());
			getColumnModel().setSelectionModel(selection.getColumnSelection());
			selectColumnAndAllRows(ACTIVE_COLUMN);
		}

		@Override
		public int getRowCount() {
			return 1;
		}
	}
}
