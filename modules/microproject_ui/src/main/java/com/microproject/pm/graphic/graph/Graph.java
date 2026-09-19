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
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;

import com.microproject.pm.graphic.graph.event.GraphEvent;
import com.microproject.pm.graphic.graph.event.GraphListener;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.field.Field;
import com.microproject.field.FieldConverter;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.pm.task.Project;
import com.microproject.util.FlatUiSupport;

/**
 *
 */
public abstract class Graph extends JComponent implements GraphListener, GraphParams{
    protected BarStyles barStyles = null;
    protected Project project;
    protected GraphModel model;
    
	/**
	 * @param project
	 * 
	 */
	public Graph(Project project,String viewName) {
		this(new GraphModel(project,viewName),project);
	}
	protected Graph(GraphModel model, Project project) {
		super();
		this.project = project;
		setModel(model);
		ToolTipManager.sharedInstance().registerComponent(this);
		//renderer=new GanttTaskRenderer();
		updateUI();
		
		
		FlatUiSupport.applyDataSurface(this);
		setDoubleBuffered(true);
		setLayout(null);
	}
	
	
	public GraphUI getUI(){
		return (GraphUI)ui;
	}
	
    public NodeModelCache getCache() {
        return model.getCache();
    }
     public void setCache(NodeModelCache cache) {
        model.setCache(cache);
    }
     
	public GraphModel getModel() {
		return model;
	}
	
	public Project getProject() {
		return project;
	}
	public void cleanUp() {
		if (this.model!=null)
			model.removeGraphListener(this);

	}
	public void setModel(GraphModel model) {
		if (this.model!=null) model.removeGraphListener(this);
		this.model = model;
		model.addGraphListener(this);
	}

     
	/**
	 * @return Returns the barStyles.
	 */
	public BarStyles getBarStyles() {
		return barStyles;
	}
	/**
	 * @param barStyles The barStyles to set.
	 */
	public void setBarStyles(BarStyles barStyles) {
		this.barStyles = barStyles;
        model.setBarStyles(barStyles);
	}
	
	

	private String getStringValue(Field field,Node node){
	    WalkersNodeModel wmodel=model.getCache().getWalkersModel();
		Object value=field.getValue(node,wmodel,null);
		return FieldConverter.toString(value,value.getClass(),null);
	}
	

		
    public void updateGraph(GraphEvent e){
    	update(e.getNodes());
    }
    public void update(List nodes){
    	((GraphUI)ui).updateShapes(nodes);
    	repaint();
    }
	
	
    //to override
	public void updateUI(){}

	
	public Rectangle getDrawingBounds(){
		return getVisibleRect();
	}
	
	protected GraphicConfiguration config=GraphicConfiguration.getInstance();
	public GraphicConfiguration getConfiguration(){
		return config;
	}
	public void setConfiguration(GraphicConfiguration config){
		this.config=config;
	}

	
}

