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
package com.microproject.timescale;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.digester.Digester;

import com.microproject.configuration.Configuration;

/**
 *
 */
public class TimeScaleManager {
	private static final int MIN_ZOOM_IN_SCALE_INDEX = 2;
	
	protected int currentScaleIndex=-1;
	protected List scales;
	protected int defaultIndex;
	protected boolean normalWidth;
	/**
	 * 
	 */
	public TimeScaleManager() {
		scales=new LinkedList();
		normalWidth=true;
	}
	
	public void addTimeScale(TimeScale scale){
		scales.add(scale);
	}
	
	/**
	 * @return Returns the scale.
	 */
	public TimeScale getScale() {
		return (TimeScale)scales.get(getCurrentScaleIndex());
	}
	
	public int getMinWidth() {
		return getScale().getMinWidth();
	}
		
	/**
	 * @return Returns the defaultScaleIndex.
	 */
	public int getDefaultIndex() {
		return defaultIndex;
	}
	/**
	 * @param defaultScaleIndex The defaultScaleIndex to set.
	 */
	public void setDefaultIndex(int defaultIndex) {
		this.defaultIndex = defaultIndex;
	}
	
	/**
	 * @return Returns the currentScaleIndex.
	 */
	public int getCurrentScaleIndex() {
		if (currentScaleIndex==-1) currentScaleIndex=defaultIndex;
		return currentScaleIndex;
	}

	public int getScaleCount() {
		return scales == null ? 0 : scales.size();
	}
	
	
    public boolean toggleMinWidth(boolean normal){
    	if (normal!=normalWidth){
    		normalWidth=normal;
    		for (Iterator i=scales.iterator();i.hasNext();){
    			TimeScale scale=(TimeScale)i.next();
    			scale.toggleWidth(normal);
    		}
    		return true;
    	}else return false;
    }

    public boolean isShowWholeDays(){
    	return getCurrentScaleIndex()<=2;
    }
	
	public boolean canZoomIn() {
		return getCurrentScaleIndex()>MIN_ZOOM_IN_SCALE_INDEX;
	}
	public boolean canZoomOut() {
		return getCurrentScaleIndex()<scales.size()-1;
	}
	public boolean zoomIn(){
		if (getCurrentScaleIndex()>MIN_ZOOM_IN_SCALE_INDEX){
			currentScaleIndex--;
			return true;
			//fireTimeScaleChanged(this);
		}else return false;
	}
	
	public boolean zoomOut(){
		if (getCurrentScaleIndex()<scales.size()-1){
			currentScaleIndex++;
			return true;
			//fireTimeScaleChanged(this);
		}else return false;
	}
	public boolean zoomReset(){
		if (currentScaleIndex!=defaultIndex){
			currentScaleIndex=defaultIndex;
			return true;
			//fireTimeScaleChanged(this);
		}else return false;
	}
	
	/*public static TimeScaleManager getInstance(){
		return Configuration.getInstance().getTimeScales();
	}*/
	public static TimeScaleManager createInstance(){
		TimeScaleManager tsManager=new TimeScaleManager();
		TimeScaleManager ref=Configuration.getInstance().getTimeScales();
		tsManager.defaultIndex=ref.defaultIndex;
		tsManager.currentScaleIndex=ref.currentScaleIndex;
		for (Iterator i=ref.scales.iterator();i.hasNext();){
			tsManager.scales.add(((TimeScale)i.next()).clone());
		}
		return tsManager;
	}
	
	public static void addDigesterEvents(Digester digester){
		digester.addObjectCreate("*/timescales", "com.microproject.timescale.TimeScaleManager");
	    digester.addSetProperties("*/timescales");
		digester.addSetNext("*/timescales", "setTimeScales", "com.microproject.timescale.TimeScaleManager");
		
		digester.addObjectCreate("*/timescales/timescale", "com.microproject.timescale.TimeScale");
	    digester.addSetProperties("*/timescales/timescale");
		digester.addSetNext("*/timescales/timescale", "addTimeScale", "com.microproject.timescale.TimeScale");

	}

	public final void setCurrentScaleIndex(int currentScaleIndex) {
		this.currentScaleIndex = currentScaleIndex;
	}
	
	
	
	
	
	
	//events handling
	
	/*protected EventListenerList listenerList = new EventListenerList();

	public void addTimeScaleListener(TimeScaleListener l) {
		listenerList.add(TimeScaleListener.class, l);
	}
	public void removeTimeScaleListener(TimeScaleListener l) {
		listenerList.remove(TimeScaleListener.class, l);
	}
	public TimeScaleListener[] getTimeScaleListeners() {
		return (TimeScaleListener[]) listenerList.getListeners(TimeScaleListener.class);
	}
	protected void fireTimeScaleChanged(Object source) {
		Object[] listeners = listenerList.getListenerList();
		TimeScaleEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TimeScaleListener.class) {
				if (e == null) {
					e = new TimeScaleEvent(source);
				}
				((TimeScaleListener) listeners[i + 1]).timeScaleChanged(e);
			}
		}
	}
    public EventListener[] getListeners(Class listenerType) { 
    	return listenerList.getListeners(listenerType); 
       }
	*/	

	
}
