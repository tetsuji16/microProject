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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.spreadsheet.common.GradientCorner;
import com.microproject.timescale.TimeScaleEvent;
import com.microproject.timescale.TimeScaleListener;
import com.microproject.util.FlatUiSupport;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/**
 *
 */
public class ScaledScrollPane extends JScrollPane implements TimeScaleListener, SavableToWorkspace {
	private static final long serialVersionUID = -6608484720122760191L;
	private static final int HORIZONTAL_SCROLL_SPEED_MULTIPLIER = 2;
	protected TimeScaleComponent timeScaleComponent;
	protected CoordinatesConverter coord;
	protected ScaledComponent main;
	protected DocumentFrame documentFrame;
	private Point lastViewportPosition = new Point();
	private boolean extendingHorizontalRange;
	private static final int EDGE_TRIGGER_PIXELS = 24;
	private static final int RANGE_EXTENSION_DAYS = 30;

	
	
	/**
	 * @param documentFrame
	 * 
	 */
	public ScaledScrollPane(ScaledComponent main,CoordinatesConverter coord, DocumentFrame documentFrame,int verticalIncrement) {
		super((JComponent)main,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		this.main=main;
		this.coord=coord;
		this.documentFrame=documentFrame;
		main.setCoord(coord);
		createLayout();
		coord.addTimeScaleListener(this);
		this.getVerticalScrollBar().setUnitIncrement(verticalIncrement);
		// Match Microsoft Project's time-axis scrollbar: expose explicit
		// decrease/increase buttons for precise horizontal navigation.  Keep this
		// scoped to the time-scaled scrollbar instead of changing the application
		// wide FlatLaf defaults or unrelated vertical scrollbars.
		this.getHorizontalScrollBar().putClientProperty("JScrollBar.showButtons", Boolean.TRUE);
		updateHorizontalScrollIncrement();
		getHorizontalScrollBar().addAdjustmentListener(event -> extendRangeAtEdge());
		
	}

	/**
	 * MSP-style browsing: reaching either edge grows only the view range. The
	 * project schedule itself remains unchanged; this also avoids requiring a
	 * dummy task merely to inspect an empty period.
	 */
	private void extendRangeAtEdge() {
		if (extendingHorizontalRange || coord == null) return;
		JViewport viewport = getViewport();
		int value = getHorizontalScrollBar().getValue();
		int extent = getHorizontalScrollBar().getVisibleAmount();
		int maximum = getHorizontalScrollBar().getMaximum();
		boolean atStart = value <= EDGE_TRIGGER_PIXELS;
		boolean atEnd = value + extent >= maximum - EDGE_TRIGGER_PIXELS;
		if (!atStart && !atEnd) return;

		extendingHorizontalRange = true;
		try {
			Point before = viewport.getViewPosition();
			long oldOrigin = coord.getOrigin();
			if (atStart) coord.extendViewBefore(RANGE_EXTENSION_DAYS);
			if (atEnd) coord.extendViewAfter(RANGE_EXTENSION_DAYS);
			if (atStart && coord.getOrigin() != oldOrigin) {
				int shift = (int) Math.round(coord.toX(oldOrigin));
				viewport.setViewPosition(new Point(Math.max(0, before.x + shift), before.y));
			}
		} finally {
			extendingHorizontalRange = false;
		}
	}

	public void createLayout(){
		setPreferredSize(new Dimension(300, 250));
		FlatUiSupport.applyViewportSurface(getViewport());
		getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		
	    //JViewport mainVP=new JViewport();
		//mainVP.setView((JComponent)main);
		
		timeScaleComponent=new TimeScaleComponent(coord);
		
		
		//JViewport tsVP=new JViewport();
		//tsVP.setView(timeScaleComponent);
		setColumnHeaderView(/*tsVP*/timeScaleComponent);
		
		
		getViewport().addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
			    updateTimeScaleComponentSize();
			    Point position = getViewport().getViewPosition();
			    if (!position.equals(lastViewportPosition)) {
					lastViewportPosition = new Point(position);
					((JComponent)main).repaint();
			    }
			}
		});
		
//		These buttons don't size correctly with substancde layout. They aren't needed anyway
//		Box zoom=new Box(BoxLayout.Y_AXIS);
//		JButton zoomIn=new JButton(IconManager.getIcon("timescale.zoomIn.icon"));
//		zoomIn.addActionListener(new ActionListener(){
//			public void actionPerformed(ActionEvent e){
//				coord.zoomIn();
//			}
//		});
//		JButton zoomOut=new JButton(IconManager.getIcon("timescale.zoomOut.icon"));
//		zoomOut.addActionListener(new ActionListener(){
//			public void actionPerformed(ActionEvent e){
//				coord.zoomOut();
//			}
//		});
//		zoom.add(zoomIn);
//		zoom.add(zoomOut);
		setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER,new GradientCorner());
	}
	
	public void timeScaleChanged(TimeScaleEvent e) {
		timeScaleComponent.repaint();
		updateHorizontalScrollIncrement();
	}

	private void updateHorizontalScrollIncrement() {
		this.getHorizontalScrollBar().setUnitIncrement(coord.getTimescaleManager().getMinWidth() * HORIZONTAL_SCROLL_SPEED_MULTIPLIER);
	}
	
	private Dimension olddmain=null;
	
	public void updateTimeScaleComponentSize(){
		Dimension dmain=getViewport().getViewSize();
		if (dmain.equals(olddmain)) return;
		olddmain=dmain;
		timeScaleComponent.setPreferredSize(new Dimension(dmain.width,timeScaleComponent.getPreferredSize().height));
		getColumnHeader().setViewSize(new Dimension(dmain.width,getColumnHeader().getViewSize().height));
	}
	
	public Component getTimeScaleComponent() {
		return timeScaleComponent;
	}
	
	protected JComponent emptyRowHeader=null;
	//protected JComponent emptyCorner=null;
	
	public void activateEmptyRowHeader(boolean activate){
		if (activate) activateEmptyRowHeader();
		else deactivateEmptyRowHeader();
	}
	
	public void activateEmptyRowHeader(/*int width*/){
		if (emptyRowHeader==null){
			int width=40;
			emptyRowHeader=new JPanel();
			FlatUiSupport.applyDataSurface(emptyRowHeader);
			emptyRowHeader.setPreferredSize(new Dimension(width,(int)getViewport().getViewSize().getHeight()));
			JViewport header=new JViewport();
			header.setView(emptyRowHeader);
			header.setPreferredSize(emptyRowHeader.getPreferredSize());
			FlatUiSupport.applyViewportSurface(header);
			setRowHeader(header);
			
			/*emptyCorner=new JPanel();
			emptyRowHeader.setBackground(getColumnHeader().getView().getBackground());
			emptyCorner.setPreferredSize(new Dimension(width,(int)getColumnHeader().getViewSize().getHeight()));
			//JViewport corner=new JViewport();
			//corner.setView(emptyCorner);
			//corner.setPreferredSize(emptyCorner.getPreferredSize());
			setCorner(JScrollPane.UPPER_LEFT_CORNER,emptyCorner);*/
		}
	}
	public void deactivateEmptyRowHeader(){
		if (emptyRowHeader!=null){
	        getRowHeader().remove(emptyRowHeader);
	        remove(getRowHeader());
	        emptyRowHeader=null;
	        
	       // getCorner(JScrollPane.UPPER_LEFT_CORNER).remove(emptyCorner);
	        /*remove(emptyCorner);
	        emptyCorner=null;*/

		}

	}
	
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
     	if (ws.viewPosition != null) {
     		getViewport().setViewPosition(ws.viewPosition);
     	}
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
   		ws.viewPosition = getViewport().getViewPosition();
		return ws;
	}

	public static class Workspace implements WorkspaceSetting { 
		private static final long serialVersionUID = 8372367946057729222L;
		Point viewPosition = null;

		public Point getViewPosition() {
			return viewPosition;
		}

		public void setViewPosition(Point viewPosition) {
			this.viewPosition = viewPosition;
		}

	}
}
