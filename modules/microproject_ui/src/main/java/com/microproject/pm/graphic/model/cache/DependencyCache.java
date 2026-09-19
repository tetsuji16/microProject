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
package com.microproject.pm.graphic.model.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;

import com.microproject.pm.graphic.model.event.CacheEvent;

/**
 *
 */
public class DependencyCache extends CellCache {

	public DependencyCache() {
		super();
	}
	
	public void updateAllVisibleElements(){
	    VisibleDependencies v;
	    for (Iterator i=visibleElements.iterator();i.hasNext();){
	        updateAllVisibleElements((VisibleDependencies)i.next());
	    }
	}
	public void updateAllVisibleElements(VisibleDependencies v){	    
		ArrayList visibleDependencies =v.getElements();
		ArrayList visibleNodes =v.getVisibleNodes().getElements();
		Collection visibleNodesCol=getContainsCollection(visibleNodes);
		visibleDependencies.clear();
		for(Iterator i=getCacheIterator();i.hasNext();){
			GraphicDependency dep=(GraphicDependency)i.next();
			if (visibleNodesCol.contains(dep.getPredecessor())&&
					visibleNodesCol.contains(dep.getSuccessor()))
			    visibleDependencies.add(dep);
		}
	}
	
	
	
	public void updateVisibleElements(Set change){
	    VisibleDependencies v;
	    for (Iterator i=visibleElements.iterator();i.hasNext();){
	        updateVisibleElements((VisibleDependencies)i.next(),change);
	    }
	}
	public void updateVisibleElements(VisibleDependencies v,Set change){
	    ArrayList visibleNodes =v.getVisibleNodes().getElements();
		ArrayList removed = new ArrayList();
		ArrayList inserted = new ArrayList();
		ArrayList changed = new ArrayList(change == null ? 0 : change.size());
		changed.addAll(change);
        updateVisibleElements(v.getElements(),visibleNodes,removed,inserted,changed);
		if (removed.size()>0) v.addEvent(new CacheEvent(this,CacheEvent.NODES_REMOVED,removed,null));
		if (inserted.size()>0) v.addEvent(new CacheEvent(this,CacheEvent.NODES_INSERTED,inserted,null));
		if (changed.size()>0) v.addEvent(new CacheEvent(this,CacheEvent.NODES_CHANGED,changed,null));
	}
	private void updateVisibleElements(ArrayList visibleDependencies, ArrayList visibleNodes, ArrayList removed, ArrayList inserted, ArrayList changed){
		Collection visibleNodesCol=getContainsCollection(visibleNodes);
		Collection visibleDependenciesCol=getContainsCollection(visibleDependencies);
		HashSet visibleDependenciesSet=(visibleDependenciesCol instanceof HashSet)?(HashSet)visibleDependenciesCol:new HashSet(visibleDependenciesCol);
		HashSet visibleNodesSet=(visibleNodesCol instanceof HashSet)?(HashSet)visibleNodesCol:new HashSet(visibleNodesCol);
		
//		long t0=System.currentTimeMillis();
		boolean containsPredecessor,containsSuccessor,containsDependency;
		for(Iterator i=getCacheIterator();i.hasNext();){
			GraphicDependency dep=(GraphicDependency)i.next();
			containsPredecessor=visibleNodesSet.contains(dep.getPredecessor());
			containsSuccessor=visibleNodesSet.contains(dep.getSuccessor());
			containsDependency=visibleDependenciesSet.contains(dep);
			
//System.out.println("contains " + dep.getPredecessor() + " / " + dep.getSuccessor() + " pred " + containsPredecessor + " succ " + containsSuccessor + " dep " + containsDependency);			
			if (containsPredecessor&&containsSuccessor&&!containsDependency){
			    visibleDependencies.add(dep);
			    visibleDependenciesSet.add(dep);
				inserted.add(dep);
				changed.remove(dep);
			}else if ((!containsPredecessor||
					!containsSuccessor)&&containsDependency){
			    visibleDependencies.remove(dep);
			    visibleDependenciesSet.remove(dep);
			    removed.add(dep);
				changed.remove(dep);
			}
		}
//		long t1=System.currentTimeMillis();
//		System.out.println("\t\tDependencyCache#1 ran in "+(t1-t0)+"ms");
		Collection cacheCol=getContainsCollection(cache);
		for(Iterator i=visibleDependencies.iterator();i.hasNext();){
			GraphicDependency dep=(GraphicDependency)i.next();
			if (!cacheCol.contains(dep)){
			    i.remove();
			    visibleDependenciesSet.remove(dep);
			    removed.add(dep);
				changed.remove(dep);
			}
		}
//		t0=System.currentTimeMillis();
//		System.out.println("\t\tDependencyCache#2 ran in "+(t0-t1)+"ms");

	}

	public Object getBase(Object base) {
		return ((GraphicDependency)base).getDependency();
	}
	
	
	
	/*protected void fireEdgesCreated(Object source, Object[] edges) {
	    for (Iterator i=visibleElements.iterator();i.hasNext();)
	        ((VisibleDependencies)i.next()).fireEdgesCreated(source,edges);
	}

	protected void fireEdgesRemoved(Object source, Object[] edges) {
	    for (Iterator i=visibleElements.iterator();i.hasNext();)
	        ((VisibleDependencies)i.next()).fireEdgesRemoved(source,edges);
	}

	protected void fireEdgesUpdated(Object source, Object[] edges) {
	    for (Iterator i=visibleElements.iterator();i.hasNext();)
	        ((VisibleDependencies)i.next()).fireEdgesUpdated(source,edges);
	}*/

	
	
}
