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
package com.microproject.pm.graphic.spreadsheet.common.transfer;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetAction;

/**
 *
 */
public class NodeListTransfertAction implements CommonSpreadSheetAction {
	protected Action action;
	protected SpreadSheet.SpreadSheetAction spreadSheetAction;
	protected CommonSpreadSheet spreadSheet;
	protected Map map=new HashMap();
	/**
	 * 
	 */
	public NodeListTransfertAction(Action action,SpreadSheet.SpreadSheetAction spreadSheetAction,SpreadSheet spreadSheet) {
		this.action=action;
		this.spreadSheetAction=spreadSheetAction;
		this.spreadSheet=spreadSheet;
	}

	public CommonSpreadSheet getSpreadSheet() {
		return spreadSheet;
	}

	public void setSpreadSheet(CommonSpreadSheet spreadSheet) {
		this.spreadSheet = spreadSheet;
	}

	public SpreadSheet.SpreadSheetAction getSpreadSheetAction() {
		return spreadSheetAction;
	}

	public void setSpreadSheetAction(SpreadSheet.SpreadSheetAction spreadSheetAction) {
		this.spreadSheetAction = spreadSheetAction;
	}

	public boolean isEnabled() {
		return action.isEnabled();
	}

	public void setEnabled(boolean b) {
		action.setEnabled(b);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		action.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		action.removePropertyChangeListener(listener);
	}
	
	
	public Object getValue(String key) {
		return (map.containsKey(key))?map.get(key):action.getValue(key);
	}

	public void putValue(String key, Object value) {
		map.put(key,value);
	}

	public void actionPerformed(ActionEvent e) {
		e.setSource(spreadSheet);
		action.actionPerformed(e);
	}

	public void execute(){
		
	}

}

