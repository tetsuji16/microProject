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
package com.microproject.dialog.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.microproject.dialog.FieldDialog;
import com.microproject.help.HelpUtil;
import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.field.ObjectRef;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.pm.task.BelongsToDocument;
/**
 *
 */
public class FieldComponentMap implements ObjectRef {
	private Object object;
	private Node node = null;
	private NodeModel nodeModel = null;
	private Collection<?> collection = null;
	private Map<String, JComponent> map = new HashMap<>();
	private FieldDialog fieldDialog;
	private NodeModelDataFactory dataFactory;
	private FieldWriter fieldWriter;

	@FunctionalInterface
	public interface FieldWriter {
		void write(Field field, FieldComponentMap target, Object source, Object value,
				FieldContext context, boolean text) throws FieldParseException;
	}

	public FieldComponentMap(Object object) {
		this.object = object;
		setDataFactoryFromObject(object);
	}
	
	public FieldComponentMap(Node node, NodeModel nodeModel) {
		this.node = node;
		this.nodeModel = nodeModel;
		dataFactory=nodeModel.getDataFactory();
	}

	public FieldComponentMap(Collection<?> collection) {
		this.collection = collection;
		if (collection!=null&& collection.size()>0){
			setDataFactoryFromObject(collection.iterator().next());
		}
	}
	
	private void setDataFactoryFromObject(Object object){
		if (object instanceof BelongsToDocument) dataFactory=(NodeModelDataFactory)((BelongsToDocument)object).getDocument();
	}
	
	public JComponent getComponent(String fieldId, int flag) {
		JComponent component = map.get(fieldId);
		if (component == null) {
			Field field = Configuration.getFieldFromId(fieldId);
			component = ComponentFactory.componentFor(field,this, flag);
			map.put(fieldId,component);
		}
		return component;
	}
	public String getLabel(String fieldId) {
		Field field = Configuration.getFieldFromId(fieldId);
		return field.getName();
	}
	
	// updates all components
	public void updateAll() {
		for (String fieldId : map.keySet()) {
			Field field = Configuration.getFieldFromId(fieldId);
			JComponent component = getComponent(fieldId, 0); // argument 0 shouldn't matter because exists already
			ComponentFactory.updateValueOfComponent(component,field,this);
		}
	}
	
	public JComponent append(DefaultFormBuilder builder, String fieldId) {
		return appendField(builder,fieldId,0);
	}

	public void append(DefaultFormBuilder builder, Collection<? extends Field> fields) {
		for (Field field : fields) {
			appendField(builder,field.getId(),0);
			builder.nextLine(2);
		}
	}

	public JComponent appendReadOnly(DefaultFormBuilder builder, String fieldId) {
		return appendField(builder,fieldId,ComponentFactory.READ_ONLY);
	}

	public JComponent appendSometimesReadOnly(DefaultFormBuilder builder, String fieldId) {
		return appendField(builder,fieldId,ComponentFactory.SOMETIMES_READ_ONLY);
	}

	public JComponent appendField(DefaultFormBuilder builder, String fieldId, int flag) {
		Field field = Configuration.getFieldFromId(fieldId);
		if (field == null)
			return null;
		JComponent component = getComponent(fieldId, flag);
		if (component instanceof JCheckBox) // checkboxes already have a label to the right
			builder.append(component);
		else 
			builder.append(getLabel(fieldId)+":",component);
		String fieldDoc = field.getHelp();
		if (fieldDoc != null)
			HelpUtil.addDocHelp(component,fieldDoc);
		return component;
	}
	
	public JComponent append(DefaultFormBuilder builder, String fieldId, int span) {
		Field field = Configuration.getFieldFromId(fieldId);
		if (field == null)
			return null;
		JComponent component = getComponent(fieldId,0);
		boolean isCheckbox = component instanceof JCheckBox;
		CellConstraints cc = new CellConstraints().xyw(builder.getColumn() + (isCheckbox ? 0 : 2), builder.getRow(), span);
		if (component instanceof JCheckBox) {// checkboxes already have a label to the right
			builder.add(component,cc);
		} else {
			builder.addLabel(getLabel(fieldId)+":");
			builder.nextColumn(2);
			builder.add(component,cc);
			builder.nextColumn(1);
		}
		String fieldDoc = field.getHelp();
		if (fieldDoc != null)
			HelpUtil.addDocHelp(component,fieldDoc);
		return component;
	}
	
	public Node getNode() {
		return node;
	}
	public WalkersNodeModel getNodeModel() {
		return nodeModel;
	}
	public Object getObject() {
		return object;
	}
	public void setObject(Object object) {
		this.object = object;
	}
	/**
	 * @return Returns the collection.
	 */
	public Collection<?> getCollection() {
		return collection;
	}
	/**
	 * @param collection The collection to set.
	 */
	public void setCollection(Collection<?> collection) {
		this.collection = collection;
	}
	
	public void setFieldDialog(FieldDialog fieldDialog) {
		this.fieldDialog = fieldDialog;
	}
	
	public NodeModelDataFactory getDataFactory(){
		return dataFactory;
	}

	public void setFieldWriter(FieldWriter fieldWriter) { this.fieldWriter = fieldWriter; }
	boolean write(Field field, Object source, Object value, FieldContext context, boolean text)
			throws FieldParseException {
		if (fieldWriter == null) return false;
		fieldWriter.write(field, this, source, value, context, text);
		return true;
	}
}
