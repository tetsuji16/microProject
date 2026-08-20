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
package com.microproject.pm.graphic.spreadsheet.common;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ListSelectionModel;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetColumnsPopupMenu;
import com.microproject.pm.graphic.spreadsheet.selection.TimeSpreadSheetColumnsPopupMenu;
import com.microproject.pm.graphic.spreadsheet.time.TimeSpreadSheet;
import com.microproject.configuration.Dictionary;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;

/**
 *
 */
public class SpreadSheetCorner extends GradientCorner implements ListSelectionListener {
	protected CommonSpreadSheet spreadSheet;
	/**
	 *
	 */
	public SpreadSheetCorner(CommonSpreadSheet spreadSheet) {
		super();
//		this.setOpaque(true);
		if (spreadSheet.isCanSelectFieldArray())
			setToolTipText("dummy"); // needed so getToolTipText will be called
		this.spreadSheet=spreadSheet;
		if (spreadSheet instanceof SpreadSheet) {
			spreadSheet.getRowHeader().getSelectionModel().addListSelectionListener(this);
			spreadSheet.getColumnModel().getSelectionModel().addListSelectionListener(this);
		}

//
//		setBackground(LafUtils.getUnselectedBackgroundColor());
		setBorder(FlatUiSupport.tableHeaderBorder());
		addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				CommonSpreadSheet spreadSheet=SpreadSheetCorner.this.spreadSheet;

				if  (SwingUtilities.isRightMouseButton(e)) {
					if (spreadSheet instanceof CommonSpreadSheet && spreadSheet.getSpreadSheetCategory() != null){
						CommonSpreadSheet sp=(CommonSpreadSheet)spreadSheet;
						if (sp.isCanSelectFieldArray()) {
							//need to build menu each time because it can change
							if (spreadSheet instanceof TimeSpreadSheet){
								TimeSpreadSheetColumnsPopupMenu columnsPopup = new TimeSpreadSheetColumnsPopupMenu((TimeSpreadSheet)sp,sp.getSpreadSheetCategory());
								columnsPopup.show(sp,e.getX(),e.getY());
							}else{
								SpreadSheetColumnsPopupMenu columnsPopup = new SpreadSheetColumnsPopupMenu(sp,sp.getSpreadSheetCategory());
								columnsPopup.show(sp,e.getX(),e.getY());
							}
						}
					}
				}else{
					spreadSheet.selectEntireSpreadsheet();
					if (Environment.isMac()) setSelected(true);
				}
			}
		});
	}


	public String getToolTipText(MouseEvent e) {
		return java.text.MessageFormat.format("<html>{0}: {1}<br>{2}<html>",
				Dictionary.getCategoryText(spreadSheet.getSpreadSheetCategory()), spreadSheet.getFieldArray(),
				Messages.getString("Text.rightClickSelectToSpreadsheet"));
	}


	public void valueChanged(ListSelectionEvent e) {
		boolean allRowsSelected = spreadSheet.getRowCount() > 0
			&& spreadSheet.getSelectedRowCount() == spreadSheet.getRowCount();
		boolean allColumnsSelected = spreadSheet.getColumnCount() > 0
			&& spreadSheet.getSelectedColumnCount() == spreadSheet.getColumnCount();
		boolean fullySelected = allRowsSelected && allColumnsSelected;
		if (Environment.isMac()) {
			if (selected != fullySelected)
				setSelected(fullySelected);
		} else {
			setSelected(fullySelected);
		}
	}


}
