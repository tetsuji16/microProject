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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public  class OptionsFilter {
	private static final Logger logger = Logger.getLogger(OptionsFilter.class.getName());
	private static final Map<MethodKey, Method> METHODS = new ConcurrentHashMap<>();

	private record MethodKey(Class<?> type, String name) {
	}
	private String method;

	public Object[] getOptionValues(Object obj, String method, String[] optionKeys, Object[] optionValues) {
		List keys=new ArrayList(optionKeys.length);
		keys.addAll(Arrays.asList(optionKeys));
		try {
			Method m=findMethod(obj, method);
			m.invoke(obj, new Object[]{keys,optionValues});
		} catch (Exception e) {
			logger.log(Level.FINE, "Failed to invoke method {0} on {1}", new Object[]{method, obj.getClass().getName()});
		}
		return keys.toArray();
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Object[] getOptions(Object[] optionKeys, List optionValues, Object obj){
		if (obj==null) return optionKeys;
		List keys=new ArrayList(optionKeys.length);
		keys.addAll(Arrays.asList(optionKeys));
		try {
			Method m=findMethod(obj, method);
			m.invoke(obj, new Object[]{keys,optionValues});
		} catch (Exception e) {
			logger.log(Level.FINE, "Failed to invoke method {0} on {1}", new Object[]{method, obj.getClass().getName()});
		}
		return keys.toArray();
	}

	private static Method findMethod(Object object, String name) throws NoSuchMethodException {
		MethodKey key = new MethodKey(object.getClass(), name);
		Method cached = METHODS.get(key);
		if (cached != null)
			return cached;
		Method resolved = object.getClass().getMethod(name, List.class, List.class);
		Method previous = METHODS.putIfAbsent(key, resolved);
		return previous == null ? resolved : previous;
	}
}
