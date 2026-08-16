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
package com.microproject.pm.graphic.spreadsheet.selection;

import java.io.Serializable;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
/**
 *
 */
public class SpreadSheetSelectionModel implements Serializable {
	private static final long serialVersionUID = -5993338419885335434L;
	protected ListSelectionModel rowSelection;
	protected ListSelectionModel columnSelection;
	protected transient JTable table;

	public SpreadSheetSelectionModel(JTable table){
		this.table=table;
	}
	//private void writeObject(ObjectOutputStream s) throws IOException {

	/**
	 * @param rowSelection
	 * @param columsSelection
	 */
	public SpreadSheetSelectionModel(JTable table,ListSelectionModel rowSelection,ListSelectionModel columnSelection) {
		this.table=table;
		this.rowSelection = rowSelection;
		this.columnSelection = columnSelection;
	}
	/**
	 * @return Returns the columsSelection.
	 */
	public ListSelectionModel getColumnSelection() {
		return columnSelection;
	}

	/**
	 * @return Returns the rowSelection.
	 */
	public ListSelectionModel getRowSelection() {
		return rowSelection;
	}

	/**
	 * @return Returns the rowSelection.
	 */
	public ListSelectionModel getSelection(boolean row) {
		return (row)?rowSelection:columnSelection;
	}

	
	/**
	 * @param columsSelection The columsSelection to set.
	 */
	public void setColumnSelection(ListSelectionModel columnSelection) {
		this.columnSelection = columnSelection;
	}

	/**
	 * @param rowSelection The rowSelection to set.
	 */
	public void setRowSelection(ListSelectionModel rowSelection) {
		this.rowSelection = rowSelection;
	}

	/**
	 * @return Returns the table.
	 */
	public JTable getTable() {
		return table;
	}

}

