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
package com.microproject.grouping.core;

import java.util.LinkedList;

import com.microproject.pm.key.HasId;
import com.microproject.server.data.DataObject;
import com.microproject.session.SessionFactory;

/**
 *
 */
public class VoidNodeImpl implements HasId, DataObject  {
	protected static LinkedList fields=new LinkedList();
	long id = 0;
	public LinkedList getFields() throws NodeException {
		return fields;
	}
	public String toString() {
		return "";
	}
	public boolean isNormal() {
		return false;
	}
	public boolean isCritical() {
		return false;
	}
	public boolean isSummary() {
		return false;
	}
	public boolean isAssignment() {
		return false;
	}
	public final long getId() {
		return id;
	}
	public final void setId(long id) {
		this.id = id;
	}



	protected String name="";
	// Issue #268 / #227: VoidNodeImpl is a placeholder/scaffolding node. Its uniqueId
	// used to be minted from LocalSession.localSeed at construction time. Because
	// localSeed is a persistent per-JVM counter and VoidNodeImpl.setUniqueId() is a
	// no-op, the minted value was serialized into the .pod and changed on every
	// load, causing non-deterministic (drifting) round-trip output. The placeholder
	// never uses its uniqueId as a real identity key (callers only check
	// `instanceof VoidNodeImpl`), so a fixed sentinel keeps the file deterministic.
	protected static final long VOID_NODE_UNIQUE_ID = -1L;
	protected long uniqueId = VOID_NODE_UNIQUE_ID;
	public String getName() {
		return name;
	}
	public long getUniqueId() {
		return uniqueId;
	}
	public boolean isDirty() {
		return false;
	}
	public void setDirty(boolean dirty) {
		//System.out.println("VoidNodeImpl _setDirty("+dirty+"): "+getName());

	}
	public void setName(String name) {
	}
	public void setUniqueId(long id) {
	}



}
