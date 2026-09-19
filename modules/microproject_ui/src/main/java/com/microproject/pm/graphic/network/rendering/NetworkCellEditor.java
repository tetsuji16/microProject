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
package com.microproject.pm.graphic.network.rendering;


import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JComponent;

import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.model.cache.GraphicNode;


public class NetworkCellEditor{
	protected GraphParams params;
	protected JComponent container;
	protected FormatSelector formatSelector;
	protected FormComponent form;
	protected GraphicNode node;
	
	
	public NetworkCellEditor(GraphParams params,JComponent container) {
		this.params=params;
		this.container=container;
		formatSelector=new FormatSelector(params);
	}


	public void initEditorComponent(GraphicNode node,int zoom,Rectangle bounds){
		cancel();
		if (node==null) return;
		//System.out.println("create editor node="+node);
		this.node=node;
		form=formatSelector.getForm(node,zoom,true);
		form.setFields(node.getNode(),params.getCache().getModel());
		
		container.add(form);
		form.setBounds(bounds);
		form.validate();
	}
	
	public void resetForms(){
		formatSelector.resetForms();
	}

	
	public void paintEditor(GraphicNode node){
		if (node==null||this.node!=node) return;
		//System.out.println("paint editor node="+node);
		paintComponentApart(form,form.getBounds());
	}
	protected void paintComponentApart(Component c,Rectangle bounds){
		boolean wasDoubleBuffered = false;
		if ((c instanceof JComponent) && ((JComponent)c).isDoubleBuffered()) {
		    wasDoubleBuffered = true;
		    ((JComponent)c).setDoubleBuffered(false);
		}

		Graphics cg = container.getGraphics().create(bounds.x, bounds.y, bounds.width, bounds.height);
		try {
			c.paint(cg);
		}
		finally {
		    cg.dispose();
		}

		if (wasDoubleBuffered && (c instanceof JComponent)) {
		    ((JComponent)c).setDoubleBuffered(true);
		}
	}
	
	public boolean isEditing(GraphicNode node){
		return node!=null&&this.node==node;
	}
	public void cancel(){
		if (node!=null){
			//System.out.println("cancel editor");
			Container parent=form.getParent();
			if (parent!=null) parent.remove(form);
			Rectangle bounds=form.getBounds();
			form=null;
			node=null;
			container.repaint(bounds);
		}
	}
	
	public List getCellEditorChange() {
		return (form==null)?null:form.getChange();
	}
	
	public GraphicNode getNode() {
		return node;
	}
}

