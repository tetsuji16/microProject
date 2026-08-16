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

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;

import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.pm.graphic.views.synchro.Synchronizer;
import com.microproject.strings.Messages;
import com.microproject.util.FlatUiSupport;

/**
 * 
 */
public abstract class SplittedView extends JSplitPane {
	private static final Logger logger = Logger.getLogger(SplittedView.class.getName());
	private static final int MINIMUM_RESTORED_PANE_WIDTH = 96;
	protected JScrollPane leftScrollPane;
	protected JScrollPane rightScrollPane;
	protected MainView parentView;	
    boolean silent=false;
    
    protected boolean sync=true;
    
    //protected boolean scaled=true;
    protected boolean needVoidBar=true;
    private Synchronizer synchronizer;
	private final PropertyChangeListener dividerLocationListener = this::dividerLocationChanged;

	public SplittedView(Synchronizer synchronizer) {
		super(JSplitPane.HORIZONTAL_SPLIT);
		this.synchronizer = synchronizer;
		setContinuousLayout(true);
		setOneTouchExpandable(true);
		setResizeWeight(0.5);
		getAccessibleContext().setAccessibleName(Messages.getString("SplitView.accessibleName"));
		getAccessibleContext().setAccessibleDescription(Messages.getString("SplitView.accessibleDescription"));
		addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, dividerLocationListener);
	}
	public void cleanUp() {
		disposeSplitView();
		parentView = null;	
	    synchronizer = null;
	}

	/**
	 * Detaches both panes and their synchronizer.  Subclasses that own resources
	 * inside the panes should release those resources before calling this method.
	 */
	protected void disposeSplitView() {
		if (sync && synchronizer != null && leftScrollPane != null && rightScrollPane != null) {
			synchronizer.removeSynchro(leftScrollPane, rightScrollPane,
					ScrollPaneSynchronizer.HORIZONTAL);
		}
		setLeftComponent(null);
		setRightComponent(null);
		leftScrollPane = null;
		rightScrollPane = null;
	}

	/**
	 * Rebuilds both sides as one unit.  Replacing only one side leaves stale
	 * listeners and makes the two view models disagree about row geometry.
	 */
	public void reinitialize() {
		disposeSplitView();
		init();
	}
	public void init() {
		if (leftScrollPane != null || rightScrollPane != null) {
			disposeSplitView();
		}
		leftScrollPane = createLeftScrollPane();
		rightScrollPane = createRightScrollPane();
		configureScrollPaneSurface(leftScrollPane);
		configureScrollPaneSurface(rightScrollPane);
		configurePaneAccessibility(leftScrollPane, "SplitView.leftPaneAccessibleName");
		configurePaneAccessibility(rightScrollPane, "SplitView.rightPaneAccessibleName");
		setLeftComponent(leftScrollPane);
		setRightComponent(rightScrollPane);

		if (sync)
			synchronizer.addSynchro(leftScrollPane, rightScrollPane,
				ScrollPaneSynchronizer.HORIZONTAL);
	}

	protected void configureScrollPaneSurface(JScrollPane scrollPane) {
		if (scrollPane == null)
			return;
		JViewport viewport = scrollPane.getViewport();
		if (viewport != null)
			FlatUiSupport.applyViewportSurface(viewport);
	}

	private void configurePaneAccessibility(JScrollPane scrollPane, String accessibleNameKey) {
		if (scrollPane == null)
			return;
		scrollPane.setMinimumSize(new Dimension(MINIMUM_RESTORED_PANE_WIDTH, 0));
		scrollPane.getAccessibleContext().setAccessibleName(Messages.getString(accessibleNameKey));
	}

	private void dividerLocationChanged(PropertyChangeEvent event) {
		if (silent) {
			silent = false;
			return;
		}
		int dividerLocation = ((Integer) event.getNewValue()).intValue();
		logger.info(() -> getClass().getSimpleName() + " divider changed to " + dividerLocation
			+ " source=" + (event.getSource() == null ? "null" : event.getSource().getClass().getSimpleName()));
		if (parentView != null)
			parentView.setChildrenDividerLocation(event.getSource(), dividerLocation);
	}

	/**
	 * Restores a pixel location saved on another window size without allowing
	 * either pane to disappear off-screen.
	 */
	protected final void restoreDividerLocation(int location) {
		int availableWidth = getWidth() - getDividerSize();
		if (availableWidth < MINIMUM_RESTORED_PANE_WIDTH * 2) {
			setDividerLocation(location);
			return;
		}
		int maximumLocation = availableWidth - MINIMUM_RESTORED_PANE_WIDTH;
		setDividerLocation(Math.max(MINIMUM_RESTORED_PANE_WIDTH, Math.min(location, maximumLocation)));
	}
	

	protected int deltaDivider=0;	
	/**
	 * @return Returns the deltaDivider.
	 */
	public int getDeltaDivider() {
		return deltaDivider;
	}
	/**
	 * @param deltaDivider The deltaDivider to set.
	 */
	public void setDeltaDivider(int deltaDivider) {
		this.deltaDivider = deltaDivider;
	}
	
	public void setDividerLocationSilent(int location){
		if (getLastDividerLocation()!=location){
			silent=true;
			super.setDividerLocation(location);
		}
	}
	
	
	protected abstract JScrollPane createLeftScrollPane();

	protected abstract JScrollPane createRightScrollPane();

	/**
	 * @return Returns the parentView.
	 */
	public MainView getParentView() {
		return parentView;
	}

	/**
	 * @param parentView
	 *            The parentView to set.
	 */
	public void setParentView(MainView parentView) {
		this.parentView = parentView;
	}
		
	
	/**
	 * @return Returns the leftScrollPane.
	 */
	public JScrollPane getLeftScrollPane() {
		return leftScrollPane;
	}
	/**
	 * @return Returns the rightScrollPane.
	 */
	public JScrollPane getRightScrollPane() {
		return rightScrollPane;
	}
	
	
	
	/**
	 * @return Returns the needVoidBar.
	 */
	public boolean isNeedVoidBar() {
		return needVoidBar;
	}
	/**
	 * @param needVoidBar The needVoidBar to set.
	 */
	public void setNeedVoidBar(boolean needVoidBar) {
		this.needVoidBar = needVoidBar;
	}
//	/**
//	 * @return Returns the scaled.
//	 */
//	public boolean isScaled() {
//		return scaled;
//	}
//	/**
//	 * @param scaled The scaled to set.
//	 */
//	public void setScaled(boolean scaled) {
//		this.scaled = scaled;
//	}
	
	
	
	public void updateSize(){
		
	}
	
}

