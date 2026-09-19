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
import java.util.Iterator;

import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.microproject.pm.graphic.spreadsheet.time.TimeSpreadSheet;
import com.microproject.configuration.Dictionary;
import com.microproject.field.Field;

/**
 *
 */
public class TimeSpreadSheetColumnsPopupMenu extends JPopupMenu {
    protected final TimeSpreadSheet spreadSheet;
    /**
     * 
     */
    
    private class MenuAction extends JRadioButtonMenuItem implements ActionListener {
    	TimeSpreadSheet spreadSheet;
    	Field field;
    	MenuAction(String text, TimeSpreadSheet spreadSheet, Field field, boolean selected) {
    		super(text);
    		this.field = field;
    		this.spreadSheet = spreadSheet;
    		this.setSelected(selected);
    		this.addActionListener(this); // it listens to itself
    	}
		public void actionPerformed(ActionEvent arg0) {
			spreadSheet.finishCurrentOperations();
			spreadSheet.selectFieldArray(field);
		}
    	
    }
    
    public TimeSpreadSheetColumnsPopupMenu(TimeSpreadSheet spreadSheet, String type) {
        super();
        this.spreadSheet=spreadSheet;
        
		Object columnDefinitions[] = Dictionary.getAll(type);
		//if (columnDefinitions==null||columnDefinitions.length==0) return;
		ArrayList fieldArray =(ArrayList) columnDefinitions[0];
		for (Iterator i=fieldArray.iterator();i.hasNext();) {
			Field field=(Field)i.next();
			boolean selected = (spreadSheet.getSelectedFieldArray().contains(field));
			add(new MenuAction(field.toString(),spreadSheet,field, selected));
		}
    }

}

