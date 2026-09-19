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
package com.microproject.reports.adapter;

import java.util.Collection;
import java.util.Iterator;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import org.apache.commons.collections.Predicate;

import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.ObjectRef;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.grouping.core.transform.filtering.PredicatedNodeFilterIterator;
import com.microproject.pm.task.Project;
import com.microproject.pm.time.MutableInterval;

/**
 * A Data Source for Jasper Reports
 */
public class DataSource implements JRDataSource, ObjectRef {

	private WalkersNodeModel nodeModel = null;
	private FieldContext context = null;
	private MutableInterval interval = null;
	private Iterator iterator;
	
	boolean nodeBased = false;
	
	public void setIterator(Iterator iterator) {
		this.iterator = iterator;
	}
	public void setPredicate(Predicate predicate) {
		((PredicatedNodeFilterIterator)iterator).setPredicate(predicate);
	}
	public void setNodeBased(boolean nodeBased) {
		this.nodeBased = nodeBased;
		if (iterator instanceof PredicatedNodeFilterIterator)
			((PredicatedNodeFilterIterator)iterator).setNodeBased(nodeBased);
	}

	public WalkersNodeModel getNodeModel() {
		return nodeModel;
	}
	
	public void setNodeModel(WalkersNodeModel nodeModel) {
		this.nodeModel = nodeModel;
	}

	private boolean isNodeBased() {
		return nodeBased;
	}

	Object currentObject = null;
	
	Project project = null;
	DecoratedField cleanField = null;
	
	public DataSource() {
	}
	
	
	
	public void setTimeBased(boolean timeBased) {
		if (timeBased) {
			// initialize interval treatment
			context = new FieldContext();
			interval = new MutableInterval(0, 0);
			context.setInterval(interval);
		}
		
	}
	
	public void setProject(Project project) {
		this.project = project;
	}
	
	public boolean next() throws JRException {
		if (!iterator.hasNext()) {
			currentObject = null;
			return false;
		}

		currentObject = iterator.next();
		return true;
	}
public Object getFieldValue(JRField jrField) throws JRException {
		// check if asked field must be converted to a String (field name begins whith Text_)
		cleanField = new DecoratedField(jrField);
		
		// get ProjectLibre equiv. field
		Field field = cleanField.fieldForReportField();
		if(cleanField.isTimeBased()) {
			setInterval(cleanField.getStart(), cleanField.getEnd());
		}
		Object result;
		if(cleanField.isTextField()) {
			result = field.getText(this, context);
		} else {
			// convert ProjectLibre type into jasper accepted types	
			result = DataSourceProvider.fieldValueConverterToPrimitiveType(field,field.getValue(this,context));
		}
		return result;
	}

	public Node getNode() {
		if (isNodeBased()) {
			return (Node)currentObject;
		}
		return null;
	}

	public Object getObject() {
		return currentObject;
	}
	public void setInterval(long start, long end) {
		interval.setStart(start);
		interval.setEnd(end);
	}
public Collection getCollection() {
		return null;
	}
	public NodeModelDataFactory getDataFactory() {
		if (nodeModel instanceof NodeModel) {
			return ((NodeModel)nodeModel).getDataFactory();
		}
		return null;
	}




}

