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
import java.awt.Component;
import java.awt.Insets;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneUI;

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
	private boolean dividerLocationManuallyAdjusted;
	private boolean responsiveDividerUpdatePending;
	private Component trackedDivider;
	private MouseAdapter dividerGestureListener;
	private int dividerPressLocation = -1;
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
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				requestResponsiveDividerLayout();
			}
		});
		addPropertyChangeListener("UI", event -> installDividerGestureTracker());
		installDividerGestureTracker();
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
		detachDividerGestureTracker();
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
		requestSplitHierarchyValidation();
	}

	/**
	 * A nested split pane is its own Swing validation root. After replacing this
	 * pane's children, request validation from the outermost attached Swing
	 * validation root so its ancestors become valid with the rebuilt subtree.
	 */
	protected final void requestSplitHierarchyValidation() {
		JComponent outermostValidationRoot = null;
		for (Container ancestor = this; ancestor != null; ancestor = ancestor.getParent()) {
			if (ancestor instanceof JComponent swingAncestor && swingAncestor.isValidateRoot())
				outermostValidationRoot = swingAncestor;
		}
		if (outermostValidationRoot == null)
			revalidate();
		else
			outermostValidationRoot.revalidate();
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
		installDividerGestureTracker();

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
		publishDividerLocationAndRequestValidation(event.getSource(), dividerLocation);
	}

	/**
	 * Restores a pixel location saved on another window size without allowing
	 * either pane to disappear off-screen.
	 */
	protected final void restoreDividerLocation(int location) {
		Insets insets = getInsets();
		int availableWidth = getWidth() - insets.left - insets.right - getDividerSize();
		int boundedLocation = location;
		if (availableWidth >= MINIMUM_RESTORED_PANE_WIDTH * 2) {
			int maximumLocation = availableWidth - MINIMUM_RESTORED_PANE_WIDTH;
			boundedLocation = insets.left + Math.max(MINIMUM_RESTORED_PANE_WIDTH,
				Math.min(location - insets.left, maximumLocation));
		}
		setDividerLocationSilently(boundedLocation);
		markDividerLocationManuallyAdjusted();
		publishDividerLocationAndRequestValidation(this, boundedLocation);
		if (parentView != null)
			parentView.markChildrenDividerLocationManuallyAdjusted(this);
	}

	/** Requests an adaptive divider update for views that provide a content-based default. */
	protected final void requestResponsiveDividerLayout() {
		if (preferredLeftPaneWidth() < 0 || dividerLocationManuallyAdjusted || responsiveDividerUpdatePending)
			return;
		responsiveDividerUpdatePending = true;
		javax.swing.SwingUtilities.invokeLater(() -> {
			responsiveDividerUpdatePending = false;
			if (preferredLeftPaneWidth() < 0 || dividerLocationManuallyAdjusted)
				return;
			Insets insets = getInsets();
			int availableWidth = getWidth() - insets.left - insets.right - getDividerSize();
			int maximumLeftWidth = availableWidth - minimumResponsiveRightPaneWidth();
			if (maximumLeftWidth < MINIMUM_RESTORED_PANE_WIDTH)
				return;
			int location = insets.left + Math.max(MINIMUM_RESTORED_PANE_WIDTH,
					Math.min(preferredLeftPaneWidth(), maximumLeftWidth));
			setDividerLocationSilently(location);
			publishDividerLocationAndRequestValidation(this, location);
		});
	}

	private void publishDividerLocationAndRequestValidation(Object source, int location) {
		if (parentView != null)
			parentView.setChildrenDividerLocation(source, location);
		requestSplitHierarchyValidation();
	}

	protected int preferredLeftPaneWidth() {
		return -1;
	}

	protected int minimumResponsiveRightPaneWidth() {
		return MINIMUM_RESTORED_PANE_WIDTH;
	}

	final boolean isDividerLocationManuallyAdjusted() {
		return dividerLocationManuallyAdjusted;
	}

	final void markDividerLocationManuallyAdjusted() {
		dividerLocationManuallyAdjusted = true;
	}

	private void installDividerGestureTracker() {
		SwingUtilities.invokeLater(() -> {
			Component component = getUI() instanceof BasicSplitPaneUI splitPaneUI ? splitPaneUI.getDivider() : null;
			if (component == trackedDivider)
				return;
			detachDividerGestureTracker();
			if (component == null)
				return;
			trackedDivider = component;
			dividerGestureListener = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent event) {
					dividerPressLocation = getDividerLocation();
				}

				@Override
				public void mouseDragged(MouseEvent event) {
					markDividerGestureIfMoved();
				}

				@Override
				public void mouseReleased(MouseEvent event) {
					if (dividerPressLocation >= 0 && dividerPressLocation != getDividerLocation()) {
						markDividerGestureIfMoved();
						if (parentView != null) {
							parentView.setChildrenDividerLocation(SplittedView.this, getDividerLocation());
							parentView.markChildrenDividerLocationManuallyAdjusted(SplittedView.this);
						}
					}
					dividerPressLocation = -1;
				}
			};
			component.addMouseListener(dividerGestureListener);
			component.addMouseMotionListener(dividerGestureListener);
		});
	}

	private void markDividerGestureIfMoved() {
		if (dividerPressLocation >= 0 && dividerPressLocation != getDividerLocation())
			dividerLocationManuallyAdjusted = true;
	}

	private void detachDividerGestureTracker() {
		if (trackedDivider != null && dividerGestureListener != null) {
			trackedDivider.removeMouseListener(dividerGestureListener);
			trackedDivider.removeMouseMotionListener(dividerGestureListener);
		}
		trackedDivider = null;
		dividerGestureListener = null;
		dividerPressLocation = -1;
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
		if (getDividerLocation()!=location){
			setDividerLocationSilently(location);
		}
	}

	private void setDividerLocationSilently(int location) {
		if (getDividerLocation() == location)
			return;
		silent = true;
		try {
			super.setDividerLocation(location);
		} finally {
			silent = false;
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

