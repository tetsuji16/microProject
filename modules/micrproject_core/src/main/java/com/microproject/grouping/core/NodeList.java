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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.grouping.core.transform.filtering.NotVoidFilter;

/**
 * It holds all the nodes of a model.
 */
public class NodeList extends ArrayList<Node> {
	/**
	 *  
	 */
	public NodeList() {
		super();
	}
	private Node getNode() throws NodeException {
		if (size() == 0)
			throw new NodeException("Empty NodeList");
		return (Node) get(0);
	}
	public Class getType() throws NodeException {
		return getNode().getClass();
	}

	public boolean isVirtual() {
		return true;
	}
	public void setVirtual(boolean virtual) {
	}

	public static void accept(NodeVisitor visitor, Iterator nodes) {
		while (nodes.hasNext())
			((Node) nodes.next()).accept(visitor);
	}
	public void accept(NodeVisitor visitor) {
		accept(visitor, iterator());
	}
	public Object getImpl() {
		return null;
	}
	
	public static List<Object> nodeListToImplList(Collection<?> nodeList) {
	    return nodeListToImplList(nodeList,NotVoidFilter.getInstance());
	}
	public static List<Object> nodeListToImplList(Collection<?> nodeList,NodeFilter filter) {
		if (nodeList == null) // happens in certain cases
			return new ArrayList<>();
		List<Object> implList = new ArrayList<>(nodeList.size());
		for (Object value : nodeList) {
			Node current = (Node) value;
			if (!current.isVirtual()&&(filter==null||filter.evaluate(current)))
				implList.add(current.getImpl());
		}
		return implList;
	}
}
