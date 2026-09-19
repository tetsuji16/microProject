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

import java.io.Serializable;

import com.microproject.server.data.CommonDataObject;
import com.microproject.server.data.DataObject;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;

/**
 *
 */
public class HasUniqueIdImpl implements Serializable{
	private static final long serialVersionUID = 939382200022L;
	protected long uniqueId = -1L;
	protected transient Session session;
	protected transient boolean local;

    /**
     *
     */
    public HasUniqueIdImpl(DataObject hasUniqueId,long uniqueId) {
    	setLocal(CommonDataObject.isLocal(uniqueId));
    	//System.out.println((hasUniqueId==null?"null":hasUniqueId.getClass()+"")+" UniqueId "+uniqueId+", local? "+local);
    }
    public HasUniqueIdImpl(boolean local,DataObject hasUniqueId) {
    	setLocal(local);
    	//System.out.println((hasUniqueId==null?"null":hasUniqueId.getClass()+"")+" UniqueId ?, local? "+local);
		uniqueId = session.getId();
    }


	/**
	 * @return Returns the uniqueId.
	 */
	public long getUniqueId() {
		return uniqueId;
	}
	/**
	 * @param uniqueId The uniqueId to set.
	 */
	public void setUniqueId(long uniqueId) {
		this.uniqueId = uniqueId;
	}

	public boolean isLocal() {
		return local;
	}
	public void setLocal(boolean local) {
		this.local=local;
		session = SessionFactory.getInstance().getSession(local);
	}

	public boolean equals(Object other) {
		if (! (other instanceof HasUniqueIdImpl))
			return false;
		return uniqueId == ((HasUniqueIdImpl)other).getUniqueId();
	}

	@Override
	public int hashCode() {
		return Long.hashCode(uniqueId);
	}

	public boolean renumber(boolean localOnly){
		if (uniqueId==-1) return false;
		if (localOnly&&!CommonDataObject.isLocal(uniqueId)) return false;
		if (localOnly&&local) setLocal(false);
		uniqueId = session.getId();
		//System.out.println("Renumber "+(hasUniqueId==null?"":(hasUniqueId.getClass()+"/"+hasUniqueId.getName()))+": "+oldUniqueId+"-->"+uniqueId);
		return true;
	}


//	public static void update(Long uniqueId,Long newUniqueId){
//		DataObject hasUniqueId=(DataObject)uniqueIds.get(uniqueId);
//		if (hasUniqueId==null){
//			System.out.println("ERROR null DataObject!");
//			return;
//		}
//		hasUniqueId.setNew(false);
//	}
//
//	public static void update(Map updateMap){
//	    for (Iterator i=updateMap.entrySet().iterator();i.hasNext();){
//	        Map.Entry entry=(Map.Entry)i.next();
//	        update((Long)entry.getKey(),(Long)entry.getValue());
//	    }
//	}


//	public boolean isNew() {
//		return newId;
//	}
//	public void setNew(boolean newId) {
//		 this.newId=newId;
//	}

}
