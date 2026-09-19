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
package com.microproject.grouping.core.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.Digester;

import com.microproject.configuration.Dictionary;
import com.microproject.configuration.NamedItem;
import com.microproject.field.InvalidFormulaException;
import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.grouping.core.transform.filtering.NotVoidFilter;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 
 */
public class TransformList implements NamedItem {
	private static final Logger logger = Logger.getLogger(TransformList.class.getName());
	public static final String category="TransformCategory";
	public String getCategory() {
		return category;
	}
	
	String name = null;
	String id = null;
    Map<String, Object> elementMap = new HashMap<>();
    Map<String, CommonTransformFactory> factoryMap = new HashMap<>();
    List<CommonTransformFactory> factories = new ArrayList<>();

	public TransformList() {}
	
	
	public void addFactory(CommonTransformFactory factory) {
    	if (factory.isServer()&&Environment.getStandAlone()) return;
		factories.add(factory);
		if (factory.getId() != null) {
			factoryMap.put(factory.getId(),factory);
		}
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public void setId(String id) {
		this.id = id;
		setName(Messages.getString(id));
	}	
	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}

	public Object getTransform(String id) {
	    if (id == null) return null;
	    Object transform=null;
	    if (elementMap.containsKey(id))
		    transform=elementMap.get(id);
	    else{
            CommonTransformFactory factory=getFactory(id);
            if (factory==null) return null;
            try {
                    transform=factory.getTransform();
                    if (transform != null) {
                    	elementMap.put(id,transform);
                    }
                } catch (InvalidFormulaException e) {
        			logger.severe("Formula not set: invalid formula text: " +factory.getFormulaText());
                }
	    }
	    return transform;
	}
	
	public CommonTransformFactory getFactory(String id) {
		return (CommonTransformFactory)factoryMap.get(id);
	}
	public List<CommonTransformFactory> getFactories() {
		return factories;
	}
	public List<CommonTransformFactory> getFactories(ViewConfiguration view,String type) {
	    List<String> authorizedList;
	    if (view==null){
	        authorizedList=new ArrayList<>();
	        if ("user_filters".equals(type)) authorizedList.add(ViewTransformer.FILTER_NONE_ID);
	        else if ("user_sorters".equals(type)) authorizedList.add(ViewTransformer.SORTER_NONE_ID);
	        else if ("user_groupers".equals(type)) authorizedList.add(ViewTransformer.GROUPER_NONE_ID);
	    }else{
	        if ("user_filters".equals(type)) authorizedList=view.getTransform().getFilterList();
	        else if ("user_sorters".equals(type)) authorizedList=view.getTransform().getSorterList();
	        else authorizedList=view.getTransform().getGrouperList();
	        if (authorizedList==null) return factories;
	    }
	    List<CommonTransformFactory> filtered=new ArrayList<>();
	    CommonTransformFactory f;
	    for (Iterator<CommonTransformFactory> i=factories.iterator();i.hasNext();){
	        f=i.next();
	        if (authorizedList.contains(f.getId())) filtered.add(f);
	    }
		return filtered;
	}
	
	public static void addDigesterEvents(Digester digester){
		//filters
	    digester.addObjectCreate("*/transform/filters", "com.microproject.grouping.core.transform.TransformList");
	    digester.addSetProperties("*/transform/filters");
		digester.addSetNext("*/transform/filters", "add", "com.microproject.configuration.NamedItem");

		digester.addObjectCreate("*/filter", "com.microproject.grouping.core.transform.filtering.NodeFilterFactory");
	    digester.addSetProperties("*/filter");
	    digester.addCallMethod("*/filter/formulaText","setFormulaText",0);
	    digester.addSetNext("*/filter", "addFactory", "com.microproject.grouping.core.transform.filtering.NodeFilterFactory");

	    
	    //sorters
		digester.addObjectCreate("*/transform/sorters", "com.microproject.grouping.core.transform.TransformList");
	    digester.addSetProperties("*/transform/sorters");
		digester.addSetNext("*/transform/sorters", "add", "com.microproject.configuration.NamedItem");
	    
	    digester.addObjectCreate("*/sorter", "com.microproject.grouping.core.transform.sorting.NodeSorterFactory");
	    digester.addSetProperties("*/sorter");
	    digester.addCallMethod("*/sorter/formulaText","setFormulaText",0);
	    digester.addCallMethod("*/sorter/groupNameFormula","setGroupNameFormula",0);
	    digester.addSetNext("*/sorter", "addFactory", "com.microproject.grouping.core.transform.sorting.NodeSorterFactory");
	    	    
	    //groupers
		digester.addObjectCreate("*/transform/groupers", "com.microproject.grouping.core.transform.TransformList");
	    digester.addSetProperties("*/transform/groupers");
		digester.addSetNext("*/transform/groupers", "add", "com.microproject.configuration.NamedItem");
	    
	    digester.addObjectCreate("*/grouper", "com.microproject.grouping.core.transform.grouping.NodeGrouper");
	    digester.addSetProperties("*/grouper");
	    digester.addSetNext("*/grouper", "addFactory", "com.microproject.grouping.core.transform.grouping.NodeGrouper");
	    
	    digester.addObjectCreate("*/grouper/group", "com.microproject.grouping.core.transform.grouping.NodeGroup");
	    digester.addSetProperties("*/grouper/group");
	    digester.addSetNext("*/grouper/group", "addGroup", "com.microproject.grouping.core.transform.grouping.NodeGroup");

	    
	    digester.addObjectCreate("*/param", "com.microproject.grouping.core.transform.TransformParameter");
	    digester.addSetProperties("*/param");
	    digester.addSetNext("*/param", "addParameter", "com.microproject.grouping.core.transform.TransformParameter");
	    
	    
		//transformers
	    digester.addObjectCreate("*/transform/transformers", "com.microproject.grouping.core.transform.TransformList");
	    digester.addSetProperties("*/transform/transformers");
		digester.addSetNext("*/transform/transformers", "add", "com.microproject.configuration.NamedItem");

		digester.addObjectCreate("*/transformer", "com.microproject.grouping.core.transform.transformer.NodeTransformerFactory");
	    digester.addSetProperties("*/transformer");
	    digester.addCallMethod("*/transformer/formulaText","setFormulaText",0);
	    digester.addSetNext("*/transformer", "addFactory", "com.microproject.grouping.core.transform.transformer.NodeTransformerFactory");

	
	}
	
	
	
	
	
	
	public static TransformList getInstance(String name){
	    return (TransformList)Dictionary.get(category,name);
	}
	
	
	
	
	
	/*public static NodeFilter getNotAssignmentFilter(){
	    return (NodeFilter)getInstance("hidden_filters").getTransform("Filter.NotAssignment");
	}*/
	
	public static NodeFilter getNotVoidFilter(){
	    return NotVoidFilter.getInstance();
	}
	
	public static NodeFilter getTrueFilter(){
	    return (NodeFilter)getInstance("user_filters").getTransform("Filter.True");
	}
	
	
	
}
