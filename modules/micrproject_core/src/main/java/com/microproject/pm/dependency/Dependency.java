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
package com.microproject.pm.dependency;

import com.microproject.association.Association;
import com.microproject.association.AssociationList;
import com.microproject.association.InvalidAssociationException;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.document.Document;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.criticalpath.ScheduleWindow;
import com.microproject.pm.task.BelongsToDocument;
import com.microproject.pm.task.HasProject;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.server.data.DataObject;
import com.microproject.strings.Messages;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.List;

public class Dependency implements Association, BelongsToDocument, DataObject {
	private static final Logger logger = Logger.getLogger(Dependency.class.getName());
	static final long serialVersionUID = 283794049292031L;
	private int dependencyType;
	private transient HasDependencies predecessor;
	private transient HasDependencies successor;
	private transient boolean disabled = false;
	private long lag;

	private transient long earlyDate;
	private transient long lateDate;
	private String name;
	private long uniqueId = Long.MIN_VALUE;
	public static final long NEEDS_CALCULATION = -1;

	public static Dependency getInstance(HasDependencies predecessor,
			HasDependencies successor) {
		return getInstance(predecessor,successor,DependencyType.FS,0);
	}

	public static Dependency getInstance(HasDependencies predecessor,
			HasDependencies successor, int dependencyType, long lead) {
		return new Dependency(predecessor, successor, dependencyType, lead);
	}

	public static Dependency getInstance(HasDependencies predecessor,
			HasDependencies successor, DependencyType.Kind dependencyType, long lead) {
		return getInstance(predecessor, successor, dependencyType.code(), lead);
	}

	private Dependency(HasDependencies predecessor, HasDependencies successor,
			int dependencyType, long lead) {
		this.predecessor = predecessor;
		this.successor = successor;
		this.dependencyType = dependencyType;
		this.lag = lead;
	}

	public void updateDependencyLists() {
		predecessor.getSuccessorList().add(this);
		successor.getPredecessorList().add(this);
	}


	public HasDependencies getPredecessor() {
		return predecessor;
	}
	public HasDependencies getSuccessor() {
		return successor;
	}


	public long getLag() {
		return lag;
	}

	public void setLag(long lead) {
		this.lag = lead;
	}

	/**
	 * Copy the fields lag and type
	 *
	 */
	public void copyPrincipalFieldsFrom(Association from) {
		this.lag = ((Dependency)from).lag;
		this.dependencyType = ((Dependency)from).dependencyType;

	}

	/**
	 * @return Returns the calendar.
	 */
	public final WorkCalendar getEffectiveWorkCalendar() {
		return predecessor.getHasCalendar().getEffectiveWorkCalendar(); // use
																		// the
																		// predecessor's
																		// calendar
	}

	/**
	 * @param predecessor
	 *            The predecessor to set.
	 */
	public void setPredecessor(HasDependencies predecessor) {
		this.predecessor = predecessor;
	}

    public void setSuccessor(HasDependencies successor) {
        this.successor = successor;
    }
	/**
	 * @return Returns the dependencyType.
	 */
	public int getDependencyType() {
		return dependencyType;
	}

	/** Returns the type-safe view of the persisted dependency code. */
	public DependencyType.Kind getDependencyKind() {
		return DependencyType.Kind.fromCode(dependencyType);
	}

	/**
	 * @param dependencyType
	 *            The dependencyType to set.
	 * @throws InvalidAssociationException
	 */
	public void setDependencyType(int dependencyType) throws InvalidAssociationException {
		if (((Task)getSuccessor()).isWbsParent()) {
			if (dependencyType == DependencyType.FF ||
					dependencyType == DependencyType.SF) {
				throw new InvalidAssociationException(Messages.getString("Message.parentSuccessorCannotHaveFinishLink"));
			}
		}

		this.dependencyType = dependencyType;
	}

	/** Sets the dependency type while keeping the persisted integer representation. */
	public void setDependencyKind(DependencyType.Kind dependencyType) throws InvalidAssociationException {
		setDependencyType(java.util.Objects.requireNonNull(dependencyType, "dependencyType").code());
	}

	boolean isCircular() {
		return predecessor.dependsOn(successor);
	}

	boolean isLinkToParent() {
		return ((Task)successor).wbsDescendentOf((Task) predecessor);
	}

	boolean isLinkToChild() {
		return ((Task)predecessor).wbsDescendentOf((Task) successor);
	}

    public void testValid(boolean allowDuplicate) throws InvalidAssociationException {
    	if (isLinkToParent() || isLinkToChild())
    		throw new InvalidAssociationException(Messages.getString("Message.cannotLinkToSummary"));

    	if (isCircular())
    		throw new InvalidAssociationException(Messages.getString("Message.circularDependency"));


    	if (!allowDuplicate && predecessor.getSuccessorList().findRight(successor) != null)
    		throw new InvalidAssociationException(Messages.getString("Message.cannotLinkTwice"));
    }


	public Object getLeft() {
		return predecessor;
	}

	public Object getRight() {
		return successor;
	}

	public void doAddService(Object eventSource) {
		DependencyService.getInstance().connect((Dependency)this,eventSource);
	}

	public void doRemoveService(Object eventSource) {
		DependencyService.getInstance().remove((Dependency)this,eventSource,true);
	}
	public void doUpdateService(Object eventSource) {
		DependencyService.getInstance().update((Dependency)this,eventSource);
	}

	@Override
	public Collection<AssociationList> getAssociationLists() {
		return List.of(predecessor.getSuccessorList(), successor.getPredecessorList());
	}

	public boolean isDefault() {
		return false;
	}

	public Document getDocument() {
		return ((BelongsToDocument)getSuccessor()).getDocument();
	}

	public boolean refersToDocument(Document document) {
		return ((Task)getSuccessor()).getMasterDocument() == document ||
				((Task)getPredecessor()).getMasterDocument() == document;

	}

	public void fireCreateEvent(Object eventSource) {
		((Task)getSuccessor()).getMasterDocument().getObjectEventManager().fireCreateEvent(eventSource,this);
//		if (isExternal())
//			((Task)getPredecessor()).getMasterDocument().getObjectEventManager().fireCreateEvent(eventSource,this);
	}
	public void fireUpdateEvent(Object eventSource) {
		((Task)getSuccessor()).getMasterDocument().getObjectEventManager().fireUpdateEvent(eventSource,this);
//		if (isExternal())
//			((Task)getPredecessor()).getMasterDocument().getObjectEventManager().fireUpdateEvent(eventSource,this);
	}
	public void fireDeleteEvent(Object eventSource) {
		((Task)getSuccessor()).getMasterDocument().getObjectEventManager().fireDeleteEvent(eventSource,this);
//		if (isExternal())
//			((Task)getPredecessor()).getMasterDocument().getObjectEventManager().fireDeleteEvent(eventSource,this);
	}


	public boolean isExternal() {
		return ((Task)getSuccessor()).getProjectId() != ((Task)getPredecessor()).getProjectId();
	}
	public boolean isCrossProject() {
		return isExternal(); // || ((Task)predecessor).isExternal() || ((Task)successor).isExternal();
	}
	public String toString() {
		return "[predecessor]" + predecessor + " [successor]" + successor;
	}

	public String getPredecessorName() {
		return ((Task)predecessor).getName();
	}

	public String getSuccessorName() {
		return ((Task)successor).getName();
	}

	public String getQualifiedPredecessorName() {
		if (isExternal())
			return ((Task)predecessor).getTaskAndProjectName();
		else
			return predecessor.toString();
	}

	public String getQualifiedSuccessorName() {
		if (isExternal())
			return ((Task)successor).getTaskAndProjectName();
		else
			return successor.toString();
	}

	public long getPredecessorId() {
		return ((Task)predecessor).getUniqueId();
	}

	public long getSuccessorId() {
		return ((Task)successor).getUniqueId();
	}
	public long getPredecessorIdNumber() {
		return ((Task)predecessor).getId();
	}

	public long getSuccessorIdNumber() {
		return ((Task)successor).getId();
	}

	//DataObject
    public String getName() {
        return name == null ? getUniqueIdString() : name;
    }
    public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("Dependency name must not be null");
		this.name = name;
    }
    public long getUniqueId() {
        if (uniqueId == Long.MIN_VALUE)
			return getPredecessorId() ^ Long.rotateLeft(getSuccessorId(), 32);
		return uniqueId;
    }
    public void setUniqueId(long id) {
		this.uniqueId = id;
    }
    public String getUniqueIdString() {
    	return getPredecessorId() + "." + getSuccessorId();
    }
    transient boolean newId=true;
    public boolean isNew(){
    	return newId;
    }
    public void setNew(boolean newId){
    	this.newId=newId;
    }
	/**
	 * Method to calculate the lead in millis from the lead value stored in the
	 * dependency. In normal cases, it just extracts the milliseconds, but if
	 * the lead is a percentage, then it is calculated based on predecessor
	 * duration. If the value is expressed as elapsed %, then the flag elapsed
	 * is applied to the resulting value. In this case, the rule is that the
	 * lead itself has an elapsed duration which is calculated based on the
	 * tasks (non-elapsed) duration.
	 *
	 * @param dependency
	 * @return lead in milliseconds with elapsed flag set if the dependency is
	 *         elapsed
	 */
	public long getLeadValue() {
		long leadWithUnits = getLag();
		if (Duration.isPercent(leadWithUnits)) {
			long lead = Duration.millis(leadWithUnits);
			float fraction = Duration.getPercentAsDecimal(lead);
			if (Duration.isElapsed(leadWithUnits)) {
				leadWithUnits = (long) (((Task) getPredecessor()).getElapsedDuration() * fraction);
				leadWithUnits = Duration.setAsElapsed(leadWithUnits); // put in elapsed  flag
			} else {
				leadWithUnits = (long) (((Task) getPredecessor()).getDuration() * fraction);
			}
		}
		return leadWithUnits;
	}

	//gets either predecessor or successor
	public HasDependencies getTask(boolean pred) {
		return pred ? predecessor : successor;
	}

	public long calcDependencyDate(boolean forward, long begin, long end, boolean hasDuration) {
		return forward
				? calcForwardDependencyDate(begin,end,hasDuration)
				: calcReverseDependencyDate(begin,end,hasDuration);
	}

	/**
	 * Calc the date that this dependency will cause its successor task (if forward scheduling) or predecessor (if reverse scheduling)
	 * @param begin
	 * @param end
	 * @param duration
	 * @return
	 */
	public long calcForwardDependencyDate(long begin, long end, boolean hasDuration) {
		if (disabled)
			return earlyDate;
		long t = 0;

		boolean canStartAtDayEnd = !hasDuration; // to handle the milestone case
		switch (DependencyType.Kind.fromCode(dependencyType)) {
			case FS:
				t = end;
				break;
			case SS:
				t = begin;
				break;
			case FF:
				t = ((ScheduleWindow)successor).calcOffsetFrom(end,end,false,false, canStartAtDayEnd);
				break;
			case SF:
				t = ((ScheduleWindow)successor).calcOffsetFrom(begin,begin,false,false, canStartAtDayEnd);
			break;
		}
		earlyDate = getEffectiveWorkCalendar().add(t,getLeadValue(), canStartAtDayEnd);
		return earlyDate;
	}
	/**
	 * get the latest finish time for the predecessor. The current
	 * ScheduleWindow in the backward pass of cp algo is the predecessor. It is
	 * possible that the milestone handling code needs more work
	 * begin is actually the late finish and end is early finish
	 */
	public long calcReverseDependencyDate(long begin, long end , boolean hasDuration) {
		if (disabled)
			return lateDate;
		long t = 0;
		boolean cannotFinishAtDayStart = !hasDuration; // to handle the milestone case
		switch (DependencyType.Kind.fromCode(getDependencyType())) {
			case FS:
				t = end;
				break;
			case SS:
				t = ((ScheduleWindow)getPredecessor()).calcOffsetFrom(end,end,false,false, cannotFinishAtDayStart);
				break;
			case FF:
				t = begin;
				break;
			case SF:
				t = ((ScheduleWindow)getPredecessor()).calcOffsetFrom(begin,begin,false,false, cannotFinishAtDayStart);
			break;
		}
		lateDate = getEffectiveWorkCalendar().add(t, getLeadValue(), cannotFinishAtDayStart);
		return lateDate;
	}

	public long getDate(boolean early) {
		return early ? earlyDate : lateDate;
	}

	public void setDate(boolean early, long date) {
		logger.log(Level.FINE, "{0} setting date to {1}", new Object[] {this, new java.util.Date(date)});
		if (early)
			earlyDate = date;
		else
			lateDate = date;
	}

	public String htmlString() {
	    StringBuilder s = new StringBuilder();
	    s.append("<html><body>");
	    s.append(Messages.getString("Gantt.tooltip.link")).append(": ");
	    s.append(DependencyType.toLongString(getDependencyType())).append(" ");
	    s.append(DurationFormat.format(getLag())).append("<br>");
	    s.append(Messages.getString("Gantt.tooltip.from")).append(": ");
	    s.append(getQualifiedPredecessorName()).append("<br>");
	    s.append(Messages.getString("Gantt.tooltip.to")).append(": ");
	    s.append(getQualifiedSuccessorName()).append("<br>");
	    s.append("</body></html>");
	    return s.toString();

	}

	//because it implements DataObject, should implement a different interface
	private transient boolean dirty;
	public boolean isDirty() {
		return dirty;
	}
	public void setDirty(boolean dirty) {
		//System.out.println("Dependency _setDirty("+dirty+"): "+getName());
		this.dirty = dirty;
		if (dirty&&predecessor!=null){
			Project project=((HasProject)predecessor).getProject();
			if (project!=null) project.setGroupDirty(true);
		}
	}

	public Document getMasterDocument() {
		return ((Task)getSuccessor()).getMasterDocument();
	}

	public void replace(Object newOne, boolean leftObject) {
		if (leftObject)
			setPredecessor((HasDependencies) newOne);
		else
			setSuccessor((HasDependencies) newOne);
	}

	public final boolean isDisabled() {
		return disabled;
	}

	public final void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

}
