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

import org.apache.commons.collections4.Transformer;

import com.microproject.field.InvalidFormulaException;
import com.microproject.strings.Messages;

/**
 *
 */
public abstract class CommonTransformFactory extends CommonTransform{
 	protected String id = null;
 	protected String name = null;
	protected String formulaText = null;
	protected String definition=null;
	protected String arguments=null;
	protected Transformer composition=null;
	
	public abstract CommonTransform getTransform() throws InvalidFormulaException;
	
	public CommonTransform getTransformFromDefinition()  throws InvalidFormulaException{
	    if (definition!=null){
	        try {
			return (CommonTransform) Class.forName(definition)
				.getConstructor(String.class)
				.newInstance(arguments);
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
	        ArrayList<Object> sub = new ArrayList<>(subTransforms.size());
	        for (Object subTransform : subTransforms)
	            sub.add(((CommonTransformFactory) subTransform).getTransform());
	        t.setSubTransforms(sub);
	    }
	    t.setParameters(getParameters());
	    t.setParametersMap(getParametersMap());
	}
	
	
	public String toString(){
		return getName();
	}

    public void setRedefinitionCallBack(Consumer<Object> callback){}
    
    
	public void addFactory(CommonTransformFactory factory){
        if (subTransforms==null) subTransforms=new ArrayList<>();
        subTransforms.add(factory);
    }
    
    
}
