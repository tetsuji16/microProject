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
package com.microproject.pm.graphic.spreadsheet.selection.event;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetListSelectionModel;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetSelectionModel;
import com.microproject.grouping.core.Node;

/**
 *
 */
public class SpreadSheetNodeSelectionListener implements ListSelectionListener {
	public SpreadSheetNodeSelectionListener(){
	}
	public void valueChanged(ListSelectionEvent lse){
		if (lse.getValueIsAdjusting()) return; //it's not a final event
		SpreadSheetListSelectionModel listSelectionModel = (SpreadSheetListSelectionModel)lse.getSource();
		SpreadSheetSelectionModel selectionModel=listSelectionModel.getSelectionModel();
		CommonSpreadSheet spreadSheet=(CommonSpreadSheet)selectionModel.getTable();
		CommonSpreadSheetModel model=(CommonSpreadSheetModel)spreadSheet.getModel();
		
		int[] rows=spreadSheet.getSelectedRows();
		List nodes=new ArrayList(rows.length);
		Node currentNode = null;
		int selectedRow = spreadSheet.getSelectedRow();
		for (int i=0;i<rows.length;i++){
		    if (rows[i] < 0 || rows[i] >= model.getRowCount()) {
		    	continue;
		    }
		    var graphicNode = model.getNode(rows[i]);
		    if (graphicNode == null || graphicNode.getNode() == null) {
		    	continue;
		    }
		    nodes.add(graphicNode.getNode());
		    if (selectedRow == rows[i])		// also set current row
		    	currentNode = graphicNode.getNode();
		    //filter void nodes?
		}

		if (nodes.size()>0) spreadSheet.fireContentsChanged(spreadSheet,nodes,currentNode);
	}
	
}

