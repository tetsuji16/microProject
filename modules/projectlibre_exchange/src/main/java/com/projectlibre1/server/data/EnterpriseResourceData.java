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

package com.projectlibre1.server.data;

import com.projectlibre1.algorithm.buffer.GroupedCalculatedValues;


/**
 *
 */
public class EnterpriseResourceData extends SerializedDataObject {
	static final long serialVersionUID = 555524422442L;
    protected CalendarData calendar;
    protected long calendarId=-1;
    protected EnterpriseResourceData parentResource;
    protected long childPosition;
    protected long parentResourceId=-1;
//    protected float version=1.0f;
//    protected boolean defaultResource;
//    protected boolean unassigned=false;
    //protected Collection assignments;

    public static final SerializedDataObjectFactory FACTORY=new SerializedDataObjectFactory(){
        public SerializedDataObject createSerializedDataObject(){
            return new EnterpriseResourceData();
        }
    };

    /*public Collection getAssignments() {
        return assignments;
    }
    public void setAssignments(Collection assignments) {
        this.assignments = assignments;
    }*/
    public long getChildPosition() {
        return childPosition;
    }
    public void setChildPosition(long childPosition) {
        this.childPosition = childPosition;
    }
    public EnterpriseResourceData getParentResource() {
        return parentResource;
    }
    public void setParentResource(EnterpriseResourceData parentResource) {
        this.parentResource = parentResource;
        setParentResourceId((parentResource==null)?-1L:parentResource.getUniqueId());
    }
    public CalendarData getCalendar() {
        return calendar;
    }
    public void setCalendar(CalendarData calendar) {
        this.calendar = calendar;
        setCalendarId((calendar==null)?-1L:calendar.getUniqueId());
    }

    public int getType(){
        return DataObjectConstants.ENTERPRISE_RESOURCE_TYPE;
    }


    protected long externalId;
    protected String emailAddress;
	protected String userAccount;


	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	public long getExternalId() {
		return externalId;
	}
	public void setExternalId(long externalId) {
		this.externalId = externalId;
	}
	public String getUserAccount() {
		return userAccount;
	}
	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}



//	public boolean isDefault() {
//		return getUniqueId()==EnterpriseResource.UNASSIGNED_ID;
//	}
//	public void setDefault(boolean unassigned) {
//		setUniqueId(EnterpriseResource.UNASSIGNED_ID);
//	}
//	public boolean isDefault() {
//		return defaultResource;
//	}
//	public void setDefault(boolean defaultResource) {
//		this.defaultResource = defaultResource;
//	}


	public long getParentResourceId() {
		return parentResourceId;
	}
	public void setParentResourceId(long parentResourceId) {
		this.parentResourceId = parentResourceId;
	}



    public long getCalendarId() {
		return calendarId;
	}
	public void setCalendarId(long calendarId) {
		this.calendarId = calendarId;
	}

	public void emtpy(){
    	super.emtpy();
    	emailAddress=null;
    	userAccount=null;
    	calendar=null;
    	parentResource=null;
    }


	protected GroupedCalculatedValues globalWorkVector;
	public GroupedCalculatedValues getGlobalWorkVector() {
		return globalWorkVector;
	}
	public void setGlobalWorkVector(GroupedCalculatedValues globalWorkVector) {
		this.globalWorkVector = globalWorkVector;
	}

    protected int[] authorizedRoles;
	public int[] getAuthorizedRoles() {
		return authorizedRoles;
	}
	public void setAuthorizedRoles(int[] authorizedRoles) {
		this.authorizedRoles = authorizedRoles;
	}

	protected int license;

	public int getLicense() {
		return license;
	}
	public void setLicense(int license) {
		this.license = license;
	}

	protected int licenseOptions;

	public int getLicenseOptions() {
		return licenseOptions;
	}
	public void setLicenseOptions(int licenseOptions) {
		this.licenseOptions = licenseOptions;
	}


}
