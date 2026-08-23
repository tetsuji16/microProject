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
package com.microproject.pm.graphic.spreadsheet;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import com.microproject.pm.graphic.spreadsheet.editor.MoneyEditor;
import com.microproject.pm.graphic.spreadsheet.editor.PercentEditor;
import com.microproject.pm.graphic.spreadsheet.editor.RateEditor;
import com.microproject.pm.graphic.spreadsheet.editor.SimpleComboBoxEditor;
import com.microproject.pm.graphic.spreadsheet.editor.SimpleEditor;
import com.microproject.pm.graphic.spreadsheet.editor.SpinEditor;
import com.microproject.pm.graphic.spreadsheet.editor.SpreadSheetCellEditorAdapter;
import com.microproject.pm.graphic.spreadsheet.editor.SpreadSheetNameCellEditor;
import com.microproject.pm.graphic.spreadsheet.renderer.DateRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.IndicatorsRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.LookupRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.OfflineCapableBooleanRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.PercentRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.RateRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.SimpleRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.SpreadSheetCellRendererAdapter;
import com.microproject.pm.graphic.spreadsheet.renderer.SpreadSheetColumnHeaderRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.SpreadSheetNameCellRenderer;
import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;

/**
 *
 */
public class SpreadSheetColumnModel extends DefaultTableColumnModel {
	int columnIndex = 0;

	int colWidth = 0;

	private ArrayList<Field> fieldArray; //changes when columns are moved - needed to update the current definition
	private ArrayList<Field> originalFieldArray; // will not change
	private Map<String,Integer> colWidthMap;
	private final Set<String> configuredWidthFields = new HashSet<>();
	private final Set<String> manuallyAdjustedWidthFields = new HashSet<>();

	boolean svg;
	/**
	 * @param fieldArray
	 *            the initial spreadsheet fields
	 *
	 */
	public SpreadSheetColumnModel(final ArrayList<Field> fieldArray) {
		this(fieldArray,null);

	}
	public SpreadSheetColumnModel(ArrayList<Field> fieldArray,List<Integer> colWidthList) {
		super();
		setFieldArray(fieldArray);
		colWidthMap=new HashMap<String, Integer>();
		if (fieldArray instanceof SpreadSheetFieldArray){
			SpreadSheetFieldArray sa=(SpreadSheetFieldArray)fieldArray;
			if (colWidthList==null&&sa!=null&&sa.getWidths()!=null&&sa.getWidths().size()>0){
				colWidthList=sa.getWidths();
			}
			if (colWidthList==null) return;
			Iterator<Field> a = sa.iterator();
			Iterator<Integer> s=colWidthList.iterator();
			int column = 0;
			while (a.hasNext()&&s.hasNext()){
				String f=a.next().getId();
				int size=s.next();
				if (!colWidthMap.containsKey(f)) {
					colWidthMap.put(f, size);
					if (size > 0 && sa.isManualWidth(column)) configuredWidthFields.add(f);
				}
				column++;
			}
		}
	}

	/**
	 * Sets the initial width of fields without a saved user width to the widest
	 * rendered header or cell value.  This is intentionally a one-time layout
	 * operation; editing a cell must not unexpectedly move the other columns.
	 */
	public void autoSizeColumnsToContent(JTable table) {
		int totalWidth = 0;
		for (int viewColumn = 0; viewColumn < getColumnCount(); viewColumn++) {
			TableColumn column = getColumn(viewColumn);
			Field field = (Field) column.getIdentifier();
			if (field == null || configuredWidthFields.contains(field.getId())) {
				totalWidth += column.getPreferredWidth();
				continue;
			}

			int width = preferredWidth(column.getHeaderRenderer()
					.getTableCellRendererComponent(table, field.getName(), false, false, -1, viewColumn));
			for (int row = 0; row < table.getRowCount(); row++) {
				Component component = table.prepareRenderer(table.getCellRenderer(row, viewColumn), row, viewColumn);
				width = Math.max(width, component.getPreferredSize().width);
			}

			column.setPreferredWidth(Math.max(1, width));
			colWidthMap.put(field.getId(), column.getPreferredWidth());
			totalWidth += column.getPreferredWidth();
		}
		colWidth = totalWidth;
	}

	private static int preferredWidth(Component component) {
		return component == null || component.getPreferredSize() == null
				? 0 : component.getPreferredSize().width;
	}

	public void addColumn(TableColumn tc) {
		tc.setHeaderRenderer(new SpreadSheetColumnHeaderRenderer());

		if (columnIndex == 0) {
			Field field = (Field) originalFieldArray.get(columnIndex);
			tc.setIdentifier(field); // store the field with the column
			// tc.setIdentifier(null); // store the field with the column
			tc.setPreferredWidth(0);

			colWidth = 0;
			// nothing
		} else {
			super.addColumn(tc);
			Field field = (Field) originalFieldArray.get(columnIndex);
			tc.setIdentifier(field); // store the field with the column
//			System.out.println("setting column " + columnIndex + " to field " + field + " ok = " + (field == getFieldInColumn(columnIndex)));

			if (field.isNameField()) {
				tc.setPreferredWidth((svg)?170:150);
				tc.setCellRenderer(new SpreadSheetNameCellRenderer());
				tc.setCellEditor(new SpreadSheetNameCellEditor(new SimpleEditor(String.class)));
			} else if (field == Configuration.getFieldFromId("Field.indicators")) {
				tc.setPreferredWidth(50);
				tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new IndicatorsRenderer()));
				tc.setHeaderRenderer(new SpreadSheetColumnHeaderRenderer(IndicatorsRenderer.getCellHeader()));
			} else if (field.getLookupTypes() != null) {
				tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new LookupRenderer()));
			} else {
				tc.setPreferredWidth(150);
				if (field.hasOptions()) {
					tc.setPreferredWidth(150);
					tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new SimpleRenderer()));
					// note that in Spreadsheet, there getCellEditor() is
					// overridden and dynamic combos are filled there
					tc.setCellEditor(new SpreadSheetCellEditorAdapter(new SimpleComboBoxEditor(new DefaultComboBoxModel<>(field.getOptions(null)))));
				} else if (field.getRange() != null) {
					if (field.isPercent()) {
						tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new PercentRenderer()));
						tc.setCellEditor(new SpreadSheetCellEditorAdapter(new PercentEditor()));
					} else {
						tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new SimpleRenderer()));
						tc.setCellEditor(new SpreadSheetCellEditorAdapter(new SpinEditor(field)));
					}
				} else if (field.isRate()) {
					tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new RateRenderer()));
					tc.setCellEditor(new SpreadSheetCellEditorAdapter(new RateEditor(null, field.isMoney(),field.isPercent(),true)));
				} else if (field.isMoney()) {
					tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new SimpleRenderer()));
					tc.setCellEditor(new SpreadSheetCellEditorAdapter(new MoneyEditor()));
				} else if (field.isPercent()) {
					tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new PercentRenderer()));
					tc.setCellEditor(new SpreadSheetCellEditorAdapter(new PercentEditor()));
				} else if (field.isDate()) {
					tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new DateRenderer()));
				} else if (field.isBoolean()){
					tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new OfflineCapableBooleanRenderer()));
				} else {
					//SimpleRenderer in other cases, LC 8/2006
					tc.setCellRenderer(new SpreadSheetCellRendererAdapter(new SimpleRenderer()));
					tc.setPreferredWidth(field.getColumnWidth(svg));
				}
			}
			Integer size=colWidthMap.get(field.getId());
			if (size==null||size<=0) colWidthMap.put(field.getId(),tc.getPreferredWidth());
			else tc.setPreferredWidth(size);
			colWidth += tc.getPreferredWidth();
			tc.addPropertyChangeListener(event -> {
				if ("width".equals(event.getPropertyName()) && event.getNewValue() instanceof Number number
						&& number.intValue() > 0) {
					manuallyAdjustedWidthFields.add(field.getId());
				}
			});
		}
		columnIndex++;
	}

	public void removeColumn(TableColumn column) {
		columnIndex--;
		super.removeColumn(column);
		if (columnIndex == 1)
			columnIndex = 0;
	}

	/***************************************************************************
	 * @see javax.swing.table.TableColumnModel#moveColumn(int, int)
	 */
	public void moveColumn(int columnIndex, int newIndex) {
		if (newIndex != -1)
			super.moveColumn(columnIndex, newIndex);

		if (columnIndex == newIndex)
			return;
		SpreadSheetFieldArray f = (SpreadSheetFieldArray) getFieldArray();
		fieldArray = f.move(columnIndex+1, newIndex+1);
	}

	public ArrayList<Field> getFieldArray() {
		return fieldArray;
	}

	public void setFieldArray(ArrayList<Field> fieldArray) {
		this.fieldArray = fieldArray;
		originalFieldArray = new ArrayList<>(fieldArray);
	}

	public int getColWidth() {
		return colWidth;
	}

	public boolean isWidthManuallyAdjusted(String fieldId) {
		return manuallyAdjustedWidthFields.contains(fieldId);
	}

	public boolean isSvg() {
		return svg;
	}

	public void setSvg(boolean svg) {
		this.svg = svg;
	}

/**
 * Normally, JTable automatically translates columns to take care of any columns that may have been moved
 * However, sometimes, such as when a column is determines from a mouse event, the column is not translated.
 * @param col
 * @return
 */
	public Field getFieldInNonTranslatedColumn(int col) {
		return (Field)fieldArray.get(col);
	}

	public Field getFieldInColumn(int col) {
//		return (Field)fieldArray.get(col);
		return (Field) originalFieldArray.get(col);

//		if (col >= getColumnCount()) // on initializing
//			return (Field) fieldArray.get(col);
//		if (col == 0) // the 0th column isn't displayed and isn't in the table, but calls are made to it
//			return (Field) fieldArray.get(col);
//		return (Field) getColumn(col - 1).getIdentifier();
	}

	/** Resolves a field using a JTable view column rather than a model column. */
	public Field getFieldInViewColumn(int viewColumn) {
		if (viewColumn < 0 || viewColumn >= getColumnCount())
			return null;
		int modelColumn = getColumn(viewColumn).getModelIndex();
		if (modelColumn < 0 || modelColumn >= originalFieldArray.size())
			return null;
		return originalFieldArray.get(modelColumn);
	}

	public int getModelColumnForViewColumn(int viewColumn) {
		if (viewColumn < 0 || viewColumn >= getColumnCount())
			return -1;
		return getColumn(viewColumn).getModelIndex();
	}

	public int findFieldColumn(Field field) {
		return originalFieldArray.indexOf(field);
//		Enumeration i = getColumns();
//		int count = 0;
//		while (i.hasMoreElements()) {
//			count++;
//			TableColumn col = (TableColumn) i.nextElement();
//			if (col.getIdentifier() == field)
//				return count;
//		}
//		if (field == fieldArray.get(0)) // in case hidden 0th column
//			return 0;
//		return -1;
	}

	public int getFieldColumnCount() {
		return getFieldArray().size();
	}

//	@Override
//	protected void fireColumnSelectionChanged(ListSelectionEvent lse) {
//		System.out.println("Model: "+((lse.getValueIsAdjusting())?"lse=":"LSE=")+lse.getFirstIndex()+", "+lse.getLastIndex());
//		super.fireColumnSelectionChanged(lse);
//	}




}
