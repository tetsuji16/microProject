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
package com.microproject.pm.calendar;

import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.microproject.configuration.Settings;
import com.microproject.document.Document;
import com.microproject.document.ObjectEventManager;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.strings.Messages;
import com.microproject.timescale.CalendarUtil;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.undo.CalendarEdit;
import com.microproject.undo.EditSupport;
import static com.microproject.util.IdentityCollections.addIdentity;
import static com.microproject.util.IdentityCollections.containsIdentity;

/**
 * Facade for manipulating calendars via a dialog or web interface
 */
public class CalendarService {
	private static CalendarService instance = null;
	ArrayList<WorkingCalendar> baseCalendars = new ArrayList<>();
	ArrayList<WorkingCalendar> derivedCalendars = new ArrayList<>();

	public static CalendarService getInstance() {
		if (instance == null)
			instance = new CalendarService();
		return instance;
	}
	/**
	 * Gets the name of a calendar
	 *
	 * @param cal
	 *            Calendar whose name to return
	 * @return name of calendar or "" if cal is null
	 */
	public String getCalendarName(WorkingCalendar cal) {
		if (cal == null)
			return "";
		return cal.getName();
	}


	/**
	 * Get the base calendar for a given calendar
	 *
	 * @param cal
	 *            whose based to get
	 * @return base calendar or null if cal has no base
	 */
	public WorkingCalendar getBaseCalendar(WorkingCalendar cal) {
		return cal == null ? null : (WorkingCalendar) cal.getBaseCalendar();
	}

	/**
	 * Get a descriptor for the day (its hours and modified status). The
	 * priority order what day is return is: 1. Derived calendar month day
	 * exception 2. Derived calendar week day exception 3. Base calendar special
	 * month day 4. Base calendar week day
	 *
	 * @param workingCalendar
	 *            The calendar to use
	 * @param date
	 *            Midnight on the day in question (see DateTime.midnightToday())
	 * @return DayDescriptor with modified info and work day
	 */
	public DayDescriptor getDay(WorkingCalendar workingCalendar, long date) {
		return workingCalendar.getMonthDayDescriptor(date);
	}
	/**
	 *
	 * @param workingCalendar
	 * @param intervals
	 * @return Common day or null
	 */
	public DayDescriptor getDay(WorkingCalendar workingCalendar, Set<?> intervals, boolean[] selectedWeekDays){
		CalendarUtil.DayIterator days=new CalendarUtil.DayIterator();
	    DayDescriptor common=null;
	    DayDescriptor current;

		for (int i = 0; i < 7; i++) {
			if (selectedWeekDays[i]) {
				current = workingCalendar.getWeekDayDescriptor(i+1);
		        common = mergeWithCommon(current,common);
		        if (common == null)
		        	return null;
		    }
		}

	    for (Object interval : intervals) {
		    days.setInterval((HasStartAndEnd) interval);
		    while(days.hasMoreDays()){
		        current=workingCalendar.getMonthDayDescriptor(days.nextDay());
		        common = mergeWithCommon(current,common);
		        if (common == null)
		        	return null;
		    }
		}
	    return common;
	}

	private DayDescriptor mergeWithCommon(DayDescriptor current, DayDescriptor common) {
        if (common==null){
            common=current;
            return common;
        }
        if (current.isModified()!=common.isModified()||
                current.isWorking()!=common.isWorking())
            return null;
        // compare working hours
        if (current.isModified()&&current.isWorking()){
            // to avoid breaking code with redefining equals in DayDescriptor...
            if (current.getWorkingHours()==null&&common.getWorkingHours()==null)
                return null;
            if ((current.getWorkingHours()==null&&common.getWorkingHours()!=null)||
                    (current.getWorkingHours()!=null&&common.getWorkingHours()==null))
                return null;
            List<WorkRange> currentIntervals = current.getWorkingHours().getIntervals();
            List<WorkRange> commonIntervals = common.getWorkingHours().getIntervals();
            if (commonIntervals.size()!=currentIntervals.size()) return null;
            Iterator<WorkRange> com = commonIntervals.iterator();
            Iterator<WorkRange> cur = currentIntervals.iterator();
            WorkRange comInterval, curInterval;
            while (com.hasNext()){
                comInterval = com.next();
                curInterval = cur.next();
                if (curInterval==null&&comInterval==null) continue;
                if (curInterval==null&&comInterval!=null) return null;
                if (curInterval!=null&&comInterval==null) return null;
                if (comInterval.getStart()!=curInterval.getStart()||comInterval.getEnd()!=curInterval.getEnd())
                    return null;
            }
        }
        return common;
	}
	/**
	 * Get a descriptor for the week day (its hours and modified status). If the
	 * derived calendar does not have a special weekday defined, then the base
	 * calendar weekday is used
	 *
	 * @param workingCalendar
	 *            The calendar to use
	 * @param dayNum
	 *            Corresponds to constants in java.util.Calendar SUNDAY=1,
	 *            MONDAY=2... SATURDAY=7
	 * @return DayDescriptor with modified info and work day
	 */
	public DayDescriptor getWeekDay(WorkingCalendar workingCalendar, int dayNum) {
		return workingCalendar.getWeekDayDescriptor(dayNum);
	}

	/**
	 * Clear out any month day exceptions for the given date
	 *
	 * @param workingCalendar
	 * @param date
	 */
	public void makeDefaultDay(WorkingCalendar workingCalendar, long date) {
		workingCalendar.makeDefaultDay(date);
	}

	/**
	 *
	 * @param workingCalendar
	 * @param intervals
	 *            with no overlap
	 * @return
	 */
	public void makeDefaultDays(WorkingCalendar workingCalendar, Set<?> intervals,boolean[] selectedDays) {
		for (int i = 0; i < 7; i++) {
			if (selectedDays[i]) {
				makeDefaultWeekDay(workingCalendar, i+1);
			}
		}


		CalendarUtil.DayIterator days=new CalendarUtil.DayIterator();
	    for (Object interval : intervals) {
		    days.setInterval((HasStartAndEnd) interval);
		    while(days.hasMoreDays())
		        workingCalendar.makeDefaultDay(days.nextDay());
		}
	}

	/**
	 * Clear out any week day exceptions for the week day
	 *
	 * @param workingCalendar
	 * @param dayNum
	 *            Corresponds to constants in java.util.Calendar SUNDAY=1,
	 *            MONDAY=2... SATURDAY=7
	 */
	public void makeDefaultWeekDay(WorkingCalendar workingCalendar, int dayNum) {
		workingCalendar.makeDefaultWeekDay(dayNum);
	}

	/**
	 * Set the working hours for a day. Working hours should be filled as
	 * follows: An existing WorkingHours should be cloned before modification in
	 * case the modification causes an exception. WorkingHours is modified by
	 * calls to WorkingHours.setInterval(int number, long start, long end) See
	 * comments in this method as to what values to use (must be on the date
	 * 1/1/70 or -1 if null)
	 *
	 * @param workingCalendar -
	 *            Calendar whose day to set
	 * @param date -
	 *            date of the exception
	 * @param workingHours -
	 *            contains the work ranges
	 * @throws WorkRangeException -
	 *             If invalid ranges, an exception is thrown
	 */
	public void setDayWorkingHours(WorkingCalendar workingCalendar, long date, WorkingHours workingHours) throws WorkRangeException{
		if (workingHours.getDuration() == 0)
			setDayNonWorking(workingCalendar,date);
		else
			workingCalendar.setDayWorkingHours(date,workingHours);
	}

	public void setDaysWorkingHours(WorkingCalendar workingCalendar, Set<?> intervals, boolean[] selectedDays, WorkingHours workingHours)  throws WorkRangeException, InvalidCalendarException{
		for (int i = 0; i < 7; i++) {
			if (selectedDays[i]) {
				setWeekDayWorkingHours(workingCalendar, i+1,workingHours);
			}
		}

		CalendarUtil.DayIterator days=new CalendarUtil.DayIterator();
	    for (Object interval : intervals) {
		    days.setInterval((HasStartAndEnd) interval);
		    while(days.hasMoreDays())
		        workingCalendar.setDayWorkingHours(days.nextDay(),workingHours);
		}
	}


	/**
	 * Set the working hours for a week day
	 *
	 * @param workingCalendar -
	 *            Calendar whose week day to set
	 * @param dayNum
	 *            Corresponds to constants in java.util.Calendar SUNDAY=1,
	 *            MONDAY=2... SATURDAY=7
	 * @param workingHours -
	 *            contains the work ranges
	 * @throws WorkRangeException -
	 *             If invalid ranges, an exception is thrown
	 * @throws InvalidCalendarException
	 */
	public void setWeekDayWorkingHours(WorkingCalendar workingCalendar, int dayNum, WorkingHours workingHours) throws WorkRangeException, InvalidCalendarException{
		if (workingHours.getDuration() == 0)
			setWeekDayNonWorking(workingCalendar,dayNum);
		else
			workingCalendar.setWeekDayWorkingHours(dayNum,workingHours);
	}

	/**
	 * Set a month day as a non working day
	 *
	 * @param workingCalendar
	 * @param date
	 *            what date is not working
	 */
	public void setDayNonWorking(WorkingCalendar workingCalendar, long date) {
		workingCalendar.setDayNonWorking(date);

	}

	public void setDaysNonWorking(WorkingCalendar workingCalendar, Set<?> intervals, boolean[] selectedDays) throws InvalidCalendarException{
		for (int i = 0; i < 7; i++) {
			if (selectedDays[i]) {
				setWeekDayNonWorking(workingCalendar,i+1);
			}
		}
		CalendarUtil.DayIterator days=new CalendarUtil.DayIterator();
	    for (Object interval : intervals) {
		    days.setInterval((HasStartAndEnd) interval);
		    while(days.hasMoreDays())
		        workingCalendar.setDayNonWorking(days.nextDay());
		}
	}



	/**
	 * Sets a week day as a non working day
	 *
	 * @param workingCalendar
	 * @param dayNum
	 *            Corresponds to constants in java.util.Calendar SUNDAY=1,
	 *            MONDAY=2... SATURDAY=7
	 */
	public void setWeekDayNonWorking(WorkingCalendar workingCalendar, int dayNum) throws InvalidCalendarException {
		int nonWorkingDays = 0;
		for (int i = 0; i < 7; i++) {
			if (!getWeekDay(workingCalendar,i+1).isWorking())
				nonWorkingDays++;
		}
		if (nonWorkingDays >= 6)
			throw new InvalidCalendarException(Messages.getString("Message.errorCalendarMustHaveWorkingDay"));
		workingCalendar.setWeekDayNonWorking(dayNum);
	}

	/** Commits one staged calendar definition and publishes its dependent-document refreshes. */
	public boolean commitCalendar(WorkingCalendar destination, WorkingCalendar source) {
		if (destination == null || source == null)
			throw new IllegalArgumentException("Both calendars are required");
		return commitCalendars(Map.of(destination, source));
	}

	/** Commits a dialog's staged calendars as one coordinated document mutation. */
	public boolean commitCalendars(Map<WorkingCalendar, WorkingCalendar> stagedCalendars) {
		return commitCalendars(stagedCalendars, null);
	}

	/** Commits staged calendars and reserves their Undo history in the same model order. */
	public boolean commitCalendars(Map<WorkingCalendar, WorkingCalendar> stagedCalendars, EditSupport editSupport) {
		if (stagedCalendars == null || stagedCalendars.isEmpty()) return false;
		List<Map.Entry<WorkingCalendar, WorkingCalendar>> entries = new ArrayList<>();
		for (Map.Entry<WorkingCalendar, WorkingCalendar> entry : stagedCalendars.entrySet())
			entries.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
		for (Map.Entry<WorkingCalendar, WorkingCalendar> entry : entries)
			if (entry.getKey() == null || entry.getValue() == null)
				throw new IllegalArgumentException("Both calendars are required");
		AtomicBoolean applied = new AtomicBoolean();
		try {
			return ProjectMutationState.coordinated(() -> commitCalendarsCoordinated(entries, applied, editSupport));
		} catch (RuntimeException | Error failure) {
			if (applied.get()) throw new CommittedMutationException(failure);
			throw failure;
		}
	}

	private boolean commitCalendarsCoordinated(List<Map.Entry<WorkingCalendar, WorkingCalendar>> entries,
		AtomicBoolean applied, EditSupport editSupport) {
		entries.removeIf(entry -> entry.getKey().hasSameDefinition(entry.getValue()));
		if (entries.isEmpty()) return false;
		List<WorkingCalendar> affectedCalendars = identityList();
		List<HasCalendar> affectedObjects = identityList();
		List<Project> projects = identityList();
		List<ResourcePool> pools = identityList();
		for (Map.Entry<WorkingCalendar, WorkingCalendar> entry : entries) {
			WorkingCalendar calendar = entry.getKey();
			if (!affectedCalendars.contains(calendar)) collectDependentCalendars(calendar, affectedCalendars);
		}
		for (WorkingCalendar calendar : affectedCalendars) {
			for (HasCalendar object : calendar.getObjectsUsing()) {
				if (containsIdentity(affectedObjects, object)) continue;
				affectedObjects.add(object);
				Document owner = object.getCalendarOwnerDocument();
				if (owner instanceof Project project) addIdentity(projects, project);
				else if (owner instanceof ResourcePool pool) addIdentity(pools, pool);
			}
		}
		List<ResourcePool> orphanPools = identityList();
		for (ResourcePool pool : List.copyOf(pools)) {
			List<Project> poolProjects = pool.getProjects();
			if (poolProjects.isEmpty()) addIdentity(orphanPools, pool);
			for (Project project : poolProjects) addIdentity(projects, project);
		}
		projects.sort(Comparator.comparingInt(System::identityHashCode));
		pools.sort(Comparator.comparingInt(System::identityHashCode));
		List<Project.AdditionalObjectEventManager> additionalManagers = new ArrayList<>();
		for (ResourcePool pool : pools) {
			List<Object> refreshObjects = new ArrayList<>();
			for (HasCalendar object : affectedObjects)
				if (object.getCalendarOwnerDocument() == pool) refreshObjects.add(object);
			additionalManagers.add(new Project.AdditionalObjectEventManager(
				pool.getObjectEventManager(), pool, this, refreshObjects));
		}
		List<CalendarEdit> calendarEdits = new ArrayList<>();
		if (editSupport != null)
			for (Map.Entry<WorkingCalendar, WorkingCalendar> entry : entries)
				calendarEdits.add(new CalendarEdit(entry.getKey(), entry.getValue()));
		EditSupport.Activation undoActivation = editSupport == null ? null : editSupport.beginCommand();
		EditSupport.Checkpoint undoCheckpoint = undoActivation == null ? null
			: editSupport.checkpoint(undoActivation.group());
		if (undoActivation != null) {
			for (CalendarEdit edit : calendarEdits) editSupport.postEdit(edit);
		}
		for (ResourcePool pool : orphanPools) pool.getObjectEventManager().beginBatch();
		Throwable failure = null;
		boolean result = false;
		List<Runnable> projectAppliedCallbacks = new ArrayList<>(projects.size());
		try {
			result = runCalendarProjectCommands(projects, additionalManagers, 0, projectAppliedCallbacks, () -> {
				for (Map.Entry<WorkingCalendar, WorkingCalendar> entry : entries) {
					entry.getKey().assignFrom(entry.getValue());
					if (!applied.get()) {
						applied.set(true);
						for (Runnable markApplied : projectAppliedCallbacks) markApplied.run();
					}
				}
				for (WorkingCalendar calendar : affectedCalendars) calendar.invalidateConcreteInstance();
				for (HasCalendar object : affectedObjects) {
					Document document = object.invalidateCalendar();
					if (document != null && document != object.getCalendarOwnerDocument())
						throw new IllegalStateException("Calendar invalidation returned an undiscovered owner document");
				}
				for (Project project : projects) project.recalculate();
				for (Project project : projects) project.fireUpdateEvent(this, project);
				for (ResourcePool pool : pools)
					if (!containsIdentity(orphanPools, pool))
						for (HasCalendar object : affectedObjects)
							if (object.getCalendarOwnerDocument() == pool) pool.fireUpdateEvent(this, object);
				return true;
			});
		} catch (Throwable current) {
			failure = current;
		} finally {
			for (ResourcePool pool : orphanPools) {
				try {
					if (applied.get())
						for (HasCalendar object : affectedObjects)
							if (object.getCalendarOwnerDocument() == pool) pool.fireUpdateEvent(this, object);
					ObjectEventManager.PendingBatch batch = pool.getObjectEventManager().detachBatch(applied.get());
					if (applied.get() && batch != null && batch.isPublishable() && batch.hasEvents()) {
						long revision = pool.advanceCoordinationRevision();
						ProjectMutationState.publishAfterAllScopes(
							() -> pool.getObjectEventManager().publishBatch(batch, revision));
					}
				} catch (Throwable current) {
					if (failure == null) failure = current;
					else failure.addSuppressed(current);
				}
			}
			if (undoActivation != null) {
				EditSupport.Group group = undoActivation.group();
				try {
					if (applied.get()) editSupport.reserveCommit(group);
					else editSupport.discardSince(group, undoCheckpoint);
				} catch (Throwable current) {
					if (failure == null) failure = current;
					else failure.addSuppressed(current);
				}
				try { undoActivation.close(); }
				catch (Throwable current) {
					if (failure == null) failure = current;
					else failure.addSuppressed(current);
				}
				ProjectMutationState.publishAfterAllScopes(() -> editSupport.completePublication(group));
			}
		}
		if (failure instanceof Error error) throw error;
		if (failure instanceof RuntimeException runtime) throw runtime;
		if (failure != null) throw new IllegalStateException("Calendar mutation failed", failure);
		return result;
	}

	/** Indicates that observer publication failed after the calendar edit itself committed. */
	public static final class CommittedMutationException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private CommittedMutationException(Throwable cause) {
			super("Calendar changes were committed, but an observer failed during publication", cause);
		}
	}

	private boolean runCalendarProjectCommands(List<Project> projects,
		List<Project.AdditionalObjectEventManager> additionalManagers, int index, List<Runnable> appliedCallbacks,
		java.util.function.BooleanSupplier operation) {
		if (index == projects.size()) return operation.getAsBoolean();
		return projects.get(index).runModelCommand(markApplied -> {
			appliedCallbacks.add(markApplied);
			boolean changed = runCalendarProjectCommands(projects, additionalManagers, index + 1,
				appliedCallbacks, operation);
			return changed;
		}, Boolean.TRUE::equals, changed -> true, additionalManagers);
	}

	private void collectDependentCalendars(WorkingCalendar base, List<WorkingCalendar> calendars) {
		addIdentity(calendars, base);
		for (WorkingCalendar derived : derivedCalendars)
			if (derived != null && derived != base && derived.dependsOn(base)) addIdentity(calendars, derived);
	}

	private static <T> List<T> identityList() {
		return new ArrayList<>();
	}

	/**
	 * Make a temporary copy of a calendar. Used in the calendar dialog - A
	 * scratch copy should be modified, and only upon validation should the
	 * calendar data be copied back into the original calendar via a call to
	 * assignCalendar
	 *
	 * @param from
	 * @return
	 */
	public WorkingCalendar makeScratchCopy(WorkingCalendar from) {
		if (from == null)
			throw new IllegalArgumentException("Calendar is required");
		return from.makeScratchCopy();
	}


/**
 * For debugging - returns a toString() for a calendar
 *
 * @param cal
 * @return String representation of calendar for debugging
 */
	public String dump(WorkingCalendar cal) {
		return cal.dump();
	}

	public WorkingCalendar getStandardBasedInstance() {
		WorkingCalendar wc = WorkingCalendar.getStandardBasedInstance();
		add(wc);
		return wc;
	}

	public WorkingCalendar getStandardInstance() {
		WorkingCalendar wc = WorkingCalendar.getStandardInstance();
		return wc;
	}

	public WorkingCalendar getDefaultInstance() {
		WorkingCalendar wc = WorkingCalendar.getDefaultInstance();
		return wc;
	}

	public void invalidate(WorkingCalendar cal) {
		if (cal == null)
			return;
		Set<Document> documents = new HashSet<>();
		for (HasCalendar hasCal : cal.getObjectsUsing()) {
			documents.add(hasCal.invalidateCalendar());
		}
		for (Document doc : documents) {
			if (doc != null)
				doc.fireUpdateEvent(this,doc);
		}
	}

	/**
	 * Clears the merged concrete cache of calendars that depend on the
	 * invalidated base calendar. Editing a base calendar's contents must be
	 * visible to derived and resource calendars without requiring a base-calendar
	 * reassignment.
	 *
	 * @param base the calendar whose contents were changed
	 */
	public void invalidateDerivedConcrete(WorkingCalendar base) {
		if (base == null)
			return;
		invalidateDerivedInList(derivedCalendars, base);
	}

	private void invalidateDerivedInList(List<WorkingCalendar> calendars, WorkingCalendar base) {
		for (WorkingCalendar calendar : calendars) {
			if (calendar != null && calendar != base && calendar.dependsOn(base))
				calendar.invalidateConcreteInstance();
		}
	}


	public void add(WorkingCalendar cal) {
		ProjectMutationState.coordinated(() -> {
			if (cal.isBaseCalendar()) {
				if (!baseCalendars.contains(cal)) {
					boolean found = findBaseCalendar(cal.getName()) != null;
					if (found) return;
					baseCalendars.add(cal);
				}
			} else if (!derivedCalendars.contains(cal) && !baseCalendars.contains(cal))
				derivedCalendars.add(cal);
		});
	}

	/**
	 * Registers a calendar created from a base calendar. Resource calendars are
	 * often created directly by the resource model and therefore do not pass
	 * through the normal calendar-list loading path.
	 */
	void registerDerivedCalendar(WorkingCalendar cal) {
		ProjectMutationState.coordinated(() -> {
			if (cal != null && cal.getBaseCalendar() != null && !derivedCalendars.contains(cal))
				derivedCalendars.add(cal);
		});
	}
	public ArrayList<WorkingCalendar> getBaseCalendars() {
		return ProjectMutationState.coordinated(() -> new ArrayList<>(baseCalendars));
	}
	public ArrayList<WorkingCalendar> getDerivedCalendars() {
		return ProjectMutationState.coordinated(() -> new ArrayList<>(derivedCalendars));
	}

	public static final Object[] allBaseCalendars() {
		return getInstance().getBaseCalendars().toArray();
	}
	public static final WorkCalendar findBaseCalendar(String name) {
		return getInstance().findBaseCalendar(name,false);
	}
	public WorkCalendar findBaseCalendar(String name, boolean importing) {
		if (name == null)
			return null;
		importing = false; // don't do the treatment - if a calendar is found
							// with same name, use it
		// previously, I would create a new calendar for the project one, but
		// this led to an explosion of calendars on repeated imports and exports
		if (importing)
			name = name + PLACE_HOLDER_NAME;
		for (WorkingCalendar current : getInstance().getBaseCalendars()) {
			if (current!=null&&current.getName()!=null&&current.getName().equals(name))
				return current;
		}
		return null;
	}
	public WorkCalendar findBaseCalendar(long id) {
		for (WorkingCalendar current : getInstance().getBaseCalendars()) {
			if (current.getUniqueId() == id)
				return current;
		}
		return null;
	}

	private static final String PLACE_HOLDER_NAME = "____~";
	public void renameImportedBaseCalendars(String documentName) {
		for (WorkingCalendar current : getInstance().getBaseCalendars()) {
			String currentName = current.getName();
			int spot = currentName.indexOf(PLACE_HOLDER_NAME);
			if (spot == -1)
				continue;
			currentName = toImportedName(currentName.substring(0,spot),documentName);
			current.setName(currentName);
		}

	}

	private static String toImportedName(String calName, String documentName) {
		return calName + Settings.LEFT_BRACKET + documentName + Settings.RIGHT_BRACKET;
	}

	public void reassignCalendar(HasCalendar object, WorkCalendar oldCal, WorkCalendar newCal) {
		ProjectMutationState.coordinated(() -> {
			if (oldCal == newCal) return;
			if (oldCal instanceof WorkingCalendar workingCalendar)
				workingCalendar.removeObjectUsing(object);
			if (newCal instanceof WorkingCalendar workingCalendar)
				workingCalendar.addObjectUsing(object);
		});
	}

}
