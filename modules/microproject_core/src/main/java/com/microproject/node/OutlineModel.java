/*******************************************************************************
 * MIT License
 *
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
package com.microproject.node;

import java.util.List;
import java.util.function.Consumer;

import com.microproject.document.Document;
import com.microproject.node.event.HierarchyListener;

/** Core outline behavior without a dependency on a particular view toolkit. */
public interface OutlineModel {
	int EVENT = 1;
	int UNDO = 2;
	int SILENT = 0;
	int NORMAL = 3;

	Hierarchy getHierarchy();
	List<?> getChildren(Node parent);
	Node getParent(Node child);
	Node search(Object key);
	boolean isSummary(Node node);
	Document getDocument();

	void add(Node child, int actionType);
	void add(Node parent, Node child, int actionType);
	void add(Node parent, Node child, int position, int actionType);
	void remove(Node node, int actionType);
	void remove(List<Node> nodes, int actionType);
	boolean hasChildren(Node node);
	boolean isLocal();
	boolean isMaster();

	interface Hierarchy {
		Node root();
		Node getParent(Node child);
		List<Node> getChildren(Node parent);
		int getLevel(Node node);
		void move(Node node, Node newParent, int actionType);
		void addHierarchyListener(HierarchyListener listener);
		void removeHierarchyListener(HierarchyListener listener);
		void visitAll(Consumer<Object> visitor);
		void visitAll(Node parent, Consumer<Object> visitor);
		void fireUpdate();
		void fireUpdate(Node[] nodes);
		void fireInsertion(Node[] nodes);
		void fireRemoval(Node[] nodes);
		int getNbEndVoidNodes();
		void setNbEndVoidNodes(int count);
		void cleanVoidChildren();
	}
}
