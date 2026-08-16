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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.microproject.util.ClassUtils;

/**
 * This class represents a set of choices where the list of choices and the finder are specified via reflection.
 * It is used by Fields
 */
public class DynamicSelect extends Select implements Finder {
	/**
	 * 
	 */
	public DynamicSelect() {
		super();
	}
	
	public Object[] getKeyArrayWithoutNull() {
		try {
			return (Object[]) listMethod.invoke(null, new Object[0]);
		} catch (Exception e) {
			Field.log.error("error calling keyArrayFromMethod for:" + listMethod);
			return null;
		}
	}
	
	public List getValueListWithoutNull(){
		throw new RuntimeException ("Not implemented");
	}
	
	private Method listMethod = null;
	private Method finderMethod = null;
	public void setList(String methodName) {
		listMethod = ClassUtils.staticVoidMethodFromFullName(methodName);
		if (listMethod == null)
			Field.log.error("invalid method in select:" + methodName);
	}
	public void setFinder(String finderName) {
		finderMethod = ClassUtils.staticMethodFromFullName(finderName, new Class[] {String.class});
		if (finderMethod == null)
			Field.log.error("invalid method in select:" + finderName);
	}

	public Object getValue(Object arg0) throws InvalidChoiceException  {
		if (arg0 == null)
			return null;
		String name = arg0.toString();
		if (StringUtils.isEmpty(name))
			return null;
		Object result = find(name,null);
		if (result == null && (!isAllowNull() || name != EMPTY))
			throw new InvalidChoiceException(ObjectUtils.toString(name));
		return result;
	}
	public Object getKey(Object arg0) {
		return ObjectUtils.toString(arg0);
	}

	public Object find(Object key, Collection container) {
		String name = (String)key;
		try {
			return finderMethod.invoke(null, new Object[] {name});
		} catch (Exception e) {
			return null;
		}
	}
	public boolean isStatic() {
		return false;
	}

}
