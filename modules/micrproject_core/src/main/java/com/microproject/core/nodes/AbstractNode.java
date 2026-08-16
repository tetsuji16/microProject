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
package com.microproject.core.nodes;

import java.util.HashMap;
import java.util.Map;

import org.projectlibre.core.configuration.Configuration;
import org.projectlibre.core.dictionary.DictionaryCategory;

import com.microproject.core.fields.Field;
import com.microproject.core.fields.FieldUtil;
import com.microproject.core.fields.HasFields;

/**
 * @author Laurent Chretienneau
 *
 */
public class AbstractNode implements Node, HasFields{
	protected NodeId id;
	protected Map<String, Object> fieldValues=new HashMap<String, Object>();
	protected NodeContainer container;
	
	@Override
	public NodeId getId() {
		return id;
	}
	@Override
	public void setId(NodeId id) {
		this.id = id;
	}
	
	@Override
	public NodeContainer getContainer() {
		return container;
	}
	@Override
	public void setContainer(NodeContainer container) {
		this.container = container;
	}
	@Override
	public Object getPropertyValue(String property) {
		return fieldValues.get("Field."+property);
	}
	@Override
	public void setPropertyValue(String property, Object value) {
		fieldValues.put("Field."+property,value);
	}
	@Override
	public Object getFieldValue(String fieldId) {
		return fieldValues.get(fieldId);
	}
	@Override
	public void setFieldValue(String fieldId, Object value) {
		fieldValues.put(fieldId,value);
	}
	
	public String toString(String tab){
		StringBuilder s = new StringBuilder();
		s.append(tab).append("id=").append(id).append('\n');
		for (String fieldId : fieldValues.keySet())
			s.append(tab).append('*').append(fieldId).append('=').append(fieldValues.get(fieldId)).append('\n');
		return s.toString();		
	}
	public String toString(){
		return toString("");
	}

}
