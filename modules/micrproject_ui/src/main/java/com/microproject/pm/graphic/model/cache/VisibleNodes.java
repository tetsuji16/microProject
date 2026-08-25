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
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.event.EventListenerList;

import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.pm.graphic.model.event.CacheEvent;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.model.transform.CacheTransformer;

/**
 *
 */
public class VisibleNodes extends VisibleElements {
    protected VisibleDependencies visibleDependencies;
	private Consumer<Runnable> listenerDispatcher = Runnable::run;
	private final Map<Object, Boolean> collapsed = new java.util.HashMap<>();
	private final ProjectionRowKeyResolver collapseKeys = new ProjectionRowKeyResolver();
    /**
     * @param transformer
     */
    public VisibleNodes(String viewName,CacheTransformer transformer) {
        super(viewName,transformer);
    }
    
    
    
    public VisibleDependencies getVisibleDependencies() {
        return visibleDependencies;
    }
    public void setVisibleDependencies(VisibleDependencies visibleDependencies) {
        this.visibleDependencies = visibleDependencies;
    }
    

    
    
    
	protected EventListenerList listenerList = new EventListenerList();

	public void addNodeModelListener(CacheListener l) {
		listenerList.add(CacheListener.class, l);
	}
	public void removeNodeModelListener(CacheListener l) {
		listenerList.remove(CacheListener.class, l);
	}
	public CacheListener[] getNodeModelListeners() {
		return (CacheListener[]) listenerList.getListeners(CacheListener.class);
	}
	void setListenerDispatcher(Consumer<Runnable> dispatcher) {
		listenerDispatcher = dispatcher == null ? Runnable::run : dispatcher;
	}
	boolean isCollapsed(GraphicNode node) {
		return node != null && collapsed.computeIfAbsent(collapseKey(node), ignored -> node.isCollapsed()).booleanValue();
	}
	void setCollapsed(GraphicNode node, boolean value) {
		if (node != null) collapsed.put(collapseKey(node), Boolean.valueOf(value));
	}
	private Object collapseKey(GraphicNode node) {
		if (node.isGroup() && getTransformer() instanceof com.microproject.pm.graphic.model.transform.NodeCacheTransformer transformer) {
			String identity = transformer.getSyntheticGroupIdentity(node);
			if (identity != null) return "GROUP:" + identity;
		}
		return collapseKeys.resolve(node);
	}
	void clearViewState() { collapsed.clear(); }

	 protected void fireGraphicNodesCompositeEvent(Object source, List nodeEvents, List edgeEvents) {
			//System.out.println("fireGraphicNodesCompositeEvent: \n\t"+nodeEvents+"\n\t"+edgeEvents/*+", source="+source*/);
			List nodeSnapshot = copyEvents(nodeEvents);
			List edgeSnapshot = copyEvents(edgeEvents);
			listenerDispatcher.accept(() -> notifyListeners(source, nodeSnapshot, edgeSnapshot));
		}

	private static List copyEvents(List events) {
		if (events == null) return null;
		// Legacy caches clear/reuse their event lists immediately after dispatch.
		// ArrayList.toArray uses one bulk copy and does not expose a fail-fast
		// iterator to that concurrent clear.
		Object[] eventValues = events.toArray();
		List<CacheEvent> snapshot = new ArrayList<>(eventValues.length);
		for (Object value : eventValues) {
			if (!(value instanceof CacheEvent event)) continue;
			List nodes = event.getNodes() == null ? null
					: new ArrayList(java.util.Arrays.asList(event.getNodes().toArray()));
			List intervals = new ArrayList();
			event.forIntervals(intervals::add);
			snapshot.add(new CacheEvent(event.getSource(), event.getType(), nodes, intervals));
		}
		return List.copyOf(snapshot);
	}

	private void notifyListeners(Object source, List nodeEvents, List edgeEvents) {
		Object[] listeners = listenerList.getListenerList();
		CompositeCacheEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == CacheListener.class) {
				if (e == null)
					e = new CompositeCacheEvent(source, nodeEvents, edgeEvents);
				((CacheListener) listeners[i + 1]).graphicNodesCompositeEvent(e);
			}
		}
	}



    public EventListener[] getListeners(Class listenerType) { 
    	return listenerList.getListeners(listenerType); 
       }

    
    public String toString(){
    	return "VisibleNodes:"+super.toString();
    }

}
