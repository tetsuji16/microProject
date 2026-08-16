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
package com.microproject.grouping.core.model;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.Transformer;

import com.microproject.document.Document;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.hierarchy.MutableNodeHierarchy;
import com.microproject.pm.task.Project;


/**
 *
 */
public class NodeModelFactory {

	protected static NodeModelFactory instance=null;
	protected NodeModelFactory() {
	}
	public static NodeModelFactory getInstance(){
		if (instance==null) instance=new NodeModelFactory();
		return instance;
	}
	
	public NodeModel createNodeModel(){
		return new DefaultNodeModel();
	}

	public NodeModel createNodeModel(NodeModelDataFactory dataFactory){
		if (dataFactory!=null&&dataFactory.containsAssignments()) return new AssignmentNodeModel(dataFactory);
		else return new DefaultNodeModel(dataFactory);
	}

//	public NodeModel createAssignmentNodeModel(NodeModelDataFactory dataFactory){
//		return new AssignmentNodeModel(dataFactory);
//	}
	

	public NodeModel createAssignmentNodeModel(DefaultNodeModel model,Document document,boolean containsLeftObjects){
		return new AssignmentNodeModel(/*(Vector)model.getList().clone(),*/(MutableNodeHierarchy)model.getHierarchy().clone(), model.getDataFactory(),document,containsLeftObjects);
	}

	public NodeModel createNodeModelFromCollection(Collection collection,NodeModelDataFactory dataFactory) {
		NodeModel nodeModel = createNodeModel(dataFactory);
		nodeModel.addImplCollection(null,collection,NodeModel.SILENT);
		return nodeModel;
	}
	public void updateNodeModelFromCollection(NodeModel nodeModel,Collection collection,NodeModelDataFactory dataFactory,int nbEndVoidNodes) {
		nodeModel.removeAll(NodeModel.SILENT);
		nodeModel.setDataFactory(dataFactory);
		nodeModel.addImplCollection(null,collection,NodeModel.SILENT);
		nodeModel.getHierarchy().setNbEndVoidNodes(nbEndVoidNodes);
		nodeModel.getHierarchy().checkEndVoidNodes(nbEndVoidNodes); // fixed bug 145
		nodeModel.getHierarchy().fireUpdate();
	}

	/**
	 * Creates a node model from a given one such that the relationships are the same, as the source hierarchy, though
	 * the nodes and their impls are different.  The impls are created by applying the transformer
	 * @param source
	 * @param transformer
	 * @return
	 */
	public NodeModel replicate(NodeModel source, Transformer transformer) {
		NodeModel newModel = getInstance().createNodeModel();
		replicate(source,null,null,newModel,transformer);
		return newModel;
	}
	
    private void replicate(NodeModel source, Node sourceParentNode, Node newParentNode, NodeModel newModel, Transformer transformer) {
    	Collection children = source.getHierarchy().getChildren(sourceParentNode);        	
    	if (children != null) {
        	Iterator i = children.iterator();
        	while (i.hasNext()) {
        		Node sourceNode = (Node)i.next();
        		Object newImpl = transformer.transform(sourceNode.getImpl()); // make a new object from source
        		if (newImpl==null) continue;
        		Node newNode = NodeFactory.getInstance().createNode(newImpl); // make a new node
        		newModel.add(newParentNode,newNode,NodeModel.SILENT);
        		replicate(source,sourceNode,newNode,newModel,transformer);
        	}
    	}
    	
    }
    
    
    //for DocumentFrame and svg export
	public static NodeModel createTaskModel(Project project) {
		NodeModel taskModel = project.getTaskOutline();
		if (taskModel instanceof AssignmentNodeModel)
			((AssignmentNodeModel) taskModel).addAssignments();
		return taskModel;
	}

	public static NodeModel createResourceModel(Project project) {
		NodeModel resourceModel = project.getResourcePool().getResourceOutline();
		if (resourceModel instanceof AssignmentNodeModel) {
			//the bug is fixed elsewhere
//			if (!resourceModel.hasChildren(null)) // if it is currently empty - fixes bug about adding a second assignment when the view is first shown
				((AssignmentNodeModel) resourceModel).addAssignments();
		}
		return resourceModel;
	}

    	
	
}
