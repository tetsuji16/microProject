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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.Transformer;

import java.util.logging.Logger;

/**
 *
 */
public abstract class CommonTransform {
	protected static final Logger logger = Logger.getLogger(CommonTransform.class.getName());

	/** Display options shared by every filter, sorter, transformer and factory; all default to showing. */
	protected boolean showEmptyLines = true;
	protected boolean showEndEmptyLines = true;
	protected boolean showSummary = true;
	protected boolean showEmptySummaries = true;
	protected boolean showAssignments = true;
	protected boolean preserveHierarchy = true;
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
	public boolean isShowSummary() {
		return showSummary;
	}
	public void setShowSummary(boolean showSummary) {
		this.showSummary = showSummary;
	}
	public boolean isShowEmptySummaries() {
		return showEmptySummaries;
	}
	public void setShowEmptySummaries(boolean showEmptySummaries) {
		this.showEmptySummaries = showEmptySummaries;
	}
	public boolean isShowAssignments() {
		return showAssignments;
	}
	public void setShowAssignments(boolean showAssignments) {
		this.showAssignments = showAssignments;
	}
	public boolean isPreserveHierarchy() {
		return preserveHierarchy;
	}
	public void setPreserveHierarchy(boolean preserveHierarchy) {
		this.preserveHierarchy = preserveHierarchy;
	}
    protected Transformer composition=null;
    public Transformer getComposition() {
        return composition;
    }
    public void setComposition(Transformer composition) {
        this.composition = composition;
    }

    public abstract void setRedefinitionCallBack(Consumer<Object> callback);


    protected List<Object> subTransforms;
    public List<Object> getSubTransforms() {
        return subTransforms;
    }
    public void setSubTransforms(List<Object> subTransforms) {
        this.subTransforms = subTransforms;
    }


	protected Consumer<Object> parameterDialog;
    protected List<TransformParameter> parameters;
    protected Map<String, Object> parametersMap;
    public List<TransformParameter> getParameters() {
        return parameters;
    }
    public Map<String, Object> getParametersMap() {
        return parametersMap;
    }
    void setParameters(List<TransformParameter> parameters) {
        this.parameters = parameters;
    }
    void setParametersMap(Map<String, Object> parametersMap) {
        this.parametersMap = parametersMap;
    }
    public void addParameter(TransformParameter parameter){
        if (parameter == null || parameter.getId() == null) {
            return;
        }
        if (parameters==null){
            parameters=new ArrayList<>();
            parametersMap=new HashMap<>();
        }
        parameters.add(parameter);
        if (parameter.getValue() != null) {
            parametersMap.put(parameter.getId(),parameter.getValue());
        }
    }

    public Object getParameter(String id){
        if (parametersMap==null) return null;
        return parametersMap.get(id);
    }
    public void setParameter(TransformParameter param){
        if (parametersMap == null || param == null || param.getId() == null || param.getValue() == null) {
            return;
        }
        parametersMap.put(param.getId(),param.getValue());
    }
    public void askForParameters(){
        if (parameters==null) return; //no parameters
        if (parameterDialog==null){
            Consumer<CommonTransform> dialog = TransformParameterDialogServices.createDialog();
            if (dialog != null)
                parameterDialog = transform -> dialog.accept((CommonTransform) transform);
        }
        if (parameterDialog!=null){
            parameterDialog.accept(this);
        }
    }

}
