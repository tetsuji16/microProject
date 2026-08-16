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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.commons.lang.ObjectUtils;

/**
 * This class manages a fixed list of values and their associated options, similar to an html select
 * It is used by Field class
 */
public class StaticSelect extends Select {
	DualHashBidiMap stringMap = new DualHashBidiMap();
	DualHashBidiMap objectMap = null;
	Object[] keyArray = null;
	ArrayList orderedValueList = new ArrayList();
	boolean integerValues = true;
	
	public StaticSelect() {
	}
	
	public void add(String key, Object value) {
		put(key, value);
	}
	
	public void addOption(SelectOption option) {
		if (integerValues)
			option.value = Integer.valueOf(option.value.toString());
		add(option.key, option.value);
		Object staticObject = option.getStaticObject();
		if (staticObject != null) { // if object associated, use it
			if (objectMap == null)
				objectMap = new DualHashBidiMap();
			objectMap.put(option.value,staticObject);
		}
	}
	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public Object put(Object arg0, Object arg1) {
		orderedValueList.add(arg1);
		return stringMap.put(arg0, arg1);
	}
	
	public String toString() {
		MapIterator i = stringMap.mapIterator();
		StringBuilder result = new StringBuilder();
		while (i.hasNext()) {
			i.next();
			result.append("[key]" + i.getKey() + " [value]" + i.getValue() + "\n");
		}
		return result.toString();
	}
	 
	public Object[] getKeyArrayWithoutNull() {
		synchronized(this) {
			if (keyArray == null) {
				keyArray = new Object[orderedValueList.size()];
				Iterator i = orderedValueList.iterator();
				int index = 0;
				while (i.hasNext()) {
					Object n = i.next();
					keyArray[index++] = stringMap.getKey(n);
				}
			}
		}
		return keyArray;
	}
	public List getValueListWithoutNull() {
		return orderedValueList;
	}

	public Object getValue(Object arg0) throws InvalidChoiceException {
		if (arg0 == EMPTY && isAllowNull())
			return null;
		Object result = stringMap.get(arg0);
		if (result == null)
			throw new InvalidChoiceException(ObjectUtils.toString(arg0));
		return result;
	}

	public Object getKey(Object arg0) {
		return stringMap.getKey(arg0);		
	}

	public boolean isStatic() {
		return true;
	}

	public final boolean isIntegerValues() {
		return integerValues;
	}

	public final void setIntegerValues(boolean integerValues) {
		this.integerValues = integerValues;
	}

	
}
