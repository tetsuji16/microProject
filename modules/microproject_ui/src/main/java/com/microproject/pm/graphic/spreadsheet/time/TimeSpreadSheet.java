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
package com.microproject.pm.graphic.spreadsheet.time;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetListSelectionModel;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetSelectionModel;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.timescale.ScaledComponent;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.ActionList;
import com.microproject.graphic.configuration.CellStyle;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.shape.Colors;
import com.microproject.pm.task.Project;
import com.microproject.timescale.TimeScaleListener;

/**
 *
 */
public class TimeSpreadSheet extends CommonSpreadSheet implements ScaledComponent{
	protected Project project;
	protected ArrayList fieldArray;
	public TimeSpreadSheet(Project project) {
		super();
		setTableHeader(null);
		this.project=project;
	}
	
	public void setCache(NodeModelCache cache, ArrayList fieldArray, CellStyle cellStyle, ActionList actionList){
		var model = new TimeSpreadSheetModel(cache, fieldArray, cellStyle, actionList);
		setModel(model,
				new TimeSpreadSheetColumnModel(this));
	}
	
	public void setFieldArray(ArrayList fieldArray){
		var model = (TimeSpreadSheetModel) getModel();
		model.setFieldArray(fieldArray);
		model.resetSelectedFieldArray();

	
	}
	
	
	public void setSelectedFieldArray(ArrayList fieldArray){
		((TimeSpreadSheetModel)getModel()).setSelectedFieldArray(fieldArray);
	}
	public ArrayList getSelectedFieldArray() {
		return ((TimeSpreadSheetModel)getModel()).getSelectedFieldArray();
	}
	public void selectFieldArray(Field field){
		((TimeSpreadSheetModel)getModel()).selectFieldArray(field);
	}

	
	
	public void setModel(CommonSpreadSheetModel spreadSheetModel,
			DefaultTableColumnModel spreadSheetColumnModel) {
		var oldModel = getModel();
	    setModel(spreadSheetModel);
	    setColumnModel(spreadSheetColumnModel);
	    
	    selection = new SpreadSheetSelectionModel(this);
		selection.setRowSelection(new SpreadSheetListSelectionModel(selection,
				true));
		selection.setColumnSelection(new SpreadSheetListSelectionModel(
				selection, false));
		setSelectionModel(selection.getRowSelection());
		//createDefaultColumnsFromModel(); done outside
		getColumnModel().setSelectionModel(selection.getColumnSelection());
		
	    registerEditors(true);
	    initRowHeader(spreadSheetModel);
	    initModel();
	    initListeners();
	    if (oldModel instanceof CommonSpreadSheetModel commonModel && oldModel != spreadSheetModel) {
	    	commonModel.getCache().removeNodeModelListener(this);
	    }
	    spreadSheetModel.getCache().addNodeModelListener(this);
	}
	public void cleanUp() {
		if (getModel() instanceof TimeSpreadSheetModel timeSpreadSheetModel && timeSpreadSheetModel.getCache() != null) {
			timeSpreadSheetModel.getCache().removeNodeModelListener(this);
		}
		var coord = getCoord();
		if (coord != null && getColumnModel() instanceof TimeScaleListener) {
     		coord.removeTimeScaleListener((TimeScaleListener) getColumnModel());
		}
		super.cleanUp();
	}

	protected void initRowHeader(CommonSpreadSheetModel spreadSheetModel){
		rowHeader.setModel(spreadSheetModel,new TimeSpreadSheetRowHeaderColumnModel());
		rowHeader.createDefaultColumnsFromModel();

		GraphicConfiguration config=GraphicConfiguration.getInstance();
		rowHeader.setRowHeight(config.getRowHeight());

	}
	
	public Project getProject() {
		return project;
	}
	
	public CoordinatesConverter getCoord() {
    	var model = (TimeSpreadSheetModel) getModel();
        return model.getCoord();
    }
	public void setCoord(CoordinatesConverter coord) {
     	var model = (TimeSpreadSheetModel) getModel();
        model.setCoord(coord);
     	var columnModel = (TimeSpreadSheetColumnModel) getColumnModel();
     	columnModel.setCoord(coord);
    }
     
	// Time-phased cells use the canonical SimpleRenderer registered by
	// CommonTable; there is no separate time-only renderer path.
    
     

     public void createDefaultColumnsFromModel() {
     	var columnModel = getColumnModel();
        if (columnModel instanceof TimeSpreadSheetColumnModel timeSpreadSheetColumnModel){
        	timeSpreadSheetColumnModel.updateColumns();
        }
    }
     
     public Class getRowClass(int row) {
        return ((TimeSpreadSheetModel)getModel()).getRowClass(row);
     }
     
     public TableCellRenderer getCellRenderer(int row, int column) {
     	if (getModel() instanceof TimeSpreadSheetModel) {
            return getDefaultRenderer(getRowClass(row));
     	}
     	return super.getCellRenderer(row, column);
     }
     
     public TableCellEditor getCellEditor(int row, int column) {
     	if (getModel() instanceof TimeSpreadSheetModel) {
            return getDefaultEditor(getRowClass(row));
     	}
     	return super.getCellEditor(row, column);
     }
    
    

     public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent){
    	 super.graphicNodesCompositeEvent(compositeEvent);
    	 setPreferredSize(new Dimension(getPreferredSize().width,getRowHeight()*getRowCount()));
     }	
     
     
 	public Component prepareRenderer(TableCellRenderer renderer, int row,
			int column) {
		Component component =  super.prepareRenderer(renderer, row, column);
		if (!getModel().isCellEditable(row, column+1))
			component.setBackground(Colors.LIGHT_GRAY);						
		return component;
	}
	          
     
    
     
}
