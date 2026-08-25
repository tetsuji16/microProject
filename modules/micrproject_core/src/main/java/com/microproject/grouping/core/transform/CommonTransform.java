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

import org.apache.commons.collections.Transformer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public abstract class CommonTransform {
	protected static final Logger logger = Logger.getLogger(CommonTransform.class.getName());
	/** Returns mutable state isolated for one view session. */
	public CommonTransform copyForSession() {
		throw new IllegalStateException("Mutable view transform does not support session isolation: "
				+ getClass().getName());
	}

    public abstract boolean isShowEmptyLines();
    public abstract void setShowEmptyLines(boolean showEmptyLines);
    public abstract boolean isShowEndEmptyLines();
    public abstract void setShowEndEmptyLines(boolean showEndEmptyLines);
    public abstract boolean isShowSummary();
    public abstract void setShowSummary(boolean showSummary);
    public abstract boolean isShowEmptySummaries();
    public abstract void setShowEmptySummaries(boolean showEmptySummaries);
//    public abstract boolean isShowBadBranches();
//    public abstract void setShowBadBranches(boolean showBadBranches);
    public abstract boolean isShowAssignments();
	public abstract void setShowAssignments(boolean showAssignments);
	public abstract boolean isPreserveHierarchy();
	public abstract void setPreserveHierarchy(boolean preserveHierarchy);
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


    private final static String REGISTERED_PARAMETER_DIALOG="com.microproject.dialog.TransformParameterDialog";
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
            try {
                parameterDialog = Class.forName(REGISTERED_PARAMETER_DIALOG).asSubclass(Consumer.class)
                    .getDeclaredConstructor().newInstance();
            } catch (Exception e) {logger.log(Level.WARNING, "Transform error", e);}
        }
        if (parameterDialog!=null){
            parameterDialog.accept(this);
        }
    }

    protected boolean server;
	public boolean isServer() {
		return server;
	}
	public void setServer(boolean server) {
		this.server = server;
	}

}
