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
package com.microproject.core.fields;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.projectlibre.core.configuration.Configuration;
import org.projectlibre.core.dictionary.HasCategories;
import org.projectlibre.core.dictionary.HasStringId;
import org.projectlibre.strings.Strings;

/**
 * @author Laurent Chretienneau
 *
 */
@XmlRootElement(name="field")
@XmlAccessorType(XmlAccessType.NONE)
public class Field implements HasStringId, HasCategories{
	protected String id;
	protected String property,confProperty;
	protected Set<String> categories;
	protected boolean readOnly;

	@XmlID
	@XmlAttribute(name="id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@XmlAttribute(name="category")
	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Set<String> categories) {
		this.categories = categories;
	}

	@XmlAttribute(name="property")
	protected String getConfProperty() {
		return confProperty;
	}

	protected void setConfProperty(String confProperty) {
		this.confProperty = confProperty;
	}
	
	public String getProperty() {
		if (property==null){
			if (confProperty==null){
				int i=id.indexOf('.');
				property=id.substring(i+1);
			} else property=confProperty;
		}
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
		confProperty=property;
	}
	
	
	@XmlAttribute(name="readOnly")
	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	
	
	
	

	public String getStringValue(Object value){
		return value==null?"":value.toString();
	}

	public String getName(){
		return Strings.getString(id);
	}
	
	
	public static Field getField(String fieldId){
		return (Field)Configuration.getInstance().getDictionary().get(Field.class, fieldId);
	}




}
