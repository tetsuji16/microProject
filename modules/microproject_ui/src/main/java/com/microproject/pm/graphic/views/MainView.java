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
package com.microproject.pm.graphic.views;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import com.microproject.pm.graphic.timescale.ScaledComponent;
import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.pm.graphic.views.synchro.Synchronizer;
import com.microproject.timescale.TimeScaleEvent;
import com.microproject.timescale.TimeScaleListener;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/**
 *  
 */
public class MainView extends JSplitPane implements TimeScaleListener, SavableToWorkspace{
	private static final long serialVersionUID = -6427979080094712783L;

	protected int defaultDividerSize;

    protected double defaultDividerLocation=0.7;
    private Synchronizer synchronizer;
    private Runnable dividerDoubleClickHandler;
    private MouseListener dividerMouseListener;
    /**
     *  
     */
    public MainView() {
        super(JSplitPane.VERTICAL_SPLIT);
        setOneTouchExpandable(true);
        defaultDividerSize = getDividerSize();
        setDividerSize(0);
        
    }
    public void setTop(Component top) {
    	if (top==null) return;
    	
		Component bottom = getBottomComponent();
		if (bottom != null) {
			setDividerSize(defaultDividerSize);
			setDividerLocation(defaultDividerLocation);
		}
		if (top instanceof SplittedView)
			((SplittedView) top).setParentView(this);
		setTopComponent(top);

		if (viewsSynchronizable()) {
		    if (bottom==null)
		        ((SplittedView) top).setDividerLocation(((SplittedView) bottom).getDividerLocation());
		    else ((SplittedView) top).setDividerLocationSilent(((SplittedView) bottom)
					.getDividerLocation()+((SplittedView) bottom).getDeltaDivider()-((SplittedView) top).getDeltaDivider()); //bottom not initialized yet, no sync
			addScaledComponentsSynchro();
		}
	}

    public void setBottom(Component bottom) {
    	if (bottom==null) return;
    	
		Component top = getTopComponent();
		if (top != null) {
			setDividerSize(defaultDividerSize);
			setDividerLocation(defaultDividerLocation);
		}
		if (bottom instanceof SplittedView)
			((SplittedView) bottom).setParentView(this);
		setBottomComponent(bottom);

		if (viewsSynchronizable()) {
			((SplittedView) bottom).setDividerLocationSilent(((SplittedView) top)
					.getDividerLocation()+((SplittedView) top).getDeltaDivider()-((SplittedView) bottom).getDeltaDivider()); //bottom not initialized yet, no sync
			addScaledComponentsSynchro();
		}
    }
    
    

    public void removeTop() {
    	removeScaledComponentsSynchro();
        setTopComponent(null);
        setDividerSize(0);
        installDividerMouseHandler();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        installDividerMouseHandler();
    }

    public void setDividerDoubleClickHandler(Runnable handler) {
        dividerDoubleClickHandler = handler;
        installDividerMouseHandler();
    }

    private void installDividerMouseHandler() {
        if (!(getUI() instanceof BasicSplitPaneUI ui))
            return;
        BasicSplitPaneDivider divider = ui.getDivider();
        if (divider == null)
            return;
        if (dividerMouseListener != null)
            divider.removeMouseListener(dividerMouseListener);
        dividerMouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1
                        && dividerDoubleClickHandler != null)
                    dividerDoubleClickHandler.run();
            }
        };
        divider.addMouseListener(dividerMouseListener);
    }

    public void removeBottom() {
    	removeScaledComponentsSynchro();
        setBottomComponent(null);
        setDividerSize(0);
    }
    
    public boolean viewsSynchronizable(){
    	Component top=getTopComponent();
    	Component bottom=getBottomComponent();
    	return (top!=null&&bottom!=null&&(top instanceof SplittedView)&&(bottom instanceof SplittedView));
    }
    
    public void addScaledComponentsSynchro(){
    	if (viewsSynchronizable()){
    		SplittedView top=(SplittedView)getTopComponent();
    		SplittedView bottom=(SplittedView)getBottomComponent();
    		JViewport bottomViewport=bottom.rightScrollPane.getViewport();
    		JViewport topViewport=top.rightScrollPane.getViewport();
    		JComponent bottomComponent=(JComponent)bottomViewport.getComponent(0);
    		JComponent topComponent=(JComponent)topViewport.getComponent(0);
    		adjustSizes();
    		((ScaledComponent)topComponent).getCoord().addTimeScaleListener(this); // listener removed in DocumentFrame
			getSynchronizer().addSynchro(top.getRightScrollPane(), bottom.getRightScrollPane(),
					ScrollPaneSynchronizer.VERTICAL,bottom.isNeedVoidBar(),false);
    	}
    }
    
    public void removeScaledComponentsSynchro(){
    	if (viewsSynchronizable()){
    		SplittedView top=(SplittedView)getTopComponent();
    		SplittedView bottom=(SplittedView)getBottomComponent();
   // 		JViewport bottomViewport=bottom.rightScrollPane.getViewport();
    		JViewport topViewport=top.rightScrollPane.getViewport();
    //		JComponent bottomComponent=(JComponent)bottomViewport.getComponent(0);
    		JComponent topComponent=(JComponent)topViewport.getComponent(0);
    		((ScaledComponent)topComponent).getCoord().removeTimeScaleListener(this);
			getSynchronizer().removeSynchro(top.getRightScrollPane(), bottom == null ? null : bottom.getRightScrollPane(),
    				ScrollPaneSynchronizer.VERTICAL);
    	}
    }
    
    
    
    public void setChildrenDividerLocation(Object source,int pos){
    	SplittedView top=null;
    	SplittedView bottom=null;
         Component c=getBottomComponent();
        if (c!=null&&(c instanceof SplittedView)) bottom=(SplittedView)c;
        c=getTopComponent();
        if (c!=null&&(c instanceof SplittedView)) top=(SplittedView)c;
        if (top==null||bottom==null) return;
        if (bottom.getDeltaDivider()<top.getDeltaDivider()){
        	SplittedView tmp=bottom;
        	bottom=top;
        	top=tmp;
        }
        int delta=bottom.getDeltaDivider()-top.getDeltaDivider();
        
        if (source==top&&bottom!=null){
    		int min=top.getMinimumDividerLocation();
    		int max=top.getMaximumDividerLocation();
    		if (pos>=max){
    			top.setDividerLocationSilent(Integer.MAX_VALUE);
    			bottom.setDividerLocationSilent(Integer.MAX_VALUE);
    		}
    		else if (pos<=min+delta){
    			top.setDividerLocationSilent(1+delta);
    			bottom.setDividerLocationSilent(1);
    		}
        	else bottom.setDividerLocationSilent(pos-delta);
        }
        if (source==bottom&&top!=null){
    		int min=bottom.getMinimumDividerLocation();
    		int max=bottom.getMaximumDividerLocation();
    		if (pos>=max-delta){
    			top.setDividerLocationSilent(Integer.MAX_VALUE);
    			bottom.setDividerLocationSilent(Integer.MAX_VALUE);
    		}
    		else if (pos<=min){
    			top.setDividerLocationSilent(1+delta);
    			bottom.setDividerLocationSilent(1);
    		}
        	else top.setDividerLocationSilent(pos+delta);
        }
    }
    
    
    
    public void adjustSizes(){
		SplittedView top=(SplittedView)getTopComponent();
		SplittedView bottom=(SplittedView)getBottomComponent();
		JViewport bottomViewport=bottom.rightScrollPane.getViewport();
		JViewport topViewport=top.rightScrollPane.getViewport();
		JComponent bottomComponent=(JComponent)bottomViewport.getView();
		JComponent topComponent=(JComponent)topViewport.getView();
		
		Dimension dtop=topComponent.getPreferredSize();
		Dimension dbottom=bottomComponent.getPreferredSize();
		dbottom=new Dimension((int)dtop.getWidth(),(int)dbottom.getHeight());
		bottomComponent.setPreferredSize(dbottom);
    }
    
    //to be notified when the time window changed
    public void timeScaleChanged(TimeScaleEvent e) {
    	if (viewsSynchronizable()){
    	    adjustSizes();
    	}
    }
	public Synchronizer getSynchronizer() {
		if (synchronizer == null)
			synchronizer = new Synchronizer();
		return synchronizer;
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.dividerLocation = getDividerLocation();
		return ws;
	}
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		setDividerLocation(ws.dividerLocation);
	}
	public static class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = -8129925562216728220L;
		int dividerLocation;
		public int getDividerLocation() {
			return dividerLocation;
		}
		public void setDividerLocation(int dividerLocation) {
			this.dividerLocation = dividerLocation;
		}
	}	
}
