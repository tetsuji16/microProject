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

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import com.microproject.field.BooleanText;
import com.microproject.field.Field;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;

/**
 * Renders a Yes/No field as Microsoft Project does: the text Yes or No, or for
 * the Task Mode field an icon and Manually Scheduled / Auto Scheduled (issue
 * #899). A null value (a row without a task) draws no text and no icon.
 */
public class YesNoRenderer extends SimpleRenderer {
	private static final int ICON_SIZE = 16;
	private final String fieldId;

	/** For a column without a known field: the generic Yes/No labels. */
	public YesNoRenderer() {
		this(null);
	}

	public YesNoRenderer(String fieldId) {
		this.fieldId = fieldId;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return render(fieldId, table, value, isSelected, hasFocus, row, column);
	}

	private Component render(String id, JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
			int column) {
		Boolean bool = toBoolean(id, value);
		String text = BooleanText.label(id, bool);
		// A non-null String keeps SimpleRenderer from replacing the label.
		JLabel label = (JLabel) super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
		label.setText(text);
		label.setIcon(bool == null || !BooleanText.isTaskMode(id) ? null : icon(bool));
		label.setHorizontalAlignment(BooleanText.isTaskMode(id) ? SwingConstants.LEFT : SwingConstants.CENTER);
		return label;
	}

	@Override
	public Component getComponent(Object value, GraphicNode node, Field field, SpreadSheetParams params) {
		Component component = render(field == null ? fieldId : field.getId(), null, value, false, false, -1, -1);
		FontManager.setComponentFont(params.getFieldArray().getCellStyle().getCellFormat(node), component);
		return component;
	}

	/**
	 * Importers and a few legacy field adapters can hand the renderer the
	 * serialized string form. A paint callback must never throw on that
	 * representation: unknown text keeps the safe No default.
	 */
	static Boolean toBoolean(String id, Object value) {
		if (value instanceof Boolean bool)
			return bool;
		if (value instanceof String text && !text.isBlank()) {
			Boolean parsed = BooleanText.parse(id, text);
			return parsed == null ? Boolean.FALSE : parsed;
		}
		return null;
	}

	/** The Task Mode icon: manual (pin) or automatic; null when unavailable. */
	public static Icon icon(boolean manuallyScheduled) {
		String name = manuallyScheduled ? "ribbon.taskModeManual" : "ribbon.taskModeAutomatic";
		Icon icon = IconManager.getRibbonIcon(name, ICON_SIZE, ICON_SIZE);
		return icon != null ? icon : IconManager.getIcon(name);
	}
}
