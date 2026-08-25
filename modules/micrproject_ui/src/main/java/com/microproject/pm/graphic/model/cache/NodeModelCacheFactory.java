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
package com.microproject.pm.graphic.model.cache;

import java.util.function.Consumer;


import com.microproject.document.Document;
import com.microproject.grouping.core.model.NodeModel;

/**
 *
 */
public class NodeModelCacheFactory {
	protected static NodeModelCacheFactory instance;
	
	public static NodeModelCacheFactory getInstance(){
		if (instance==null) instance=new NodeModelCacheFactory();
		return instance;
	}
	
	public ReferenceNodeModelCache createReferenceCache(NodeModel model, Document document,int type){
		return new ReferenceNodeModelCache(model,document,type);
	}
	
	public NodeModelCache createAntiAssignmentFilteredCache(ReferenceNodeModelCache cache,String viewName,Consumer<Object> transformerClosure){
	    return createViewCache(cache, viewName, transformerClosure);
	}
	public NodeModelCache createFilteredCache(ReferenceNodeModelCache cache,String viewName,Consumer<Object> transformerClosure){
	    return createViewCache(cache, viewName, transformerClosure);
	}
	private ViewNodeModelCache createViewCache(ReferenceNodeModelCache reference, String viewName,
			Consumer<Object> transformerClosure) {
		ViewNodeModelCache cache = new ViewNodeModelCache(reference, viewName, transformerClosure);
		var commands = reference.taskCommandGatewayOrNull();
		if (commands != null) cache.setTaskCommandGateway(commands);
		return cache;
	}
	
	public NodeModelCache createDefaultCache(NodeModel model,Document document,int type,String viewName,Consumer<Object> transformerClosure){
	    return createFilteredCache(createReferenceCache(model,document,type),viewName,transformerClosure);
	}

	
//	public ReferenceNodeModelCache createReferenceCache(Document document){
//	return new ReferenceNodeModelCache(document);
//}
//	public NodeModelCache createDefaultCache(Document document,String viewName){
//    return createFilteredCache(createReferenceCache(document),viewName);
//}
	
	
//	public ReferenceNodeModelCache createReferenceCache(NodeModel model){
//	return new ReferenceNodeModelCache(model);
//}
//	public NodeModelCache createDefaultCache(NodeModel model,String viewName){
//    return createFilteredCache(createReferenceCache(model),viewName);
//}

	
	
	
    //for DocumentFrame and svg export
	public static ReferenceNodeModelCache createTaskNodeModelCache(Document document,NodeModel model) {
		ReferenceNodeModelCache taskCache = NodeModelCacheFactory.getInstance()
					.createReferenceCache(model, document,NodeModelCache.TASK_TYPE|NodeModelCache.ASSIGNMENT_TYPE);
		return taskCache;
	}

	public static ReferenceNodeModelCache createResourceNodeModelCache(Document document,NodeModel model) {
		ReferenceNodeModelCache resourceCache = NodeModelCacheFactory.getInstance()
					.createReferenceCache(model, document,NodeModelCache.RESOURCE_TYPE|NodeModelCache.ASSIGNMENT_TYPE);
		return resourceCache;
	}

}
