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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.pm.graphic.ChangeAwareComponent;
import com.microproject.pm.graphic.ChangeAwareTextField;
import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.field.FieldConverter;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.FormBox;
import com.microproject.graphic.configuration.FormBoxLayout;
import com.microproject.graphic.configuration.FormFormat;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.FlatUiSupport;

public class FormComponent extends JPanel{
	protected int maxRows=3;
	protected int maxCols=2;
	protected Map<String, Component> fieldComponents;
	protected List<BarFormat> selectedFormats;
	protected boolean editor;
	protected int zoom;
	protected boolean texture=true;
	
	
	public FormComponent(List<BarFormat> selectedFormats,int zoom,boolean editor,boolean texture){
		super();
		fieldComponents=new HashMap<>();
		this.selectedFormats=selectedFormats;
		this.editor=editor;
		this.zoom=zoom;
		this.texture=texture;
		BarFormat format;
		if (selectedFormats==null||selectedFormats.size()==0) format=null;
		else format=(BarFormat)selectedFormats.get(0);
		init(format);
		setOpaque(false);
		setForeground(FlatUiSupport.tableForeground());
		setBackground(FlatUiSupport.panelBackground());
	}
	
	
	public boolean isEditor() {
		return editor;
	}
	public void setEditor(boolean editor) {
		this.editor = editor;
	}
	
	public void init(BarFormat format) {
		if (format==null) return;
		FormFormat form=format.getForm();
		if (form==null) return;
		List boxes=form.getBoxes();
		if (boxes==null||boxes.size()==0){
			return;
		}
		FormBoxLayout formBoxLayout=form.getLayout(zoom);
		FormLayout layout = new FormLayout(
				formBoxLayout.getColumnGrid(),
				formBoxLayout.getRowGrid());
		DefaultFormBuilder builder = new DefaultFormBuilder(this,layout);
		if (formBoxLayout.getBorder()==null) builder.setDefaultDialogBorder();
		else builder.setBorder(Borders.createEmptyBorder(formBoxLayout.getBorder()));
		CellConstraints cc = new CellConstraints();
		for (Object item : boxes){
			FormBox box = (FormBox)item;
			if (zoom<box.getMinZoom()) return;
			JComponent component;
			if(box.getFieldId()==null) component=new JLabel(Messages.getString(box.getTextId()));
			else{
				if (editor&&!box.getField().isReadOnly()){
					component=new ChangeAwareTextField();
					component.setBorder(null);
					//component.setOpaque(false);
				}else component=new JLabel();
				
				//if (box.getRow()==1&&!editor) ((JLabel)component).setHorizontalAlignment(SwingConstants.CENTER);
				//bug workaround, not possible to center with classic method when rowSpan>1
				
				fieldComponents.put(box.getFieldId(),component);
			}
			Font font=formBoxLayout.getFont(box.getFont());
			if (font!=null) component.setFont(font);
			builder.add(component,(box.getAlignment()==null)?
					cc.xywh(box.getColumn(),box.getRow(),box.getColumnSpan(),box.getRowSpan()):
					cc.xywh(box.getColumn(),box.getRow(),box.getColumnSpan(),box.getRowSpan(),box.getAlignment()));
		}
	}
	
	
	public Component getComponent(String fieldId){
		return (Component)fieldComponents.get(fieldId);
	}
	
	
	
	public void setFields(Node node,NodeModel model){
		for (String fieldId : fieldComponents.keySet()){
			Field field=Configuration.getFieldFromId(fieldId);
			Object value=field.getValue(node,model,null);
			
			String stringValue="";
			if (value != null)
				stringValue = FieldConverter.toString(value);
			Component textComp=fieldComponents.get(fieldId);
			if (textComp instanceof JLabel)
				((JLabel)textComp).setText(stringValue);
			else{
				((ChangeAwareTextField)textComp).setText(stringValue);
				((ChangeAwareComponent)textComp).resetChange();
			}
		}
	}
	
	
	public List<FieldChange> getChange(){
		ArrayList<FieldChange> change = new ArrayList<>();
		for (String fieldId : fieldComponents.keySet()){
			Component component=fieldComponents.get(fieldId);
			if (component instanceof ChangeAwareComponent&&
					((ChangeAwareComponent)component).hasChanged()){
				String stringValue;
				if (component instanceof JTextField)
					stringValue=((JTextField)component).getText();
				//hangle other components here
				else continue;
				
				Field field=Configuration.getFieldFromId(fieldId);
				try {
					Object value=FieldConverter.fromString(stringValue,field.getDisplayType());
					change.add(new FieldChange(field,value));
				} catch (Exception e) {
					Alert.error(e.getMessage());
				}
			}
		}
		return change;
	}
	
	

	void paintSelectedBars(Graphics2D g2, double width, double height){
		for (BarFormat format : selectedFormats){
			if (format.getMiddle()!=null) format.getMiddle().draw(g2,
					width,
					height,
					0,
					+height/2,
					texture);
			if (format.getStart()!=null) format.getStart().draw(g2,
					width,
					height,
					0,
					+height/2,
					texture);
			if (format.getEnd()!=null) format.getEnd().draw(g2,
					width,
					height,
					0,
					+height/2,
					texture);
		}
		
	}
	
	
	
	public void paint(Graphics g) {
		/*CommonGraphCell cell=(CommonGraphCell)view.getCell();
		if (cell.getNode().isVoid()) return;*/
		
		Graphics2D g2=(Graphics2D)g;
		Dimension d=getSize();
		double w=d.getWidth();
		double h=d.getHeight();
		
		
		paintSelectedBars(g2,w-1,h-1);
//		ImageIcon link=IconManager.getIcon("common.link.image");
//		g2.drawImage(link.getImage(),(int)(w-link.getIconWidth()),(int)(h-link.getIconHeight()),this);
		//x=w and y=h are outside
		
		try {
			//if (preview && !isDoubleBuffered)
			//	setOpaque(false);
			super.paint(g);
			//paintSelectionBorder(g);
		} catch (IllegalArgumentException e) {
			// JDK Bug: Zero length string passed to TextLayout constructor
		}
	}
	
	
	
	
	
	
	
	

	/**
	 * Provided for subclassers to paint a selection border.
	 */
	/*protected void paintSelectionBorder(Graphics g) {
		((Graphics2D) g).setStroke(GraphConstants.SELECTION_STROKE);
		if (childrenSelected)
			g.setColor(graph.getGridColor());
		else if (hasFocus && selected)
			g.setColor(graph.getLockedHandleColor());
		else if (selected)
			g.setColor(graph.getHighlightColor());
		if (childrenSelected || selected) {
			Dimension d = getSize();
			g.drawRect(0, 0, d.width - 1, d.height - 1);
		}
	}*/
	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	protected void firePropertyChange(
		String propertyName,
		Object oldValue,
		Object newValue) {
		if ("text".equals(propertyName))
			super.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		byte oldValue,
		byte newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		char oldValue,
		char newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		short oldValue,
		short newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		int oldValue,
		int newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		long oldValue,
		long newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		float oldValue,
		float newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		double oldValue,
		double newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 * See the <a href="#override">Implementation Note</a>
	 * for more information.
	 */
	public void firePropertyChange(
		String propertyName,
		boolean oldValue,
		boolean newValue) {
	}


	
	

}

