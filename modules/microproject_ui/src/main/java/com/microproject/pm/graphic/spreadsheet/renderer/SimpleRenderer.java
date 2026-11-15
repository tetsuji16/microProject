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
package com.microproject.pm.graphic.spreadsheet.renderer;

import java.awt.Component;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.TaskFontStyle;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.field.Field;
import com.microproject.field.FieldConverter;
import com.microproject.pm.task.ScheduleDiagnosticsService;
import com.microproject.pm.task.Task;
import com.microproject.preference.GlobalPreferences;

/**
 *
 */
public class SimpleRenderer extends DefaultTableCellRenderer/*ContextSensitiveCellRenderer*/  implements OfflineRenderer{
	static Font boldFont = null;
	protected boolean compact=false;
	/** Task Inspector diagnostics drawn under the date and duration cells; renderers are shared, so reset per cell. */
	public enum Diagnostic { NONE, WARNING, SUGGESTION }
	private Diagnostic diagnostic = Diagnostic.NONE;
	/**
	 * 
	 */
	public SimpleRenderer() {
		super();
	}
	public SimpleRenderer(boolean compact) {
		super();
		this.compact=true;
	}
	
	/**
	 * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		JLabel component;
		diagnostic = Diagnostic.NONE;
		if (table==null){
			setValue(null);
			component=this;
		}
		else{
			component=(JLabel)super.getTableCellRendererComponent(table, value, isSelected,hasFocus, row, column);
			// Plain tables (dialogs) have no cell properties or task rows to style.
			if (table.getModel() instanceof CommonSpreadSheetModel model) {
				FontManager.setComponentFont(model.getCellProperties(model.getNode(row)),component);
				GraphicNode node = model.getNode(row);
				Object impl = node == null || node.getNode() == null ? null : node.getNode().getImpl();
				TaskFontStyle.apply(component, impl, isSelected);
				diagnostic = diagnosticFor(impl, fieldIdInColumn(table, model, column));
			}
		}
				
		
		if (value!=null){
			if (compact) component.setText(FieldConverter.toString(value, value.getClass(), FieldConverter.COMPACT_CONVERTER_CONTEXT));
			else component.setText(FieldConverter.toString(value));
		}
		if (value instanceof String) component.setHorizontalAlignment(SwingConstants.LEFT);
		else component.setHorizontalAlignment(SwingConstants.RIGHT);
		return component;
	}
	

	
	
	/** Diagnostic underline currently set for the last rendered cell. */
	public Diagnostic getDiagnostic() {
		return diagnostic;
	}

	private static String fieldIdInColumn(JTable table, CommonSpreadSheetModel model, int column) {
		try {
			Field field = model.getFieldInColumn(table.convertColumnIndexToModel(column));
			return field == null ? null : field.getId();
		} catch (RuntimeException outOfRange) {
			return null;
		}
	}

	/** Warnings and suggestions are shown under the Start, Finish and Duration cells of the affected task only. */
	public static Diagnostic diagnosticFor(Object impl, String fieldId) {
		if (!(impl instanceof Task task) || fieldId == null) return Diagnostic.NONE;
		if (!"Field.start".equals(fieldId) && !"Field.finish".equals(fieldId) && !"Field.duration".equals(fieldId))
			return Diagnostic.NONE;
		if (GlobalPreferences.isTaskWarningsShown() && ScheduleDiagnosticsService.showsWarning(task))
			return Diagnostic.WARNING;
		if (GlobalPreferences.isTaskSuggestionsShown() && ScheduleDiagnosticsService.showsSuggestion(task))
			return Diagnostic.SUGGESTION;
		return Diagnostic.NONE;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (diagnostic == Diagnostic.NONE || getText() == null || getText().isEmpty()) return;
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			FontMetrics metrics = g2.getFontMetrics(getFont());
			Insets insets = getInsets();
			int textWidth = metrics.stringWidth(getText());
			int available = getWidth() - insets.left - insets.right;
			int x0 = getHorizontalAlignment() == SwingConstants.RIGHT
				? getWidth() - insets.right - Math.min(textWidth, available) : insets.left;
			int x1 = x0 + Math.min(textWidth, available);
			int y = Math.min(getHeight() - 2, (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2 + metrics.getDescent() + 1);
			g2.setColor(diagnostic == Diagnostic.WARNING ? new Color(0xD1, 0x34, 0x38) : new Color(0x10, 0x7C, 0x10));
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int[] xs = new int[Math.max(2, (x1 - x0) / 2 + 2)];
			int[] ys = new int[xs.length];
			for (int i = 0; i < xs.length; i++) {
				xs[i] = Math.min(x1, x0 + i * 2);
				ys[i] = y - (i % 2 == 0 ? 0 : 2);
			}
			g2.drawPolyline(xs, ys, xs.length);
		} finally {
			g2.dispose();
		}
	}

	public Component getComponent(Object value, GraphicNode node,Field field,SpreadSheetParams params){
		Component component=getTableCellRendererComponent(null, value, false, false, -1, -1);
	    //set font here since it's not set in getTableCellRendererComponent
		FontManager.setComponentFont(params.getFieldArray().getCellStyle().getCellFormat(node),component);
		return component;
	}


}
