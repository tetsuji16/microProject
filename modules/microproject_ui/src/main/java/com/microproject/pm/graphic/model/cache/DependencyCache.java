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
public class DependencyCache extends CellCache<GraphicDependency, VisibleDependencies> {

	public DependencyCache() {
		super();
	}
	
	public void updateAllVisibleElements(){
	    for (VisibleDependencies element : visibleElements) {
	        updateAllVisibleElements(element);
	    }
	}
	public void updateAllVisibleElements(VisibleDependencies v){	    
		ArrayList<GraphicDependency> visibleDependencies =v.getElements();
		ArrayList<GraphicNode> visibleNodes =v.getVisibleNodes().getElements();
		Collection<GraphicNode> visibleNodesCol=getContainsCollection(visibleNodes);
		visibleDependencies.clear();
		for (GraphicDependency dep : cache) {
			if (visibleNodesCol.contains(dep.getPredecessor())&&
					visibleNodesCol.contains(dep.getSuccessor()))
			    visibleDependencies.add(dep);
		}
	}
	
	
	
	public void updateVisibleElements(Set<?> change){
	    for (VisibleDependencies element : visibleElements) {
	        updateVisibleElements(element,change);
	    }
	}
	public void updateVisibleElements(VisibleDependencies v,Set<?> change){
	    ArrayList<GraphicNode> visibleNodes =v.getVisibleNodes().getElements();
		ArrayList<GraphicDependency> removed = new ArrayList<>();
		ArrayList<GraphicDependency> inserted = new ArrayList<>();
		ArrayList<Object> changed = new ArrayList<>(change == null ? 0 : change.size());
		if (change != null) changed.addAll(change);
        updateVisibleElements(v.getElements(),visibleNodes,removed,inserted,changed);
		if (removed.size()>0) v.addEvent(new CacheEvent(this,CacheEvent.NODES_REMOVED,removed,null));
		if (inserted.size()>0) v.addEvent(new CacheEvent(this,CacheEvent.NODES_INSERTED,inserted,null));
		if (changed.size()>0) v.addEvent(new CacheEvent(this,CacheEvent.NODES_CHANGED,changed,null));
	}
	private void updateVisibleElements(ArrayList<GraphicDependency> visibleDependencies, ArrayList<GraphicNode> visibleNodes, ArrayList<GraphicDependency> removed, ArrayList<GraphicDependency> inserted, ArrayList<Object> changed){
		Collection<GraphicNode> visibleNodesCol=getContainsCollection(visibleNodes);
		Collection<GraphicDependency> visibleDependenciesCol=getContainsCollection(visibleDependencies);
		HashSet<GraphicDependency> visibleDependenciesSet=asHashSet(visibleDependenciesCol);
		HashSet<GraphicNode> visibleNodesSet=asHashSet(visibleNodesCol);
		
//		long t0=System.currentTimeMillis();
		boolean containsPredecessor,containsSuccessor,containsDependency;
		for (GraphicDependency dep : cache) {
			containsPredecessor=visibleNodesSet.contains(dep.getPredecessor());
			containsSuccessor=visibleNodesSet.contains(dep.getSuccessor());
			containsDependency=visibleDependenciesSet.contains(dep);
			
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
		Collection<GraphicDependency> cacheCol=getContainsCollection(cache);
		for(Iterator<GraphicDependency> i=visibleDependencies.iterator();i.hasNext();){
			GraphicDependency dep=i.next();
			if (!cacheCol.contains(dep)){
			    i.remove();
			    visibleDependenciesSet.remove(dep);
			    removed.add(dep);
				changed.remove(dep);
			}
		}
//		t0=System.currentTimeMillis();

	}

	@SuppressWarnings("unchecked")
	private static <T> HashSet<T> asHashSet(Collection<T> collection){
		return collection instanceof HashSet<?> ? (HashSet<T>)collection : new HashSet<>(collection);
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
