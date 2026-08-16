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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * abastract Base class for selection lists
 */
public abstract class Select implements Map {

	private String name;
	private boolean allowNull = false;
	protected boolean sortKeys = false;
	public static final String EMPTY=" ";
	public abstract boolean isStatic();
	/**
	 * @param arg0
	 * @return
	 */
	public abstract Object getValue(Object arg0) throws InvalidChoiceException;

	/**
	 * @param arg0
	 * @return
	 */
	public abstract Object getKey(Object arg0);

	public abstract Object[] getKeyArrayWithoutNull();

	public Object[] getKeyArray() {
		Object[] result = getKeyArrayWithoutNull();
		if (result == null || !allowNull)
			return result;
		// if a null element should be added, add it at front
		Object[] resultWithNull = new Object[result.length+1];
		System.arraycopy(result,0,resultWithNull,1,result.length);
		resultWithNull[0] = EMPTY;
		return resultWithNull;
	}

	public abstract List getValueListWithoutNull();
	
	public List getValueList() {
		List result = getValueListWithoutNull();
		if (result == null || !allowNull)
			return result;
		// if a null element should be added, add it at front
		List resultWithNull=new ArrayList(result.size()+1);
		resultWithNull.add(null);
		return resultWithNull;
	}

	
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	public static class InvalidChoiceException extends Exception {
		/**
		 * 
		 */
		public InvalidChoiceException() {
			super();
		}

		/**
		 * @param arg0
		 */
		public InvalidChoiceException(String arg0) {
			super(arg0);
		}

		/**
		 * @param arg0
		 */
		public InvalidChoiceException(Throwable arg0) {
			super(arg0);
		}

		/**
		 * @param arg0
		 * @param arg1
		 */
		public InvalidChoiceException(String arg0, Throwable arg1) {
			super(arg0, arg1);
		}

	}

	public int size() {
		return 0;
	}

	public void clear() {
	}

	public boolean isEmpty() {
		return false;
	}

	public boolean containsKey(Object arg0) {
		return false;
	}

	public boolean containsValue(Object arg0) {
		return false;
	}

	public Collection values() {
		return null;
	}

	public void putAll(Map arg0) {
	}

	public Set entrySet() {
		return null;
	}

	public Set keySet() {
		return null;
	}

	public Object get(Object arg0) {
		try {
			return getValue(arg0);
		} catch (InvalidChoiceException e) {
			return null;
		}
	}

	public Object remove(Object arg0) {
		return null;
	}

	public Object put(Object arg0, Object arg1) {
		return null;
	}
	/**
	 * @return Returns the allowNull.
	 */
	public boolean isAllowNull() {
		return allowNull;
	}
	/**
	 * @param allowNull The allowNull to set.
	 */
	public void setAllowNull(boolean allowNull) {
		this.allowNull = allowNull;
	}
	public static String toConfigurationXMLOptions(LinkedHashMap map, String keyPrefix) {
//		MapIterator i = map.i();
		Iterator i = map.keySet().iterator();
		StringBuilder buf = new StringBuilder();
		HashSet duplicateSet = new HashSet(); // don't allow duplicate keys
		while (i.hasNext()) {
			String key = (String) i.next();
			// notion of key and value is switched
			String value = (String)map.get(key);
			int dupCount = 2;
			String newKey = key;
			while (duplicateSet.contains(newKey)) {
				newKey = key + "-" + dupCount++;
			}
			key = newKey;
			duplicateSet.add(key);
			if (key == null || key.length() == 0)
				continue;
			if (value == null || value.length() == 0)
				continue;
			key = keyPrefix + key;
//			String key = "<html>" + keyPrefix + ": " + "<b>" + i.getValue() +"</b></html>";
			buf.append(SelectOption.toConfigurationXML(key, value));
		}
		return buf.toString();
	}
	public final boolean isSortKeys() {
		return sortKeys;
	}
	public final void setSortKeys(boolean sortKeys) {
		this.sortKeys = sortKeys;
	}
	public String documentOptions() {
		StringBuilder result = new StringBuilder();
		for (Object key : getKeyArrayWithoutNull()) {
			if (result.length() > 0)
				result.append(", ");
			result.append(get(key)).append("=").append(key);
		}
		return result.toString();
	}

} 
