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
package com.microproject.pm.resource;

import com.microproject.field.FieldContext;

/**
 * Fields specific to resource class
 */
public interface ResourceSpecificFields  {
	double getRemainingOvertimeCost();
	String getGroup();
	void setGroup(String group);
	String getInitials();
	void setInitials(String initials);
	String getPhonetics();
	void setPhonetics(String phonetics);
	String getRbsCode();
	void setRbsCode(String wbsCode);
	String getEmailAddress();
	void setEmailAddress(String emailAddress);
	String getMaterialLabel();
	void setMaterialLabel(String materialLabel);
	boolean isReadOnlyMaterialLabel(FieldContext fieldContext);
	String getUserAccount();
	void setUserAccount(String userAccount);
	boolean isGeneric();
	void setGeneric(boolean generic);
	boolean isInactive();
	void setInactive(boolean inactive);
	boolean isWork();
	boolean isMaterial();
	boolean isMe();
	long getExternalId();
	void setExternalId(long externalId);
}
