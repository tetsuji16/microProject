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
package com.microproject.server.data;

import com.microproject.pm.key.uniqueid.UniqueIdException;
import com.microproject.session.SessionFactory;

/**
 *
 */
public class CommonDataObject implements DataObject{
	static final long serialVersionUID = 182832738299990L;

	protected long uniqueId=-1L;
    protected String name;
//    protected boolean dirty=true;


//    protected Map details=null;

    /**
     *
     */
    public CommonDataObject() {
        super();
    }
    public long getUniqueId() {
        return uniqueId;
    }
    public void setUniqueId(long id) {
        this.uniqueId = id;
    }
//	public boolean isNew() {
//		return isNew;
//	}
//	public void setNew(boolean isNew) {
//		this.isNew = isNew;
//	}
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public boolean isLocal(){
        return isLocal(this);
    }

    public boolean isDirty() {
		return false;//return dirty;
	}
	public void setDirty(boolean dirty) {
		//System.out.println("CommonDataObject _setDirty("+dirty+"): "+getName());
		//this.dirty = dirty;
	}

	public String toString(){
    	return name;//+"("+uniqueId+")";
    }

	public boolean equals(Object obj){
		if (obj==null||!(obj instanceof DataObject)) return false;
		else return ((DataObject)obj).getUniqueId()==getUniqueId();
	}

	@Override
	public int hashCode() {
		return Long.hashCode(getUniqueId());
	}

    public static boolean isLocal(DataObject data){
        return isLocal(data.getUniqueId());
    }
    public static boolean isLocal(long uniqueId){
        return uniqueId<1000000000L;
    }
    public static void makeGlobal(DataObject data) throws UniqueIdException{
    	if (isLocal(data)) data.setUniqueId(SessionFactory.getInstance().getSession(false).getId());
    }

    public void renumber(IDGenerator idGenerator){
    	/*if (getUniqueId()>0)*/ setUniqueId(idGenerator.getId(getUniqueId()));
    }

}
