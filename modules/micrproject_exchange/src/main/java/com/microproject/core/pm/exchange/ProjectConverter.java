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
package com.microproject.core.pm.exchange;

import java.lang.reflect.Method;

/**
 * @author Laurent Chretienneau
 *
 */
public class ProjectConverter {
	public static enum Type{
		OPTIONS("Options"),
		CALENDAR("Calendar"),
		PROJECT("Project"),
		TASK("Task"),
		RESOURCE("Resource"),
		ASSIGNMENT("Assignment"),
		DEPENDENCY("Dependency"),
		TIMEPHASED("Timephased");
		
		protected String name;
		private Type(String name){
			this.name=name;
		}
		public String getName(){
			return name;
		}
	}
	
	protected static ProjectConverter instance;
	public static ProjectConverter getInstance(){
		if (instance==null)
			instance=new ProjectConverter();
		return instance;
	}
	
	public Object convert(String format, Type type, boolean from, Object externalObject, Object projectlibreObject, Object state) throws Exception{
		String converterName = resolveConverterName(format, type);
		Class<?> converterClass;
		try {
			converterClass = Class.forName(converterName);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Unsupported converter for format " + format + " and type " + type.getName(), e);
		}
		Method method = resolveConverterMethod(converterClass, converterName, from);
		Object converter = converterClass.getDeclaredConstructor().newInstance();
		if (method.getReturnType().equals(Void.TYPE)) {
			method.invoke(converter, new Object[] { externalObject, projectlibreObject, state });
			return from ? projectlibreObject : externalObject;
		}
		return method.invoke(converter, new Object[] { from ? externalObject : projectlibreObject, state });
	}

	private String resolveConverterName(String format, Type type) {
		if (format == null) {
			throw new IllegalArgumentException("Unsupported import/export format: null");
		}
		String converterName;
		if (format.equalsIgnoreCase("mpx")) {
			converterName = "com.microproject.core.pm.exchange.converters.mpx.Mpx";
		} else if (format.equalsIgnoreCase("op")) {
			converterName = "com.microproject.core.pm.exchange.converters.op.Op";
		} else {
			throw new IllegalArgumentException("Unsupported import/export format: " + format);
		}
		return converterName + type.getName() + "Converter";
	}

	private Method resolveConverterMethod(Class<?> converterClass, String converterName, boolean from) {
		for (Method method : converterClass.getMethods()) {
			if ((from && "from".equals(method.getName())) || (!from && "to".equals(method.getName()))) {
				return method;
			}
		}
		throw new IllegalStateException("No suitable converter method found on " + converterName);
	}
	
}
