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
package com.microproject.grouping.core.event;

import java.util.EventObject;

import com.microproject.grouping.core.Node;

/**
 *
 */
public class HierarchyEvent extends EventObject {
	public static final int NODES_CHANGED=0;
	public static final int NODES_INSERTED=1;
	public static final int NODES_REMOVED=2;
	public static final int STRUCTURE_CHANGED=3;
	
	protected int type;
	protected Object[] nodes;
	protected Object flag;
	protected Object[] oldNodes;
	protected boolean consumed=false;
	
	
	
	/**
	 * @param source
	 * @param type
	 * @param change
	 */
	public HierarchyEvent(Object source, int type, Object[] nodes) {
		super(source);
		this.type = type;
		this.nodes = nodes;
	}
	public HierarchyEvent(Object source, int type, Object[] nodes, Object[] oldNodes, Object flag) {
		super(source);
		this.type = type;
		this.nodes = nodes;
		this.oldNodes = oldNodes;
		this.flag=flag;
	}
	
	
	
	
	
	public Object[] getNodes() {
		return nodes;
	}
	public void setNodes(Object[] nodes) {
		this.nodes = nodes;
	}
	public Object[] getOldNodes() {
		return oldNodes;
	}
	public void setOldNodes(Object[] oldNodes) {
		this.oldNodes = oldNodes;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
    public Object getFlag() {
        return flag;
    }
    public void setFlag(Object flag) {
        this.flag = flag;
    }
    public boolean isVoid() {
    	if (nodes.length != 1)
    		return false;
    	return ((Node)nodes[0]).isVoid();
    }
    
	public boolean isConsumed() {
		return consumed;
	}
	public void consume() {
		this.consumed = true;
	}
    
    
    
}
