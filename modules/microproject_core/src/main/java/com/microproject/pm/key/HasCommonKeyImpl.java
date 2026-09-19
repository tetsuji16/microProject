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
package com.microproject.pm.key;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.server.data.DataObject;
public class HasCommonKeyImpl extends HasUniqueIdImpl implements HasKey{
	private static final long serialVersionUID = 7392928769651766L;
	protected Date created = new Date();
	protected String name = "";

	private static Field nameFieldInstance = null;
	public static Field getNameField() {
		if (nameFieldInstance == null)
			nameFieldInstance = Configuration.getFieldFromId("Field.name");
		return nameFieldInstance;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	public String getName(FieldContext fieldContext) {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	public HasCommonKeyImpl(DataObject hasUniqueId,long uniqueId) {
		super(hasUniqueId,uniqueId);
	}
	public HasCommonKeyImpl(boolean local,DataObject hasUniqueId) {
		super(local,hasUniqueId);
	}

	public long getId() {
		return uniqueId;
	}

	public void setId(long id) {
		uniqueId = id;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public boolean equals(Object other) {
		if (! (other instanceof HasKey))
			return false;
		return uniqueId == ((HasKey)other).getUniqueId();
	}

	@Override
	public int hashCode() {
		// consistent with the uniqueId-based equals above (issue #177)
		return Long.hashCode(uniqueId);
	}

	public void serialize(ObjectOutputStream s) throws IOException {
	    s.writeLong(getUniqueId());
	    //s.writeLong(getId());
	}

	//call init to complete initialization
	public static HasCommonKeyImpl deserialize(ObjectInputStream s,DataObject hasUniqueId) throws IOException, ClassNotFoundException  {
		long uniqueId=s.readLong();
		HasCommonKeyImpl hasKey=new HasCommonKeyImpl(hasUniqueId,uniqueId);
	    //hasKey.setUniqueId(uniqueId);
	    return hasKey;
	}

	//because it implements DataObject, should implement a different interface
	private transient boolean dirty;
	public boolean isDirty() {
		return dirty;
	}
	public void setDirty(boolean dirty) {
		//System.out.println("HasCommonKeyImpl _setDirty("+dirty+"): "+getName());
		this.dirty = dirty;
	}



}
