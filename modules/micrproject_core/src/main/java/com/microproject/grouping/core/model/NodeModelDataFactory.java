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

import com.microproject.undo.DataFactoryUndoController;

/**
 * Creates, validates, and removes backing objects for a {@link NodeModel}.
 */
public interface NodeModelDataFactory {
/**
 * Creates an unvalidated backing object for a child of the given parent.
 * The returned object is expected to be registered later through
 * {@link #addUnvalidatedObject(Object, NodeModel, Object)} and
 * {@link #validateObject(Object, NodeModel, Object, Object, boolean)}.
 * @param nodeModel node model requesting the object
 * @param parent parent implementation, or {@code null} for a root child
 * @return newly constructed object
 */
	Object createUnvalidatedObject(NodeModel nodeModel, Object parent);
	
	/**
	 * Registers an object that has been created but not yet validated.
	 * @param object backing object to register
	 * @param nodeModel node model that owns the object
	 * @param parent parent implementation, or {@code null} for a root child
	 */
	void addUnvalidatedObject(Object object,NodeModel nodeModel, Object parent);
 
 /**
  * Validates an object that was previously registered as unvalidated.
 * @param newlyCreated object to validate
 * @param nodeModel node model that owns the object
 * @param eventSource source of the change event
 * @param hierarchyInfo auxiliary hierarchy data associated with the object
 * @param isNew {@code true} when the object has just been created
  */	
 	void validateObject(Object newlyCreated, NodeModel nodeModel, Object eventSource, Object hierarchyInfo, boolean isNew);
 /**
  * Removes a backing object from the model.
 * @param toRemove backing object to remove
 * @param nodeModel node model that owns the object
 * @param deep whether child objects should be removed too
 * @param undo whether the removal is part of an undo action
 * @param cleanDependencies whether dependencies should be cleaned up
  */
	void remove(Object toRemove, NodeModel nodeModel,boolean deep,boolean undo,boolean cleanDependencies);
	
	
	
	
	public boolean isGroupDirty();
	public void setGroupDirty(boolean isGroupDirty);
	
	public DataFactoryUndoController getUndoController();
	

	public void rollbackUnvalidated(NodeModel nodeModel, Object object);
//	public void fireCreated(Object newlyCreated);
	
	public void initOutline(NodeModel nodeModel);
	NodeModelDataFactory getFactoryToUseForChildOfParent(Object impl);
	
	public boolean containsAssignments();


}
