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
package com.microproject.pm.graphic.network;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.graph.LinkRouting;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.network.layout.NetworkLayout;
import com.microproject.pm.graphic.network.layout.NetworkLayoutEvent;
import com.microproject.pm.graphic.link_routing.DefaultNetworkLinkRouting;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.GraphicConfiguration;

public class NetworkParamsImpl implements NetworkParams,Cloneable {
	protected AffineTransform transform;
	protected int zoom;
	protected BarStyles barStyles;
	protected GraphicConfiguration configuration;
	protected NodeModelCache cache;
	protected LinkRouting routing;
	protected NetworkLayout networkLayout;
	protected Rectangle printBounds;

	public NetworkParamsImpl(){
		configuration=GraphicConfiguration.getInstance();
		routing=new DefaultNetworkLinkRouting();
		transform=new AffineTransform();
	}

	public AffineTransform getTransform() {
		return transform;
	}

	public int getZoom() {
		return zoom;
	}

	public void setZoom(int zoom) {
		if (zoom==this.zoom) return;
		boolean in=(zoom-this.zoom)>0;
		while (this.zoom!=zoom){
			transform.concatenate(AffineTransform.getScaleInstance(barStyles.getRatioX(this.zoom,in),barStyles.getRatioY(this.zoom,in)));
			if (in) this.zoom++;
			else this.zoom--;
		}
	}

	public BarStyles getBarStyles() {
		return barStyles;
	}

	public NodeModelCache getCache() {
		return cache;
	}

	public GraphicConfiguration getConfiguration() {
		return configuration;
	}

	public LinkRouting getRouting() {
		return routing;
	}

	public void setBarStyles(BarStyles barStyles) {
		this.barStyles=barStyles;
		networkLayout.setBarStyles(barStyles);
	}

	public void setCache(NodeModelCache cache) {
		this.cache=cache;
		networkLayout.setCache(cache);
	}

	public void setConfiguration(GraphicConfiguration configuration) {
		this.configuration=configuration;
	}

	public void setRouting(LinkRouting routing) {
		this.routing=routing;
	}

	public boolean useTextures() {
		return false;
	}





	public GeneralPath scale(GeneralPath path){
		if (path == null)
			return null;
		GeneralPath transformed=(GeneralPath)path.clone();
		transformed.transform(getTransform());
		return transformed;
	}
	public Point2D scale(Point2D p){
		AffineTransform t=getTransform();
		return new Point2D.Double(p.getX()*t.getScaleX()+t.getTranslateX(),p.getY()*t.getScaleY()+t.getTranslateY());
	}
	public Point2D scaleVector(Point2D p){
		AffineTransform t=getTransform();
		return new Point2D.Double(p.getX()*t.getScaleX(),p.getY()*t.getScaleY());
	}
	public Point2D scaleVector_1(Point2D p){
		AffineTransform t=getTransform();
		return new Point2D.Double(p.getX()/t.getScaleX(),p.getY()/t.getScaleY());
	}

	public Rectangle scale(Rectangle r){
		AffineTransform t=getTransform();
		if (t==null) return r;
		Rectangle sr=new Rectangle();
		sr.setFrameFromDiagonal(
				r.getMinX()*t.getScaleX()+t.getTranslateX(),
				r.getMinY()*t.getScaleY()+t.getTranslateY(),
				r.getMaxX()*t.getScaleX()+t.getTranslateX(),
				r.getMaxY()*t.getScaleY()+t.getTranslateY()
			);
		return sr;
	}



	public Rectangle getDrawingBounds() {
		Rectangle bounds=networkLayout.getBounds();
		GraphicConfiguration config=GraphicConfiguration.getInstance();
		return new Rectangle(bounds.x+bounds.width+config.getPertXOffset(),bounds.y+bounds.height+config.getPertYOffset());
	}

	@Override
	public void layoutChanged(NetworkLayoutEvent e) {
	}

	public NetworkLayout getNetworkLayout() {
		return networkLayout;
	}

	public void setNetworkLayout(NetworkLayout networkLayout) {
		this.networkLayout = networkLayout;
	}

	public void updateLayout(){
		networkLayout.updateBounds();
	}

	public Rectangle getPrintBounds() {
		return printBounds;
	}

	public void setPrintBounds(Rectangle printBounds) {
		this.printBounds = printBounds;
	}

	public int getPrintCols(){
		return (int)Math.ceil(getDrawingBounds().getWidth()/getPrintBounds().getWidth());
	}
	public int getPrintRows(){
		return (int)Math.ceil(getDrawingBounds().getHeight()/getPrintBounds().getHeight());
	}

	public Rectangle getNetworkPrintBounds(int row,int col){
		int colCount=getPrintCols();
		int rowCount=getPrintRows();
		int w,h;
		if (col==colCount-1) w=getDrawingBounds().width%getPrintBounds().width;
		else w=getPrintBounds().width;
		if (row==rowCount-1) h=getDrawingBounds().height%getPrintBounds().height;
		else h=getPrintBounds().height;
		return new Rectangle(col*printBounds.width, row*printBounds.height,w,h);
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


	public Object clone(){
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	public GraphParams createSafePrintCopy(){
		NetworkParamsImpl c=(NetworkParamsImpl)clone();
		if (c.printBounds!=null) c.printBounds=(Rectangle)c.printBounds.clone();
		return c;
	}



}

