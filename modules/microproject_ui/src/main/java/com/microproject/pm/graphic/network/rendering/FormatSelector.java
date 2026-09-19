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
package com.microproject.pm.graphic.network.rendering;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.grouping.core.Node;
/**
 *
 */
public class FormatSelector {
	protected List<BarFormat> selectedFormats;
	protected GraphParams params;
	protected Map<String, FormComponent> formMap=new HashMap<>();

	public FormatSelector(GraphParams params){
		this.params=params;
		selectedFormats=new LinkedList<>();
	}
	
	
	FormatSelectionFunctor selectionFunctor = new FormatSelectionFunctor();
	
	
	private class FormatSelectionFunctor implements Consumer<Object> {
		void initialize() {
			selectedFormats.clear();
		}
		public void accept(Object arg0) {			
			BarFormat format = (BarFormat)arg0;
			if (!format.isMain()) return;
			selectedFormats.add(format);
		}
	}
	
	void selectFormats(GraphicNode gnode){
		Node node=gnode.getNode();
		Object ganttableObject =node.getImpl();
		selectionFunctor.initialize(); // functor is recycled
		params.getBarStyles().apply(ganttableObject,selectionFunctor); // functional call.  See functor above
	}

	
	
//	private List getSelectedFormats() {
//		return selectedFormats;
//	}
	
	
	
	private FormComponent getForm(BarFormat format,int zoom,boolean editor){
		FormComponent form=formMap.get(format.getId());
		if (form==null){
			form=new FormComponent(selectedFormats,zoom,editor,params.useTextures());
			formMap.put(format.getId(),form);
		}
		return form;
	}
	
	
	FormComponent getForm(int zoom,boolean editor){
		if (selectedFormats.size()==0) return null;//should not happen
		BarFormat format=selectedFormats.get(0);
		return getForm(format,zoom, editor);
	}
	
	
	public FormComponent getForm(GraphicNode gnode,int zoom,boolean editor){
		selectFormats(gnode);
		return getForm(zoom,editor);
	}

	public void resetForms(){
		formMap.clear();
	}
	
}

