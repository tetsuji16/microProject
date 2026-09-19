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
package com.microproject.print;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.microproject.offline_graphics.GanttSVGRenderer;
import com.microproject.offline_graphics.NetworkSVGRenderer;
import com.microproject.offline_graphics.SVGRenderer;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.transform.NodeCacheTransformer;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.views.BaseView;
import com.microproject.pm.graphic.views.GanttView;
import com.microproject.pm.graphic.views.PertView;
import com.microproject.pm.graphic.views.ProjectView;
import com.microproject.pm.graphic.views.ResourceView;
import com.microproject.pm.graphic.views.TreeView;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.pm.task.Portfolio;

public class PrintDocumentFactory {
	protected static PrintDocumentFactory instance;
	public static PrintDocumentFactory getInstance(){
		if (instance==null) instance=new PrintDocumentFactory();
		return instance;
	}
	public GraphPageable createDocument(DocumentFrame frame,boolean printOnly,boolean pdfAsDefault){
		BaseView view=frame.getActiveTopView();
		SVGRenderer renderer;
		NodeModelCache cache;
		if (view instanceof GanttView){
			renderer=new GanttSVGRenderer();
			SpreadSheet sp=frame.getActiveSpreadSheet();
			SpreadSheetFieldArray fieldArray=sp.getFieldArrayWithWidths(null);
			List<Integer> colWidth=null;
//			if (sp!=null){
//				fieldArray=(SpreadSheetFieldArray)sp.getFieldArray();
//				colWidth=getColWidth(sp, fieldArray);
//			}
			cache=NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)frame.getReferenceCache(true),"OfflineGantt",null);
			((GanttSVGRenderer)renderer).init(frame.getProject(),cache,fieldArray,colWidth,frame.getScale(),true);
			renderer.getParams().setSupportLeftAndRightParts(true);
		}else if (view instanceof ResourceView){
			renderer=new GanttSVGRenderer();
			SpreadSheet sp=frame.getActiveSpreadSheet();
			SpreadSheetFieldArray fieldArray=sp.getFieldArrayWithWidths(null);
			List<Integer> colWidth=null;
//			if (sp!=null){
//				fieldArray=(SpreadSheetFieldArray)sp.getFieldArray();
//				colWidth=getColWidth(sp, fieldArray);
//			}
			cache=NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)frame.getReferenceCache(false),"OfflineResources",null);
			((GanttSVGRenderer)renderer).init(frame.getProject(),cache,fieldArray,colWidth,frame.getScale(),false);
		}else if (view instanceof ProjectView){
			renderer=new GanttSVGRenderer();
			SpreadSheet sp=frame.getActiveSpreadSheet();
			SpreadSheetFieldArray fieldArray=sp.getFieldArrayWithWidths(null);
			List<Integer> colWidth=null;
//			if (sp!=null){
//				fieldArray=(SpreadSheetFieldArray)sp.getFieldArray();
//				colWidth=getColWidth(sp, fieldArray);
//			}
			Portfolio portfolio = frame.getGraphicManager().getProjectFactory().getPortfolio();
			cache=NodeModelCacheFactory.getInstance().createDefaultCache(portfolio.getNodeModel(), portfolio,NodeModelCache.PROJECT_TYPE,"OfflineProjects",null);
			((GanttSVGRenderer)renderer).init(frame.getProject(),cache,fieldArray,colWidth,frame.getScale(),false);
		}else if (view instanceof PertView){
			renderer=new NetworkSVGRenderer();
			cache=NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)frame.getReferenceCache(true),"Network",null);
			((NetworkSVGRenderer)renderer).init(frame.getProject(),cache,NetworkSVGRenderer.PERT,frame.getScale());
		}else if (view instanceof TreeView){
			renderer=new NetworkSVGRenderer();
			TreeView treeView=(TreeView)view;
			if ("WBS".equals(treeView.getViewName())){
				cache=NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)frame.getReferenceCache(true),"WBS",null);
				((NetworkSVGRenderer)renderer).init(frame.getProject(),cache,NetworkSVGRenderer.WBS,frame.getScale());
			}else{
				cache=NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)frame.getReferenceCache(false),"RBS",null);
				((NetworkSVGRenderer)renderer).init(frame.getProject(),cache,NetworkSVGRenderer.RBS,frame.getScale());
			}
		}else return null;
		NodeModelCache srcCache=view.getCache();
		ViewTransformer transformer=((NodeCacheTransformer)cache.getVisibleNodes().getTransformer()).getTransformer();
		ViewTransformer srcTransformer=((NodeCacheTransformer)srcCache.getVisibleNodes().getTransformer()).getTransformer();
		transformer.setUserFilterId(srcTransformer.getUserFilterId()); //this is valid just because the views have the same transformers
		transformer.setUserSorterId(srcTransformer.getUserSorterId()); //this is valid just because the views have the same transformers
		transformer.setUserGrouperId(srcTransformer.getUserGrouperId()); //this is valid just because the views have the same transformers
		GraphPageable document=new GraphPageable(renderer,printOnly,pdfAsDefault,true);
		return document;
	}

//	private List<Integer> getColWidth(SpreadSheet sp,SpreadSheetFieldArray fieldArray){
//		List<Integer> colWidth=new ArrayList<Integer>(fieldArray.size());
//			colWidth.add(sp.getRowHeader().getColumnModel().getColumn(0).getWidth());
//			TableColumnModel columnModel=sp.getColumnModel();
//			TableColumn tc;
//			for (int i=0;i<columnModel.getColumnCount();i++){
//				tc=columnModel.getColumn(i);
//				colWidth.add(tc.getWidth());
//			}
//		return colWidth;
//	}
}

