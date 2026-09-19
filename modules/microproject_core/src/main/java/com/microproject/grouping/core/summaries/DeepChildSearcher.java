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
package com.microproject.grouping.core.summaries;

import com.microproject.util.DataUtils;

import java.util.Collection;
import java.util.function.Consumer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.key.HasKey;

/**
 * 
 */
public class DeepChildSearcher extends NodeWalker {
	FindClosure findClosure;
	private static class FindClosure implements Consumer<Object> {
		Predicate condition;
		Object result = null;
		FindClosure(Predicate condition) {
			this.condition = condition;
		}
		public void accept(Object arg0) {
			Node node = (Node) arg0;
			Object impl = node.getImpl();
			if (condition.evaluate(impl))
				result = impl;
		}
	}
	private DeepChildSearcher(FindClosure f) {
		super(f);
		this.findClosure = f;
	}

	
	public void accept(Object arg0) {
		if (findClosure.result != null)
			return;
		Node node = (Node) arg0;
		if (node != null)
			closure.accept(node);
		Collection nodeList = nodeModel.getChildren(node);
		if (nodeList != null)
			DataUtils.forAllDo(nodeList.iterator(), this);
	}

	/**
	 * Applies a closure to the starting node and recursively all children
	 * @param nodeModel
	 * @param node
	 * @param closure
	 */
	public static Object search(NodeModel nodeModel, Predicate condition) {
		FindClosure f = new FindClosure(condition);
		DeepChildSearcher walker = new DeepChildSearcher(f);
		walker.setNodeModel(nodeModel);
		walker.accept(null);
		return f.result;
	}
	
	public static Object searchForUniqueId(NodeModel nodeModel, final long uniqueId) {
		return search(nodeModel, new Predicate() {
			public boolean evaluate(Object arg0) {
				return ((HasKey)arg0).getUniqueId() == uniqueId;
			}});
	}

	
}
