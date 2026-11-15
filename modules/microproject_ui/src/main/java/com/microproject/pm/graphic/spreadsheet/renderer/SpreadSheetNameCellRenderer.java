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

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.OutlineNumbering;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnModel;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.field.Field;
import com.microproject.pm.task.Task;
import com.microproject.pm.resource.Resource;
import com.microproject.ui.privacy.PrivacyDisplayMode;

/**
 *
 */
public class SpreadSheetNameCellRenderer extends DefaultTableCellRenderer  implements OfflineRenderer{
	/**
	 * 
	 */
	public SpreadSheetNameCellRenderer() {
		super();
	}
	public Component getTableCellRendererComponent (JTable table, Object value,boolean isSelected, boolean hasFocus, int row, int column){
		//JComponent textComponent=new JLabel();
		Object display = value;
		if (table != null && table.getModel() instanceof SpreadSheetModel model) {
			var node = model.getNodeForDisplayRow(table.convertRowIndexToModel(row));
			if (node != null && node.getImpl() instanceof Task task && PrivacyDisplayMode.isMasked(task.getProject()))
				display = PrivacyDisplayMode.taskName(task);
			else if (node != null && node.getImpl() instanceof Resource resource)
				display = PrivacyDisplayMode.resourceName(resource);
			if (node != null && table instanceof SpreadSheet sheet && sheet.isOutlineNumbersVisible()
					&& node.getImpl() instanceof Task && display != null)
				display = OutlineNumbering.prefix(node, display.toString());
		}
		NameCellComponent component = NameCellComponent.getComponent(table,display,isSelected,hasFocus,row,column/*,textComponent*/);
		applyAlignmentOverride(component, table, column);
		return component;
	}
	/** Column alignment chosen on the ribbon; the name keeps its left alignment unless one is set. */
	private static void applyAlignmentOverride(NameCellComponent component, JTable table, int column) {
		if (table == null || !(table.getColumnModel() instanceof SpreadSheetColumnModel columns)
				|| !(table.getModel() instanceof CommonSpreadSheetModel model)
				|| !(component.getTextComponent() instanceof javax.swing.JLabel label))
			return;
		String fieldId;
		try {
			var field = model.getFieldInColumn(table.convertColumnIndexToModel(column));
			fieldId = field == null ? null : field.getId();
		} catch (RuntimeException outOfRange) {
			return;
		}
		int alignment = fieldId == null ? SpreadSheetFieldArray.ALIGN_DEFAULT : columns.getAlignment(fieldId);
		if (alignment == SpreadSheetFieldArray.ALIGN_LEFT) label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		else if (alignment == SpreadSheetFieldArray.ALIGN_CENTER) label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		else if (alignment == SpreadSheetFieldArray.ALIGN_RIGHT) label.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		else label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
	}
	public Component getComponent(Object value, GraphicNode node,Field field,SpreadSheetParams params){
		//JComponent textComponent=new JLabel();
		Object display = value;
		if (node != null && node.getNode() != null && node.getNode().getImpl() instanceof Task task
				&& PrivacyDisplayMode.isMasked(task.getProject())) {
			display = PrivacyDisplayMode.taskName(task);
		}
		if (node != null && node.getNode() != null && node.getNode().getImpl() instanceof Resource resource)
			display = PrivacyDisplayMode.resourceName(resource);
		return NameCellComponent.getComponent(display, node, params/*, textComponent*/);
	}
}
