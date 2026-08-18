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

import net.sf.jasperreports.engine.JRField;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;

/**
 *
 */
public class DecoratedField {
	/**
	 * @return Returns the fieldName.
	 */
	public String getFieldName() {
		return fieldName;
	}
	/**
	 * @param fieldName The fieldName to set.
	 */
	private void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	/**
	 * @return Returns the subField.
	 */
	public boolean isSubField() {
		return subField;
	}
	/**
	 * @param subField The subField to set.
	 */
	private void setSubField(boolean subField) {
		this.subField = subField;
	}
	/**
	 * @return Returns the textField.
	 */
	public boolean isTextField() {
		return textField;
	}
	/**
	 * @param textField The textField to set.
	 */
	private void setTextField(boolean textField) {
		this.textField = textField;
	}

	private String fieldName = "";
	private boolean textField = false;
	private boolean subField = false;
	private Class clazz = null;
	private String method = "";
	/**
	 * @return Returns the timeBased.
	 */
	public boolean isTimeBased() {
		return timeBased;
	}
	/**
	 * @param timeBased The timeBased to set.
	 */
	public void setTimeBased(boolean timeBased) {
		this.timeBased = timeBased;
	}
	private boolean timeBased = false;
	/**
	 * @return Returns the end.
	 */
	public long getEnd() {
		return end;
	}
	/**
	 * @param end The end to set.
	 */
	public void setEnd(long end) {
		this.end = end;
	}
	/**
	 * @return Returns the start.
	 */
	public long getStart() {
		return start;
	}
	/**
	 * @param start The start to set.
	 */
	public void setStart(long start) {
		this.start = start;
	}
	private long start = 0;
	private long end = 0;

	/**
	 * @return Returns the method.
	 */
	public String getMethod() {
		return method;
	}
	/**
	 * @param method The method to set.
	 */
	private void setMethod(String method) {
		this.method = method;
	}
	Field fieldForReportField() {
		return Configuration.getFieldFromId("Field." + fieldName);
	}
	/**
	 * @return Returns the clazz.
	 */
	public Class getClazz() {
		return clazz;
	}
	/**
	 * @param clazz The clazz to set.
	 */
	private void setClazz(Class clazz) {
		this.clazz = clazz;
	}
	
	public DecoratedField(JRField jrField) {

		String name = jrField.getName();
		
		String option = "MODText";
		if (name.indexOf(option) >= 0) {
			setTextField(true);
		}

		option = "METHOD";
		if(name.indexOf(option) >= 0) {
			String method = name.substring(name.indexOf(option) + option.length());
			method = method.substring(0, method.indexOf('_'));
			setMethod(method);
		}
		
		option = "FIELD";
		if(name.indexOf(option) >= 0) {
			fieldName = name.substring(name.indexOf(option) + option.length());
		}
		else
		{
			fieldName = name;
		}
		
		setFieldName(fieldName);

		option = "TIME";
		if(name.indexOf(option) >= 0) {
			String timeString = name.substring(name.indexOf(option) + option.length());
			String startString = timeString.substring(0, timeString.indexOf('_'));
			String endString = timeString.substring(timeString.indexOf('_') + 1);
//			System.out.println("time based field start " + startString + ", end " + endString);
			endString = endString.substring(0, endString.indexOf('_'));
//			System.out.println("time based field start " + startString + ", end " + endString);
			setTimeBased(true);
			setStart(Long.valueOf(startString).longValue());
			setEnd(Long.valueOf(endString).longValue());
//			System.out.println("time based field start " + getStart() + ", end " + getEnd());
		}
	}
}
