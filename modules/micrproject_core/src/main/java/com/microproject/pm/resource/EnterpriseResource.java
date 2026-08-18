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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import com.microproject.algorithm.ReverseQuery;
import com.microproject.algorithm.TimeIteratorGenerator;
import com.microproject.algorithm.buffer.CalculatedValues;
import com.microproject.algorithm.buffer.GroupedCalculatedValues;
import com.microproject.association.AssociationList;
import com.microproject.company.ApplicationUser;
import com.microproject.configuration.CircularDependencyException;
import com.microproject.datatype.ImageLink;
import com.microproject.datatype.Rate;
import com.microproject.datatype.RateFormat;
import com.microproject.datatype.TimeUnit;
import com.microproject.document.Document;
import com.microproject.field.CustomFields;
import com.microproject.field.CustomFieldsImpl;
import com.microproject.field.FieldContext;
import com.microproject.interval.InvalidValueObjectForIntervalException;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.HasAssignments;
import com.microproject.pm.assignment.HasAssignmentsImpl;
import com.microproject.pm.assignment.TimeDistributedFields;
import com.microproject.pm.assignment.timesheet.TimesheetHelper;
import com.microproject.pm.availability.AvailabilityTable;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.costing.Accrual;
import com.microproject.pm.costing.CostRateTable;
import com.microproject.pm.costing.CostRateTables;
import com.microproject.pm.costing.EarnedValueCalculator;
import com.microproject.pm.key.HasKeyImpl;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.util.Environment;

/**
 * A global resource that belongs to the enterprise resource pool
 */
public class EnterpriseResource implements Resource {
	static final long serialVersionUID = 273977742329L;
	private static Resource UNASSIGNED = null;
	public static final int UNASSIGNED_ID = -65535; // correponds to MSDI


	public long getEarliestAssignmentStart() {
		return hasAssignments.getEarliestAssignmentStart();
	}

	public boolean hasActiveAssignment(long start, long end) {
		return hasAssignments.hasActiveAssignment(start, end);
	}

	public EnterpriseResource(ResourcePool resourcePool) {
		this(resourcePool==null||resourcePool.isLocal(),resourcePool);
	}
	public EnterpriseResource(boolean local,ResourcePool resourcePool) {
		hasKey = new HasKeyImpl(local,this);
		this.resourcePool = resourcePool;
		if (resourcePool != null) {
			workCalendar = WorkingCalendar.getInstanceBasedOn(resourcePool.getDefaultCalendar());
			workCalendar.setName("");
		}
	}

	/**
	 * @return
	 */
	public static Resource getUnassignedInstance() {
		if (UNASSIGNED == null) {
			UNASSIGNED = new EnterpriseResource(null); //local
			UNASSIGNED.setName(Messages.getString("Text.Unassigned"));
			UNASSIGNED.setUniqueId(UNASSIGNED_ID);
		}
		return UNASSIGNED;
	}

	public boolean isDefault(){
		return getUniqueId()==UNASSIGNED_ID;
	}




	transient HasAssignments hasAssignments = new HasAssignmentsImpl();
	private transient HasKeyImpl hasKey;
	protected transient ResourcePool resourcePool;

	protected String notes = "";
	protected String group = "";
	protected String initials = "";
	protected String phonetics = "";
	protected String rbsCode = "";
	protected String emailAddress="";
	protected String materialLabel="";
	protected String userAccount="";
	protected int resourceType = ResourceType.WORK;
	protected transient CostRateTables costRateTables = new CostRateTables();
	protected double maximumUnits = 1.0D;
	protected boolean generic = false;
	protected boolean inactive = false;
	protected transient CustomFieldsImpl customFields = new CustomFieldsImpl();
	protected long externalId=-1;

	public long getExternalId() {
		return externalId;
	}

	public void setExternalId(long externalId) {
		this.externalId = externalId;
	}

	public int getResourceType() {
		return resourceType;
	}

	public String toString() {
		return getName();
	}


	public void setResourceType(int resourceType) {
		if (resourceType == this.resourceType)
			return;
		boolean oldIsLabor = isLabor();
		this.resourceType = resourceType;

		// if resource type changes to/from labor, then initialize rates
		if (oldIsLabor != isLabor()) {
			setStandardRate(new Rate());
			setOvertimeRate(new Rate());

			if (!isLabor()) { // Non labor resources have no time unit
				getStandardRate().setTimeUnit(TimeUnit.NON_TEMPORAL);
				getOvertimeRate().setTimeUnit(TimeUnit.NON_TEMPORAL);
			}
		}
	}


	public double getCostPerUse() {
		return costRateTables.getCostPerUse();
	}

	public Rate getOvertimeRate() {
		return costRateTables.getOvertimeRate();
	}

	public Rate getStandardRate() {
		return costRateTables.getStandardRate();
	}



	public void setCostPerUse(double costPerUse) {
		costRateTables.setCostPerUse(costPerUse);
	}



	public void setOvertimeRate(Rate overtimeRate) {
		if (!isLabor())
			overtimeRate.makeUnitless();
		costRateTables.setOvertimeRate(overtimeRate);
	}



	public void setStandardRate(Rate standardRate) {
		if (!isLabor())
			standardRate.makeUnitless();
		costRateTables.setStandardRate(standardRate);
	}


	public long getEffectiveDate() {
		return costRateTables.getEffectiveDate();
	}
	public void setEffectiveDate(long effectiveDate) throws InvalidValueObjectForIntervalException {
		costRateTables.setEffectiveDate(effectiveDate);
	}
	public boolean isReadOnlyEffectiveDate(FieldContext fieldContext) {
		return costRateTables.isReadOnlyEffectiveDate(fieldContext);
	}

	/**
	 * @return Returns the costRateTable.
	 */
	public CostRateTable getCostRateTable(int costRateIndex) {
		return costRateTables.getCostRateTable(costRateIndex);
	}


	protected int accrueAt = Accrual.PRORATED;
	public int getAccrueAt() {
		return accrueAt;
	}



	public void setAccrueAt(int accrueAt) {
		this.accrueAt = accrueAt;
	}

	public void addAssignment(Assignment assignment) {
		hasAssignments.addAssignment(assignment);
	}
	/*public void addDefaultAssignment() {
		hasAssignments.addAssignment(newDefaultAssignment());
	}
	private Assignment newDefaultAssignment() {
		return Assignment.getInstance(NormalTask
				.getUnassignedInstance(),this, 1.0, 0);
	}*/

	/**
	 * @param reverseQuery
	 */
	public void buildReverseQuery(ReverseQuery reverseQuery) {
		hasAssignments.buildReverseQuery(reverseQuery);
	}




	/**
	 * @param resource
	 * @return
	 */
	public Assignment findAssignment(Resource resource) {
		return hasAssignments.findAssignment(resource);
	}

	/**
	 * @param task
	 * @return
	 */
	public Assignment findAssignment(Task task) {
		return hasAssignments.findAssignment(task);
	}

	/**
	 * @return Returns the maxUnits.
	 */
	public double getMaximumUnits() {
		return maximumUnits;
	}
	/**
	 * @param maxUnits The maxUnits to set.
	 */
	public void setMaximumUnits(double maxUnits) {
		this.maximumUnits = maxUnits;
	}
	/**
	 * @return
	 */
	public AssociationList getAssignments() {
		return hasAssignments.getAssignments();
	}

	public int getSchedulingType() {
		return hasAssignments.getSchedulingType();
	}

	public int hashCode() {
		return hasAssignments.hashCode();
	}

	public boolean isEffortDriven() {
		return false;
	}

	public void removeAssignment(Assignment assignment) {
		hasAssignments.removeAssignment(assignment);
	}

	public void setEffortDriven(boolean effortDriven) {
		hasAssignments.setEffortDriven(effortDriven);
	}

	public void setSchedulingType(int schedulingType) {
		hasAssignments.setSchedulingType(schedulingType);
	}

	public void updateAssignment(Assignment modified) {
		hasAssignments.updateAssignment(modified);
	}


	public static Consumer<Object> forAllAssignments(Consumer<Object> visitor) {
		return HasAssignmentsImpl.forAllAssignments(visitor);
	}

	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	public void forEachWorkingInterval(Consumer<Object> visitor, boolean mergeWorking, WorkCalendar workCalendar) {
		hasAssignments.forEachWorkingInterval(visitor, mergeWorking, workCalendar);
	}



	protected WorkCalendar workCalendar = null;

	public void setWorkCalendar(WorkCalendar workCalendar) {
		this.workCalendar = workCalendar;
	}



	public WorkCalendar getWorkCalendar() {
		return workCalendar;
	}



	public WorkCalendar getEffectiveWorkCalendar() {
		return workCalendar; // can be null
	}



	public boolean isReadOnlyEffortDriven(FieldContext fieldContext) {
		return hasAssignments.isReadOnlyEffortDriven(fieldContext);
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public String getInitials() {
		return initials;
	}
	public void setInitials(String initials) {
		this.initials = initials;
		if (getName() == null) // for the case where the resource is created by entering initials, set name too
			setName(initials);
	}
	public String getPhonetics() {
		return phonetics;
	}
	public void setPhonetics(String phonetics) {
		this.phonetics = phonetics;
	}



	public double getRemainingOvertimeCost() {
		// TODO implement this
		return -1;
	}
	public Date getCreated() {
		return hasKey.getCreated();
	}
	public long getId() {
		return hasKey.getId();
	}
	public String getName() {
		return hasKey.getName();
	}
	public long getUniqueId() {
		return hasKey.getUniqueId();
	}
//	public void setNew(boolean isNew) {
//		hasKey.setNew(isNew);
//	}
	/**
	 * @param created
	 */
	public void setCreated(Date created) {
		hasKey.setCreated(created);
	}
	/**
	 * @param id
	 */
	public void setId(long id) {
		hasKey.setId(id);
	}
	/**
	 * @param name
	 */
	public void setName(String name) {
		hasKey.setName(name);
		// set initials too to first character of name if initials is empty
		if (getInitials() == null || getInitials().length() == 0) {
			if (name != null && name.length() > 0)
				setInitials(name.substring(0,1));
		}
		if (workCalendar != null)
			workCalendar.setName(name);

	}
	/**
	 * @param id
	 */
	public void setUniqueId(long id) {
		hasKey.setUniqueId(id);
	}

	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double actualCost(long start, long end) {
		return hasAssignments.actualCost(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public long actualWork(long start, long end) {
		return hasAssignments.actualWork(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public long remainingWork(long start, long end) {
		return hasAssignments.remainingWork(start, end);
	}


	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double acwp(long start, long end) {
		return hasAssignments.acwp(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double bac(long start, long end) {
		return hasAssignments.bac(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double bcwp(long start, long end) {
		return hasAssignments.bcwp(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double bcws(long start, long end) {
		return hasAssignments.bcws(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double cost(long start, long end) {
		return hasAssignments.cost(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public long work(long start, long end) {
		return hasAssignments.work(start, end);
	}
	/**
	 * @param context
	 * @return
	 */
	public String getName(FieldContext context) {
		return hasKey.getName(context);
	}
	/**
	 * @param type
	 * @param generator
	 * @param values
	 */
	public void calcDataBetween(Object type, TimeIteratorGenerator generator,
			CalculatedValues values) {
		hasAssignments.calcDataBetween(type, generator, values);
	}
	/**
	 * @return Returns the rbsCode.
	 */
	public String getRbsCode() {
		return rbsCode;
	}
	/**
	 * @param rbsCode The rbsCode to set.
	 */
	public void setRbsCode(String rbsCode) {
		this.rbsCode = rbsCode;
	}
	/**
	 * @return Returns the resourcePool.
	 */
	public ResourcePool getResourcePool() {
		return resourcePool;
	}
	public void setResourcePool(ResourcePool resourcePool) {
		this.resourcePool = resourcePool;
	}
	public Document getDocument() {
		return resourcePool;
	}
	/**
	 * @return
	 */
	public Collection childrenToRollup() {
		return hasAssignments.childrenToRollup();
	}
	public double getCost(FieldContext fieldContext) {
		return cost(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public long getWork(FieldContext fieldContext) {
		return work(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getActualCost(FieldContext fieldContext) {
		return actualCost(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public long getActualWork(FieldContext fieldContext) {
		return actualWork(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public long getRemainingWork(FieldContext fieldContext) {
		return remainingWork(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getRemainingCost(FieldContext fieldContext) {
		return getCost(fieldContext) - getActualCost(fieldContext);
	}


	public double getAcwp(FieldContext fieldContext) {
		return acwp(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getBac(FieldContext fieldContext) {
		return bac(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getBcwp(FieldContext fieldContext) {
		return bcwp(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getBcws(FieldContext fieldContext) {
		return bcws(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getCv(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().cv(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getSv(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().sv(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getEac(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().eac(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getVac(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().vac(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getCpi(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().cpi(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getSpi(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().spi(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getCsi(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().csi(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getCvPercent(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().cvPercent(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getSvPercent(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().svPercent(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getTcpi(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().tcpi(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}

	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double baselineCost(long start, long end) {
		return hasAssignments.baselineCost(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public long baselineWork(long start, long end) {
		return hasAssignments.baselineWork(start, end);
	}
	public double getBaselineCost(int numBaseline, FieldContext fieldContext) {
		return baselineCost(FieldContext.start(fieldContext),FieldContext.end(fieldContext));	}
	public long getBaselineWork(int numBaseline, FieldContext fieldContext) {
		return baselineWork(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	private boolean isFieldHidden(FieldContext fieldContext) {
		return false;
	}

	private boolean isBaselineFieldHidden(int numBaseline, FieldContext fieldContext) {
		boolean foundChild = false;
		Iterator i = childrenToRollup().iterator();
		while (i.hasNext()) {
			Object child = i.next();
			if (!(child instanceof TimeDistributedFields)) {
				continue;
			}
			foundChild = true;
			if (!((TimeDistributedFields) child).fieldHideBaselineCost(numBaseline, fieldContext)) {
				return false;
			}
		}
		return !foundChild;
	}

	public boolean fieldHideCost(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideWork(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideActualCost(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideActualWork(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideBaselineCost(int numBaseline,FieldContext fieldContext) {
		return isBaselineFieldHidden(numBaseline,fieldContext);
	}
	public boolean fieldHideBaselineWork(int numBaseline,FieldContext fieldContext) {
		return isBaselineFieldHidden(numBaseline,fieldContext);
	}
	public boolean fieldHideAcwp(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideBac(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideBcwp(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideBcws(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideCv(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideSv(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideEac(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideVac(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideCpi(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideSpi(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideCvPercent(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideSvPercent(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideTcpi(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	/**
	 * @param workCalendar
	 * @return
	 */
	public long calcActiveAssignmentDuration(WorkCalendar workCalendar) {
		return hasAssignments.calcActiveAssignmentDuration(workCalendar);
	}


	public boolean isAssignment() { //for filters
		return false;
	}


	/**
	 * @return Returns the emailAddress.
	 */
	public String getEmailAddress() {
		return emailAddress;
	}
	/**
	 * @param emailAddress The emailAddress to set.
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	/**
	 * @return Returns the materialLabel.
	 */
	public String getMaterialLabel() {
		return materialLabel;
	}
	/**
	 * @param materialLabel The materialLabel to set.
	 */
	public void setMaterialLabel(String materialLabel) {
		this.materialLabel = materialLabel;
	}
	public boolean isLabor() {
		return resourceType == ResourceType.WORK; // work resources are time based

	}
	public boolean isReadOnlyMaterialLabel(FieldContext fieldContext) {
		return isLabor();
	}

	public String getUserAccount() {
		return userAccount;
	}
	public final void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}

	/**
	 * @return Returns the active.
	 */
	public boolean isInactive() {
		return inactive;
	}
	/**
	 * @param inactive The active to set.
	 */
	public void setInactive(boolean inactive) {
		this.inactive = inactive;
	}
	/**
	 * @return Returns the generic.
	 */
	public boolean isGeneric() {
		return generic;
	}
	/**
	 * @param generic The generic to set.
	 */
	public void setGeneric(boolean generic) {
		this.generic = generic;
	}

	private static short DEFAULT_VERSION=2;
	private short version=DEFAULT_VERSION;

	public short getVersion() {
		return version;
	}
	private void writeObject(ObjectOutputStream s) throws IOException {
	    s.defaultWriteObject();
	    hasKey.serialize(s);
	    costRateTables.serialize(s);
	    customFields.serialize(s);
    	s.writeInt(hasAssignments.getSchedulingType());
    	s.writeBoolean(hasAssignments.isEffortDriven());
	    availabilityTable.serialize(s);
	}
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException  {
	    s.defaultReadObject();
	    hasKey=HasKeyImpl.deserialize(s,this);
	    costRateTables=CostRateTables.deserialize(s);
	    try {
	    	customFields=CustomFieldsImpl.deserialize(s);
	    } catch (java.io.OptionalDataException e) {
	    	// to ensure compatibilty with old files
	    	customFields = new CustomFieldsImpl();
	    }
		hasAssignments = new HasAssignmentsImpl();
	    if (version>=2){
	    	hasAssignments.setSchedulingType(s.readInt());
	    	hasAssignments.setEffortDriven(s.readBoolean());
		    availabilityTable=AvailabilityTable.deserialize(s);
	    }else availabilityTable=new AvailabilityTable(null);
	    version=DEFAULT_VERSION;
	}

	public Object clone(){
		try {
			EnterpriseResource resource=(EnterpriseResource)super.clone();
			resource.hasKey=new HasKeyImpl(isLocal()&&Environment.getStandAlone(),resource);
			resource.setName(getName());
			if (notes!=null) resource.notes = notes;
			if (group!=null)resource.group = group;
			if (initials!=null)resource.initials = initials;
			if (phonetics!=null)resource.phonetics = phonetics;
			if (rbsCode!=null)resource.rbsCode = rbsCode;
			if (emailAddress!=null)resource.emailAddress = emailAddress;
			if (materialLabel!=null)resource.materialLabel = materialLabel;
			if (userAccount != null)
			   resource.userAccount = userAccount;

			resource.costRateTables=(CostRateTables)costRateTables.clone();
			resource.hasAssignments=(HasAssignments)((HasAssignmentsImpl)hasAssignments).cloneWithResource(resource);
			resource.customFields=(CustomFieldsImpl)customFields.clone();

			resource.availabilityTable=(AvailabilityTable)availabilityTable.clone();
			resource.availabilityTable.initAfterCloning();



			return resource;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	public void cleanClone(){
		resourcePool=null;
	}

	public WorkCalendar getBaseCalendar() {
		if (getWorkCalendar() == null)
			return null;
		return (WorkingCalendar) ((WorkingCalendar)getWorkCalendar()).getBaseCalendar();
	}

	public void setBaseCalendar(WorkCalendar baseCalendar) throws CircularDependencyException {
		WorkCalendar old = getWorkCalendar();
		if (old == null)
			return;

		CalendarService.getInstance().reassignCalendar(this,old,baseCalendar);

		((WorkingCalendar)getWorkCalendar()).changeBaseCalendar(baseCalendar);
		invalidateAssignmentCalendars(); // assignments intersection calendars need to be recalculated

	}

	// these fields are not modifiable
	public void setWork(long work, FieldContext fieldContext) {
		//do nothing
	}
	public void setRemainingWork(long work, FieldContext fieldContext) {
		//do nothing
	}
	public void setActualWork(long work, FieldContext fieldContext) {
		//do nothing
	}
	public boolean isReadOnlyWork(FieldContext fieldContext) {
		return true;
	}
	public boolean isReadOnlyActualWork(FieldContext fieldContext) {
		return true;
	}
	public boolean isReadOnlyRemainingWork(FieldContext fieldContext) {
		return true;
	}
	public double getActualFixedCost(FieldContext fieldContext) {
		return 0;
	}
	public boolean fieldHideActualFixedCost(FieldContext fieldContext) {
		return true;
	}

	public double fixedCost(long start, long end) {
		return 0;
	}

	public double actualFixedCost(long start, long end) {
		return 0;
	}

	public double getFixedCost(FieldContext fieldContext) {
		return 0;
	}

	public void setFixedCost(double fixedCost, FieldContext fieldContext) {
	}

	public boolean isReadOnlyFixedCost(FieldContext fieldContext) {
		return true;
	}

	public String getTimeUnitLabel() {
		if (getResourceType() == ResourceType.WORK)
			return null;
		return getMaterialLabel();
	}
	public boolean fieldHideOvertimeRate(FieldContext fieldContext) {
		return !isLabor();
	}

	public boolean fieldHideBaseCalendar(FieldContext fieldContext) {
		return !isLabor();
	}

//	public boolean isNew() {
//		return hasKey.isNew();
//	}
	public boolean hasLaborAssignment() {
			return isLabor() && !getAssignments().isEmpty();
	}
	public void invalidateAssignmentCalendars() {
		hasAssignments.invalidateAssignmentCalendars();
	}

	public Document invalidateCalendar() {
		invalidateAssignmentCalendars();
		return getResourcePool();
	}

	public boolean isWork() {
		return getResourceType() == ResourceType.WORK;
	}

	public boolean isMaterial() {
		return getResourceType() == ResourceType.MATERIAL;
	}

	public boolean isCost() {
		return getResourceType() == ResourceType.COST;
	}

	public boolean isMe() {
		if (userAccount==null) return false;
		return userAccount.equals(Environment.getLogin());
	}

	public boolean isParent() {
		// currently the model contains ResourceImpls and not enterprise resources
		return false;
	}

	public long getParentId(int outlineNumber) {
		// currently the model contains ResourceImpls and not enterprise resources
		return 0;
	}
	public double getCustomCost(int i) {
		return customFields.getCustomCost(i);
	}
	public long getCustomDate(int i) {
		return customFields.getCustomDate(i);
	}
	public long getCustomDuration(int i) {
		return customFields.getCustomDuration(i);
	}
	public long getCustomFinish(int i) {
		return customFields.getCustomFinish(i);
	}
	public boolean getCustomFlag(int i) {
		return customFields.getCustomFlag(i);
	}
	public double getCustomNumber(int i) {
		return customFields.getCustomNumber(i);
	}
	public long getCustomStart(int i) {
		return customFields.getCustomStart(i);
	}
	public String getCustomText(int i) {
		return customFields.getCustomText(i);
	}
	public void setCustomCost(int i, double cost) {
		customFields.setCustomCost(i, cost);
	}
	public void setCustomDate(int i, long date) {
		customFields.setCustomDate(i, date);
	}
	public void setCustomDuration(int i, long duration) {
		customFields.setCustomDuration(i, duration);
	}
	public void setCustomFinish(int i, long finish) {
		customFields.setCustomFinish(i, finish);
	}
	public void setCustomFlag(int i, boolean flag) {
		customFields.setCustomFlag(i, flag);
	}
	public void setCustomNumber(int i, double number) {
		customFields.setCustomNumber(i, number);
	}
	public void setCustomStart(int i, long start) {
		customFields.setCustomStart(i, start);
	}
	public void setCustomText(int i, String text) {
		customFields.setCustomText(i, text);
	}
	public CustomFields getCustomFields() {
		return customFields;
	}

	public boolean applyTimesheet(Collection fieldArray, long timesheetUpdateDate) {
		return TimesheetHelper.applyTimesheet(getAssignments(),fieldArray,timesheetUpdateDate);
	}

	public long getLastTimesheetUpdate() {
		return TimesheetHelper.getLastTimesheetUpdate(getAssignments());
	}

	public boolean isPendingTimesheetUpdate() {
		return TimesheetHelper.isPendingTimesheetUpdate(getAssignments());
	}

	public int getTimesheetStatus() {
		return TimesheetHelper.getTimesheetStatus(getAssignments());
	}

	public String getTimesheetStatusName() {
		return TimesheetHelper.getTimesheetStatusName(getTimesheetStatus());
	}

	private transient boolean dirty;
	public boolean isDirty() {
		return dirty;
	}
	public void setDirty(boolean dirty) {
		//System.out.println("EnterpriseResource _setDirty("+dirty+"): "+getName());
		this.dirty = dirty;
	}

	public boolean isReadOnly() {
		return !master && !isLocal() && !Environment.getStandAlone();
	}

	protected transient boolean master;


	public boolean isLocal() {
		return hasKey.isLocal();
	}

	public void setLocal(boolean local) {
		hasKey.setLocal(local);
	}

	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
	}

	protected transient GroupedCalculatedValues globalWorkVector;
	public GroupedCalculatedValues getGlobalWorkVector() {
		return globalWorkVector;
	}
	public void setGlobalWorkVector(GroupedCalculatedValues globalWorkVector) {
		this.globalWorkVector = globalWorkVector;
	}


	public long getFinishOffset() {
		return 0;
	}

	public long getStartOffset() {
		return 0;
	}

	public RateFormat getRateFormat(){
		return RateFormat.getInstance(getTimeUnitLabel(), false, isLabor(), isLabor());
	}
	public String getResourceName(){
		return getName();
	}

	public ImageLink getBudgetStatusIndicator() {
		return EarnedValueCalculator.getInstance().getBudgetStatusIndicator(getCpi(null));
	}

	public ImageLink getScheduleStatusIndicator() {
		return EarnedValueCalculator.getInstance().getScheduleStatusIndicator(getSpi(null));
	}

	public boolean isUser() {
		return userAccount != null && userAccount.length() > 0;
	}

	public boolean renumber(boolean localOnly){
		return hasKey.renumber(localOnly);
	}

	public boolean isAssignedToSomeProject() {
		if (hasAssignments.getAssignments().size() > 0)
			return true;
		if (globalWorkVector == null || globalWorkVector.size() == 0) // note that this doesn't mean there isn't baseline info assigned
			return false;
		return true;
	}

	private transient Set<Integer> authorizedRoles;

	public Set<Integer> getAuthorizedRoles() {
		return authorizedRoles;
	}
	public void setAuthorizedRoles(Set<Integer> authorizedRoles) {
		this.authorizedRoles = authorizedRoles;
	}
	public void filterRoles(List keys,List values){
		if (authorizedRoles==null) return;
		Iterator k=keys.iterator();
		Iterator<Integer> v=((List<Integer>)values).iterator();
		Object inactiveKey=null;
		while (v.hasNext()) {
			Object key=k.next();
			int r=v.next();
			if (r==ApplicationUser.INACTIVE) inactiveKey=key;
			if ((r==ApplicationUser.INACTIVE&&getAssignments().size()>0)||
					!authorizedRoles.contains(r)) k.remove();
		}
		if (keys.size()==0) keys.add(inactiveKey); //occurs when an user becomes "inactive"
	}
	private transient int defaultRole;

	public int getDefaultRole() {
		return defaultRole;
	}
	public void setDefaultRole(int defaultRole) {
		this.defaultRole = defaultRole;
	}

	private transient int license;
	private transient int licenseOptions;

	public int getLicense(){
		return license;
	}
	public void setLicense(int license) {
		this.license = license;
	}

	public int getLicenseOptions() {
		return licenseOptions;
	}

	public void setLicenseOptions(int licenseOptions) {
		this.licenseOptions = licenseOptions;
	}

	public boolean isInactiveLicense(){
		return license==ApplicationUser.INACTIVE;
	}

	public boolean isExternal(){
		return (licenseOptions&ApplicationUser.EXTERNAL)==ApplicationUser.EXTERNAL;
	}

	public boolean isAdministrator(){
		return (licenseOptions&ApplicationUser.ADMINISTRATOR)==ApplicationUser.ADMINISTRATOR;
	}



	private transient AvailabilityTable availabilityTable = new AvailabilityTable(null);
	public AvailabilityTable getAvailabilityTable() {
		return availabilityTable;
	}


	protected transient Object serverMeta;


	public Object getServerMeta() {
		return serverMeta;
	}

	public void setServerMeta(Object serverMeta) {
		this.serverMeta = serverMeta;
	}


}
