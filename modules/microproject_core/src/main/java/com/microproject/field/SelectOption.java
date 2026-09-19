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
package com.microproject.field;

import org.apache.commons.lang.StringEscapeUtils;

import com.microproject.strings.Messages;
import com.microproject.util.ClassUtils;

import java.util.logging.Level;
import java.util.logging.Logger;


public class SelectOption {
	private static final Logger logger = Logger.getLogger(SelectOption.class.getName());
	String key;
	Object value;
	String object = null;
	String objectField = null;
		/**
	 * @return Returns the key.
	 */
	public String getKey() {
		return key;
	}
	/**
	 * @param key The key to set.
	 */
	public void setKey(String key) {
		this.key = Messages.getStringOrSelf(key);
	}
	/**
	 * @return Returns the value.
	 */
	public Object getValue() {
		return value;
	}
	/**
	 * @param value The value to set.
	 */
	public void setValue(Object value) {
		this.value = value;
	}
	
	 
	
	/**
	 * @return Returns the object.
	 */
	public Object getObject() {
		return object;
	}
	/**
	 * @param object The object to set.
	 */
	public void setObject(String object) {
		this.object = object;
	}
	/**
	 * @return Returns the objectField.
	 */
	public String getObjectField() {
		return objectField;
	}
	/**
	 * @param objectField The objectField to set.
	 */
	public void setObjectField(String objectField) {
		this.objectField = objectField;
	}
	
	Object getStaticObject() {
		if (object == null || objectField == null)
			return null;
		
		try {
			return ClassUtils.forName(object).getField(objectField).get(null);
		} catch (IllegalArgumentException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (IllegalAccessException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (NoSuchFieldException e) {
			logger.log(Level.WARNING, "Error", e);
		} catch (ClassNotFoundException e) {
			logger.log(Level.WARNING, "Error", e);
		}
		return null;
	}
	
	public static String toConfigurationXML(String key, String value) {
		return "<option key=\"" + StringEscapeUtils.escapeXml(key) + "\" value=\"" + StringEscapeUtils.escapeXml(value) + "\"/>";
	}
}
