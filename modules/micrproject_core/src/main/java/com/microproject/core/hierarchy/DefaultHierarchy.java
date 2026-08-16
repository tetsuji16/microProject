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
package com.microproject.core.hierarchy;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.microproject.core.nodes.DefaultNode;
import com.microproject.core.nodes.Node;

/**
 * @author Laurent Chretienneau
 *
 */
public class DefaultHierarchy implements Hierarchy{
	protected HierarchyNode root;
	protected Map<Node,HierarchyNode> reverseIndex=new HashMap<Node, HierarchyNode>();
	public DefaultHierarchy(){
		root=new DefaultHierarchyNode(new DefaultNode());
	}
	
	//find
	protected void addIndexEntry(HierarchyNode node){
		reverseIndex.put(node.getNode(),node);
	}
	protected HierarchyNode removeIndexEntry(Node node){
		return reverseIndex.remove(node);
	}

	@Override
	public HierarchyNode findHierarchyNode(Node node) {
		return reverseIndex.get(node);		
	}
	
	
	//add
	@Override
	public void add(Node node) {
		root.add(node);
	}	

	@Override
	public void add(Node node, Node parent) {
		HierarchyNode hparent;
		if (parent==null)
			hparent=root;
		else hparent=findHierarchyNode(parent);
		add(node,hparent);
	}

	@Override
	public void add(Node node, HierarchyNode parentHierarchyNode) {
		if (parentHierarchyNode==null)
			parentHierarchyNode=root;
		addIndexEntry((parentHierarchyNode==null ? root : parentHierarchyNode).add(node));
	}
	
	
	//visit
	@Override
	public void visit(Visitor visitor) {
		visit(visitor,VisitType.PRE_ORDER);
	}

	@Override
	public void visit(Visitor visitor,VisitType visitType) {
		visit(visitor,visitType,root);
	}

	@Override
	public void visit(Visitor visitor, VisitType visitType, HierarchyNode parent) {
		visitChildren(visitor,parent,visitType);
	}
	
	protected void visitChildren(Visitor visitor, HierarchyNode parent, VisitType visitType){ //iterative post-order more efficient than recursive version
		if (parent==null)
			parent=root;
		
		HierarchyNode c=parent;
		Stack<Integer> position=new Stack<Integer>();
		int pos;
		do {
			//down to the lower left leaf
			while(c.hasChildren()){  //visit down
				c=c.getChildren().getFirst();
				if (visitType==VisitType.PRE_ORDER) visitor.visit(c);
				position.push(1);
			}
			//up and right
			while (c!=parent){ //visit up
				if (visitType==VisitType.POST_ORDER) visitor.visit(c); 
				c=c.getParent();
				pos=position.pop();
				if (pos<c.getChildrenCount()){
					c=c.getChildren().get(pos);
					if (visitType==VisitType.PRE_ORDER) visitor.visit(c); 
					position.push(pos+1);
					break;
				}
			}

		} while (c!=parent);
	}

	
}
