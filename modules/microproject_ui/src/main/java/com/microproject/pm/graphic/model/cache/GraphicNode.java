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

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.scheduling.IntervalConsumer;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.grouping.core.GroupNodeImpl;
import com.microproject.grouping.core.LazyParent;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.transform.HierarchicObject;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.pm.task.Task;
import com.microproject.server.data.CommonDataObject;
import com.microproject.server.data.DataObject;
import com.microproject.util.GanttProgress;
/**
 *
 */
public class GraphicNode implements HierarchicObject{
	protected Node node;
	protected int level;
	protected int pertLevel;
	protected boolean voidNode;
	protected boolean composite;
	protected boolean summary;
	protected boolean collapsed;
	protected boolean dirty;



	/**
	 * @param node
	 * @param level
	 */
	public GraphicNode(Node node, int level) {
		setNode(node);
		this.level = level;
		dirty=false;
		pertLevel=-1;
		setScheduleCaching(false);
	}

	/**
	 * @return Returns the level.
	 */
	public int getLevel() {
		return level;
	}
	/**
	 * @param level The level to set.
	 */
	void setLevel(int level) {
		this.level = level;
		dirty=true;
	}

    public int getPertLevel() {
        return pertLevel;
    }
    void setPertLevel(int pertLevel) {
        this.pertLevel = pertLevel;
    }
	/**
	 * @return Returns the node.
	 */
	public Node getNode() {
		return node;
	}
	/**
	 * @param node The node to set.
	 */
	public void setNode(Node node) {
		this.node = node;
		dirty=true;
	}

	/**
	 * @return Returns the composite.
	 */
	public  boolean isComposite() {
		return composite;
	}
	/**
	 * @param composite The composite to set.
	 */
	public void setComposite(boolean composite) {
		this.composite = composite;
		dirty=true;
	}

	public boolean isSummary() {
		return summary;
	}
	public void setSummary(boolean summary) {
		this.summary = summary;
		dirty=true;
	}
	public boolean isLazyParent() {
		return node.getImpl() instanceof LazyParent;
	}
	public boolean isValidLazyParent() {
		if (node.getImpl() instanceof LazyParent)
			return ((LazyParent)node.getImpl()).isValid();
		return false;
	}
	public boolean isFetched() {
		if (node.getImpl() instanceof LazyParent)
			return ((LazyParent)node.getImpl()).isDataFetched();
		else
			return true;
	}
	public boolean fetch() {
		if (node.getImpl() instanceof LazyParent)
			return ((LazyParent)node.getImpl()).fetchData(node);
		return true;
	}
	/**
	 * @return Returns the collapsed.
	 */
	public boolean isCollapsed() {
		return collapsed;
	}
	/**
	 * @param collapsed The collapsed to set.
	 */
	public void setCollapsed(boolean collapsed) {
		this.collapsed = collapsed;
		dirty=true;
	}

	public boolean isVoid(){
	    return voidNode;//getNode().isVoid();
	}
	public void setVoid(boolean voidNode) {
		this.voidNode=voidNode;
		dirty=true;
	}


	public boolean isAssignment(){
	    return getNode().getImpl() instanceof Assignment;
	}
	public boolean isGroup(){
	    return getNode().getImpl() instanceof GroupNodeImpl;
	}
	public int getSubprojectLevel(){
//		if (getNode().getImpl() instanceof Task){
//			Task task=(Task)getNode().getImpl();
//			if (task.isInSubproject()) return 1;
//		}

//		Node node=getNode();
//		int level=0;
//		while (node.isInSubproject()){
//			node=(Node)node.getParent();
//			level+=1;
//		}
//		return level;

		//if (getNode().isInSubproject()) return 1;
		//return 0;

		return node.getSubprojectLevel();
	}

	public boolean isLinkable() {
		Object impl = getNode().getImpl();
		if (impl instanceof Assignment)
			return false;
		if (impl instanceof Task && ((Task)impl).isExternal())
			return false;
		return true;
	}
	public boolean isServer(){
	    Object impl=getNode().getImpl();
	    if (!(impl instanceof DataObject)) return false;
	    return !CommonDataObject.isLocal((DataObject)impl);
	}


	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
//		System.out.println("GraphicNode _setDirty");
		this.dirty = dirty;
	}

	public String toString(){
	    return node.toString();
	}


	public static Object getImpl(Object obj) {
		if (obj instanceof GraphicNode)
			return ((GraphicNode)obj).getNode().getImpl();
		else if (obj instanceof Node)
			return ((Node)obj).getImpl();
		else
			return obj;
	}

	public static boolean isVoid(Object obj) {
		if (obj instanceof GraphicNode)
			return ((GraphicNode)obj).isVoid();
		else if (obj instanceof Node)
			return ((Node)obj).isVoid();
		else
			return obj == null;
	}



	protected boolean scheduleCaching;
	protected ArrayList intervals =null;
	protected long start=-1;
	protected long end=-1;
	protected int intervalCount=1;

	public long getStart(){
		return (scheduleCaching||!isSchedule())?start:((Schedule)node.getImpl()).getStart();
	}
	public long getEnd(){
		return (scheduleCaching||!isSchedule())?end:((Schedule)node.getImpl()).getEnd();
	}

	public int getIntervalCount() {
		return intervalCount;
	}

	public boolean isScheduleCaching() {
		return scheduleCaching;
	}
	public void setScheduleCaching(boolean scheduleCaching) {
		this.scheduleCaching = scheduleCaching;
		intervals=(scheduleCaching)?new ArrayList():null;
		ContainsIntervalConsumer containsConsumer=null;//clean if it wasn't scheduleCaching before
	}

	public void updateScheduleCache(){
		if (scheduleCaching || GraphicConfiguration.getInstance().getGanttBarMinWidth()>0){
			Object impl=node.getImpl();
			if (!isSchedule()) return;
			intervalConsumer.initCache(this,intervals);
			ScheduleService.getInstance().consumeIntervals((Schedule)impl,intervalConsumer);
			intervalCount=intervalConsumer.size>0?intervalConsumer.size:1;
		}
	}
	protected static CacheIntervalConsumer intervalConsumer=new CacheIntervalConsumer();
	protected static class CacheIntervalConsumer implements IntervalConsumer{
		protected List cache=null;
		protected GraphicNode gnode=null;
		int size;
		public void initCache(GraphicNode gnode,List cache){
			size=0;
			if (cache!=null) cache.clear();
			this.cache=cache;
			this.gnode=gnode;
		}
		public void consumeInterval(ScheduleInterval interval){
			if (size++==0) gnode.start=interval.getStart();
			gnode.end=interval.getEnd();
			if (cache!=null) cache.add(interval);
		}
	}

	public void consumeIntervals(IntervalConsumer consumer) {
		if (scheduleCaching){
			for (Iterator i=intervals.iterator();i.hasNext();){
				consumer.consumeInterval((ScheduleInterval)i.next());
			}
		}else{
			Object impl=node.getImpl();
			if (isSchedule()) ScheduleService.getInstance().consumeIntervals((Schedule)impl,consumer);
		}
	}



	//contains
	private ContainsIntervalConsumer containsConsumer=null; //need when no schedule caching
	private static class ContainsIntervalConsumer implements IntervalConsumer{
		ScheduleInterval interval=null;
		double t,deltaT1,deltaT2;
		CoordinatesConverter coord;
		GraphicNode node;
		public void init(double t,double deltaT1,double deltaT2,CoordinatesConverter coord,GraphicNode node){
			interval=null;
			this.t=t;
			this.deltaT1=deltaT1;
			this.deltaT2=deltaT2;
			this.coord=coord;
			this.node=node;
		}
		public ScheduleInterval getInterval(){
			return interval;
		}
		public void consumeInterval(ScheduleInterval interval){
			if (coord!=null) interval=coord.adaptSmallBarTimeInterval(interval, node, null);
			if (t>=interval.getStart()-deltaT1&&t<=interval.getEnd()+deltaT2) this.interval=interval;
		}

	}
	public ScheduleInterval contains(double t,double deltaT1,double deltaT2,CoordinatesConverter coord){
		if (scheduleCaching){
			ScheduleInterval interval;
			for (Iterator i=intervals.iterator();i.hasNext();){
				interval=(ScheduleInterval)i.next();
				if (coord!=null) interval=coord.adaptSmallBarTimeInterval(interval, this, null);
				if (t>=interval.getStart()-deltaT1&&t<=interval.getEnd()+deltaT2) return interval;
			}
			return null;
		}else{
			if (containsConsumer==null) containsConsumer=new ContainsIntervalConsumer();
			containsConsumer.init(t,deltaT1,deltaT2,coord,this);
			Object impl=node.getImpl();
			if (isSchedule()) ScheduleService.getInstance().consumeIntervals((Schedule)impl,containsConsumer);
			return containsConsumer.getInterval();
		}
	}
//	public boolean contains(double t,CoordinatesConverter coord){
//		return contains(t,0,0,coord)!=null;
//	}




	public boolean isSchedule(){
		return node.getImpl() instanceof Schedule;
	}



	protected double ganttShapeOffset=0,ganttShapeHeight=GraphicConfiguration.getInstance().getGanttBarHeight();;
	public double getGanttShapeHeight() {
		return ganttShapeHeight;
	}
	public void setGanttShapeHeight(double ganttShapeHeight) {
		this.ganttShapeHeight = ganttShapeHeight;
	}
	public double getGanttShapeOffset() {
		return ganttShapeOffset;
	}
	public void setGanttShapeOffset(double ganttShapeOffset) {
		this.ganttShapeOffset = ganttShapeOffset;
	}


	protected int row; //tmp value for performance reasons
	public int getRow() {
		return row;
	}
	public void setRow(int row) {
		this.row = row;
	}
	protected GeneralPath pertShape=null;
	protected GeneralPath xbsShape=null;
	protected Point2D pertCenter=null;
	protected Point2D xbsCenter=null;
	public GeneralPath getPertShape() {
		return pertShape;
	}
	public void setPertShape(GeneralPath pertShape,double centerX, double centerY) {
		this.pertShape = pertShape;
		if (pertCenter==null)
			pertCenter=new Point2D.Double();
		pertCenter.setLocation(centerX,centerY);
	}
	public GeneralPath getXbsShape() {
		return xbsShape;
	}
	public void setXbsShape(GeneralPath xbsShape,double centerX, double centerY) {
		this.xbsShape = xbsShape;
		setXbsCenter(centerX,centerY);
	}
	private void setXbsCenter(double centerX, double centerY) {
		if (xbsCenter==null)
			xbsCenter=new Point2D.Double();
		xbsCenter.setLocation(centerX,centerY);
	}
	public Point2D getPertCenter() {
		return pertCenter;
	}
	public Point2D getXbsCenter() {
		return xbsCenter;
	}
	public void translatePertShape(double dx,double dy){
		AffineTransform t=AffineTransform.getTranslateInstance(dx,dy);
		getPertShape().transform(t);
		Point2D point=getPertCenter();
		point.setLocation(point.getX()+dx,point.getY()+dy);
	}
	public void translateXbsShape(double dx,double dy){
		AffineTransform t=AffineTransform.getTranslateInstance(dx,dy);
		getXbsShape().transform(t);
		Point2D point=getXbsCenter();
		point.setLocation(point.getX()+dx,point.getY()+dy);
	}

	public long getCompleted(){
		if (!(getNode().getImpl() instanceof Schedule)) return 0;
		long completedT=ScheduleService.getInstance().getCompleted((Schedule)getNode().getImpl());
		return (completedT==0)?getStart():completedT;
	}
	public boolean isStarted(){
		if (!(getNode().getImpl() instanceof Schedule)) return false;
		return GanttProgress.hasVisibleProgress(getNode().getImpl());
//		return ScheduleService.getInstance().getCompleted((Schedule)getNode().getImpl())!=0;
	}

//	protected boolean manualPert=false;
//	protected boolean manualXbs=false;
//
//
//	public boolean isManualPert() {
//		return manualPert;
//	}
//	public void setManualPert(boolean manualPert) {
//		this.manualPert = manualPert;
//	}
//	public boolean isManualXbs() {
//		return manualXbs;
//	}
//	public void setManualXbs(boolean manualXbs) {
//		this.manualXbs = manualXbs;
//	}


	protected List tmpChildren=new ArrayList();
	public List getChildren() {
		return tmpChildren;
	}
	protected boolean tmpFiltered;
	public boolean isFiltered() {
		return tmpFiltered;
	}

	public void setFiltered(boolean filtered) {
		this.tmpFiltered = filtered;
	}

}
