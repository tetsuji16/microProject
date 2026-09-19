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

import java.util.EventListener;
import java.util.List;

import javax.swing.event.EventListenerList;

import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.model.transform.CacheTransformer;

/**
 *
 */
public class VisibleNodes extends VisibleElements {
    protected VisibleDependencies visibleDependencies;
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
	 protected void fireGraphicNodesCompositeEvent(Object source, List nodeEvents, List edgeEvents) {
			//System.out.println("fireGraphicNodesCompositeEvent: \n\t"+nodeEvents+"\n\t"+edgeEvents/*+", source="+source*/);
			Object[] listeners = listenerList.getListenerList();
			CompositeCacheEvent e = null;
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == CacheListener.class) {
					if (e == null) {
						e = new CompositeCacheEvent(source,
								nodeEvents,edgeEvents);
					}
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

