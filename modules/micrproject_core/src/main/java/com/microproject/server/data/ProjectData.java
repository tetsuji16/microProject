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

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.microproject.company.ApplicationUser;
import com.microproject.configuration.Configuration;
import com.microproject.datatype.ImageLink;
import com.microproject.field.DelegatesFields;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.pm.costing.EarnedValueCalculator;
import com.microproject.pm.costing.EarnedValueIndicatorFields;
import com.microproject.pm.key.HasName;
import com.microproject.session.SessionFactory;

/**
 *
 */
public class ProjectData extends DocumentData implements HasName,DelegatesFields,EarnedValueIndicatorFields,Comparable {
	static final long serialVersionUID = 722537477839L;
	//web
	public static final long GANTT=1L;
	public static final long NETWORK=16L;
	public static final long SVG=32L;
	public static final long PNG=64L;
	public static final long PDF=128L;
	//database
	public static final long GANTT_SVG=GANTT|SVG;
	public static final long GANTT_PDF=GANTT|PDF;
	public static final long GANTT_PNG=GANTT|PNG;
	public static final long NETWORK_SVG=NETWORK|SVG;
	public static final long NETWORK_PDF=NETWORK|PDF;
	public static final long NETWORK_PNG=NETWORK|PNG;


    protected CalendarData calendar;
    protected Collection<? extends DataObject> resources;
    protected Collection<? extends DataObject> tasks;
    protected long calendarId=-1;
    protected long lockedById;
    protected String lockedByName;
    protected long idleTime,allowedIdleTime;
    protected Date creationDate,lastModificationDate;
    protected Collection<DistributionData> distributions;
    protected Map<String, Object> fieldValues;
    protected Map<String, Object> extraFields;
    protected Collection<? extends DataObject> referringSubprojectTasks;
    protected long availableImages=GANTT_SVG|GANTT_PNG|NETWORK_SVG|NETWORK_PNG;
    protected String group;
    protected String division;
    protected int expenseType;
    protected int projectType;
    protected int projectStatus;
    protected int accessControlPolicy;
    protected float version=1.2f;
    protected long[] unchangedTasks;
    protected long[] unchangedLinks;
    protected boolean incrementalDistributions;
	//protected transient long externalId=-1L;
	protected transient Map<String, Object> attributes;

    public static final SerializedDataObjectFactory FACTORY=new SerializedDataObjectFactory(){
        public SerializedDataObject createSerializedDataObject(){
            return new ProjectData();
        }
    };

    public CalendarData getCalendar() {
        return calendar;
    }
    public void setCalendar(CalendarData calendar) {
        this.calendar = calendar;
        setCalendarId((calendar==null)?-1L:calendar.getUniqueId());
    }
    public Collection<? extends DataObject> getResources() {
        return resources;
    }
    public void setResources(Collection<? extends DataObject> resources) {
        this.resources = resources;
    }
    public Collection<? extends DataObject> getTasks() {
        return tasks;
    }
    public void setTasks(Collection<? extends DataObject> tasks) {
        this.tasks = tasks;
    }

    public int getType(){
        return DataObjectConstants.PROJECT_TYPE;
    }
	public long getCalendarId() {
		return calendarId;
	}
	public void setCalendarId(long calendarId) {
		this.calendarId = calendarId;
	}

    public String getLockedByName() {
		return lockedByName;
	}
	public void setLockedByName(String lockedByName) {
		this.lockedByName = lockedByName;
	}
    public long getLockedById() {
		return lockedById;
	}
	public void setLockedById(long lockedById) {
		this.lockedById = lockedById;
	}
	public long getIdleTime() {
		return idleTime;
	}
	public void setIdleTime(long idleTime) {
		this.idleTime = idleTime;
	}
	public long getAllowedIdleTime() {
		return allowedIdleTime;
	}
	public void setAllowedIdleTime(long allowedIdleTime) {
		this.allowedIdleTime = allowedIdleTime;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public Date getLastModificationDate() {
		return lastModificationDate;
	}
	public void setLastModificationDate(Date lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}
    public Collection<DistributionData> getDistributions() {
        return distributions;
    }
    public void setDistributions(Collection<DistributionData> distributions) {
        this.distributions = distributions;
    }
	public void emtpy(){
    	super.emtpy();
    	calendar=null;
    	resources=null;
    	tasks=null;
    }

    public boolean canBeUsed(){
    	if (lockedById<=0) return true;
    	ApplicationUser user=SessionFactory.getInstance().getSession(false).getUser();
    	if (user==null/*for offline gantt*/||lockedById==user.getUniqueId()) return true;
    	return (idleTime>allowedIdleTime);
    }
    public boolean isLocked() {
    	return lockedByName != null && lockedByName.length() > 0;
    }

    public String getLockerInfo(){
		String lockerName=getLockedByName();
		if (lockerName==null) return null;
		if (getIdleTime()>allowedIdleTime)
			lockerName+="("+"Idle: "+(getIdleTime()/60000)+"min)";
		return lockerName;

    }
	public final Map<String, Object> getFieldValues() {
		return fieldValues;
	}
	public final void setFieldValues(Map<String, Object> fieldValues) {
		this.fieldValues = fieldValues;
	}
	public final Map<String, Object> getExtraFields() {
		return extraFields;
	}
	public final void setExtraFields(Map<String, Object> extraFields) {
		this.extraFields = extraFields;
	}
    public final Collection<? extends DataObject> getReferringSubprojectTasks() {
        return referringSubprojectTasks;
    }
    public final void setReferringSubprojectTasks(Collection<? extends DataObject> referringSubprojectTasks) {
        this.referringSubprojectTasks = referringSubprojectTasks;
    }

	public long getAvailableImages() {
		return availableImages;
	}
	public void setAvailableImages(long availableImages) {
		this.availableImages = availableImages;
	}


	public Object getDelegatedFieldValue(Field field) {
		if (fieldValues == null)
			return null;
		return fieldValues.get(field.getId());
	}
	public boolean delegates(Field field) {
		if (field == getGanttSnapshotField()
			|| field == getNetworkSnapshotField()
			|| field.getId().equals("Field.creationDate")
			|| field.getId().equals("Field.lastModificationDate")
			|| field.getId().equals("Field.lockedByName")
			|| field.getId().equals("Field.locked")
			|| field.getId().equals("Field.name")
			|| field.getId().equals("Field.scheduleStatusIndicator")
			|| field.getId().equals("Field.statusIndicator")
			|| field.getId().equals("Field.budgetStatusIndicator"))


			return false;
		return true;
	}


	private static Field ganttSnapshotFieldInstance = null;
	public static Field getGanttSnapshotField() {
		if (ganttSnapshotFieldInstance == null)
			ganttSnapshotFieldInstance = Configuration.getFieldFromId("Field.ganttSnapshot");
		return ganttSnapshotFieldInstance;
	}
	public ImageLink getGanttSnapshot() {
		return new ImageLink("Gantt Snapshot"
				,"gantt"
				,((availableImages&GANTT_SVG)==GANTT_SVG)?"/img/littleGantt.jpg":""
				,"application.icon"
				,""+getUniqueId(),true);

	}

	private static Field networkSnapshotFieldInstance = null;
	public static Field getNetworkSnapshotField() {
		if (networkSnapshotFieldInstance == null)
			networkSnapshotFieldInstance = Configuration.getFieldFromId("Field.networkSnapshot");
		return networkSnapshotFieldInstance;
	}
	public ImageLink getNetworkSnapshot() {
		return new ImageLink("Network Snapshot"
				,"network"
				,((availableImages&NETWORK_SVG)==NETWORK_SVG)?"/img/littleNetwork.png":""
				,"network.icon"
				,""+getUniqueId(),true);

	}

	public ImageLink getScheduleStatusIndicator() {
		Double spi = (Double)fieldValues.get("Field.spi");
		if (spi == null)
			spi = Double.valueOf(0.0D);
		return EarnedValueCalculator.getInstance().getScheduleStatusIndicator(spi.doubleValue());
	}
	public ImageLink getBudgetStatusIndicator() {
		Double cpi = (Double)fieldValues.get("Field.cpi");
		if (cpi == null)
			cpi = Double.valueOf(0.0D);
		return EarnedValueCalculator.getInstance().getBudgetStatusIndicator(cpi.doubleValue());
	}
	public ImageLink getStatusIndicator() {


		Double csi = (Double)fieldValues.get("Field.csi");
		if (csi == null) {
			Double spi = (Double)fieldValues.get("Field.spi");
			Double cpi = (Double)fieldValues.get("Field.cpi");
			if (spi == null || cpi == null)
				csi = Double.valueOf(0.0D);
			else
				csi = Double.valueOf(spi.doubleValue() * cpi.doubleValue());
		}
		return EarnedValueCalculator.getInstance().getStatusIndicator(csi.doubleValue());
	}

	public String getDivision() {
		return division;
	}
	public void setDivision(String division) {
		this.division = division;
	}
	public int getExpenseType() {
		return expenseType;
	}
	public void setExpenseType(int expenseType) {
		this.expenseType = expenseType;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public int getProjectType() {
		return projectType;
	}
	public void setProjectType(int projectType) {
		this.projectType = projectType;
	}
	public int getProjectStatus() {
		return projectStatus;
	}
	public void setProjectStatus(int projectStatus) {
		this.projectStatus = projectStatus;
	}
	public String getName(FieldContext context) {
		return getName();
	}
	public int compareTo(Object o) {
		return getName().compareTo(((HasName)o).getName());
	}
	public int getAccessControlPolicy() {
		return accessControlPolicy;
	}
	public void setAccessControlPolicy(int accessControlPolicy) {
		this.accessControlPolicy = accessControlPolicy;
	}
	public float getVersion() {
		return version;
	}

	public void setVersion(float version) {
		this.version = version;
	}
	public long[] getUnchangedTasks() {
		return unchangedTasks;
	}
	public void setUnchangedTasks(long[] unchangedTasks) {
		this.unchangedTasks = unchangedTasks;
	}
	public long[] getUnchangedLinks() {
		return unchangedLinks;
	}
	public void setUnchangedLinks(long[] unchangedLinks) {
		this.unchangedLinks = unchangedLinks;
	}
	public boolean isIncrementalDistributions() {
		return incrementalDistributions;
	}
	public void setIncrementalDistributions(boolean incrementalDistributions) {
		this.incrementalDistributions = incrementalDistributions;
	}

//	public long getExternalId() {
//		return externalId;
//	}
//
//	public void setExternalId(long externalId) {
//		this.externalId = externalId;
//	}
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}


}
