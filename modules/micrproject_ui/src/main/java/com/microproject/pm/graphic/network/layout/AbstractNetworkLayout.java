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
package com.microproject.pm.graphic.network.layout;

import java.awt.Rectangle;
import java.util.EventListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.swing.event.EventListenerList;

import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.network.NetworkParams;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.document.ObjectEvent;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.scheduling.ScheduleEvent;

/**
 *
 */
public abstract class AbstractNetworkLayout implements NetworkLayout {
	protected NodeModelCache cache;
	protected BarStyles barStyles;
	protected Rectangle bounds;
	protected NetworkParams network;
	private final Map<GraphicNode, ViewShape> shapes = new IdentityHashMap<>();
	private record ViewShape(GeneralPath path, Point2D center) { }
	public AbstractNetworkLayout(NetworkParams network){
		this.network=network;
		addNetworkLayoutListener(network);
	}
	public void setCache(NodeModelCache cache){
		this.cache=cache;
		bounds=new Rectangle();
	}
	public void setBarStyles(BarStyles barStyles) {
		this.barStyles = barStyles;
	}
	
	public abstract void updateBounds();
	
	
	
	public Rectangle getBounds() {
		return bounds;
	}
	protected void setEmpty(){
		bounds.setSize(0,0);
	}
	protected boolean isEmpty(){
		return bounds.isEmpty();
	}
	public GeneralPath getShape(GraphicNode node) { ViewShape value = shapes.get(node); return value == null ? null : value.path(); }
	public Point2D getCenter(GraphicNode node) { ViewShape value = shapes.get(node); return value == null ? null : value.center(); }
	protected void setShape(GraphicNode node, GeneralPath path, double centerX, double centerY) {
		shapes.put(node, new ViewShape(path, new Point2D.Double(centerX, centerY)));
	}
	public void translateShape(GraphicNode node, double dx, double dy) {
		ViewShape value = shapes.get(node);
		if (value == null) return;
		value.path().transform(AffineTransform.getTranslateInstance(dx, dy));
		value.center().setLocation(value.center().getX() + dx, value.center().getY() + dy);
	}
	
	public void scheduleChanged(ScheduleEvent evt) {
	}
	public void objectChanged(ObjectEvent objectEvent) {
	}
	public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent) {
	}
	
	
	
	protected EventListenerList listenerList = new EventListenerList();

	public void addNetworkLayoutListener(NetworkLayoutListener l) {
		listenerList.add(NetworkLayoutListener.class, l);
	}
	public void removeNetworkLayoutListener(NetworkLayoutListener l) { 
		listenerList.remove(NetworkLayoutListener.class, l);
	}
	public NetworkLayoutListener[] getNetworkLayoutListeners() {
		return (NetworkLayoutListener[]) listenerList.getListeners(NetworkLayoutListener.class);
	}
	protected void fireLayoutChanged() {
		Object[] listeners = listenerList.getListenerList();
		NetworkLayoutEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == NetworkLayoutListener.class) {
				if (e == null) {
					e = new NetworkLayoutEvent(this);
				}
				((NetworkLayoutListener) listeners[i + 1]).layoutChanged(e);
			}
		}
	}
    public EventListener[] getListeners(Class listenerType) { 
    	return listenerList.getListeners(listenerType); 
       }

	
}
