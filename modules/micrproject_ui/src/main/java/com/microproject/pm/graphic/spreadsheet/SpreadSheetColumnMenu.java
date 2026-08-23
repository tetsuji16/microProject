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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.microproject.dialog.AutoFilterDialog;
import com.microproject.dialog.ColumnDialog;
import com.microproject.dialog.FieldAliasDialog;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

/**
 * 
 */
public class SpreadSheetColumnMenu extends JPopupMenu {
	private static final long serialVersionUID = -8788124911790572547L;

	private JMenuItem insert = new JMenuItem(Messages.getString("SpreadSheetColumnMenu.InsertColumn")); //$NON-NLS-1$

	private JMenuItem hide = new JMenuItem(Messages.getString("SpreadSheetColumnMenu.HideColumn")); //$NON-NLS-1$
	private JMenuItem rename = new JMenuItem(Messages.getString("RenameDialog.Rename")); //$NON-NLS-1$
	private JMenuItem find = new JMenuItem(Messages.getString("LookupDialog.Find")); //$NON-NLS-1$
	private JMenuItem autoFilter = new JMenuItem(Messages.getString("SpreadSheetColumnMenu.AutoFilter")); //$NON-NLS-1$

	/**
	 * @param col column that was clicked on
	 * 
	 */
	public SpreadSheetColumnMenu(CommonSpreadSheet spreadSheet, final int col) {
		super();
		// setLabel("");
		final CommonSpreadSheet sp = spreadSheet;
		final SpreadSheetFieldArray fields = (SpreadSheetFieldArray) sp.getFieldArray();
		insert.setIcon(IconManager.getIcon("menu.insertColumn")); //$NON-NLS-1$
		insert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Field field = ColumnDialog.getFieldFromDialog(sp,sp.getAvailableFields(),fields);
				if (field != null) {
					int c = col <= 0 ? fields.size() : Math.min(col, fields.size());
					if (sp instanceof SpreadSheet sheet) {
						sheet.insertColumn(c, field);
					} else {
						sp.setFieldArray(fields.insertField(c, field));
					}
				}
			}
		});
		hide.setIcon(IconManager.getIcon("menu.hideColumn")); //$NON-NLS-1$

		hide.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (fields.size() > 2 ) { // there is always the hidden Id field, so only allow delete if more than one other field
					sp.setFieldArray(fields.removeField(col));
				} else {
					Alert.warn(Messages.getString("Message.cantEmptySpreadsheet"),sp); //$NON-NLS-1$
				}
			}
		});

		final Field f = (Field) fields.get(col);
		
		rename.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				if (fields.size() > 2 ) { // there is always the hidden Id field, so only allow delete if more than one other field
					FieldAliasDialog.doRename(f);
					sp.setFieldArray(fields);
				} else {
					Alert.warn(Messages.getString("Message.cantEmptySpreadsheet"),sp); //$NON-NLS-1$
				}
			}
		});		
		
		find.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				GraphicManager.getInstance().doFind(sp,f);
			}
		});		
		
		autoFilter.setIcon(IconManager.getIcon("menu.filter")); //$NON-NLS-1$
		autoFilter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				AutoFilterDialog.open(GraphicManager.getInstance().getFrame(), sp, f);
			}
		});
		
		add(insert);
		add(hide);
		if (f.isCustom())
			add(rename);
		add(find);
		add(autoFilter);
	}
}
