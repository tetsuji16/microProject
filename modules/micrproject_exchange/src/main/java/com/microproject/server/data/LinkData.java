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


/**
 *
 */
public class LinkData extends SerializedDataObject {
	static final long serialVersionUID = 44526718293877L;
    protected TaskData predecessor;
    protected TaskData successor;
    protected long predecessorId=-1L;
//    protected long externalId=-1L;

    public static final SerializedDataObjectFactory FACTORY=new SerializedDataObjectFactory(){
        public SerializedDataObject createSerializedDataObject(){
            return new LinkData();
        }
    };

    public TaskData getPredecessor() {
        return predecessor;
    }
    public void setPredecessor(TaskData predecessor) {
        this.predecessor = predecessor;
        setPredecessorId(predecessor.getUniqueId());
    }
    public TaskData getSuccessor() {
        return successor;
    }
    public void setSuccessor(TaskData successor) {
        this.successor = successor;
        setSuccessorId(successor.getUniqueId());
    }


    public long getPredecessorId() {
		return predecessorId;
	}
	public void setPredecessorId(long predecessorId) {
		this.predecessorId = predecessorId;
	}
    public long getSuccessorId() {
		return getUniqueId();
	}
	public void setSuccessorId(long successorId) {
		setUniqueId(successorId);
	}

//	public long getExternalId() {
//		return externalId;
//	}
//	public void setExternalId(long externalId) {
//		this.externalId = externalId;
//	}
	public int getType(){
        return DataObjectConstants.LINK_TYPE;
    }

    public void emtpy(){
    	predecessor=null;
    	successor=null;
    }

	public boolean equals(Object obj){
		if (!super.equals(obj)) return false;
		if (obj instanceof LinkData){
			LinkData data=(LinkData)obj;
			return data.getPredecessorId()==getPredecessorId();
		}else return false;
	}
	@Override
	public int hashCode(){
		// consistent with the uniqueId + predecessorId equals above (issue #177)
		return Long.hashCode(getUniqueId()) * 31 + Long.hashCode(getPredecessorId());
	}

	public String toString() {
		return "predId = " + getPredecessorId()
		+ ", succId = " + getSuccessorId()
		+ ", pred=" + predecessor
		+ ", succ=" + successor;
	}


    public void renumber(IDGenerator idGenerator){
    	super.renumber(idGenerator);
    	setPredecessorId(idGenerator.getId(getPredecessorId()));
    }


}
