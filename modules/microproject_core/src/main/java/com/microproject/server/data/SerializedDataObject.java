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
 * preserialization to avoid deserialization followed by serialization on server
 *
 */
public class SerializedDataObject extends CommonDataObject {
	static final long serialVersionUID = 16280304846919L;

	public final static int UPDATE=1;
	public final static int MOVE=2;
//	public final static int INSERT=3;
//	public final static int MOVE=4;
//	public final static int REMOVE=8;
	protected int status;


    protected byte[] serialized;


    public byte[] getSerialized() {
        return serialized;
    }
    public void setSerialized(byte[] serialized) {
        this.serialized = serialized;
    }

	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}

	public void emtpy(){
		serialized=null;
		name=null;
		status=0;
	}


    public int getType(){
        return 0;
    }

    public String getPrefix(){
    	if (getType()==DataObjectConstants.COMPANY_TYPE) return "Company";
    	else if (getType()==DataObjectConstants.CALENDAR_TYPE) return "Calendar";
    	else if (getType()==DataObjectConstants.PROJECT_TYPE) return "Project";
    	else if (getType()==DataObjectConstants.ENTERPRISE_RESOURCE_TYPE) return "EnterpriseResource";
    	else if (getType()==DataObjectConstants.RESOURCE_TYPE) return "Resource";
    	else if (getType()==DataObjectConstants.TASK_TYPE) return "Task";
    	else if (getType()==DataObjectConstants.ASSIGNMENT_TYPE) return "Assignment";
    	else if (getType()==DataObjectConstants.LINK_TYPE) return "Link";
    	else if (getType()==DataObjectConstants.BANKING_INFO_TYPE) return "BankingInfo";
    	else if (getType()==DataObjectConstants.CREDIT_CARD_BANKING_INFO_TYPE) return "CreditCardBankingInfo";
    	else if (getType()==DataObjectConstants.CHECK_BANKING_INFO_TYPE) return "CheckBankingInfo";
       	else if (getType()==DataObjectConstants.USER_TYPE) return "User";
       	else if (getType()==DataObjectConstants.PARTNER_USER_TYPE) return "PartnerUser";
       	else if (getType()==DataObjectConstants.PARTNER_COMPANY_TYPE) return "PartnerCompany";

    	else return "Unknown";
    }

	protected long uniqueId=-1L;
    protected String name;
//    protected boolean dirty=true;


//    protected Map details=null;

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
		return (status&UPDATE)==UPDATE;
	}
	public void setDirty(boolean dirty) {
		//System.out.println("SerializedDataObject _setDirty("+dirty+"): "+getName());
		if (dirty) status|=UPDATE;
		else status=0;
	}
    public boolean isMoved() {
		return (status&MOVE)==MOVE;
	}
	public void setMoved(boolean moved) {
		if (moved) status|=MOVE;
	}

	public String toString(){
    	return name;//+"("+uniqueId+")";
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

	public boolean isExternal() { // will be true for external tasks
		return false;
	}


}
