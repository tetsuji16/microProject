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
package com.microproject.toolbar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import com.microproject.grouping.core.transform.CommonTransformFactory;
import com.microproject.grouping.core.transform.TransformList;
import com.microproject.grouping.core.transform.ViewConfiguration;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.strings.Messages;

/**
 * 
 */
public class TransformComboBoxModel extends AbstractListModel implements ComboBoxModel {
	public static final int FILTER=1;
	public static final int SORTER=2;
	public static final int GROUPER=3;

	protected int type;
	protected String stype;
	protected TransformList transformList;
	protected CommonTransformFactory selected;
	protected ViewConfiguration view;
	protected Map<ViewConfiguration, CommonTransformFactory> viewMap=new HashMap<>();
	private String tipText;
	/**
	 * 
	 */
	public TransformComboBoxModel(int type) {
		this.type=type;
		switch (type) {
		case SORTER:
			stype="user_sorters";
			tipText=Messages.getString("Text.Sort");
			break;
		case GROUPER:
			stype="user_groupers";
			tipText=Messages.getString("Text.Group");
			break;
		default:
			stype="user_filters";
			tipText=Messages.getString("Text.Filter");
			break;
		}
		transformList=TransformList.getInstance(stype);
		//selected=(CommonTransformFactory)getElementAt(0);
	}
	
	
	public void setView(ViewConfiguration view){
		if (view==null) return;
		int max=getSize()-1;
		if (max>=0) fireIntervalRemoved(this,0,max);
		this.view=view;
		factories=transformList.getFactories(view,stype);
		selected=(CommonTransformFactory)viewMap.get(view);
		if (selected==null){
			ViewTransformer transformer=view.getTransform();
			switch (type) {
			case SORTER:
				selected=transformList.getFactory(transformer.getUserSorterId());
				break;
			case GROUPER:
				selected=transformList.getFactory(transformer.getUserGrouperId());
				break;
			default:
				selected=transformList.getFactory(transformer.getUserFilterId());
				break;
			}
			viewMap.put(view,selected);
		}
		int max2=getSize()-1;
		if (max2>=0) fireIntervalAdded(this,0,max2);
		
		
	}
	
	public ViewConfiguration getView() {
		return view;
	}
	
	public void changeTransform(CommonTransformFactory factory){
		if (view==null) return;	
		ViewTransformer transformer=view.getTransform();
		switch (type) {
			case SORTER:
				transformer.setUserSorterId(factory.getId());
				break;
			case GROUPER:
				transformer.setUserGrouperId(factory.getId());
				break;
			default:
				transformer.setUserFilterId(factory.getId());
				break;
			}
	}
	
	public Object getSelectedItem() {
		return selected;
	}
	public void setSelectedItem(Object obj) {
		selected=(CommonTransformFactory)obj;
		if (view != null && selected != null) {
			viewMap.put(view,selected);
		}
	}

    
    
	protected List<?> factories=null;
	public int getSize() {
	    if (factories==null) return 0;
	    return factories.size();
	}
	public CommonTransformFactory getElementAt(int index) {
	   return (CommonTransformFactory)factories.get(index);
	}


	final String getTipText() {
		return tipText;
	}


}

