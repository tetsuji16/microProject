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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.microproject.pm.graphic.model.event.CacheEvent;



/**
 *
 */
public class NodeCache extends CellCache {

	public NodeCache() {
		super();
	}
	
	public void updateVisibleElements(Set updates){
	    //dumpVoids();
	    VisibleNodes v;
	    HashSet u=new HashSet();
	    for (Iterator i=visibleElements.iterator();i.hasNext();){
	        v=(VisibleNodes)i.next();
	        u.clear();
	        u.addAll(updates);
	        updateVisibleElements(v,u);
	    }
	}
	public void updateVisibleElements(VisibleNodes v, Set updates){
//		long t0=System.currentTimeMillis();

		ArrayList visibleElements =v.getElements();
	    ArrayList oldList =(ArrayList) visibleElements.clone();
		
		visibleElements.clear();
		int minLevel=-1;
		for(Iterator i=getCacheIterator();i.hasNext();){
			GraphicNode node=(GraphicNode)i.next();
			if (minLevel!=-1&&node.getLevel()>minLevel) continue;
			minLevel=-1;
			visibleElements.add(node);
			if (node.isComposite()&&v.isCollapsed(node)) minLevel=node.getLevel();
		}
//		long t1=System.currentTimeMillis();
//		System.out.println("\t\tcache NodeCache#1 ran in "+(t1-t0)+"ms");

		v.applyTransformer();
//		t0=System.currentTimeMillis();
//		System.out.println("\t\tcache NodeCache#2 ran in "+(t0-t1)+"ms");

		applyUpdates(oldList, visibleElements, updates, v.getEvents(), this);
//		t1=System.currentTimeMillis();
//		System.out.println("\t\tcache NodeCache#3 ran in "+(t1-t0)+"ms");

	}

	//for schedule caching option
//	public void updateCachedSchedule(){
//		GraphicNode node;
//		for (Iterator i=cache.iterator();i.hasNext();){
//			node=(GraphicNode)i.next();
//			node.updateScheduleCache();
//		}
//	}

	
	public static void applyUpdates(ArrayList oldList, ArrayList newList, Set updates, List events, Object source){
	    ArrayList o =(ArrayList) oldList.clone();
		ArrayList n =(ArrayList) newList.clone();
		
//		long t0=System.currentTimeMillis();
		ArrayList removeList =null;
		ArrayList removeNodeList =null;
		//if (removeFunctor!=null){
			removeList=new ArrayList();
			removeNodeList=new ArrayList();
			createRemoveDiff(o,n,removeNodeList,removeList,updates);
			if (removeList.size()>0){
				//removeFunctor.execute(removeNodeList,removeList);
			    events.add(new CacheEvent(source,CacheEvent.NODES_REMOVED,(List)removeNodeList.clone(),(List)removeList.clone()));
			}
		//}
//			long t1=System.currentTimeMillis();
//			System.out.println("\t\t\tcache applyUpdates#1 ran in "+(t1-t0)+"ms");
		
		ArrayList insertList =null;
		ArrayList insertNodeList =null;
		//if (insertFunctor!=null){
			insertList=new ArrayList();
			insertNodeList=new ArrayList();
			createRemoveDiff(n,o,insertNodeList,insertList,updates);
			if (insertList.size()>0){
			    events.add(new CacheEvent(source,CacheEvent.NODES_INSERTED,(List)insertNodeList.clone(),(List)insertList.clone()));
				//insertFunctor.execute(insertNodeList,insertList);
			}
		//}
//			t0=System.currentTimeMillis();
//			System.out.println("\t\t\tcache applyUpdates#2 ran in "+(t0-t1)+"ms");
		
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
			    events.add(new CacheEvent(source,CacheEvent.NODES_INSERTED,(List)insertNodeList.clone(),(List)insertList.clone()));
				//insertFunctor.execute(insertNodeList,insertList);
			}
//			t1=System.currentTimeMillis();
//			System.out.println("\t\t\tcache applyUpdates#3 ran in "+(t1-t0)+"ms");
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
//			System.out.println("\t\t\tcache applyUpdates#4 ran in "+(t0-t1)+"ms");
		
	}
	
	
	protected static void createRemoveDiff(ArrayList oldList, ArrayList newList, ArrayList nodeDiff, ArrayList intervaldiff,Set updates){
		Collection newCol=getContainsCollection(newList);
		int row=0;
		int begin=-1;
		int end=-1;
		Object current;
		for (ListIterator i=oldList.listIterator();i.hasNext();row++){
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
	
	
	protected static void createPermutationDiff(ArrayList oldList, ArrayList newList, ArrayList removeNodeList, ArrayList insertNodeList, ArrayList removeIntervalList, ArrayList insertIntervalList,
			Set updates){
	    //oldList and newList have the same size and contains the same elements
	    ListIterator o=oldList.listIterator();
	    ListIterator n=newList.listIterator();
	    int startRow=-1;;
	    for(int row=0;o.hasNext();row++){
	        Object oelement=o.next();
	        Object nelement=n.next();
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
	
	
	protected static void createUpdateDiff(ArrayList newList, ArrayList nodeDiff, ArrayList diff,Set updates){
		if (updates!=null&&updates.size()>0){
			Collection updatesCol=getContainsCollection(updates);
			int begin=-1;
			int end=-1;
			int row=0;
			Object current;
			for (Iterator i=newList.iterator();i.hasNext();row++){
				if (updatesCol.contains(current=i.next())){
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
			}		
			if (begin!=-1) diff.add(new CacheInterval(begin,end));
		}
		
	}
	
	public Object getBase(Object base) {
		return ((GraphicNode)base).getNode();
	}
	
//	
//	private void dumpVoids(){
//	    Object current;
//	    List vn;
//	    GraphicNode node;
//	    for (Iterator i=voidNodes.keySet().iterator();i.hasNext();){
//	        current=i.next();
//	        System.out.println(current+":");
//	        vn=(List)voidNodes.get(current);
//	        for (Iterator j=vn.iterator();j.hasNext();){
//	            node=(GraphicNode)j.next();
//	            System.out.println("\t"+node+": "+node.getLevel());
//	        }
//	    }
//	}

	protected void fireEvents(Object source, List nodeEvents, List edgeEvents) {
        if (nodeEvents.size()>0||edgeEvents.size()>0)
	    for (Iterator i=visibleElements.iterator();i.hasNext();)
	        ((VisibleNodes)i.next()).fireGraphicNodesCompositeEvent(source,nodeEvents,edgeEvents);
	}
//	protected void fireScheduleEvent(Object source, ScheduleEvent scheduleEvent) {
//	    for (Iterator i=visibleElements.iterator();i.hasNext();)
//	        ((VisibleNodes)i.next()).fireGraphicNodesCompositeEvent(source,null,null,scheduleEvent,null);
//	}
//	protected void fireObjectEvent(Object source, ObjectEvent objectEvent) {
//	    for (Iterator i=visibleElements.iterator();i.hasNext();)
//	        ((VisibleNodes)i.next()).fireGraphicNodesCompositeEvent(source,null,null,null, objectEvent);
//	}
	public void fireEvents(Object source, VisibleNodes nodes) {
        List nodeEvents=nodes.getEvents();
        List edgeEvents=nodes.getVisibleDependencies().getEvents();
        if (nodeEvents.size()>0||edgeEvents.size()>0){
		    nodes.fireGraphicNodesCompositeEvent(source,nodeEvents,edgeEvents);
	        nodes.clearEvents();
	        nodes.getVisibleDependencies().clearEvents();
	
        }
	}
	public void fireEvents(Object source) {
	    for (Iterator i=visibleElements.iterator();i.hasNext();){
	        VisibleNodes v=(VisibleNodes)i.next();
	        List nodeEvents=v.getEvents();
	        List edgeEvents=v.getVisibleDependencies().getEvents();
	        if (nodeEvents.size()>0||edgeEvents.size()>0){
	            v.fireGraphicNodesCompositeEvent(source,nodeEvents,edgeEvents);
	            v.clearEvents();
	            v.getVisibleDependencies().clearEvents();
	        }
	    }
	}

}
