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

import java.awt.Color;
import java.awt.Stroke;
import java.util.List;
import java.util.ListIterator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.awt.geom.GeneralPath;

import com.microproject.pm.graphic.Renderer;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.graphic.configuration.shape.Colors;
import com.microproject.graphic.configuration.shape.PredefinedStroke;

public abstract class GraphRenderer extends Renderer{
	private transient Map<GraphicDependency, GeneralPath> dependencyPaths = new IdentityHashMap<>();
	private transient long dependencyPathGeometryRevision = Long.MIN_VALUE;
	public static Stroke DISABLED_LINK_STROKE = PredefinedStroke.SPARSE_DASHED;
	public static Color EXTERNAL_LINK_COLOR = Color.LIGHT_GRAY;
	public static Color NON_WORKING_COLOR = Colors.VERY_LIGHT_GRAY;;
	
	public GraphRenderer(GraphParams graphInfo){
		super(graphInfo);
	}
	public GraphRenderer(){
		super();
	}
	
	public boolean useTextures(){
		return graphInfo.useTextures();
	}
	
    public void updateShapes(){
		beginGeometryPass();
    	//System.out.println("Deep update");
    	if (graphInfo.getCache() == null) return;
    	updateShapes(graphInfo.getCache().getIterator());
    }
    public void updateShapes(List nodes){
    	//System.out.println("Shallow update");
		if (nodes==null) updateShapes();
		else { beginGeometryPass(); updateShapes(nodes.listIterator()); }
    }
    public void updateShapes(ListIterator nodeIterator){
    	GraphicNode node;
		for (ListIterator i=nodeIterator;i.hasNext();){
			node=(GraphicNode)i.next();
			if (!node.isVoid()) updateShape(node);
		}
    }

	
    public LinkRouting getRouting() {
		return graphInfo.getRouting();
	}

	public void setRouting(LinkRouting routing) {
		graphInfo.setRouting(routing);
	}

	public void updateShape(GraphicNode node){}

	public GeneralPath getDependencyPath(GraphicDependency dependency) {
		if (dependencyPaths == null)
			dependencyPaths = new IdentityHashMap<>();
		return dependencyPaths.computeIfAbsent(dependency, ignored -> new GeneralPath());
	}

	public GeneralPath findDependencyPath(GraphicDependency dependency) {
		return geometryContextRevision() == dependencyPathGeometryRevision && dependencyPaths != null
				? dependencyPaths.get(dependency) : null;
	}

	protected void beginGeometryPass() {
		if (dependencyPaths == null) dependencyPaths = new IdentityHashMap<>();
		else dependencyPaths.clear();
		dependencyPathGeometryRevision = geometryContextRevision();
	}

	protected long currentTopologyRevision() {
		return graphInfo != null && graphInfo.getCache() instanceof ViewNodeModelCache cache
				? cache.getProjectionSnapshot().topologyRevision() : -1L;
	}

	/** Domain/topology generation used to reject stale hit-test geometry. */
	protected long geometryContextRevision() {
		if (graphInfo != null && graphInfo.getCache() instanceof ViewNodeModelCache cache) {
			ViewNodeModelCache.InstalledProjectionSnapshot installed = cache.getInstalledProjectionSnapshot();
			return mix(installed.topology().domainRevision(), installed.topology().topologyRevision());
		}
		return -1L;
	}

	protected static long mix(long seed, long value) {
		return (seed ^ value) * 0x9E3779B97F4A7C15L;
	}

}
