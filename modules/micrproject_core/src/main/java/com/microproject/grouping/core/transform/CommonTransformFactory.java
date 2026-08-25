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
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.HashMap;

import org.apache.commons.collections.Transformer;

import com.microproject.field.InvalidFormulaException;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;

/**
 *
 */
public abstract class CommonTransformFactory extends CommonTransform{
 	protected String id = null;
 	protected String name = null;
	protected String formulaText = null;
	protected boolean showSummary = true;
	protected boolean showEmptyLines=true;
	protected boolean showEndEmptyLines=true;
	protected String definition=null;
	protected String arguments=null;
	protected Transformer composition=null;
	protected boolean server;
	
	public abstract CommonTransform getTransform() throws InvalidFormulaException;
	
	public CommonTransform getTransformFromDefinition()  throws InvalidFormulaException{
	    if (definition!=null){
	        try {
	            /*if (arguments==null)
	                return (CommonTransform)Class.forName(definition).newInstance();
	            else*/ return (CommonTransform)Class.forName(definition).
	            	getConstructor(new Class[]{String.class}).newInstance(new Object[]{arguments});
            } catch (Exception e) {
                throw new InvalidFormulaException(e);
            }
	    }
	    return null;

	}
	
	
    public String getFormulaText() {
        return formulaText;
    }
    public void setFormulaText(String formulaText) {
        this.formulaText = formulaText;
    }
    public boolean isShowSummary() {
        return showSummary;
    }
    public void setShowSummary(boolean showSummary) {
        this.showSummary = showSummary;
    }

    public String getDefinition() {
        return definition;
    }
    public void setDefinition(String definition) {
        this.definition = definition;
    }
    
    public String getArguments() {
        return arguments;
    }
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
        if (name == null)
        	setName(Messages.getString(id));
    }
	public void setNameId(String id) {
		this.name = Messages.getString(id);
	}
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    
    public boolean isShowEmptyLines() {
        return showEmptyLines;
    }
    public void setShowEmptyLines(boolean showEmptyLines) {
        this.showEmptyLines = showEmptyLines;
    }

    public boolean isShowEndEmptyLines() {
		return showEndEmptyLines;
	}
	public void setShowEndEmptyLines(boolean showEndEmptyLines) {
		this.showEndEmptyLines = showEndEmptyLines;
	}
	
	protected boolean showEmptySummaries = true;
	public boolean isShowEmptySummaries() {
		return showEmptySummaries;
	}
	public void setShowEmptySummaries(boolean showEmptySummaries) {
		this.showEmptySummaries = showEmptySummaries;
	}
	
	protected boolean showAssignments = true;
    public boolean isShowAssignments() {
		return showAssignments;
	}
	public void setShowAssignments(boolean showAssignments) {
		this.showAssignments = showAssignments;
	}
	
	protected boolean preserveHierarchy = true;
	public boolean isPreserveHierarchy() {
		return preserveHierarchy;
	}
	public void setPreserveHierarchy(boolean preserveHierarchy) {
		this.preserveHierarchy = preserveHierarchy;
	}

    

	public boolean isServer() {
		return server;
	}

	public void setServer(boolean server) {
		this.server = server;
	}

	public Transformer getComposition() {
        return composition;
    }
    public void setComposition(Transformer composition) {
        this.composition = composition;
    }
    
    
	protected void setProperties(CommonTransform t) throws InvalidFormulaException{
	    t.setShowEmptyLines(isShowEmptyLines());
	    t.setShowEndEmptyLines(isShowEndEmptyLines());
	    t.setShowSummary(isShowSummary());
	    t.setShowEmptySummaries(isShowEmptySummaries());
	    t.setShowAssignments(isShowAssignments());
	    t.setPreserveHierarchy(isPreserveHierarchy());
	    if (subTransforms!=null){
	        ArrayList sub = new ArrayList();
	        for (Iterator i=subTransforms.iterator();i.hasNext();)
	            sub.add(((CommonTransformFactory)i.next()).getTransform());
	        t.setSubTransforms(sub);
	    }
	    if (parameters != null) {
	        ArrayList<TransformParameter> parameterCopies = new ArrayList<>(parameters.size());
	        HashMap<String, Object> valueCopies = new HashMap<>();
	        for (TransformParameter parameter : parameters) {
	            TransformParameter copy = new TransformParameter();
	            copy.setId(parameter.getId());
	            copy.setValue(parameter.getValue());
	            parameterCopies.add(copy);
	            if (copy.getId() != null && copy.getValue() != null)
	                valueCopies.put(copy.getId(), copy.getValue());
	        }
	        t.setParameters(parameterCopies);
	        t.setParametersMap(valueCopies);
	    }
	}
	
	
	public String toString(){
		return getName();
	}

    public void setRedefinitionCallBack(Consumer<Object> callback){}
    
    
    public void addFactory(CommonTransformFactory factory){
    	if (factory.isServer()&&Environment.getStandAlone()) return;
        if (subTransforms==null) subTransforms=new ArrayList();
        subTransforms.add(factory);
    }
    
    
}
