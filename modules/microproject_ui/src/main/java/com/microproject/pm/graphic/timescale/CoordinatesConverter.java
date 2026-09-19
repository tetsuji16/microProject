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
package com.microproject.pm.graphic.timescale;

import java.io.Serializable;
import java.util.Calendar;
import java.util.EventListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.scheduling.ScheduleEventListener;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.task.Project;
import com.microproject.timescale.TimeInterval;
import com.microproject.timescale.TimeIterator;
import com.microproject.timescale.TimeScaleEvent;
import com.microproject.timescale.TimeScaleListener;
import com.microproject.timescale.TimeScaleManager;
import com.microproject.util.DateTime;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/**
 *
 */
public class CoordinatesConverter implements ScheduleEventListener, Serializable, SavableToWorkspace {
	private static final long serialVersionUID = 3657308109433257760L;
	private static final Logger logger = Logger.getLogger(CoordinatesConverter.class.getName());
	protected TimeScaleManager timescaleManager;
	protected long origin;
	protected long end;
	
	protected Project project;
	
	
	public CoordinatesConverter(Project project){
		this(project,TimeScaleManager.createInstance());
	}
	/**
	 * 
	 */
	public CoordinatesConverter(Project project,TimeScaleManager timescaleManager) {
		this.project=project;
		this.timescaleManager=timescaleManager;
		updateLargeInterval(false);
		project.addScheduleListener(this);
	}
	

	/**
	 * @return Returns the origin.
	 */
	public long getOrigin() {
		//adaptOrigin(getLargeStart(),true);
		return origin;
	}
    public long getEnd() {
		//adaptEnd(getLargeEnd(),true);
        return end;
    }
	/*public long getFloorOrigin() {
		long t=getTimescaleManager().getScale().floor1(origin);
		logger.log(Level.FINE, "Origin: {0}/{1}", new Object[] { CalendarUtil.toString(t), CalendarUtil.toString(origin) });
		return t;
	}
    public long getCeilEnd() {
		long t=getTimescaleManager().getScale().ceil1(end);
		logger.log(Level.FINE, "End: {0}/{1}", new Object[] { CalendarUtil.toString(end), CalendarUtil.toString(t) });
		return t;
    }*/
    
	protected void adaptOrigin(Calendar calendar,boolean event){
		//System.out.println("adaptOrigin: begin");
		getTimescaleManager().getScale().floor1(calendar,-1);
		
		long tmp = calendar.getTimeInMillis();
		if (this.origin!=tmp){
			//System.out.println("adaptOrigin: change: old="+CalendarUtil.toString(this.origin)+", new="+CalendarUtil.toString(tmp));
			this.origin=tmp;
			if (event) fireTimeScaleChanged(this,TimeScaleEvent.ORIGIN_AND_END_CHANGE);
		}
		//System.out.println("adaptOrigin: end");
	}
	protected void adaptEnd(Calendar calendar,boolean event){
		//System.out.println("adaptEnd: begin");
		getTimescaleManager().getScale().ceil1(calendar,-1);
		
		long tmp = calendar.getTimeInMillis();
		
		if (this.end!=tmp){
			//System.out.println("adaptEnd: change: old="+CalendarUtil.toString(this.end)+", new="+CalendarUtil.toString(tmp));
			this.end=tmp;
			if (event) fireTimeScaleChanged(this,TimeScaleEvent.END_ONLY_CHANGE);
		}
		//System.out.println("adaptEnd: end");
	}
	
	protected void adaptInterval(Calendar origin,Calendar end,boolean event){
		int modifType=0;
		long tmp=this.end;
		adaptEnd(end,false);
		if (this.end!=tmp)
			modifType=TimeScaleEvent.END_ONLY_CHANGE;
		tmp=this.origin;
		adaptOrigin(origin,false);
		if (this.origin!=tmp)
			modifType=TimeScaleEvent.ORIGIN_AND_END_CHANGE;
		if (modifType>0&&event) fireTimeScaleChanged(this,modifType);
		
	}
	
	private long getProjectStart(){
		long start=project.getEarliestStartingTaskOrStart();
		return (start==0)?System.currentTimeMillis():start;
	}
	private long getProjectEnd(){
		return project.getLatestFinishingTask();
	}
	
	private Calendar getLargeStart(){
		Calendar calendar=DateTime.calendarInstance();
		calendar.setTimeInMillis(getProjectStart());
		calendar.add(Calendar.DAY_OF_MONTH,-3);
		//CalendarUtil.roundTime(calendar);
		return calendar;
		
	}
	private Calendar getLargeEnd(){
		long end=getProjectEnd();
		Calendar calendar=DateTime.calendarInstance();
		calendar.setTimeInMillis(getProjectEnd());
		calendar.add(Calendar.DAY_OF_MONTH,30);
		//CalendarUtil.roundTime(calendar);
		return calendar;
	}

	/**
	 * Extends the scrollable view before the current origin without changing
	 * any project scheduling dates.  This is used when the user reaches the
	 * leading edge of a Gantt/timesheet view, matching Microsoft Project's
	 * ability to browse empty time before the first task.
	 */
	public void extendViewBefore(int days) {
		if (days <= 0 || origin == 0) return;
		Calendar calendar = DateTime.calendarInstance();
		calendar.setTimeInMillis(origin);
		calendar.add(Calendar.DAY_OF_MONTH, -days);
		adaptOrigin(calendar, true);
	}

	/**
	 * Extends the scrollable view after the current end without changing
	 * project scheduling dates.  This permits browsing empty time after the
	 * last task, as in Microsoft Project.
	 */
	public void extendViewAfter(int days) {
		if (days <= 0 || end == 0) return;
		Calendar calendar = DateTime.calendarInstance();
		calendar.setTimeInMillis(end);
		calendar.add(Calendar.DAY_OF_MONTH, days);
		adaptEnd(calendar, true);
	}
	
    protected void updateLargeInterval(boolean event){
    	adaptInterval(getLargeStart(),getLargeEnd(),event);
    	//System.out.println("updateLargeInterval: "+CalendarUtil.toString(getOrigin())+", "+CalendarUtil.toString(getEnd()));
    }
    
    
    public void toggleMinWidth(boolean normal){
    	if (timescaleManager.toggleMinWidth(normal)){
    		fireTimeScaleChanged(this,TimeScaleEvent.SCALE_CHANGE);
    	}
    }
   
    public boolean canZoomIn() {
    	return timescaleManager.canZoomIn();
    }
    public boolean canZoomOut() {
    	return timescaleManager.canZoomOut();
    }
    
	public void zoomIn(){
		if(timescaleManager.zoomIn()){
			updateLargeInterval(false);
			fireTimeScaleChanged(this,TimeScaleEvent.SCALE_CHANGE);
		}
	}
	
	public void zoomOut(){
		if(timescaleManager.zoomOut()){
			updateLargeInterval(false);
			fireTimeScaleChanged(this,TimeScaleEvent.SCALE_CHANGE);
		}
	}
	
	public void zoomReset(){
		if(timescaleManager.zoomReset()){
			updateLargeInterval(false);
			fireTimeScaleChanged(this,TimeScaleEvent.SCALE_CHANGE);
		}
	}
	public long getIntervalDuration() {
		return getTimescaleManager().getScale().getIntervalDuration(); 
	}
	/**
	 * @return Returns the timescaleManager.
	 */
	public TimeScaleManager getTimescaleManager() {
		return timescaleManager;
	}
	/**
	 * @param timescaleManager The timescaleManager to set.
	 */
	public void setTimescaleManager(TimeScaleManager timescaleManager) {
		this.timescaleManager = timescaleManager;
	}
	
	public double toTime(double x){
		return getOrigin()+timescaleManager.getScale().toTime(x);
	}
	public double toDuration(double w){
		return timescaleManager.getScale().toTime(w);
	}
	public double toX(double t){
		return timescaleManager.getScale().toX(t-getOrigin());
	}
	public double toW(double d){
		return timescaleManager.getScale().toX(d);
	}
	
	public double getWidth(){
		return toW(getEnd()-getOrigin());
	}
	
	public TimeIterator getTimeIterator(double x1,double x2){
		return new TimeIterator(toTime(x1),toTime(x2),timescaleManager.getScale(),getOrigin());
	}
	public TimeIterator getTimeIterator(double x1,double x2,boolean largeScale){
		return new TimeIterator(toTime(x1),toTime(x2),timescaleManager.getScale(),getOrigin(),largeScale);
	}

//	public TimeIterator getTimeIteratorFromDates(long start, long end){
//		return new TimeIterator(start,end,timescaleManager.getScale(),getOrigin());
//	}
	
	public TimeIterator getProjectTimeIterator(){
		return new TimeIterator(getOrigin(),getEnd(),timescaleManager.getScale(),getOrigin());
	}
	
	public int countProjectIntervals(){
		int count=0;
		TimeIterator iterator=getProjectTimeIterator();
		TimeInterval interval;
		while (iterator.hasNext()){
			interval=iterator.next();
			count++;
		}
		return count;
	}

	
	public void scheduleChanged(ScheduleEvent evt) {
		updateLargeInterval(true);
		//if project start or end have changed, it triggers a TimeScaleEvent
	}
	
	
	
	
	
	//events handling
	
	protected EventListenerList listenerList = new EventListenerList();

	public void addTimeScaleListener(TimeScaleListener l) {
		listenerList.add(TimeScaleListener.class, l);
	}
	public void removeTimeScaleListener(TimeScaleListener l) { 
		listenerList.remove(TimeScaleListener.class, l);
	}
	public TimeScaleListener[] getTimeScaleListeners() {
		return (TimeScaleListener[]) listenerList.getListeners(TimeScaleListener.class);
	}
	protected void fireTimeScaleChanged(Object source,int type) {
		Object[] listeners = listenerList.getListenerList();
		TimeScaleEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == TimeScaleListener.class) {
				if (e == null) {
					e = new TimeScaleEvent(source,type);
				}
				((TimeScaleListener) listeners[i + 1]).timeScaleChanged(e);
			}
		}
	}
    public EventListener[] getListeners(Class listenerType) { 
    	return listenerList.getListeners(listenerType); 
       }
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		timescaleManager.setCurrentScaleIndex(ws.currentScaleIndex);
		origin = ws.origin;
		end = ws.end;
	}
	
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.currentScaleIndex = timescaleManager.getCurrentScaleIndex();
		ws.origin = origin;
		ws.end = end;
		return ws;
	}
	public static class Workspace implements WorkspaceSetting { 
		private static final long serialVersionUID = -6767009284584575457L;
		int currentScaleIndex;	
		long origin;
		long end;
		public final int getCurrentScaleIndex() {
			return currentScaleIndex;
		}
		public final void setCurrentScaleIndex(int currentScaleIndex) {
			this.currentScaleIndex = currentScaleIndex;
		}
		public final long getEnd() {
			return end;
		}
		public final void setEnd(long end) {
			this.end = end;
		}
		public final long getOrigin() {
			return origin;
		}
		public final void setOrigin(long origin) {
			this.origin = origin;
		}
	}
	public Project getProject() {
		return project;
	}

    public static double adaptSmallBarEndX(double start,double end, GraphicNode node, GraphicConfiguration config){
    	if (config==null) config=GraphicConfiguration.getInstance();
    	if (config.getGanttBarMinWidth()==0 || node==null || node.getIntervalCount()>1) return end;
    	if (start<end && end-start<config.getGanttBarMinWidth() && config.getGanttBarMinWidth()>0
    			) return start+config.getGanttBarMinWidth();
    	else return end;
    }
    public ScheduleInterval adaptSmallBarTimeInterval(ScheduleInterval interval, GraphicNode node,GraphicConfiguration config){
    	if (config==null) config=GraphicConfiguration.getInstance();
    	if (config.getGanttBarMinWidth()==0 || node==null || node.getIntervalCount()>1) return interval;
    	if (config.getGanttBarMinWidth()>0){
	    	double minT=timescaleManager.getScale().toTime(config.getGanttBarMinWidth());
	    	if (interval.getStart()!=interval.getEnd() && interval.getEnd()-interval.getStart()<minT){
	    		return new ScheduleInterval(interval.getStart(),interval.getStart()+(long)minT);
	    	}
    	}
    	return interval;
    }
	
	
}

