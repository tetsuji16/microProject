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
package com.microproject.pm.resource;

import java.util.ArrayList;

import com.microproject.grouping.core.model.DefaultNodeModel;
import com.microproject.undo.DataFactoryUndoController;



/**
 *
 */
public class ResourcePoolFactory {
	private static ResourcePoolFactory instance = null;
	private ArrayList resourcePools = new ArrayList();
	private String name;
	public static ResourcePoolFactory getInstance() {
		if (instance == null)
			instance = new ResourcePoolFactory();
		return instance;
	}

	public ResourcePool createResourcePool(String name,DataFactoryUndoController undo) {
		ResourcePool resourcePool=ResourcePool.createRourcePool(name, undo);
		addPool(resourcePool);
		((DefaultNodeModel)resourcePool.getResourceOutline()).setDataFactory(resourcePool);
		((DefaultNodeModel)resourcePool.getResourceOutline()).setUndoController(resourcePool.getUndoController());

		return resourcePool;
	}

	/**
	 * @return Returns the resourcePools.
	 */
	public ArrayList getResourcePools() {
		return resourcePools;
	}
	
	private void addPool(ResourcePool pool) {
		resourcePools.add(pool);
	}
	
	private void removePool(ResourcePool pool) {
		resourcePools.remove(pool);
	}


}
