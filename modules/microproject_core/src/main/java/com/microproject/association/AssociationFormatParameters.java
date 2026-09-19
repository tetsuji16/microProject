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
package com.microproject.association;

import com.microproject.pm.dependency.HasDependencies;

/**
 * Used to customize the formatting and parsing of associations
 */
public class AssociationFormatParameters {
	Object thisObject;
	boolean leftAssociation;
	com.microproject.field.Field idField;
	boolean encloseInBrackets = false;
	private String error;
	private boolean allowDetailsEntry;
	public static AssociationFormatParameters getInstance(HasDependencies thisObject,boolean leftAssociation, com.microproject.field.Field idField, boolean encloseInBrackets, boolean allowDetailsEntry) {
		return new AssociationFormatParameters(thisObject,leftAssociation,idField, encloseInBrackets, allowDetailsEntry);
	}
	
	private AssociationFormatParameters(HasDependencies thisObject,boolean leftAssociation, com.microproject.field.Field idField, boolean encloseInBrackets, boolean allowDetailsEntry) {
		this.thisObject = thisObject;
		this.leftAssociation = leftAssociation;
		this.idField = idField;
		this.encloseInBrackets = encloseInBrackets;
		this.allowDetailsEntry = allowDetailsEntry;
	}
	/**
	 * @return Returns the error.
	 */
	public String getError() {
		return error;
	}
	/**
	 * @param error The error to set.
	 */
	public void setError(String error) {
		this.error = error;
	}
	/**
	 * @return Returns the encloseInBrackets.
	 */
	public boolean isEncloseInBrackets() {
		return encloseInBrackets;
	}
	/**
	 * @return Returns the idField.
	 */
	public com.microproject.field.Field getIdField() {
		return idField;
	}
	/**
	 * @return Returns the leftAssociation.
	 */
	public boolean isLeftAssociation() {
		return leftAssociation;
	}
	/**
	 * @return Returns the thisObject.
	 */
	public Object getThisObject() {
		return thisObject;
	}
	/**
	 * @return Returns the allowDetailsEntry.
	 */
	public boolean isAllowDetailsEntry() {
		return allowDetailsEntry;
	}
}
