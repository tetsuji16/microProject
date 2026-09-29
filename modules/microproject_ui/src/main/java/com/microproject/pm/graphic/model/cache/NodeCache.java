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
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.microproject.pm.graphic.model.event.CacheEvent;



/**
 *
 */
public class NodeCache extends CellCache<GraphicNode, VisibleNodes> {

	public NodeCache() {
		super();
	}
	
	public void updateVisibleElements(Set<?> updates){
	    HashSet<Object> u=new HashSet<>(updates == null ? 0 : updates.size());
	    for (VisibleNodes v : visibleElements) {
	        u.clear();
	        if (updates != null) u.addAll(updates);
	        updateVisibleElements(v,u);
	    }
	}
	public void updateVisibleElements(VisibleNodes v, Set<?> updates){
//		long t0=System.currentTimeMillis();

		ArrayList<GraphicNode> visibleElements =v.getElements();
	    ArrayList<GraphicNode> oldList =new ArrayList<>(visibleElements);
		
		visibleElements.clear();
		int minLevel=-1;
		for (GraphicNode node : cache) {
			if (minLevel!=-1&&node.getLevel()>minLevel) continue;
			minLevel=-1;
			visibleElements.add(node);
			if (node.isComposite()&&node.isCollapsed()) minLevel=node.getLevel();
		}
//		long t1=System.currentTimeMillis();

		v.applyTransformer();
//		t0=System.currentTimeMillis();

		applyUpdates(oldList, visibleElements, updates, v.getEvents(), this);
//		t1=System.currentTimeMillis();

	}

	public static <T> void applyUpdates(ArrayList<T> oldList, ArrayList<T> newList, Set<?> updates, List<CacheEvent> events, Object source){
	    ArrayList<T> o =new ArrayList<>(oldList);
		ArrayList<T> n =new ArrayList<>(newList);
		
//		long t0=System.currentTimeMillis();
		ArrayList<CacheInterval> removeList =null;
		ArrayList<T> removeNodeList =null;
		//if (removeFunctor!=null){
			removeList=new ArrayList<>();
			removeNodeList=new ArrayList<>();
			createRemoveDiff(o,n,removeNodeList,removeList,updates);
			if (removeList.size()>0){
				//removeFunctor.execute(removeNodeList,removeList);
			    events.add(new CacheEvent(source,CacheEvent.NODES_REMOVED,new ArrayList<>(removeNodeList),new ArrayList<>(removeList)));
			}
		//}
//			long t1=System.currentTimeMillis();
		
		ArrayList<CacheInterval> insertList =null;
		ArrayList<T> insertNodeList =null;
		//if (insertFunctor!=null){
			insertList=new ArrayList<>();
			insertNodeList=new ArrayList<>();
			createRemoveDiff(n,o,insertNodeList,insertList,updates);
			if (insertList.size()>0){
			    events.add(new CacheEvent(source,CacheEvent.NODES_INSERTED,new ArrayList<>(insertNodeList),new ArrayList<>(insertList)));
				//insertFunctor.execute(insertNodeList,insertList);
			}
		//}
//			t0=System.currentTimeMillis();
		
		//if (removeFunctor!=null&&insertFunctor!=null){
			removeList.clear();
			removeNodeList.clear();
			insertList.clear();
			insertNodeList.clear();
			createPermutationDiff(o,n,removeNodeList,insertNodeList,removeList,insertList,updates);
			if (removeList.size()>0){
			    events.add(new CacheEvent(source,CacheEvent.NODES_REMOVED,removeNodeList,removeList));
				//removeFunctor.execute(removeNodeList,removeList);
			}
			if (insertList.size()>0){
			    events.add(new CacheEvent(source,CacheEvent.NODES_INSERTED,new ArrayList<>(insertNodeList),new ArrayList<>(insertList)));
				//insertFunctor.execute(insertNodeList,insertList);
			}
//			t1=System.currentTimeMillis();
		//}
		
		//if (updateFunctor!=null){
			insertList.clear();
			insertNodeList.clear();
			createUpdateDiff(newList,insertNodeList,insertList,updates);
			if (insertList.size()>0){
			    events.add(new CacheEvent(source,CacheEvent.NODES_CHANGED,insertNodeList,insertList));
			    //updateFunctor.execute(insertNodeList,insertList);
			}
		//}
//			t0=System.currentTimeMillis();
		
	}
	
	
	protected static <T> void createRemoveDiff(ArrayList<T> oldList, ArrayList<T> newList, ArrayList<T> nodeDiff, ArrayList<CacheInterval> intervaldiff,Set<?> updates){
		Collection<T> newCol=getContainsCollection(newList);
		int row=0;
		int begin=-1;
		int end=-1;
		T current;
		for (ListIterator<T> i=oldList.listIterator();i.hasNext();row++){
			if (!newCol.contains(current=i.next())){
				nodeDiff.add(current);
				if (updates!=null) updates.remove(current); //to avoid remove/insert followed by update
				if (begin==-1){
					begin=row;
					end=row;
				}else{
					if (row==end+1) end=row;
					else{
						intervaldiff.add(new CacheInterval(begin,end));
						begin=row;
						end=row;
					}
				}
				i.remove();
			}
		}
		if (begin!=-1) intervaldiff.add(new CacheInterval(begin,end));
	}
	
	
	protected static <T> void createPermutationDiff(ArrayList<T> oldList, ArrayList<T> newList, ArrayList<T> removeNodeList, ArrayList<T> insertNodeList, ArrayList<CacheInterval> removeIntervalList, ArrayList<CacheInterval> insertIntervalList,
			Set<?> updates){
	    //oldList and newList have the same size and contains the same elements
	    ListIterator<T> o=oldList.listIterator();
	    ListIterator<T> n=newList.listIterator();
	    int startRow=-1;;
	    for(int row=0;o.hasNext();row++){
	        T oelement=o.next();
	        T nelement=n.next();
	        if (oelement.equals(nelement)){
	            if (startRow!=-1&&startRow<row){
	                CacheInterval interval=new CacheInterval(startRow,row-1);
	                removeIntervalList.add(interval);
	                insertIntervalList.add(interval);
	                startRow=-1;
	            }
	        }else{
	            if (startRow==-1) startRow=row;
	            removeNodeList.add(oelement);
	            insertNodeList.add(nelement);
	        }
	    }
        if (startRow!=-1){
            CacheInterval interval=new CacheInterval(startRow,oldList.size()-1);
            removeIntervalList.add(interval);
            insertIntervalList.add(interval);
        }
	}
	
	
	protected static <T> void createUpdateDiff(ArrayList<T> newList, ArrayList<T> nodeDiff, ArrayList<CacheInterval> diff,Set<?> updates){
		if (updates!=null&&updates.size()>0){
			Collection<?> updatesCol=getContainsCollection(updates);
			int begin=-1;
			int end=-1;
			int row=0;
			for (T current : newList) {
				if (updatesCol.contains(current)){
				    nodeDiff.add(current);
					if (begin==-1){
						begin=row;
						end=row;
					}else{
						if (row==end+1) end=row;
						else{
							diff.add(new CacheInterval(begin,end));
							begin=row;
							end=row;
						}
					}
				}
				row++;
			}		
			if (begin!=-1) diff.add(new CacheInterval(begin,end));
		}
		
	}
	
	public Object getBase(Object base) {
		return ((GraphicNode)base).getNode();
	}
	
	protected void fireEvents(Object source, List<CacheEvent> nodeEvents, List<CacheEvent> edgeEvents) {
        if (nodeEvents.size()>0||edgeEvents.size()>0)
	    for (VisibleNodes element : visibleElements)
	        element.fireGraphicNodesCompositeEvent(source,nodeEvents,edgeEvents);
	}
	public void fireEvents(Object source, VisibleNodes nodes) {
	    List<CacheEvent> nodeEvents=nodes.getEvents();
	    List<CacheEvent> edgeEvents=nodes.getVisibleDependencies().getEvents();
        if (nodeEvents.size()>0||edgeEvents.size()>0){
		    nodes.fireGraphicNodesCompositeEvent(source,nodeEvents,edgeEvents);
	        nodes.clearEvents();
	        nodes.getVisibleDependencies().clearEvents();
	
        }
	}
	public void fireEvents(Object source) {
	    for (VisibleNodes v : visibleElements) {
	        List<CacheEvent> nodeEvents=v.getEvents();
	        List<CacheEvent> edgeEvents=v.getVisibleDependencies().getEvents();
	        if (nodeEvents.size()>0||edgeEvents.size()>0){
	            v.fireGraphicNodesCompositeEvent(source,nodeEvents,edgeEvents);
	            v.clearEvents();
	            v.getVisibleDependencies().clearEvents();
	        }
	    }
	}

}

