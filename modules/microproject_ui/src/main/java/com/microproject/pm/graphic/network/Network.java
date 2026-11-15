/*******************************************************************************
 * MIT License
 *
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
package com.microproject.pm.graphic.network;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JViewport;

import com.microproject.pm.graphic.graph.Graph;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.graph.LinkRouting;
import com.microproject.pm.graphic.network.layout.NetworkLayout;
import com.microproject.pm.graphic.network.layout.NetworkLayoutEvent;
import com.microproject.pm.graphic.network.layout.NetworkLayoutListener;
import com.microproject.pm.graphic.link_routing.DefaultNetworkLinkRouting;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.pm.task.Project;
import com.microproject.util.FlatUiSupport;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/**
 *
 */
public class Network extends Graph implements NetworkLayoutListener, NetworkParams, SavableToWorkspace{
	private static final Logger logger = Logger.getLogger(Network.class.getName());
	private static final long serialVersionUID = -7976852605189565105L;
	protected AffineTransform transform;
    protected int zoom;

	public Network(Project project,String viewName) {
		this(new NetworkModel(project,viewName),project);
		transform=new AffineTransform();
	}
	protected Network(NetworkModel model, Project project) {
		super(model,project);
		setForeground(FlatUiSupport.tableForeground());
	}



	public void updateSize(){
		Rectangle bounds=((NetworkModel)getModel()).getBounds();
		GraphicConfiguration config=GraphicConfiguration.getInstance();
		setPreferredSize(new Dimension(bounds.x+bounds.width+config.getPertXOffset(),bounds.y+bounds.height+config.getPertYOffset()));
	}

	public void layoutChanged(NetworkLayoutEvent e){
		updateSize();
		revalidate();
		repaint();
	}

	public void zoomIn(){
		if (zoom==barStyles.getMaxZoom()) return;
		((NetworkUI)ui).resetForms();
		transform.concatenate(AffineTransform.getScaleInstance(barStyles.getRatioX(zoom,true),barStyles.getRatioY(zoom++,true)));
		((NetworkModel)getModel()).updateCellBounds();
	}
	public void zoomOut(){
		if (zoom==barStyles.getMinZoom()) return;
		((NetworkUI)ui).resetForms();
		transform.concatenate(AffineTransform.getScaleInstance(barStyles.getRatioX(zoom,false),barStyles.getRatioY(zoom--,false)));
		((NetworkModel)getModel()).updateCellBounds();
	}
	public boolean canZoomIn() {
		return zoom!=barStyles.getMaxZoom();
	}
	public boolean canZoomOut() {
		return zoom!=barStyles.getMinZoom();
	}
	public AffineTransform getTransform() {
		return transform;
	}
	public int getZoom() {
		return zoom;
	}

   	protected LinkRouting routing=new DefaultNetworkLinkRouting();
	public LinkRouting getRouting(){
		return routing;
	}
	private final NetworkDisplayOptions displayOptions = new NetworkDisplayOptions();
	@Override public NetworkDisplayOptions getDisplayOptions() {
		return displayOptions;
	}
	/** Switches the link routing between orthogonal and straight lines, keeping the diagram orientation. */
	public void setStraightLinks(boolean straight) {
		displayOptions.setStraightLinks(straight);
		boolean vertical = !(routing instanceof com.microproject.pm.graphic.link_routing.NetworkLinkRouting nr) || nr.isVertical();
		com.microproject.pm.graphic.link_routing.NetworkLinkRouting next = straight
			? new com.microproject.pm.graphic.link_routing.StraightNetworkLinkRouting()
			: new DefaultNetworkLinkRouting();
		next.setVertical(vertical);
		this.routing = next;
		repaint();
	}
	/** Repaints after a display option changed that alters the box size or spacing. */
	public void relayout() {
		((NetworkUI)ui).resetForms();
		if (getNetworkLayout() != null) getNetworkLayout().updateBounds();
		repaint();
	}
	public void setRouting(LinkRouting routing) {
		this.routing=routing;
	}
	private void makeZoom(int newZoom) {
		int factor = newZoom - zoom;
		if (factor > 0) {
			for (int i =0; i < factor; i++)
				zoomIn();
		} else {
			for (int i =0; i > factor; i--)
				zoomOut();
		}
	}
	public boolean useTextures() {
		return true;
	}
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		makeZoom(ws.zoom);
		displayOptions.setProgressMarks(ws.progressMarks != null && ws.progressMarks.booleanValue());
		displayOptions.setLinkLabels(ws.linkLabels != null && ws.linkLabels.booleanValue());
		displayOptions.setCollapsed(ws.collapsedBoxes != null && ws.collapsedBoxes.booleanValue());
		displayOptions.setBoxSpacing(ws.boxSpacing == null ? 0 : ws.boxSpacing.intValue());
		displayOptions.decodeStyles(ws.boxStyles);
		setStraightLinks(ws.straightLinks != null && ws.straightLinks.booleanValue());
		if (ws.collapsedBoxes != null || ws.boxSpacing != null) relayout();
     	Container p = getParent();
     	if (p instanceof JViewport && ws.viewPosition != null) {
     		try {
     		((JViewport)p).setViewPosition(ws.viewPosition);
     		} catch (RuntimeException e) {
     			logger.log(Level.FINE, "problem restoring viewport to point {0}", ws.viewPosition);
     		}
     	}

	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.zoom = zoom;
		ws.progressMarks = Boolean.valueOf(displayOptions.isProgressMarks());
		ws.linkLabels = Boolean.valueOf(displayOptions.isLinkLabels());
		ws.straightLinks = Boolean.valueOf(displayOptions.isStraightLinks());
		ws.collapsedBoxes = Boolean.valueOf(displayOptions.isCollapsed());
		ws.boxSpacing = Integer.valueOf(displayOptions.getBoxSpacing());
		ws.boxStyles = displayOptions.encodeStyles();
     	Container p = getParent();
     	if (p instanceof JViewport) {
     		ws.viewPosition = ((JViewport)p).getViewPosition();
     	}
		return ws;
	}
	public static class Workspace implements WorkspaceSetting  {
		private static final long serialVersionUID = 7804032466144588065L;
		int zoom;
		Point viewPosition = null;
		Boolean progressMarks;
		Boolean linkLabels;
		Boolean straightLinks;
		Boolean collapsedBoxes;
		Integer boxSpacing;
		String boxStyles;
		public int getZoom() {
			return zoom;
		}

		public void setZoom(int zoom) {
			this.zoom = zoom;
		}

		public Point getViewPosition() {
			return viewPosition;
		}

		public void setViewPosition(Point viewPosition) {
			this.viewPosition = viewPosition;
		}
	}
	public Rectangle getPrintBounds() {
		return null;
	}
	public void setPrintBounds(Rectangle printBounds) {
	}
	public int getPrintCols() {
		return 0;
	}
	public int getPrintRows() {
		return 0;
	}
	public NetworkLayout getNetworkLayout() {
		return ((NetworkModel)getModel()).getNetworkLayout();
	}
	public boolean isLeftPartVisible() {
		return true;
	}
	public boolean isRightPartVisible() {
		return true;
	}
	public void setLeftPartVisible(boolean visible){}
	public void setRightPartVisible(boolean visible){}
	public boolean isSupportLeftAndRightParts(){return false;}
	public void setSupportLeftAndRightParts(boolean supports){}
	public GraphParams createSafePrintCopy() {return this;}


}

