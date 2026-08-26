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
package com.microproject.pm.graphic.graph;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.microproject.dialog.DependencyDialog;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.graphic.configuration.GraphicConfiguration;

/**
 *
 */
public abstract class GraphInteractor implements MouseListener, MouseMotionListener, Serializable{
	protected static final int NOTHING_SELECTED=0;
	protected static final int LINK_CREATION=1;
	protected static final int LINK_SELECTION=2;
	protected static final int BAR_MOVE=3;
	protected GraphUI ui;
	protected int state;
	protected Object selected=null;
	protected GraphZone selectedZone=null;
	protected boolean selection;
	protected double x0,y0;
	protected GraphicConfiguration config;
	protected GraphPopupMenu popup=null;
	protected DependencyDialog dependencyPropertiesDialog;
	// Drag scroll throttling: avoid excessive scrollRectToVisible calls during mouse drag
	private static final int SCROLL_THROTTLE_PX = 30;
	private int lastScrollX = Integer.MIN_VALUE;
	private int lastScrollY = Integer.MIN_VALUE;
	private transient boolean installed;
	/**
	 *
	 */
	public GraphInteractor(GraphUI ui) {
		this.ui=ui;
		config=GraphicConfiguration.getInstance();
		state=NOTHING_SELECTED;
		selection=true;
		defaultCursor=getGraph().getCursor();
	}

	final void install() {
		if (installed) return;
		getGraph().addMouseListener(this);
		getGraph().addMouseMotionListener(this);
		installed = true;
	}

	final void uninstall() {
		if (!installed) return;
		getGraph().removeMouseListener(this);
		getGraph().removeMouseMotionListener(this);
		installed = false;
		reset();
		if (dependencyPropertiesDialog != null) {
			dependencyPropertiesDialog.dispose();
			dependencyPropertiesDialog = null;
		}
	}

    protected void reset(){
     	lastShadowX=-1;
     	lastShadowY=-1;
    	lastLinkShadowX=-1;
    	lastLinkShadowY=-1;
       	sourceNode=null;
    	destinationNode=null;
		selection=true;
		linkSelectionHighlights.clear();
		getGraph().repaint();
    	// Reset scroll throttle so next drag starts fresh
    	lastScrollX = Integer.MIN_VALUE;
    	lastScrollY = Integer.MIN_VALUE;
    }

    protected boolean selectedIsNonSummaryNode(){
    	return ((selected instanceof GraphicNode)&&!((GraphicNode)selected).isSummary());
    }
    public Graph getGraph(){
    	return ui.getGraph();
    }

	public Object getSelectedObject() {
		return selected;
	}


	public void showDependencyPropertiesDialog(GraphicDependency dependency) {
		var modelDependency = dependency.getDependency();
		long revision = modelDependency.getPredecessor() instanceof com.microproject.pm.task.Task task
				&& task.getOwningProject() != null
				? task.getOwningProject().getDomainChangeJournal().revision() : -1L;
		showDependencyPropertiesDialog(dependency, revision);
	}

	protected void showDependencyPropertiesDialog(GraphicDependency dependency, long expectedDomainRevision) {
    	if (dependencyPropertiesDialog == null) {
    		Frame parent=JOptionPane.getFrameForComponent(getGraph());
		dependencyPropertiesDialog = new DependencyDialog(parent,dependency.getDependency());
	    }
	    if (getGraph().getCache() instanceof com.microproject.pm.graphic.model.cache.ViewNodeModelCache viewCache)
		dependencyPropertiesDialog.setTaskCommandGateway(viewCache.getTaskCommandGateway());
		boolean didAction = DependencyDialog.doDialog(dependencyPropertiesDialog, dependency.getDependency(),
				expectedDomainRevision);
    }


    //CURSORS
    protected Cursor defaultCursor;
	protected Cursor progressCursor;
	protected Cursor getProgressCursor(){
        if (progressCursor==null){
            try{
            	progressCursor=Toolkit.getDefaultToolkit().createCustomCursor(
              		IconManager.getImage("gantt.progress.cursor"),
					new Point(15, 5),
					"ProgressCursor");
            }catch (Exception e) {
              progressCursor=new Cursor(Cursor.HAND_CURSOR);
            }
        }
      return progressCursor;
	}
	protected Cursor linkCursor;
	protected Cursor getLinkCursor(){
        if (linkCursor==null){
            try{
            	linkCursor=Toolkit.getDefaultToolkit().createCustomCursor(
              		IconManager.getImage("gantt.link.cursor"),
					new Point(7,3),
					"linkCursor");
            }catch (Exception e) {
            	linkCursor=new Cursor(Cursor.HAND_CURSOR);
            }
        }
        return linkCursor;
	}

	protected Cursor splitCursor;
	protected Cursor getSplitCursor(){
        if (splitCursor==null){
            try{
            	splitCursor=Toolkit.getDefaultToolkit().createCustomCursor(
              		IconManager.getImage("gantt.split.cursor"),
					new Point(10,4),
					"splitCursor");
            }catch (Exception e) {
                splitCursor=new Cursor(Cursor.HAND_CURSOR);
            }
        }
        return splitCursor;
	}


    public Cursor selectCursor(){
    	Cursor cursor=defaultCursor;
    	switch (state) {
		case BAR_MOVE:
			cursor=new Cursor(Cursor.MOVE_CURSOR);
			break;
		case LINK_CREATION:
			cursor=getLinkCursor();
			break;
		case LINK_SELECTION:
			cursor=new Cursor(Cursor.CROSSHAIR_CURSOR);
			break;
		}
    	getGraph().setCursor(cursor);
    	return  cursor;
    }

    //Drawings
    protected void findState(double x,double y){
		state=NOTHING_SELECTED;
		if (selected==null) return;
		if (selected instanceof GraphicNode){
			computeNodeSelection(x,y);
		}else if (selected instanceof GraphicDependency){
			state=LINK_SELECTION;
		}
    }

    protected double lastShadowX=-1;
    protected double lastShadowY=-1;
    protected void drawBarShadow(double x,double y, boolean alternate){
		if (x==-1) return;
		if (alternate && lastShadowX == x && lastShadowY == y) {
			lastShadowX=-1;
			lastShadowY=-1;
		} else {
			lastShadowX=x;
			lastShadowY=y;
		}
		getGraph().repaint();
    }



    protected GraphicNode sourceNode=null;
    protected GraphicNode destinationNode=null;
	private final Set<GraphicNode> linkSelectionHighlights = new LinkedHashSet<>();
    protected void drawLinkSelectionBarShadow(GraphicNode node){
		if (node==null) return;
		if (!linkSelectionHighlights.add(node)) linkSelectionHighlights.remove(node);
		getGraph().repaint();
    }



    protected double lastLinkShadowX=-1;
    protected double lastLinkShadowY=-1;
    protected double x0link,y0link;
    private void drawLinkShadow(double x,double y,boolean alternate){
		if (x==-1||y==-1) return;
		if (alternate && lastLinkShadowX == x && lastLinkShadowY == y) {
			lastLinkShadowX=-1;
			lastLinkShadowY=-1;
		} else {
			lastLinkShadowX=x;
			lastLinkShadowY=y;
		}
		getGraph().repaint();
    }

	final void paintPreview(Graphics2D graphics) {
		Graphics2D preview=(Graphics2D)graphics.create();
		try {
			preview.setColor(getGraph().getForeground());
			if (lastShadowX!=-1) {
				Shape bar=getBarShadowBounds(lastShadowX,lastShadowY);
				if (bar!=null) {
					preview.setStroke(new BasicStroke(3));
					preview.draw(bar);
				}
			}
			for (GraphicNode node:linkSelectionHighlights) {
				Rectangle2D highlight=getLinkSelectionShadowBounds(node);
				if (highlight!=null) {
					preview.setStroke(new BasicStroke(3));
					preview.draw(highlight);
				}
			}
			if (lastLinkShadowX!=-1 && lastLinkShadowY!=-1) {
				preview.setStroke(new BasicStroke(2));
				preview.draw(new Line2D.Double(x0link,y0link,lastLinkShadowX,lastLinkShadowY));
			}
		} finally {
			preview.dispose();
		}
	}


    //Mouse
    public void mouseClicked(MouseEvent e){}


    public void mousePressed(MouseEvent e){
    	if (isReadOnly()) return;
    	if (SwingUtilities.isRightMouseButton(e)){
    		if (popup!=null) popup.show(getGraph(),e.getX(),e.getY());
	    }else{
	    	if (selected==null) return;
	    	if (isMove()){
	    		ScrollPaneSynchronizer.invalidateZoomRestore(getGraph());
	    		selection=false;
	    		x0=e.getX();
	    		y0=e.getY();
	    		drawBarShadow(x0,y0,true);
	    	}else if (isDirectAction()){
	    		if (isZoomRestoreInvalidatingDirectAction()) {
	    			ScrollPaneSynchronizer.invalidateZoomRestore(getGraph());
	    		}
	    		executeAction(e.getX(),e.getY());
	    		state=NOTHING_SELECTED;
	    	}
    	}
    }

    public void mouseReleased(MouseEvent e){
    	if (isReadOnly()) return;
    	if (!SwingUtilities.isLeftMouseButton(e)) return;
    	if (selected==null||state==NOTHING_SELECTED) return;
		if (isRepaintOnRelease()) getGraph().repaint();

    	double x1=e.getX();
    	double y1=e.getY();
    	executeAction(x1,y1);
    	reset();
    	findState(x1,y1);
    }

    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}

    private void scrollToVisible(int x,int y){
     	// Throttle: skip if mouse hasn't moved far enough since last scroll.
     	// This prevents excessive scrollRectToVisible calls during smooth dragging.
     	if (Math.abs(x - lastScrollX) < SCROLL_THROTTLE_PX && Math.abs(y - lastScrollY) < SCROLL_THROTTLE_PX) {
     		return;
     	}
     	lastScrollX = x;
     	lastScrollY = y;
     	int scrollingDistance=100;
     	getGraph().scrollRectToVisible(new Rectangle(x-scrollingDistance,y-scrollingDistance,scrollingDistance*2,scrollingDistance*2));
    }
    protected boolean allowLinkSelectionToMove(){
    	return false;
    }
    protected int beforeLinkState=0;
    public void mouseDragged(MouseEvent e){
    	if (isReadOnly()) return;
    	if (!SwingUtilities.isLeftMouseButton(e)) return;
    	if (selected instanceof GraphicNode){
    		GraphicNode node=(GraphicNode)selected;
    		boolean sw=switchOnLinkCreation(e.getX(),e.getY());
    		if (state!=LINK_CREATION&&sw){
    			drawBarShadow(lastShadowX,lastLinkShadowY,true);
    			beforeLinkState=state;
    			state=LINK_CREATION;
    			selectCursor();

    			sourceNode=(GraphicNode)selected;
    			drawLinkSelectionBarShadow(sourceNode);

    			setLinkOrigin();
    		}
    		else if (state==LINK_CREATION&&!sw&&allowLinkSelectionToMove()){
    			drawLinkShadow(lastLinkShadowX,lastLinkShadowY,true);
    			drawBarShadow(lastShadowX,lastLinkShadowY,true);
    			state=beforeLinkState;
    			selectCursor();

    			sourceNode=null;

    		}
    	}
    	if (state==LINK_CREATION){
    		GraphZone zone=ui.getNodeAt(e.getX(),e.getY());
			GraphicNode newDestinationNode=zone==null?null:(GraphicNode)zone.getObject();
			drawLinkSelectionBarShadow(destinationNode);
    		drawLinkShadow(lastLinkShadowX,lastLinkShadowY,true);
    		scrollToVisible(e.getX(),e.getY());
			drawLinkShadow(e.getX(),e.getY(),true);
			if (newDestinationNode!=null && newDestinationNode.isLinkable() && sourceNode != newDestinationNode){
				destinationNode=newDestinationNode;
				drawLinkSelectionBarShadow(destinationNode);
			}else destinationNode=null;
    	}else if (isMove()){
    		drawBarShadow(lastShadowX,lastShadowY,true);
    		scrollToVisible(e.getX(),e.getY());
    		drawBarShadow(e.getX(),e.getY(),true);
    	}
    }
    public void mouseMoved(MouseEvent e){
    	if (isReadOnly()) return;
    	select(e.getX(),e.getY());
    }




    public boolean isReadOnly(){
    	return getGraph().getModel().isReadOnly();
    }








    protected abstract void computeNodeSelection(double x,double y);
    protected abstract Shape getBarShadowBounds(double x,double y);
    protected abstract Rectangle2D getLinkSelectionShadowBounds(GraphicNode node);


    protected abstract void setLinkOrigin();
    protected abstract boolean switchOnLinkCreation(double x, double y);

    public abstract boolean executeAction(double x,double y);




    protected void select(int x,int y){
    	if (selection){
       	selectedZone=ui.getObjectAt(x,y);
			// Do not keep the previous task selected when the pointer is over
			// empty graph space.  GanttInteractor uses a null selection to enter
			// pan mode, so retaining the stale selection makes the chart appear
			// immovable after the user has selected a bar once.
			selected=selectedZone == null ? null : selectedZone.getObject();
	    	if (selected==null){
	    		state=NOTHING_SELECTED;
	    	}else{
	    		 findState(x,y);
	    	}
	    	selectCursor();
    	}
    }

    protected boolean isMove(){
    	return state==BAR_MOVE;
    }
    protected boolean isDirectAction(){
    	return state==LINK_SELECTION;
    }
    protected boolean isZoomRestoreInvalidatingDirectAction(){
    	return false;
    }
    protected boolean isRepaintOnRelease(){
    	return state==BAR_MOVE||state==LINK_CREATION;
    }




}
