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
package com.microproject.pm.graphic.xbs;

import java.awt.Frame;
import java.util.function.Consumer;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.tree.TreeNode;


import com.microproject.dialog.XbsDependencyDialog;
import com.microproject.pm.graphic.graph.GraphUI;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.network.NetworkInteractor;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeBridge;
import com.microproject.application.task.TaskCommands.TaskHierarchyRelocateCommand;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.Task;

/**
 *
 */
public class XbsInteractor extends NetworkInteractor {

	/**
	 * @param ui
	 */
	public XbsInteractor(GraphUI ui) {
		super(ui);
	}

    public boolean executeAction(double x,double y){
    	if (super.executeAction(x,y)) return true;
    	if (selected==null) return false;
    	switch (state) {
		case LINK_CREATION:
			if (sourceNode!=null&&destinationNode!=null){
				try {
					getGraph().getModel().getCache().createHierarchyDependency(sourceNode, destinationNode);
				} catch (com.microproject.association.InvalidAssociationException failure) {
					com.microproject.util.Alert.warn(failure.getMessage(), getGraph());
					return false;
				}
			}
			return true;
		case LINK_SELECTION:
			showDependencyPropertiesDialog((GraphicDependency)selected);
			return true;

    	}
    	return false;
    }

    protected XbsDependencyDialog dependencyPropertiesDialog;
	public void showDependencyPropertiesDialog(final GraphicDependency dependency) {
    	if (dependencyPropertiesDialog == null) {
    		Frame parent=JOptionPane.getFrameForComponent(getGraph());
    		dependencyPropertiesDialog = new XbsDependencyDialog(parent,dependency);
    	}
    	boolean didAction = XbsDependencyDialog.doDialog(dependencyPropertiesDialog,dependency,new Consumer<Object>() { public void accept(Object arg0) {
    			Node child=dependency.getSuccessor().getNode();
    			int position=0;
    			TreeNode[] path=((NodeBridge)child).getPath();
    			if (path.length>1){
    				NodeBridge previous=(NodeBridge)path[1];
    				position=previous.getRoot().getIndex(previous)+1;
    			}
				if (child.getImpl() instanceof Task task && getGraph().getCache() instanceof ViewNodeModelCache cache) {
					ProjectTaskKey key = ProjectTaskKey.from(task).orElse(null);
					if (key != null) cache.getTaskCommandGateway().relocateHierarchy(new TaskHierarchyRelocateCommand(
							List.of(key), null, position, cache.getProjectionSnapshot().domainRevision()));
				}
		}
    	});
    }	


}
