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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.microproject.dialog.RenameDialog;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.NamedItem;
import com.microproject.strings.Messages;

/**
 *
 */
public class SpreadSheetColumnsPopupMenu extends JPopupMenu {
    protected final CommonSpreadSheet spreadSheet;
	private String type;
    /**
     * 
     */
    
    private class MenuAction extends JRadioButtonMenuItem implements ActionListener {
    	CommonSpreadSheet spreadSheet;
    	ArrayList fields;
    	private boolean current = false;
    	MenuAction(String text, CommonSpreadSheet spreadSheet, ArrayList fields, boolean selected) {
    		super(text);
    		if (selected)
    			setText("<html><span color=\"blue\"><u><b>" + text + " " + Messages.getString("Text.clickToRename") + "</b></u></span></html>");
    		this.fields = fields;
    		this.spreadSheet = spreadSheet;
    		current = selected;
    		this.setSelected(selected);
    		this.addActionListener(this); // it listens to itself
    	}
		public void actionPerformed(ActionEvent arg0) {
			spreadSheet.finishCurrentOperations();
			if (current) {
				RenameDialog.doRename(spreadSheet,(NamedItem) fields);
			} else {
				spreadSheet.setFieldArray(fields);
			}
		}
    	
    }
    private void setContents() {
		Object columnDefinitions[] = Dictionary.getAll(type);
		for (int i=0; i < columnDefinitions.length; i++) {
			boolean selected = (spreadSheet.getFieldArray() == columnDefinitions[i]);
			add(new MenuAction(columnDefinitions[i].toString(),spreadSheet,(ArrayList) columnDefinitions[i], selected));
		}
    	
    }
    public SpreadSheetColumnsPopupMenu(CommonSpreadSheet spreadSheet, String type) {
        super();
        this.spreadSheet=spreadSheet;
        this.type = type;
		setContents();
    }

}

