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
package com.microproject.server.data.linker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.hierarchy.NodeHierarchy;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.Project;
/**
 *
 */
public abstract class ResourceLinker extends Linker {
	public void initIterator(){
		iterator=resourcesInExportOrder((Project)getParent()).iterator();
	}
	public Object executeNext(){
		return iterator.next();
	}

	/** Canonical resource traversal used by both MSPDI export and MPO sidecars. */
	public static List<ResourceImpl> resourcesInExportOrder(Project project) {
		ArrayList<ResourceImpl> result = new ArrayList<>();
		Map<ResourceImpl, Boolean> seen = new IdentityHashMap<>();
		for (java.util.Iterator<?> iterator = project.getResourcePool().getResourceOutline().iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			if (item instanceof Node node && node.getImpl() instanceof ResourceImpl resource)
				addResource(result, seen, resource);
		}
		// Keep detached pool members in the same order used by the export fallback.
		for (Resource item : project.getResourcePool().getResourceList())
			if (item instanceof ResourceImpl resource) addResource(result, seen, resource);
		return Collections.unmodifiableList(result);
	}

	private static void addResource(List<ResourceImpl> result, Map<ResourceImpl, Boolean> seen,
		ResourceImpl resource) {
		if (seen.put(resource, Boolean.TRUE) == null) result.add(resource);
	}
    public NodeHierarchy getHierarchy(){return ((Project)getParent()).getResourcePool().getResourceOutline().getHierarchy();}

}
