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
package com.microproject.grouping.core;

import com.microproject.grouping.core.transform.grouping.NodeGroup;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Use this factory to create nodes
 */
public class NodeFactory {

	protected static NodeFactory instance=null;
	protected NodeFactory() {
	}
	public static NodeFactory getInstance(){
		if (instance==null) instance=new NodeFactory();
		return instance;
	}

	/**
	 * consolidated node
	 * @param nodeClass
	 */
	public Node createVirtualNode(Class nodeClass){
		Node node=createNode(nodeClass);
		node.setVirtual(true);
		return node;
	}
	public Node createNode(Class nodeClass){
		try{
			Node node=(Node)nodeClass.getConstructor(new Class[]{}).newInstance(new Object[]{});
			return new NodeBridge(node);
		}catch (Exception e) {return null;}
	}
	public Node createNode(Object impl){
		try{
			return new NodeBridge(impl);
		}catch (Exception e) {return null;}
	}
	public Node createTask(Project project){
		return new NodeBridge(new NormalTask(project));
	}
	public Node createResource(ResourcePool resourcePool){
		return new NodeBridge(new ResourceImpl(new EnterpriseResource(resourcePool)));
	}
	public Node createVoidNode(){
		try{
			Node node=new NodeBridge(new VoidNodeImpl());
			node.setVoid(true);
			return node;
		}catch (Exception e) {return null;}
	}
	public Node createRootNode(){
		try{
			Node node=new NodeBridge(new VoidNodeImpl());
			node.setRoot(true);
			return node;
		}catch (Exception e) {return null;}
	}
	public Node createGroup(NodeGroup group,String name){
		try{
			GroupNodeImpl groupImpl=new GroupNodeImpl();
			groupImpl.setName((name==null)?group.getSorterId():name);
			//groupImpl.setGroupFields(group.getSorter().getFields());
			return new NodeBridge(groupImpl);
		}catch (Exception e) {return null;}
	}
	public Node createTestNode(){
		try{
			com.microproject.pm.task.Project project = Project.createProject(null,new DataFactoryUndoController());
			com.microproject.pm.task.NormalTask task = project.newNormalTaskInstance();
			task.setName("this is the task name");
			return new NodeBridge(task);
		}catch (Exception e) {return null;}
	}
}
