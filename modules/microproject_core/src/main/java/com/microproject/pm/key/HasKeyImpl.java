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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.field.FieldContext;
import com.microproject.server.data.DataObject;

/**
 *
 */
public class HasKeyImpl extends HasUniqueIdImpl implements HasKey{
	private static final long serialVersionUID = 739020202L;
	private static final int MAX_NAME_LENGTH = 255;
	private static final Logger logger = Logger.getLogger(HasKeyImpl.class.getName());
	long id = 0L;
	Date created = new Date();
	String name = "";

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
		if (name != null && name.length() > MAX_NAME_LENGTH) {
			logger.log(Level.INFO, "Truncating long name from {0} chars to {1} chars", new Object[] {name.length(), MAX_NAME_LENGTH});
			name = name.substring(0, MAX_NAME_LENGTH);
		}
		this.name = name;
	}

	/**
	 *
	 */
	public HasKeyImpl(DataObject hasUniqueId,long uniqueId) {
		super(hasUniqueId,uniqueId);
	}
	public HasKeyImpl(boolean local,DataObject hasUniqueId) {
		super(local,hasUniqueId);
	}



	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getCreated() {
		return created;
	}


	/**
	 * @param created The created to set.
	 */
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
	    s.writeLong(getId());
	}

	//call init to complete initialization
	public static HasKeyImpl deserialize(ObjectInputStream s,DataObject hasUniqueId) throws IOException, ClassNotFoundException  {
		HasKeyImpl hasKey=new HasKeyImpl(hasUniqueId,s.readLong());
	    //hasKey.setUniqueId(s.readLong());
	    hasKey.setId(s.readLong());
//	    hasKey.setNew(false);
	    return hasKey;
	}

//	public boolean isNew() {
//		return CommonDataObject.isLocal(getUniqueId());
//	}

	//because it implements DataObject, should implement a different interface
	private transient boolean dirty;
	public boolean isDirty() {
		return dirty;
	}
	public void setDirty(boolean dirty) {
		//System.out.println("HasKeyImpl _setDirty("+dirty+"): "+getName());
		this.dirty = dirty;
	}


}
