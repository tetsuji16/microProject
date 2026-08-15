/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *******************************************************************************/
package com.projectlibre1.grouping.core.summaries;

import java.util.Collection;

import com.projectlibre1.grouping.core.Node;

public class CountNonsummariesWalker extends NodeWalker {
	public CountNonsummariesWalker(SummaryVisitor visitor) {
		super(visitor);
	}

	public void accept(Object arg0) {
		countNonSummaryDescendants((Node)arg0);
	}

	private void countNonSummaryDescendants(Node node) {
		Collection nodeList = nodeModel.getChildren(node);
		if (nodeList == null) {
			return;
		}
		for (Object childObject : nodeList) {
			Node child = (Node) childObject;
			if (!child.isVoid() && !child.isRoot() && !nodeModel.isSummary(child)) {
				visitor.accept(child);
			}
			countNonSummaryDescendants(child);
		}
	}
}
