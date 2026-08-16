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
package com.microproject.pm.dependency;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Used by TaskInformation dialog
 */
public class DependencyNodeModelDataFactory implements NodeModelDataFactory {
	private static final Logger logger = Logger.getLogger(DependencyNodeModelDataFactory.class.getName());

	/**
	 * 
	 */
	public DependencyNodeModelDataFactory() {
	}

	public Object createUnvalidatedObject(NodeModel nodeModel, Object parent) {
		return null;
	}

	public void addUnvalidatedObject(Object object, NodeModel nodeModel,
			Object parent) {
	}

	public void validateObject(Object newlyCreated, NodeModel nodeModel,
			Object eventSource, Object hierarchyInfo, boolean isNew) {
	}
	public NodeModelDataFactory getFactoryToUseForChildOfParent(Object impl) {
		return this;
	}

//	public void fireCreated(Object newlyCreated) {
//	}

	public void remove(Object toRemove, NodeModel nodeModel, boolean deep,boolean undo,boolean removeDependencies){
		DependencyService.getInstance().remove((Dependency)toRemove,this,undo);
		logger.log(Level.FINE, "DependencyNodeModelDataFactory.remove");
	}

	public boolean isGroupDirty() {
		return false;
	}

	public void setGroupDirty(boolean isGroupDirty) {
	}

	public DataFactoryUndoController getUndoController() {
		return null;
	}

	public void rollbackUnvalidated(NodeModel nodeModel, Object object) {
	}

	public void initOutline(NodeModel nodeModel){}
	
	public boolean containsAssignments(){return false;}


}
