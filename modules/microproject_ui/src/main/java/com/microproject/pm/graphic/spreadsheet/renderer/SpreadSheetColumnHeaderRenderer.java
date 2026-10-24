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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.field.Field;
import com.microproject.util.FlatUiSupport;
/**
 *
 */
public class SpreadSheetColumnHeaderRenderer extends DefaultTableCellRenderer implements OfflineRenderer{
	private static final long serialVersionUID = 8545499263038985045L;
	JLabel override = null;
	/** Narrowest content width that still wraps; below this the heading stays on one line with an ellipsis. */
	private static final int MIN_WRAP_WIDTH = 16;
	/** Wrapped heading lines when the current heading needs more than one line, otherwise null. */
	private List<String> wrappedLines = null;
	/**
	 *
	 */
	public SpreadSheetColumnHeaderRenderer(JLabel override) {
		super();
		this.override = override;
	}

	public SpreadSheetColumnHeaderRenderer() {
		super();
	}
	Component last=null;
	public Component getTableCellRendererComponent (JTable table, Object value,boolean isSelected, boolean hasFocus, int row, int column){
		JLabel component;
		boolean active = false;
		if (override != null)
			component = override;
		else if (table==null){
			setValue(null);
			component=this;
		} else component =(JLabel)super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);


		if (table!=null){
			active = table instanceof CommonSpreadSheet spreadSheet
					? spreadSheet.getActiveHeaderColumn() == column
					: table.getSelectedColumn() == column;
			FlatUiSupport.applyTableHeaderCellStyle(component, isSelected, active);
			Border border = active ? FlatUiSupport.spreadsheetActiveCellBorder() : FlatUiSupport.tableHeaderBorder();
			component.setBorder(border);
		}

		component.setHorizontalAlignment (CENTER);
		wrappedLines = null;
		if (override == null) {
			component.setText (value == null ? "" : value.toString ());
			// Microsoft Project "Header Wrapping" (default on): a heading that does not fit
			// its column wraps onto several lines instead of being cut off.
			if (table != null && column >= 0 && column < table.getColumnModel().getColumnCount()
					&& (!(table.getColumnModel() instanceof SpreadSheetColumnModel columns) || columns.isHeaderWrap(column))) {
				List<String> lines = wrap(component.getText(), component.getFont(),
						table.getColumnModel().getColumn(column).getWidth() - horizontalInsets(component),
						component.getFontMetrics(component.getFont()).getFontRenderContext());
				if (lines.size() > 1)
					wrappedLines = lines;
			}
		}
		return component;
	}

	/** Heading lines (a single entry when the text fits or cannot be wrapped). */
	public List<String> getWrappedLines() {
		return wrappedLines == null ? List.of(getText()) : List.copyOf(wrappedLines);
	}

	/** The single-line content width of a heading, independent of the current column width. */
	public int singleLineWidth(JTable table, Object value, int column) {
		List<String> saved = wrappedLines;
		try {
			Component component = getTableCellRendererComponent(table, value, false, false, -1, -1);
			return component.getPreferredSize().width;
		} finally {
			wrappedLines = saved;
		}
	}

	/** The line height a heading of this column needs, including the border (never below one line). */
	public int preferredHeightFor(JTable table, Object value, int column) {
		Component component = getTableCellRendererComponent(table, value, false, false, -1, column);
		return component.getPreferredSize().height;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension size = super.getPreferredSize();
		if (wrappedLines != null) {
			FontMetrics metrics = getFontMetrics(getFont());
			Insets insets = stableInsets(this);
			size = new Dimension(size.width, Math.max(size.height,
					wrappedLines.size() * metrics.getHeight() + insets.top + insets.bottom));
		}
		return size;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (wrappedLines == null) {
			super.paintComponent(g);
			return;
		}
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setFont(getFont());
			g2.setColor(getForeground());
			FontMetrics metrics = g2.getFontMetrics();
			Insets insets = stableInsets(this);
			int block = wrappedLines.size() * metrics.getHeight();
			int top = insets.top + Math.max(0, (getHeight() - insets.top - insets.bottom - block) / 2);
			int contentWidth = getWidth() - insets.left - insets.right;
			for (int index = 0; index < wrappedLines.size(); index++) {
				String line = wrappedLines.get(index);
				int x = insets.left + Math.max(0, (contentWidth - metrics.stringWidth(line)) / 2);
				g2.drawString(line, x, top + index * metrics.getHeight() + metrics.getAscent());
			}
		} finally {
			g2.dispose();
		}
	}

	/** Insets that do not depend on whether the column is currently the active one. */
	private static Insets stableInsets(JComponent component) {
		Insets normal = FlatUiSupport.tableHeaderBorder().getBorderInsets(component);
		Insets active = FlatUiSupport.spreadsheetActiveCellBorder().getBorderInsets(component);
		return new Insets(Math.max(normal.top, active.top), Math.max(normal.left, active.left),
				Math.max(normal.bottom, active.bottom), Math.max(normal.right, active.right));
	}

	private static int horizontalInsets(JComponent component) {
		Insets insets = stableInsets(component);
		return insets.left + insets.right + 2;
	}

	/**
	 * Splits text into lines no wider than the width, breaking between words (or between
	 * characters for scripts without spaces such as Japanese) and inside a word only when
	 * the word alone is wider than the column.
	 */
	public static List<String> wrap(String text, Font font, int width, FontRenderContext context) {
		if (text == null || text.isEmpty() || width < MIN_WRAP_WIDTH)
			return List.of(text == null ? "" : text);
		if (font.getStringBounds(text, context).getWidth() <= width)
			return List.of(text);
		AttributedString attributed = new AttributedString(text);
		attributed.addAttribute(java.awt.font.TextAttribute.FONT, font);
		LineBreakMeasurer measurer = new LineBreakMeasurer(attributed.getIterator(),
				BreakIterator.getLineInstance(Locale.ROOT), context);
		List<String> lines = new ArrayList<>();
		while (measurer.getPosition() < text.length()) {
			int start = measurer.getPosition();
			TextLayout layout = measurer.nextLayout(width);
			int end = Math.max(measurer.getPosition(), start + 1);
			measurer.setPosition(end);
			String line = text.substring(start, end).stripTrailing();
			if (!line.isEmpty() || lines.isEmpty())
				lines.add(line);
			if (layout == null) break;
		}
		return lines;
	}


	public Component getComponent(Object value, GraphicNode node,Field field,SpreadSheetParams params){
		JComponent component=(JComponent)getTableCellRendererComponent(null, value, false, false, -1, -1);
		component.setBorder(null);
		return component;
	}

}
