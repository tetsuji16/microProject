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
package com.microproject.pm.task;

import com.microproject.util.DataUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.function.Predicate;



import com.microproject.algorithm.ReverseQuery;
import com.microproject.association.Association;
import com.microproject.association.AssociationList;
import com.microproject.association.InvalidAssociationException;
import com.microproject.configuration.CircularDependencyException;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.FieldDictionary;
import com.microproject.configuration.Settings;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationDefaults;
import com.microproject.datatype.Hyperlink;
import com.microproject.datatype.ImageLink;
import com.microproject.document.Document;
import com.microproject.document.ObjectEvent;
import com.microproject.document.ObjectEventManager;
import com.microproject.document.ObjectSelectionEventManager;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.HasExtraFields;
import com.microproject.pm.scheduling.IntervalConsumer;
import com.microproject.graphic.configuration.GanttBarFormatOverrides;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeException;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.NodeList;
import com.microproject.grouping.core.NodeVisitor;
import com.microproject.grouping.core.OutlineCollection;
import com.microproject.grouping.core.OutlineCollectionImpl;
import com.microproject.grouping.core.event.HierarchyEvent;
import com.microproject.grouping.core.event.HierarchyListener;
import com.microproject.grouping.core.hierarchy.NodeHierarchy;
import com.microproject.grouping.core.model.AssignmentNodeModel;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.NodeModelFactory;
import com.microproject.grouping.core.transform.filtering.NotAssignmentFilter;
import com.microproject.options.CalendarOption;
import com.microproject.options.ScheduleOption;
import com.microproject.options.TimesheetOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.HasTimeDistributedData;
import com.microproject.pm.assignment.TimeDistributedDataConsolidator;
import com.microproject.pm.assignment.TimeDistributedFields;
import com.microproject.pm.assignment.TimeDistributedFieldVisibility;
import com.microproject.pm.assignment.timesheet.TimesheetHelper;
import com.microproject.pm.assignment.timesheet.TimesheetStatus;
import com.microproject.pm.assignment.timesheet.UpdatesFromTimesheet;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.HasBaseCalendar;
import com.microproject.pm.calendar.HasCalendar;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.costing.EarnedValueCalculator;
import com.microproject.pm.costing.EarnedValueFields;
import com.microproject.pm.costing.EarnedValueValues;
import com.microproject.pm.costing.ExpenseType;
import com.microproject.pm.costing.HasExpenseType;
import com.microproject.pm.criticalpath.CriticalPath;
import com.microproject.pm.criticalpath.TaskSchedule;
import com.microproject.pm.criticalpath.HasSentinels;
import com.microproject.pm.criticalpath.SchedulingAlgorithm;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.key.HasId;
import com.microproject.pm.key.HasKey;
import com.microproject.pm.key.HasKeyImpl;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.snapshot.DataSnapshot;
import com.microproject.pm.scheduling.BarClosure;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.scheduling.ScheduleEventListener;
import com.microproject.pm.scheduling.ScheduleEventManager;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleUtil;
import com.microproject.pm.snapshot.BaselineScheduleFields;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.snapshot.SnapshottableImpl;
import com.microproject.pm.time.MutableHasStartAndEnd;
import com.microproject.print.PrintSettings;
import com.microproject.server.access.ErrorLogger;
import com.microproject.server.data.DataObject;
import com.microproject.server.data.DistributionComparator;
import com.microproject.server.data.DistributionConverter;
import com.microproject.server.data.DistributionData;
import com.microproject.session.FileHelper;
import com.microproject.strings.Messages;
import com.microproject.transaction.MultipleTransactionManager;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.EditSupport;
import com.microproject.util.DateTime;
import com.microproject.util.Environment;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.util.UiDispatch;

/**
 * Project class
 */
public class Project implements Document, BelongsToDocument, HasKey, HasPriority, MutableHasStartAndEnd, ProjectSpecificFields, HasNotes, HasBaseCalendar, HasCalendar, NodeModelDataFactory, HierarchyListener, HasTimeDistributedData, TimeDistributedFields, EarnedValueValues, EarnedValueFields, DataObject, HasSentinels, BaselineScheduleFields, Schedule,UpdatesFromTimesheet, HasExtraFields, HasExpenseType, SavableToWorkspace {
	private record LeafScheduleTransition(NormalTask task, RollupSpan span, double percentComplete) { }
	static final long serialVersionUID = 17283790404932L;
	private static final Logger logger = Logger.getLogger(Project.class.getName());
	private long statusDate = 0;
	private String manager="";
	private transient HasKeyImpl hasKey;
	private String notes="";
	private transient LinkedList<Task> tasks = new LinkedList<>();
	private transient ResourcePool resourcePool = null;
	private transient SchedulingAlgorithm schedulingAlgorithm = null;
	private transient boolean initialized = false;
	private transient ScheduleEventManager scheduleEventManager = new ScheduleEventManager();
	private transient MultipleTransactionManager multipleTransactionManager = new MultipleTransactionManager();
	private transient ObjectEventManager objectEventManager = new ObjectEventManager();
	private transient ObjectSelectionEventManager objectSelectionEventManager = new ObjectSelectionEventManager();
	private transient int eventBatchDepth;
	private transient List<AdditionalObjectEventManager> additionalEventManagers = new ArrayList<>();
	private transient com.microproject.undo.EditSupport.Activation undoBatchActivation;
	private transient List<EditSupport.Checkpoint> undoBatchCheckpoints = new ArrayList<>();

	private transient int taskIdCounter = 0;
	private transient boolean isGroupDirty = false;
	private transient boolean isDirty = false;
	private transient boolean readOnly = false;
	private transient SubprojectHandler subprojectHandler;
	public transient static Project lastDeserialized = null;
	long start;
	long end;
	private SummaryEnvelope summaryEnvelope = new SummaryEnvelope();
	long duration;
	boolean forward = true;
	int priority = 500;
	long currentDate = 0;
	private Map<String, Object> extraFields = null;
	/** Project-scoped report definitions persisted with native project files. */
	private Map<String, String> customReportPresets = new LinkedHashMap<>();
	private GanttBarFormatOverrides ganttBarFormatOverrides = new GanttBarFormatOverrides();
	private double risk = 0.0D;
	private double netPresentValue = 0.0D;
	private int benefit = 0;
	/** Canonical file path of the resource-pool project used by this sharer. */
	private String sharedResourcePoolFile;
	/** Stable numeric identity of the persisted resource-pool project. */
	private long sharedResourcePoolProjectId;
	/** True while the persisted shared-pool reference cannot be resolved. */
	private boolean sharedResourcePoolUnresolved;
	/** MSP-compatible conflict policy: true means the resource pool wins. */
	private boolean resourcePoolTakesPrecedence = true;
	transient int projectStatus = ProjectStatus.Kind.PLANNING.code(); // exposed in database
	transient int projectType = ProjectType.Kind.OTHER.code(); // exposed in database
	transient int expenseType = ExpenseType.Kind.NONE.code();// exposed in database
	transient String group;// exposed in database
	transient String division;// exposed in database

	private transient boolean openedAsSubproject = false;
	private transient Hyperlink documentFolderUrl = null;
	private transient long earliestStartingTask = 0L; // used for subprojects
	private transient long latestFinishingTask = 0L; // used for subprojects
	private static Project dummy = null;


	private transient NodeModel taskModel = null;
	private transient NodeModel resourceModel = null;
	private transient List<Task> repaired = null;
	private transient Date creationDate,lastModificationDate;
	private transient IdentityFacade identityFacade = new IdentityFacade();
	/**
	 * Issue #227: created timestamp persisted on the object (not the transient
	 * identityFacade) so it survives load/save round-trips. See com.microproject.pm.task.Task#created.
	 */
	protected Date created = new Date();
	/**
	 * Stable document identity for cross-project references and MPOF manifests.
	 * It is deliberately separate from the legacy numeric unique id, which can
	 * be reassigned by importers.  Older serialized projects lazily receive an
	 * identity the first time they participate in a multi-project workflow.
	 */
	private String documentId = java.util.UUID.randomUUID().toString();
	private transient BaselineFacade baselineFacade = new BaselineFacade();
	private transient SubprojectFacade subprojectFacade = new SubprojectFacade();
	private transient ScheduleFacade scheduleFacade = new ScheduleFacade();
	private transient TaskAggregationFacade taskAggregationFacade = new TaskAggregationFacade();
	private transient TaskLifecycleFacade taskLifecycleFacade = new TaskLifecycleFacade();
	/*
	 * Runtime extensions belong to a document instance, but must never become
	 * part of the legacy POD object graph.  Feature-specific services use their
	 * state type as the key, which prevents process-global registries and makes
	 * disposal deterministic.
	 */
	private transient Map<Class<?>, Object> transientDocumentState;

	public final synchronized <T> T findTransientDocumentState(Class<T> type) {
		if (transientDocumentState == null) return null;
		return type.cast(transientDocumentState.get(type));
	}

	public final synchronized <T> T getOrCreateTransientDocumentState(Class<T> type, Supplier<? extends T> factory) {
		if (type == null || factory == null) throw new IllegalArgumentException("Transient document state type and factory are required");
		Object current = transientDocumentState == null ? null : transientDocumentState.get(type);
		if (current != null) return type.cast(current);
		T created = type.cast(factory.get());
		if (created == null) throw new IllegalArgumentException("Transient document state factory returned null");
		if (transientDocumentState == null) transientDocumentState = new HashMap<Class<?>, Object>();
		transientDocumentState.put(type, created);
		return created;
	}

	public final synchronized void removeTransientDocumentState(Class<?> type) {
		if (transientDocumentState != null) transientDocumentState.remove(type);
	}

	/** Returns this document's transient mutation barrier and committed revision. */
	public final ProjectMutationState getMutationState() {
		return getOrCreateTransientDocumentState(ProjectMutationState.class, ProjectMutationState::new);
	}

	/** Runs one model command at the project revision/event publication boundary. */
	public boolean runModelMutation(ModelMutation operation) {
		java.util.Objects.requireNonNull(operation, "operation");
		return runModelCommand(operation::apply, Boolean.TRUE::equals, changed -> Boolean.TRUE);
	}

	/** Runs a typed command through the shared project barrier and event-batch lifecycle. */
	public <T, E extends Exception> T runModelCommand(ModelCommand<T, E> operation,
			java.util.function.Predicate<? super T> changedResult,
			java.util.function.UnaryOperator<T> resultWhenEventsWereEmitted) throws E {
		return runModelCommand(operation, changedResult, resultWhenEventsWereEmitted, List.of());
	}

	/** Runs a command while additional document observers join the same revisioned publication batch. */
	public <T, E extends Exception> T runModelCommand(ModelCommand<T, E> operation,
			java.util.function.Predicate<? super T> changedResult,
			java.util.function.UnaryOperator<T> resultWhenEventsWereEmitted,
			Collection<AdditionalObjectEventManager> additionalManagers) throws E {
		return runModelCommand(operation, changedResult, resultWhenEventsWereEmitted, additionalManagers, false);
	}

	/** Runs a model command that restores its own model changes when its operation fails. */
	public <T, E extends Exception> T runAtomicModelCommand(ModelCommand<T, E> operation,
			java.util.function.Predicate<? super T> changedResult,
			java.util.function.UnaryOperator<T> resultWhenEventsWereEmitted) throws E {
		return runModelCommand(operation, changedResult, resultWhenEventsWereEmitted, List.of(), true);
	}

	/** Runs an atomic model command while additional document observers join its event boundary. */
	public <T, E extends Exception> T runAtomicModelCommand(ModelCommand<T, E> operation,
			java.util.function.Predicate<? super T> changedResult,
			java.util.function.UnaryOperator<T> resultWhenEventsWereEmitted,
			Collection<AdditionalObjectEventManager> additionalManagers) throws E {
		return runModelCommand(operation, changedResult, resultWhenEventsWereEmitted, additionalManagers, true);
	}

	private <T, E extends Exception> T runModelCommand(ModelCommand<T, E> operation,
			java.util.function.Predicate<? super T> changedResult,
			java.util.function.UnaryOperator<T> resultWhenEventsWereEmitted,
			Collection<AdditionalObjectEventManager> additionalManagers, boolean atomicFailure) throws E {
		java.util.Objects.requireNonNull(operation, "operation");
		java.util.Objects.requireNonNull(changedResult, "changedResult");
		java.util.Objects.requireNonNull(resultWhenEventsWereEmitted, "resultWhenEventsWereEmitted");
		java.util.Objects.requireNonNull(additionalManagers, "additionalManagers");
		ProjectMutationState state = getMutationState();
		ProjectMutationState.Scope mutation = state.begin();
		AtomicEventCheckpoint eventCheckpoint = atomicFailure ? atomicEventCheckpoint(additionalManagers) : null;
		int initialPendingEvents = pendingEventCount();
		beginEventBatch(additionalManagers);
		boolean[] applied = { false };
		T result;
		PendingEventBatch pending;
		try {
			result = operation.apply(() -> applied[0] = true);
		} catch (RuntimeException | Error failure) {
			if (atomicFailure) rollbackAtomicModelCommand(mutation, eventCheckpoint, failure);
			else finishModelCommandFailure(mutation, initialPendingEvents, applied[0], failure);
			throw failure;
		} catch (Exception failure) {
			if (atomicFailure) rollbackAtomicModelCommand(mutation, eventCheckpoint, failure);
			else finishModelCommandFailure(mutation, initialPendingEvents, applied[0], failure);
			@SuppressWarnings("unchecked") E typedFailure = (E) failure;
			throw typedFailure;
		}
		boolean hasFacts = pendingEventCount() > initialPendingEvents;
		if (hasFacts) result = resultWhenEventsWereEmitted.apply(result);
		boolean changed = changedResult.test(result) || hasFacts;
		if (changed && !hasFacts)
			fireScheduleChanged(this, com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
		pending = commitEventBatch(mutation, changed);
		publishEventBatch(pending);
		return result;
	}

	private record AtomicEventCheckpoint(int objectEvents, int scheduleEvents,
		java.util.IdentityHashMap<ObjectEventManager, Integer> additionalEvents) { }

	private AtomicEventCheckpoint atomicEventCheckpoint(Collection<AdditionalObjectEventManager> requestedManagers) {
		java.util.IdentityHashMap<ObjectEventManager, Integer> additional = new java.util.IdentityHashMap<>();
		for (AdditionalObjectEventManager participant : additionalEventManagers)
			additional.put(participant.manager(), participant.manager().pendingEventCount());
		for (AdditionalObjectEventManager participant : requestedManagers)
			additional.putIfAbsent(participant.manager(), participant.manager().pendingEventCount());
		return new AtomicEventCheckpoint(objectEventManager.pendingEventCount(),
			scheduleEventManager.pendingEventCount(), additional);
	}

	private void rollbackAtomicModelCommand(ProjectMutationState.Scope mutation,
			AtomicEventCheckpoint checkpoint, Throwable failure) {
		try {
			objectEventManager.discardPendingEventsAfter(checkpoint.objectEvents());
			scheduleEventManager.discardPendingEventsAfter(checkpoint.scheduleEvents());
			for (var entry : checkpoint.additionalEvents().entrySet())
				entry.getKey().discardPendingEventsAfter(entry.getValue());
			synchronized (this) {
				if (undoBatchActivation != null && !undoBatchCheckpoints.isEmpty())
					undoController.getEditSupport().discardSince(undoBatchActivation.group(),
						undoBatchCheckpoints.getLast());
			}
			endEventBatch(true);
		} catch (Throwable cleanupFailure) {
			if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
		} finally {
			try { mutation.close(); }
			catch (Throwable cleanupFailure) {
				if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
			}
		}
	}

	public record AdditionalObjectEventManager(ObjectEventManager manager, ResourcePool revisionOwner, Object source,
			List<?> refreshObjects) {
		public AdditionalObjectEventManager {
			java.util.Objects.requireNonNull(manager, "manager");
			java.util.Objects.requireNonNull(revisionOwner, "revisionOwner");
			java.util.Objects.requireNonNull(source, "source");
			if (manager != revisionOwner.getObjectEventManager())
				throw new IllegalArgumentException("The event manager must belong to its revision owner");
			refreshObjects = List.copyOf(refreshObjects);
		}
	}

	private void finishModelCommandFailure(ProjectMutationState.Scope mutation, int initialPendingEvents,
			boolean applied, Throwable failure) {
		try {
			boolean hasFacts = pendingEventCount() > initialPendingEvents;
			if (applied && !hasFacts)
				fireScheduleChanged(this, com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
			if (applied || hasFacts) {
				PendingEventBatch pending = commitEventBatch(mutation, true);
				try {
					publishEventBatch(pending);
				} catch (Throwable publicationFailure) {
					failure.addSuppressed(publicationFailure);
				}
			} else {
				try {
					endEventBatch(false);
				} finally {
					mutation.close();
				}
			}
		} catch (Throwable cleanupFailure) {
			if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
		}
	}

	@FunctionalInterface
	public interface ModelMutation {
		boolean apply(Runnable markApplied);
	}

	@FunctionalInterface
	public interface ModelCommand<T, E extends Exception> {
		T apply(Runnable markApplied) throws E;
	}

	public NodeModel getTaskModel() {
		if (taskModel == null)
			taskModel=NodeModelFactory.createTaskModel(this);
		return taskModel;
	}

	public NodeModel getResourceModel() {
		if (resourceModel == null)
			resourceModel=NodeModelFactory.createResourceModel(this);
		return resourceModel;
	}


	private Project(boolean local) {
		super();
		initSubprojectHandler();
		hasKey =new HasKeyImpl(local,this);
		setWorkCalendar(CalendarService.getInstance().getDefaultInstance());

		start = CalendarOption.getInstance().makeValidStart(DateTime.midnightToday(), true);
		start = getEffectiveWorkCalendar().adjustInsideCalendar(start,false);
		end = start;
		calendarOption = CalendarOption.getDefaultInstance();
		durationDefaults = DurationDefaults.from(calendarOption, ScheduleOption.getInstance());
	}


	private Project(ResourcePool resourcePool,DataFactoryUndoController undo) {
		this(resourcePool.isLocal());
		this.resourcePool = resourcePool;
		undoController=undo;
	}

	public void dispose() {
		logger.fine("disposing project " + this);
		synchronized (this) {
			transientDocumentState = null;
		}
	}
	public static Project getDummy() {
		if (dummy == null)
			dummy = new Project(true);
		return dummy;
	}

	public static Project createProject(ResourcePool resourcePool,DataFactoryUndoController undo) {
		Project project=new Project(resourcePool,undo);
		project.initializeProject();
		project.setUndoController(undo); //undo not properly initialized in new Project(resourcePool,undo)
		return project;
	}

	public void initializeOutlines(){
		bindAssignmentOutlines();
		initializeDefaultOutline();
	}

	private void bindAssignmentOutlines() {
		int count=Settings.numHierarchies();
		for (int i=0;i<count;i++){
			NodeModel model=taskOutlines.getOutline(i);
			if (model==null) continue;
			if (model instanceof AssignmentNodeModel assignmentModel){
				assignmentModel.setContainsLeftObjects(true);
				assignmentModel.setDocument(this);
			}
			model.setUndoController(undoController);
		}
	}

	public void disconnectOutlines(){
		int count=Settings.numHierarchies();
		for (int i=0;i<count;i++){
			NodeModel model=taskOutlines.getOutline(i);
			if (model instanceof AssignmentNodeModel assignmentModel){
				assignmentModel.setDocument(null); //remove ObjectListener
			}
		}
		disconnectDefaultOutline();
	}


	public long getStartConstraint() {
		long result;
	    long constraint = getReferringSubprojectTaskDependencyDate();
	   	if (constraint > getStart())
	   		result = getEffectiveWorkCalendar().adjustInsideCalendar(constraint,false);
	   	else
	   		result = getStart();
	   	return result;
	}

	public void initialize(final boolean subproject,boolean updateDistribution) {
	    initialized = true;
	    repairTasks();
	    if (!subproject)
	    	schedulingAlgorithm.initialize(this);
	    if (getStart() == 0L) {
	    	logger.fine("no start so using earliest");
        UiDispatch.invokeLater(() -> {
            recalculate();
            setStart(getEarliestStartingTaskOrStart());
        });
	    }
	    initializeDefaultOutline();
	    if (TimesheetOption.getInstance().isAutomaticallyIntegrateTimecardData())
	    	applyTimesheet(TimesheetOption.getInstance().getTimesheetFieldArray());
	    setAllTasksAsUnchangedFromPersisted(false);

	    if (updateDistribution) updateDistributionMap();
	}

/**
 * This will set the start and end date of a project to the earliest starting task and the latest finishing
 * Its purpose is for use in handling subprojects, when we'd like the subproject's external constraints to determine its start and end, and also
 * have it the subproject show up with correct start and end dates when shown upopened in another project
 *
 */
	void setEarliestAndLatestDatesFromSchedule() {
		long s = Long.MAX_VALUE;
		long e = 0;
		for (Task t : tasks) {
			if (t.isExternal() || t.getOwningProject() != this)
				continue;
			s = Math.min(s,t.getStart());
			e = Math.max(e,t.getEnd());
		}
		if (s != Long.MAX_VALUE)
			earliestStartingTask = s;
		else
			earliestStartingTask = getStart();

		if (e != 0 )
			latestFinishingTask = e;
		else
			latestFinishingTask = getEnd();
	}





	public void initializeProject(){
		setSchedulingAlgorithm(new CriticalPath(this));
		initializeOutlines();

	}

	public void disconnect(){
	    disconnectOutlines();
	    removeObjectListener(getSchedulingAlgorithm());
	    schedulingAlgorithm = null; // help with gc
	}


	private void initializeDefaultOutline() {
		taskOutlines.getDefaultOutline().getHierarchy().addHierarchyListener(this);
	}
	private void disconnectDefaultOutline() {
		taskOutlines.getDefaultOutline().getHierarchy().removeHierarchyListener(this);
	}


	public NormalTask newNormalTaskInstance() {
		return newNormalTaskInstance(true);
	}

	public void initializeId(Task task) {
		taskLifecycleFacade.initializeId(task);
	}


	public NormalTask newNormalTaskInstance(boolean userCreated) {
		return taskLifecycleFacade.newNormalTaskInstance(userCreated);
	}


	public void setLocalParent(Task child, Task parent) {
		Node childNode = getTaskModel().search(child);
		Node parentNode = parent == null ? null : getTaskModel().search(parent);
		setLocalParent(childNode,parentNode);
	}

	public void setLocalParent(Node childNode, Node parentNode) {
		Task child = (Task) childNode.getImpl();
		Task parent = (Task) (parentNode == null ? null : parentNode.getImpl());
		if (child.getWbsParentTask() == parent)
			return;
		ArrayList<Node> temp = new ArrayList<>(1);
		temp.add(childNode);
		int position = parentNode == null ? getTaskModel().getChildren(null).size() : parentNode.getChildCount();
		getTaskModel().relocate(temp, parentNode, position, NodeModel.NORMAL);
		setDefaultRelationship(parentNode,childNode);
	}

	public Node createLocalTaskNode(Node parentNode) {
		return createLocalTaskNode(parentNode, true);
	}

	public Node createLocalTaskNode(Node parentNode, boolean userCreated) {
		NormalTask task=new NormalTask(this);
		Node childNode = NodeFactory.getInstance().createNode(task); // get a node for this task
		connectTask(task);
		addToDefaultOutline(parentNode,childNode);
		getSchedulingAlgorithm().addObject(task);
		return childNode;
	}


	/**
	 * Used when creating a task on spreadsheet that may not be valid
	 * @return
	 */
	public NormalTask newStandaloneNormalTaskInstance() {
		return taskLifecycleFacade.newStandaloneNormalTaskInstance();
	}
	public NormalTask createScriptedTask() {
		return taskLifecycleFacade.createScriptedTask();
	}

   public void connectTask(Task task) {
	   taskLifecycleFacade.connectTask(task);
	}


	/**
	 * @return Returns the statusDate.
	 */
	public long getStatusDate() {
		if (statusDate == 0) // if date not set, then use last instant of this day incude all of today
			return workCalendar.adjustInsideCalendar(DateTime.midnightTomorrow() -1,true);

		return statusDate;
	}

	public boolean isStatusDateSet() {
		return statusDate != 0;
	}

	/** Clears an explicitly configured status date (used by undo). */
	public void clearStatusDate() {
		this.statusDate = 0L;
	}

	/**
	 * @param statusDate The statusDate to set.
	 */
	public void setStatusDate(long statusDate) {
		statusDate = DateTime.midnightNextDay(statusDate) -1; // last instant of today
		statusDate = workCalendar.adjustInsideCalendar(statusDate, true);
		this.statusDate = statusDate;
	}

	/**
	 * Restores an already-normalized status date from persisted project state.
	 * Unlike {@link #setStatusDate(long)}, this preserves the exact stored instant
	 * instead of reapplying day and work-calendar normalization.
	 *
	 * @param persistedStatusDate the persisted status date, or zero when unset
	 * @throws IllegalArgumentException if the value is negative or outside the supported date range
	 */
	public void restoreStatusDate(long persistedStatusDate) {
		validatePersistedStatusDate(persistedStatusDate);
		this.statusDate = persistedStatusDate;
	}

	/** Validates a persisted status date without requiring a project instance. */
	public static void validatePersistedStatusDate(long persistedStatusDate) {
		if (persistedStatusDate < 0L)
			throw new IllegalArgumentException("Persisted status date cannot be negative");
		if (persistedStatusDate > 0L) {
			long lastSupportedInstant = DateTime.midnightNextDay(DateTime.getMaxDate().getTime()) - 1L;
			if (persistedStatusDate > lastSupportedInstant)
				throw new IllegalArgumentException("Persisted status date is outside the supported date range");
		}
	}
	


	public final int getPriority() {
		return priority;
	}
	public final void setPriority(int priority) {
		this.priority = priority;
	}
	public void add(Task task) {
		tasks.add(task);
	}
	/**
	 * @return Returns the tasks.
	 */
	public LinkedList<Task> getTasks() {
		return tasks;
	}
	public List<Task> getTaskList() {
		return tasks;
	}

	/**
	 * @return Returns the resourcePool.
	 */
	public ResourcePool getResourcePool() {
		return com.microproject.transaction.ProjectMutationState.coordinated(() -> resourcePool);
	}
    public void setResourcePool(ResourcePool resourcePool) {
        com.microproject.transaction.ProjectMutationState.coordinated(() -> this.resourcePool = resourcePool);
    }

	/** Shared-pool metadata snapshot used only while atomically resolving a stored pool reference. */
	public SharedPoolState captureSharedPoolState() {
		return new SharedPoolState(resourcePool, sharedResourcePoolFile, sharedResourcePoolProjectId,
			sharedResourcePoolUnresolved, resourcePoolTakesPrecedence);
	}
	public void restoreSharedPoolState(SharedPoolState state) {
		com.microproject.transaction.ProjectMutationState.coordinated(() -> {
			resourcePool = state.resourcePool();
			sharedResourcePoolFile = state.file();
			sharedResourcePoolProjectId = state.projectId();
			sharedResourcePoolUnresolved = state.unresolved();
			resourcePoolTakesPrecedence = state.poolTakesPrecedence();
		});
	}
	public record SharedPoolState(ResourcePool resourcePool, String file, long projectId, boolean unresolved,
		boolean poolTakesPrecedence) { }

	public String getSharedResourcePoolFile() {
		return sharedResourcePoolFile;
	}

	public void setSharedResourcePoolFile(String sharedResourcePoolFile) {
		this.sharedResourcePoolFile = sharedResourcePoolFile;
		this.sharedResourcePoolUnresolved = sharedResourcePoolFile != null && !sharedResourcePoolFile.isBlank();
	}
	public long getSharedResourcePoolProjectId() { return sharedResourcePoolProjectId; }
	public void setSharedResourcePoolProjectId(long sharedResourcePoolProjectId) {
		this.sharedResourcePoolProjectId = sharedResourcePoolProjectId;
	}

	public boolean isSharedResourcePoolUnresolved() {
		return sharedResourcePoolUnresolved;
	}

	public void setSharedResourcePoolUnresolved(boolean unresolved) {
		this.sharedResourcePoolUnresolved = unresolved;
	}

	public boolean isResourcePoolTakesPrecedence() {
		return resourcePoolTakesPrecedence;
	}

	public void setResourcePoolTakesPrecedence(boolean resourcePoolTakesPrecedence) {
		this.resourcePoolTakesPrecedence = resourcePoolTakesPrecedence;
	}

	public void accept(NodeVisitor visitor) {
		visitor.accept(this);
	}

	public Class<?> getType() throws NodeException {
		return getClass();
	}

	public boolean isVirtual() {
		return false;
	}

	public void setVirtual(boolean virtual) {
	}

	private transient OutlineCollection taskOutlines = new OutlineCollectionImpl(Settings.numHierarchies(),this);

	public NodeModel getTaskOutline() {
		return taskOutlines.getOutline();
	}
	public NodeModel getTaskOutline(int outlineNumber) {
		return taskOutlines.getOutline(outlineNumber);
	}

	public void addToDefaultOutline(Node parentNode, Node childNode) {
		taskOutlines.addToDefaultOutline(parentNode,childNode);
		if (parentNode == null)
			return;
		setDefaultRelationship(parentNode,childNode);
	}
	public void addToDefaultOutline(Node parentNode, Node childNode, int position,boolean event) {
		taskOutlines.addToDefaultOutline(parentNode,childNode,position,event);
		if (parentNode == null||childNode.isVoid())
			return;
		setDefaultRelationship(parentNode,childNode);
	}
	public OutlineCollection getTaskOutlines() {
		return taskOutlines;
	}
	private void setDefaultRelationship(Node parentNode, Node childNode) {
		Task childTask = (Task)childNode.getImpl();
		if (parentNode == null) {
			childTask.setWbsParent(null);
		} else {
			Task parentTask = (Task)parentNode.getImpl();
			childTask.setWbsParent(parentTask);
			if (parentTask != null)
				parentTask.setWbsChildrenNodes(taskOutlines.getDefaultOutline().getHierarchy().getChildren(parentNode));
		}
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}
	/**
	 * @return
	 */
	public long getDuration() {
		return getEffectiveWorkCalendar().compare(end,start,false);
	}


	WorkCalendar workCalendar = null;
	/**
	 * @return
	 */
	public WorkCalendar getWorkCalendar() {
		return workCalendar;
	}

	/**
	 * @param workCalendar
	 */
	public void setWorkCalendar(WorkCalendar workCalendar) {
		com.microproject.transaction.ProjectMutationState.coordinated(() -> {
			if (this.workCalendar != null)
				((WorkingCalendar)this.workCalendar).removeObjectUsing(this);
			this.workCalendar = workCalendar;
			((WorkingCalendar)this.workCalendar).addObjectUsing(this);
		});
	}

	public WorkCalendar getEffectiveWorkCalendar() {
		return workCalendar;
	}

	public HasCalendar getHasCalendar() {
		return this;
	}
	/**
	 * @param end
	 */
	public void setEnd(long end) {
		scheduleFacade.setEnd(end);
	}
	public void setEnd(long end, FieldContext fieldContext) {
		if (FieldContext.isTaskSheetUpdate(fieldContext)) {
			TaskSheetScheduleWorkflow.applyProjectFinish(this, end);
			return;
		}
		setEnd(end);
	}
	/**
	 * @param start
	 */
	public void setStart(long start) {
		scheduleFacade.setStart(start);
	}
	public void setStart(long start, FieldContext fieldContext) {
		if (FieldContext.isTaskSheetUpdate(fieldContext)) {
			TaskSheetScheduleWorkflow.applyProjectStart(this, start);
			return;
		}
		setStart(start);
	}



	/**
	 * Quick function to find a task by id.  Should probably replaced with hash table
	 * @param idObject
	 * @param project
	 * @return
	 */
	public static Task findTaskById(Object idObject, Collection<?> taskList) {
		return findTaskById(taskList, ((Number)idObject).intValue());
	}
	public Task findByUniqueId(long id) {
		return findTaskByUniqueId(getTaskOutlineIterator(), id);
	}

	private static Task findTaskById(Collection<?> taskList, int id) {
		for (Object taskObject : taskList) {
			Task task = (Task) taskObject;
			if (task.getId() == id) {
				return task;
			}
		}
		return null;
	}

	private static Task findTaskByUniqueId(Iterator<Task> tasks, long id) {
		while (tasks.hasNext()) {
			Task task = tasks.next();
			if (task.getUniqueId() == id) {
				return task;
			}
		}
		return null;
	}
	/**
	 * @return Returns the initialized.
	 */
	public boolean isInitialized() {
		return initialized;
	}
	/**
	 * @return Returns the manager.
	 */
	public String getManager() {
		return manager;
	}
	/**
	 * @param manager The manager to set.
	 */
	public void setManager(String manager) {
		this.manager = manager;
	}
	/**
	 * @return Returns the notes.
	 */
	public String getNotes() {
		return notes;
	}
	/**
	 * @param notes The notes to set.
	 */
	public void setNotes(String notes) {
		this.notes = notes;
	}
	/**
	 * @return
	 */
	public Date getCreated() {
		return created;
	}
	public synchronized String getDocumentId() {
		if (documentId == null || documentId.isBlank())
			documentId = java.util.UUID.randomUUID().toString();
		return documentId;
	}
	public synchronized void setDocumentId(String documentId) {
		if (documentId == null || documentId.isBlank()) {
			this.documentId = java.util.UUID.randomUUID().toString();
			return;
		}
		this.documentId = java.util.UUID.fromString(documentId).toString();
	}
	/**
	 * @return
	 */
	public long getId() {
		return identityFacade.getId();
	}
	/**
	 * @return
	 */
	public String getName() {
		return identityFacade.getName();
	}
	/**
	 * @return
	 */
	public long getUniqueId() {
		return identityFacade.getUniqueId();
	}
	/**
	 * @param created
	 */
	public void setCreated(Date created) {
		this.created = created;
		identityFacade.setCreated(created);
	}
	/**
	 * @param id
	 */
	public void setId(long id) {
		identityFacade.setId(id);
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		if (name == null || name.length() == 0)
			return;
		String oldName=getName();
		identityFacade.setName(name);
		if ((oldName==null&&name!=null)||(!oldName.equals(name))) fireNameChanged(this, oldName);
		if (getWorkCalendar() == null)
			logger.warning("error work calendar is null on project");
	}

	protected transient CopyOnWriteArrayList<ProjectListener> projectListenerList = new CopyOnWriteArrayList<>();

	public void addProjectListener(ProjectListener l) {
		if (l != null)
			projectListenerList.add(l);
	}
	public void removeProjectListener(ProjectListener l) {
		if (l != null)
			projectListenerList.remove(l);
	}
	public ProjectListener[] getProjectListeners() {
		List<ProjectListener> listeners = new ArrayList<>(projectListenerList);
		Collections.reverse(listeners);
		return listeners.toArray(ProjectListener[]::new);
	}
	public EventListener[] getProjectListeners(Class listenerType) {
		Objects.requireNonNull(listenerType, "listenerType");
		if (listenerType == ProjectListener.class)
			return getProjectListeners();
		return (EventListener[]) java.lang.reflect.Array.newInstance(listenerType, 0);
	}

 	protected void fireNameChanged(Object source,String oldName) {
		ProjectEvent e = null;
		for (ProjectListener listener : projectListenerList) {
			if (e == null) {
				e = new ProjectEvent(source, ProjectEvent.Kind.NAME_CHANGED, this, oldName);
			}
			listener.nameChanged(e);
		}
	}
 	protected void fireGroupDirtyChanged(Object source,boolean oldName) {
		ProjectEvent e = null;
		for (ProjectListener listener : projectListenerList) {
			if (e == null) {
				e = new ProjectEvent(source, ProjectEvent.Kind.GROUP_DIRTY_CHANGED, this, oldName);
			}
			listener.groupDirtyChanged(e);
		}
	}




	/**
	 * @param id
	 */
	public void setUniqueId(long id) {
		identityFacade.setUniqueId(id);
	}

	public Object createUnvalidatedObject(NodeModel nodeModel, Object parent) {
		return taskLifecycleFacade.createUnvalidatedObject(nodeModel, parent);
	}
	public void addUnvalidatedObject(Object object, NodeModel nodeModel, Object parent) {
		taskLifecycleFacade.addUnvalidatedObject(object, nodeModel, parent);

	}
	public NodeModelDataFactory getFactoryToUseForChildOfParent(Object parent) {
		if (!(parent instanceof Task task))
			return this;
		return task.getEnclosingProject();
	}
	public void validateObject(Object newlyCreated, NodeModel nodeModel, Object eventSource, Object hierarchyInfo,boolean isNew) {
		taskLifecycleFacade.validateObject(newlyCreated, nodeModel, eventSource, hierarchyInfo, isNew);

	}
	public void updateScheduling(Object source,Object newlyCreated,int type){
		ObjectEvent evt = ObjectEvent.getInstance(source,newlyCreated,type,null);
		getSchedulingAlgorithm().objectChanged(evt);
		evt.recycle();
	}
	public void updateScheduling(Object source,Object newlyCreated,int type,Field field){
		ObjectEvent evt = ObjectEvent.getInstance(source,newlyCreated,type,null);
		evt.setField(field);
		getSchedulingAlgorithm().objectChanged(evt);
		evt.recycle();
	}
	public void remove(Object toRemove, NodeModel nodeModel,boolean deep,boolean undo,boolean cleanDependencies){
		taskLifecycleFacade.remove(toRemove, nodeModel, deep, undo, cleanDependencies);
	}

	public void removeExternal(Task toRemove) {
		tasks.remove(toRemove);
		taskOutlines.removeFromAll(toRemove,null); // update all node models except the one passed in
		objectEventManager.fireDeleteEvent(this,toRemove);
	}

	/** Removes an external placeholder and returns a rollback action for an enclosing open-time transaction. */
	public Runnable removeExternalForResolution(Task toRemove) {
		int taskIndex = tasks.indexOf(toRemove);
		List<ExternalOutlineEntry> outlineEntries = new ArrayList<>();
		for (NodeModel model : taskOutlines.getOutlines()) {
			if (model == null) continue;
			Node node = model.search(toRemove);
			if (node == null) continue;
			Node parent = model.getParent(node);
			int position = parent == null ? -1 : parent.getIndex(node);
			outlineEntries.add(new ExternalOutlineEntry(model, node, parent, position));
		}
		Runnable rollback = () -> {
			if (taskIndex >= 0 && !tasks.contains(toRemove))
				tasks.add(Math.min(taskIndex, tasks.size()), toRemove);
			for (ExternalOutlineEntry entry : outlineEntries) {
				if (entry.model().search(toRemove) == null)
					entry.model().add(entry.parent(), entry.node(), entry.position(), NodeModel.SILENT);
			}
		};
		try {
			removeExternal(toRemove);
		} catch (RuntimeException | Error failure) {
			try { rollback.run(); }
			catch (RuntimeException | Error rollbackFailure) { failure.addSuppressed(rollbackFailure); }
			throw failure;
		}
		return rollback;
	}

	private record ExternalOutlineEntry(NodeModel model, Node node, Node parent, int position) { }



	public ReversibleModelChange saveCurrentToSnapshot(Object snapshotId, boolean entireProject, List<?> selection) {
		return saveCurrentToSnapshot(snapshotId, entireProject, selection, BaselineRollupOptions.NONE);
	}

	/** Saves selected task baselines and, when requested, recomputes affected summary baselines. */
	public ReversibleModelChange saveCurrentToSnapshot(Object snapshotId, boolean entireProject,
		List<?> selection, BaselineRollupOptions rollupOptions) {
		BaselineRollupOptions options = rollupOptions == null ? BaselineRollupOptions.NONE : rollupOptions;
		BaselineMutationScope scope = resolveBaselineMutationScope(snapshotId, entireProject, selection, options);
		List<NormalTask> selectedTasks = scope.selectedTasks();
		List<NormalTask> affectedTasks = scope.affectedTasks();
		SnapshotState before = captureSnapshotState(snapshotId, false, affectedTasks);
		boolean rollsUp = !entireProject && isBaselineSlot(snapshotId)
			&& (options.toSummaryTasks() || options.fromSubtasks());
		List<PreparedBaselineRollup> preparedRollups = List.of();
		Map<NormalTask, TaskSnapshot> selectedSnapshots = Map.of();
		if (rollsUp) {
			selectedSnapshots = prepareSelectedBaselineSnapshots(selectedTasks);
			preparedRollups = prepareBaselineRollups(snapshotId, selectedTasks, scope.rollupSummaries(), selectedSnapshots);
		}
		Map<NormalTask, TaskSnapshot> stagedSelectedSnapshots = selectedSnapshots;
		List<PreparedBaselineRollup> stagedRollups = preparedRollups;
		Map<NormalTask, Boolean> dirtyBefore = new java.util.IdentityHashMap<>();
		for (NormalTask task : affectedTasks) dirtyBefore.put(task, task.isDirty());
		return runAtomicModelCommand(markApplied -> {
			markApplied.run();
			try {
				if (entireProject) forTasks(new SnapshottableImpl.SaveCurrentToSnapshotClosure(snapshotId));
				else if (rollsUp) {
					for (NormalTask task : selectedTasks) {
						task.setSnapshot(snapshotId, stagedSelectedSnapshots.get(task));
						task.markTaskAsNeedingRecalculation();
						task.setDirty(true);
					}
					for (PreparedBaselineRollup prepared : stagedRollups) {
						prepared.summary().setSnapshot(snapshotId, prepared.snapshot());
						for (Assignment assignment : prepared.assignments())
							prepared.snapshot().addBaselineRollupAssignment(prepared.summary(),
								((Integer) snapshotId).intValue(), assignment);
						prepared.summary().markTaskAsNeedingRecalculation();
						prepared.summary().setDirty(true);
					}
				} else DataUtils.forAllDo(selection.iterator(), new SnapshottableImpl.SaveCurrentToSnapshotClosure(snapshotId));
				// Summary snapshots changed by roll-up must be included in the redo state too.
				SnapshotState after = !affectedTasks.isEmpty() && !entireProject
					? captureSnapshotState(snapshotId, false, new ArrayList<>(affectedTasks))
					: captureSnapshotState(snapshotId, entireProject, selection);
				fireCurrentBaselineRowState(snapshotId);
				return ReversibleModelChange.changed(
					() -> restoreSnapshotState(snapshotId, before),
					() -> restoreSnapshotState(snapshotId, after));
			} catch (RuntimeException | Error failure) {
				try {
					restoreSnapshotState(snapshotId, before);
					dirtyBefore.forEach(NormalTask::setDirty);
				} catch (RuntimeException | Error rollbackFailure) {
					if (rollbackFailure != failure) failure.addSuppressed(rollbackFailure);
				}
				throw failure;
			}
		}, ReversibleModelChange::hasChanged, change -> change);
	}

	private BaselineMutationScope resolveBaselineMutationScope(Object snapshotId, boolean entireProject,
		Collection<?> selection, BaselineRollupOptions options) {
		List<NormalTask> selectedTasks = snapshotTasks(entireProject, selection);
		LinkedHashSet<NormalTask> selectedSummaries = new LinkedHashSet<>();
		LinkedHashSet<NormalTask> ancestorSummaries = new LinkedHashSet<>();
		if (!entireProject && isBaselineSlot(snapshotId)
				&& (options.toSummaryTasks() || options.fromSubtasks())) {
			for (NormalTask task : selectedTasks) {
				if (options.fromSubtasks() && hasWbsChildren(task)) selectedSummaries.add(task);
				if (options.toSummaryTasks())
					for (Task parent = task.getWbsParentTask(); parent != null; parent = parent.getWbsParentTask())
						if (parent instanceof NormalTask summary && hasWbsChildren(summary))
							ancestorSummaries.add(summary);
			}
		}
		LinkedHashSet<NormalTask> affected = new LinkedHashSet<>(selectedTasks);
		affected.addAll(selectedSummaries);
		affected.addAll(ancestorSummaries);
		LinkedHashSet<NormalTask> rollups = new LinkedHashSet<>(selectedSummaries);
		rollups.addAll(ancestorSummaries);
		return new BaselineMutationScope(selectedTasks, List.copyOf(selectedSummaries),
			List.copyOf(ancestorSummaries), List.copyOf(affected), deepestFirst(rollups));
	}

	private Map<NormalTask, TaskSnapshot> prepareSelectedBaselineSnapshots(List<NormalTask> selectedTasks) {
		Map<NormalTask, TaskSnapshot> snapshots = new java.util.IdentityHashMap<>();
		for (NormalTask task : selectedTasks) {
			if (!(task.getCurrentSnapshot() instanceof TaskSnapshot current)) continue;
			TaskSnapshot snapshot = (TaskSnapshot) task.cloneSnapshot(current);
			snapshot.setFixedCostCalendar(task.getEffectiveWorkCalendar());
			snapshots.put(task, snapshot);
		}
		return snapshots;
	}

	private List<PreparedBaselineRollup> prepareBaselineRollups(Object snapshotId, List<NormalTask> selectedTasks,
		List<NormalTask> summaries, Map<NormalTask, TaskSnapshot> stagedSnapshots) {
		ArrayList<PreparedBaselineRollup> prepared = new ArrayList<>(summaries.size());
		for (NormalTask summary : summaries) {
			ArrayList<BaselineContribution> contributions = new ArrayList<>();
			collectBaselineContributions(summary, snapshotId, stagedSnapshots, contributions);
			TaskSnapshot previous = (TaskSnapshot) summary.getSnapshot(snapshotId);
			TaskSnapshot template = previous;
			if (template == null) template = (TaskSnapshot) summary.getCurrentSnapshot();
			TaskSnapshot rolled = (TaskSnapshot) template.deepCloneWithTask(summary);
			// The rolled assignment and fixed-cost profiles become the new authority.
			// A scalar imported from MSPDI describes the old aggregate and must not mask them.
			rolled.setImportedBaselineCost(null);
			TaskSchedule schedule = rolled.getCurrentSchedule();
			long start = Long.MAX_VALUE;
			long finish = Long.MIN_VALUE;
			rolled.getAssignments().clear();
			ArrayList<Assignment> assignments = new ArrayList<>();
			Set<Long> profileIds = new java.util.HashSet<>();
			for (BaselineContribution contribution : contributions) {
				TaskSchedule childSchedule = contribution.snapshot().getCurrentSchedule();
				if (childSchedule.getStart() != 0L) start = Math.min(start, childSchedule.getStart());
				if (childSchedule.getFinish() != 0L) finish = Math.max(finish, childSchedule.getFinish());
				for (Object value : contribution.snapshot().getAssignments())
					if (value instanceof Assignment childAssignment && !childAssignment.isDefault()) {
						Assignment clone = (Assignment) childAssignment.cloneWithTask(summary);
						if (!clone.isBaselineAssignment() || clone.getTask() != summary || clone.isDefault()
								|| !profileIds.add(Long.valueOf(clone.getUniqueId())))
							throw new IllegalArgumentException("Invalid or duplicate baseline roll-up assignment profile");
						assignments.add(clone);
					}
			}
			long rolledStart = start == Long.MAX_VALUE ? 0L : start;
			long rolledFinish = finish == Long.MIN_VALUE ? 0L : finish;
			schedule.setStart(rolledStart);
			schedule.setFinish(rolledFinish);
			schedule.setRawDuration(rolledStart == 0L || rolledFinish == 0L ? 0L
				: summary.getEffectiveWorkCalendar().compare(rolledFinish, rolledStart, false));
			double ownFixedCost = selectedTasks.contains(summary)
				? ((TaskSnapshot) summary.getCurrentSnapshot()).getFixedCost()
				: previous == null ? 0.0d : previous.getFixedCost();
			rolled.setFixedCost(ownFixedCost);
			if (selectedTasks.contains(summary) || previous == null)
				rolled.setFixedCostCalendar(summary.getEffectiveWorkCalendar());
			ArrayList<TaskSnapshot.FixedCostProfile> fixedCostProfiles = new ArrayList<>();
			collectBaselineFixedCostProfiles(summary, snapshotId, stagedSnapshots, fixedCostProfiles);
			rolled.setRolledUpFixedCostProfiles(fixedCostProfiles);
			stagedSnapshots.put(summary, rolled);
			prepared.add(new PreparedBaselineRollup(summary, rolled, assignments));
		}
		return List.copyOf(prepared);
	}

	private record BaselineMutationScope(List<NormalTask> selectedTasks, List<NormalTask> selectedSummaries,
		List<NormalTask> ancestorSummaries, List<NormalTask> affectedTasks, List<NormalTask> rollupSummaries) { }
	private record PreparedBaselineRollup(NormalTask summary, TaskSnapshot snapshot, List<Assignment> assignments) { }

	private List<NormalTask> snapshotTasks(boolean entireProject, Collection<?> selection) {
		ArrayList<NormalTask> result = new ArrayList<>();
		Iterator<?> tasks = getSnapshotIterator(entireProject, selection);
		if (tasks == null) return result;
		while (tasks.hasNext())
			if (tasks.next() instanceof NormalTask task) result.add(task);
		return result;
	}

	private static boolean hasWbsChildren(Task task) {
		Collection<Node> children = task.getWbsChildrenNodes();
		return children != null && !children.isEmpty();
	}

	private List<NormalTask> deepestFirst(Collection<NormalTask> tasks) {
		ArrayList<NormalTask> result = new ArrayList<>(tasks);
		result.sort((left, right) -> Integer.compare(wbsDepth(right), wbsDepth(left)));
		return result;
	}

	private static int wbsDepth(Task task) {
		int depth = 0;
		for (Task parent = task.getWbsParentTask(); parent != null; parent = parent.getWbsParentTask()) depth++;
		return depth;
	}

	private void collectBaselineContributions(NormalTask task, Object snapshotId,
		Map<NormalTask, TaskSnapshot> stagedSnapshots, List<BaselineContribution> contributions) {
		TaskSnapshot snapshot = stagedSnapshots.containsKey(task) ? stagedSnapshots.get(task)
			: (TaskSnapshot) task.getSnapshot(snapshotId);
		Collection<?> rollupChildren = task.childrenToRollup();
		boolean assignedDirectly = false;
		if (rollupChildren != null)
			for (Object child : rollupChildren)
				if (child instanceof Assignment assignment && !assignment.isDefault()) {
					assignedDirectly = true;
					break;
				}
		if (assignedDirectly) {
			if (snapshot != null) contributions.add(new BaselineContribution(task, snapshot));
			return;
		}
		if (rollupChildren != null && !rollupChildren.isEmpty()) {
			for (Object child : rollupChildren)
				if (child instanceof NormalTask normalChild)
					collectBaselineContributions(normalChild, snapshotId, stagedSnapshots, contributions);
			return;
		}
		if (snapshot != null) contributions.add(new BaselineContribution(task, snapshot));
	}

	private void collectBaselineFixedCostProfiles(NormalTask summary, Object snapshotId,
		Map<NormalTask, TaskSnapshot> stagedSnapshots, List<TaskSnapshot.FixedCostProfile> profiles) {
		Collection<Node> children = summary.getWbsChildrenNodes();
		if (children == null) return;
		for (Node childNode : children) {
			if (!(childNode.getImpl() instanceof NormalTask child)) continue;
			TaskSnapshot childSnapshot = stagedSnapshots.containsKey(child) ? stagedSnapshots.get(child)
				: (TaskSnapshot) child.getSnapshot(snapshotId);
			if (childSnapshot != null) {
				TaskSnapshot.FixedCostProfile profile = childSnapshot.captureOwnFixedCostProfile(
					child.getEffectiveWorkCalendar());
				if (profile != null) profiles.add(profile);
			}
			collectBaselineFixedCostProfiles(child, snapshotId, stagedSnapshots, profiles);
		}
	}

	private record BaselineContribution(NormalTask task, TaskSnapshot snapshot) { }

	public ReversibleModelChange clearSnapshot(final Object snapshotId, boolean entireProject, List<?> selection) {
		SnapshotState before = captureSnapshotState(snapshotId, entireProject, selection);

		clearSnapshotState(snapshotId, entireProject, selection);
		if (!before.hasRestorableState()) {
			return ReversibleModelChange.unchanged();
		}
		return ReversibleModelChange.changed(
			() -> restoreSnapshotState(snapshotId, before),
			() -> clearSnapshotState(snapshotId, entireProject, selection));

	}

	/**
	 * Checks whether saving the requested baseline would replace data in its scope.
	 * This is a read-only preflight; it does not create snapshots or change imported
	 * assignment values. Imported assignment-only baselines count even when no task
	 * snapshot is present.
	 */
	public boolean hasBaselineData(int baselineNumber, boolean entireProject, Collection<?> selection) {
		return hasBaselineData(baselineNumber, entireProject, selection, BaselineRollupOptions.NONE);
	}

	public boolean hasBaselineData(int baselineNumber, boolean entireProject, Collection<?> selection,
		BaselineRollupOptions rollupOptions) {
		if (baselineNumber < 0 || baselineNumber > 10)
			throw new IllegalArgumentException("Baseline number must be between 0 and 10");
		BaselineMutationScope scope = resolveBaselineMutationScope(Integer.valueOf(baselineNumber), entireProject,
			selection, rollupOptions == null ? BaselineRollupOptions.NONE : rollupOptions);
		Integer slot = Integer.valueOf(baselineNumber);
		for (NormalTask task : scope.affectedTasks()) {
			if (task.getSnapshot(slot) != null) return true;
			for (Object association : task.getAssignments()) {
				if (association instanceof Assignment assignment
						&& assignment.getImportedBaselineValues(baselineNumber) != null) return true;
			}
		}
		return false;
	}

	private void clearSnapshotState(Object snapshotId, boolean entireProject, List<?> selection) {
		if (entireProject) forTasks(new SnapshottableImpl.ClearSnapshotClosure(snapshotId));
		else DataUtils.forAllDo(selection.iterator(), new SnapshottableImpl.ClearSnapshotClosure(snapshotId));
		fireCurrentBaselineRowState(snapshotId);
	}

	private SnapshotState captureSnapshotState(Object snapshotId, boolean entireProject, List<?> selection) {
		Iterator<?> tasks = getSnapshotIterator(entireProject, selection);
		ArrayList<SnapshotTaskState> taskStates = new ArrayList<>();
		boolean capturesImportedBaselineValues = isBaselineSlot(snapshotId);
		if (tasks != null) {
			while (tasks.hasNext()) {
				Object value = tasks.next();
				if (!(value instanceof NormalTask task)) continue;
				DataSnapshot snapshot = task.getSnapshot(snapshotId);
				Map<Assignment, Assignment.BaselineValues> importedValues = new HashMap<>();
				if (capturesImportedBaselineValues) {
					for (Object assignmentValue : task.getAssignments()) {
						Assignment assignment = (Assignment) assignmentValue;
						Assignment.BaselineValues baselineValues = assignment.getImportedBaselineValues((Integer) snapshotId);
						if (baselineValues != null) importedValues.put(assignment, baselineValues);
					}
				}
				taskStates.add(new SnapshotTaskState(task,
					snapshot == null ? null : task.cloneSnapshot(snapshot), importedValues));
			}
		}
		return new SnapshotState(taskStates);
	}

	private void restoreSnapshotState(Object snapshotId, SnapshotState state) {
		for (SnapshotTaskState taskState : state.tasks) {
			NormalTask task = taskState.task;
			if (taskState.snapshot == null) task.clearSnapshot(snapshotId);
			else {
				task.setSnapshot(snapshotId, task.cloneSnapshot(taskState.snapshot));
				task.markTaskAsNeedingRecalculation();
				task.setDirty(true);
			}
			if (isBaselineSlot(snapshotId))
				taskState.importedBaselineValues.forEach((assignment, values) ->
					assignment.restoreImportedBaselineValues((Integer) snapshotId, values));
		}
		fireCurrentBaselineRowState(snapshotId);
	}

	private record SnapshotTaskState(NormalTask task, DataSnapshot snapshot,
		Map<Assignment, Assignment.BaselineValues> importedBaselineValues) {
	}

	private static final class SnapshotState {
		private final List<SnapshotTaskState> tasks;

		private SnapshotState(List<SnapshotTaskState> tasks) {
			this.tasks = tasks;
		}

		private boolean hasRestorableState() {
			return tasks.stream().anyMatch(task -> task.snapshot != null || !task.importedBaselineValues.isEmpty());
		}

	}

	private void fireCurrentBaselineRowState(Object snapshotId) {
		fireSnapshotBaselineChanged(snapshotId, hasProjectBaselineSnapshot(snapshotId));
	}

	private boolean hasProjectBaselineSnapshot(Object snapshotId) {
		if (!isBaselineSlot(snapshotId)) return false;
		for (Iterator<Task> tasks = getTaskOutlineIterator(); tasks.hasNext();) {
			if (tasks.next().getSnapshot(snapshotId) != null) return true;
		}
		return false;
	}

	private static boolean isBaselineSlot(Object snapshotId) {
		return snapshotId instanceof Integer slot && slot.intValue() >= 0 && slot.intValue() <= 10;
	}

	private Iterator<?> getSnapshotIterator(boolean entireProject, Collection<?> selection) {
		if (entireProject) {
			return getTaskOutlineIterator();
		}
		return selection == null ? null : selection.iterator();
	}

	private void fireSnapshotBaselineChanged(Object snapshotId, boolean save) {
		fireBaselineChanged(this, null, (Integer)snapshotId, save);
	}
	/**
	 * @param context
	 * @return
	 */
	public String getName(FieldContext context) {
		return identityFacade.getName(context);
	}

	public String toString() {
		return getName();
	}
	/**
	 * @param listener
	 */
	public void addScheduleListener(ScheduleEventListener listener) {
		scheduleEventManager.addListener(listener);
	}
	/**
	 * @param listener
	 */
	public void removeScheduleListener(ScheduleEventListener listener) {
		scheduleEventManager.removeListener(listener);
	}

	/**
	 * @param listener
	 */
	public void addObjectListener(ObjectEvent.Listener listener) {
		objectEventManager.addListener(listener);
	}
	/**
	 * @param listener
	 */
	public void removeObjectListener(ObjectEvent.Listener listener) {
		objectEventManager.removeListener(listener);
	}

	public ObjectEventManager getObjectEventManager() {
		return objectEventManager;
	}


	public void fireScheduleChanged(Object source, String type) {
		scheduleEventManager.fire(source,type);
	}
	public void fireScheduleChanged(Object source, String type, Object object) {
		scheduleEventManager.fire(source,type,object);
	}
	/** Starts a project-local observer batch for an atomic model transaction. */
	public void beginEventBatch() {
		beginEventBatch(List.of());
	}
	private synchronized void beginEventBatch(Collection<AdditionalObjectEventManager> requestedManagers) {
		if (eventBatchDepth == 0 && undoController != null) {
			undoBatchActivation = undoController.getEditSupport().beginCommand();
		}
		if (undoBatchActivation != null)
			undoBatchCheckpoints.add(undoController.getEditSupport().checkpoint(undoBatchActivation.group()));
		objectEventManager.beginBatch();
		scheduleEventManager.beginBatch();
		for (AdditionalObjectEventManager participant : additionalEventManagers)
			participant.manager().beginBatch();
		eventBatchDepth++;
		for (AdditionalObjectEventManager participant : requestedManagers) {
			int existingIndex = -1;
			for (int index = 0; index < additionalEventManagers.size(); index++)
				if (additionalEventManagers.get(index).manager() == participant.manager()) {
					existingIndex = index;
					break;
				}
			if (existingIndex < 0) {
				for (int depth = 0; depth < eventBatchDepth; depth++)
					participant.manager().beginBatch();
				additionalEventManagers.add(participant);
			} else {
				AdditionalObjectEventManager existing = additionalEventManagers.get(existingIndex);
				List<Object> refreshObjects = new ArrayList<>(existing.refreshObjects());
				for (Object refreshObject : participant.refreshObjects())
					if (refreshObjects.stream().noneMatch(existingObject -> existingObject == refreshObject))
						refreshObjects.add(refreshObject);
				additionalEventManagers.set(existingIndex, new AdditionalObjectEventManager(existing.manager(),
					existing.revisionOwner(), existing.source(), refreshObjects));
			}
		}
	}
	/** Number of deferred observer facts in the current nested transaction batches. */
	public int pendingEventCount() {
		int pending = objectEventManager.pendingEventCount() + scheduleEventManager.pendingEventCount();
		for (AdditionalObjectEventManager participant : additionalEventManagers)
			pending += participant.manager().pendingEventCount();
		return pending;
	}
	/** Completes the current observer batch, publishing only when the model committed. */
	public void endEventBatch(boolean publish) {
		publishEventBatch(detachEventBatch(publish));
	}
	/** Detaches both observer batches so a committed revision cannot mix with a later mutation. */
	public PendingEventBatch detachEventBatch(boolean publish) {
		com.microproject.document.ObjectEventManager.PendingBatch objectEvents = objectEventManager.detachBatch(publish);
		com.microproject.pm.scheduling.ScheduleEventManager.PendingBatch scheduleEvents = scheduleEventManager.detachBatch(publish);
		List<AdditionalPendingBatch> additionalEvents = new ArrayList<>();
		com.microproject.undo.EditSupport.Group undoGroup = null;
		synchronized (this) {
			if (undoBatchActivation != null) {
				EditSupport.Checkpoint checkpoint = undoBatchCheckpoints.removeLast();
				if (!publish) undoController.getEditSupport().discardSince(undoBatchActivation.group(), checkpoint);
			}
			for (AdditionalObjectEventManager participant : additionalEventManagers) {
				ObjectEventManager.PendingBatch events = participant.manager().detachBatch(publish);
				if (events != null) additionalEvents.add(new AdditionalPendingBatch(participant, events,
					additionalPublicationRevision(participant)));
			}
			if (--eventBatchDepth == 0) {
				if (undoBatchActivation != null) {
					undoGroup = undoBatchActivation.group();
					undoBatchActivation.close();
					undoBatchActivation = null;
				}
				additionalEventManagers.clear();
			}
		}
		return new PendingEventBatch(getMutationState().revision(), objectEvents, scheduleEvents,
			List.copyOf(additionalEvents), undoGroup, false);
	}
	private long additionalPublicationRevision(AdditionalObjectEventManager participant) {
		return participant.revisionOwner().getCoordinationRevision();
	}
	/** Atomically detaches notifications and commits the revision before releasing the mutation barrier. */
	public PendingEventBatch commitEventBatch(com.microproject.transaction.ProjectMutationState.Scope mutation,
		boolean changed) {
		PendingEventBatch detached = detachEventBatch(true);
		com.microproject.document.ObjectEventManager.PendingBatch objectEvents = detached.objectEvents();
		com.microproject.pm.scheduling.ScheduleEventManager.PendingBatch scheduleEvents = detached.scheduleEvents();
		if (changed && ((objectEvents != null && !objectEvents.isPublishable())
				|| (scheduleEvents != null && !scheduleEvents.isPublishable()))) {
			// A nested operation rolled back its accumulated facts. Do not publish
			// potentially stale pooled facts; the committed outer state needs a fresh
			// schedule invalidation instead.
			objectEvents = null;
			scheduleEventManager.beginBatch();
			scheduleEventManager.fire(this, com.microproject.pm.scheduling.ScheduleEvent.SCHEDULE);
			scheduleEvents = scheduleEventManager.detachBatch(true);
		}
		List<AdditionalPendingBatch> additionalEvents = new ArrayList<>(detached.additionalEvents());
		if (changed) {
			for (int index = 0; index < additionalEvents.size(); index++) {
				AdditionalPendingBatch additional = additionalEvents.get(index);
				if (!additional.events().isPublishable()) {
					ObjectEventManager manager = additional.participant().manager();
					manager.beginBatch();
					for (Object refreshObject : additional.participant().refreshObjects())
						manager.fireUpdateEvent(additional.participant().source(), refreshObject);
					additionalEvents.set(index, new AdditionalPendingBatch(additional.participant(), manager.detachBatch(true)));
				}
			}
		}
		for (int index = 0; index < additionalEvents.size(); index++) {
			AdditionalPendingBatch additional = additionalEvents.get(index);
			if (additional.events().isPublishable() && additional.events().hasEvents())
				additionalEvents.set(index, new AdditionalPendingBatch(additional.participant(), additional.events(),
					additional.participant().revisionOwner().advanceCoordinationRevision()));
		}
		mutation.commit(changed);
		if (changed && detached.undoGroup() != null)
			undoController.getEditSupport().reserveCommit(detached.undoGroup());
		long revision = mutation.projectedRevision();
		boolean coordinatedPublication = com.microproject.transaction.ProjectMutationState.shouldDeferPublication();
		boolean hasPublication = objectEvents != null || scheduleEvents != null || !additionalEvents.isEmpty()
			|| detached.undoGroup() != null;
		PendingEventBatch pending = new PendingEventBatch(revision, objectEvents, scheduleEvents,
			List.copyOf(additionalEvents), detached.undoGroup(), coordinatedPublication);
		if (hasPublication && coordinatedPublication) {
			mutation.afterCommitRevision(committedRevision -> {
				if (committedRevision != revision)
					throw new IllegalStateException("Project publication revision changed before commit");
				return () -> publishEventBatchNow(pending);
			});
		}
		mutation.closeAndGetRevision();
		return pending;
	}
	/** Dispatches events after the mutation barrier is released. */
	public void publishEventBatch(PendingEventBatch batch) {
		if (batch == null) return;
		if (batch.coordinatedPublication()) return;
		publishEventBatchNow(batch);
	}
	private void publishEventBatchNow(PendingEventBatch batch) {
		if (batch == null) return;
		com.microproject.undo.EditSupport support = batch.undoGroup() == null ? null : undoController.getEditSupport();
		com.microproject.undo.EditSupport.Activation activation = support == null ? null
			: support.activatePublication(batch.undoGroup());
		Throwable failure = null;
		try {
			try {
				objectEventManager.publishBatch(batch.objectEvents(), batch.revision());
			} catch (Throwable current) {
				failure = current;
			}
			try {
				scheduleEventManager.publishBatch(batch.scheduleEvents(), batch.revision());
			} catch (Throwable current) {
				if (failure == null) failure = current;
				else failure.addSuppressed(current);
			}
			for (AdditionalPendingBatch additional : batch.additionalEvents()) {
				try {
				additional.participant().manager().publishBatch(additional.events(), additional.revision());
				} catch (Throwable current) {
					if (failure == null) failure = current;
					else failure.addSuppressed(current);
				}
			}
			} finally {
			if (activation != null) {
				activation.close();
				try { support.completePublication(batch.undoGroup()); }
				catch (Throwable current) {
					if (failure == null) failure = current; else failure.addSuppressed(current);
				}
			}
		}
		if (failure instanceof Error error) throw error;
		if (failure instanceof RuntimeException runtime) throw runtime;
		if (failure != null) throw new IllegalStateException("Project event publication failed", failure);
	}
	public record PendingEventBatch(
		long revision,
		com.microproject.document.ObjectEventManager.PendingBatch objectEvents,
		com.microproject.pm.scheduling.ScheduleEventManager.PendingBatch scheduleEvents,
		List<AdditionalPendingBatch> additionalEvents,
		com.microproject.undo.EditSupport.Group undoGroup,
		boolean coordinatedPublication) { }
	public record AdditionalPendingBatch(AdditionalObjectEventManager participant,
		ObjectEventManager.PendingBatch events, long revision) {
		public AdditionalPendingBatch(AdditionalObjectEventManager participant, ObjectEventManager.PendingBatch events) {
			this(participant, events, 0L);
		}
	}
	public void fireBaselineChanged(Object source, Object object, Integer baselineNumber, boolean save) {
		scheduleEventManager.fireBaselineChanged(source, null, baselineNumber, save);
	}
	public Document getDocument() {
		return this;
	}
	public void buildReverseQuery(ReverseQuery reverseQuery) {
		for (Task task : tasks)
			task.buildReverseQuery(reverseQuery);
	}

	public void forEachWorkingInterval(Consumer<Object> visitor, boolean mergeWorking, WorkCalendar workCalendar) {
		for (Task task : tasks)
			task.forEachWorkingInterval(visitor,mergeWorking, workCalendar);
	}

	public double acwp(long start, long end) {
		return TimeDistributedDataConsolidator.acwp(start,end,childrenToRollup());
	}

	public double bac(long start, long end) {
		return TimeDistributedDataConsolidator.bac(start,end,childrenToRollup());
	}

	public double bcwp(long start, long end) {
		return TimeDistributedDataConsolidator.bcwp(start,end,childrenToRollup());
	}

	public double bcws(long start, long end) {
		return TimeDistributedDataConsolidator.bcws(start,end,childrenToRollup());
	}

	public double baselineCost(long start, long end) {
		return TimeDistributedDataConsolidator.baselineCost(start,end,childrenToRollup());
	}

	public long baselineWork(long start, long end) {
		return TimeDistributedDataConsolidator.baselineWork(start,end,childrenToRollup(),true);
	}

	public double cost(long start, long end) {
		return TimeDistributedDataConsolidator.cost(start,end,activeTasksForRollup());
	}

	public double actualCost(long start, long end) {
		return TimeDistributedDataConsolidator.actualCost(start,end,childrenToRollup());
	}

	public double fixedCost(long start, long end) {
		return TimeDistributedDataConsolidator.fixedCost(start,end,activeTasksForRollup());
	}

	public double actualFixedCost(long start, long end) {
		return TimeDistributedDataConsolidator.actualFixedCost(start,end,childrenToRollup());
	}

	public boolean fieldHideActualFixedCost(FieldContext fieldContext) {
		return false;
	}
	public long work(long start, long end) {
		return TimeDistributedDataConsolidator.work(start,end,activeTasksForRollup(),true);
	}

	public long actualWork(long start, long end) {
		return TimeDistributedDataConsolidator.actualWork(start,end,childrenToRollup(),true);
	}
	public long remainingWork(long start, long end) {
		return TimeDistributedDataConsolidator.remainingWork(start,end,activeTasksForRollup(),true);
	}
	boolean isInRange(long start, long finish) {
		long s = getStart();
		return (finish > s && start < getEnd());
	}

	private boolean isFieldHidden(FieldContext fieldContext) {
		return fieldContext != null && !isInRange(fieldContext.getStart(),fieldContext.getEnd());
	}

	private boolean isBaselineFieldHidden(int numBaseline, FieldContext fieldContext) {
		return TimeDistributedFieldVisibility.isBaselineFieldHidden(childrenToRollup(), numBaseline, fieldContext);
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

	public double getCost(FieldContext fieldContext) {
		return getFixedCost(fieldContext) +
		cost(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getFixedCost(FieldContext fieldContext) {
		return fixedCost(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getActualFixedCost(FieldContext fieldContext) {
		return actualFixedCost(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}

	public long getWork(FieldContext fieldContext) {
		return work(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public double getActualCost(FieldContext fieldContext) {
		return getActualFixedCost(fieldContext) +
			actualCost(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
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

	public double getBaselineCost(FieldContext fieldContext) {
		return baselineCost(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public long getBaselineWork(FieldContext fieldContext) {
		return baselineWork(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}

	public void nodeRemoved(HierarchyEvent e) {

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
	public Collection childrenToRollup() {
		return tasks;
	}
	private Collection<Task> activeTasksForRollup() {
		return tasks.stream().filter(task -> !task.isInactiveTask()).toList();
	}
	public String getSchedulingMethod() {
		return schedulingAlgorithm.getName();
	}
	public double getBaselineCost(int numBaseline, FieldContext fieldContext) {
		return baselineCost(FieldContext.start(fieldContext),FieldContext.end(fieldContext));	}
	public long getBaselineWork(int numBaseline, FieldContext fieldContext) {
		return baselineWork(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	/**
	 * @param initialized The initialized to set.
	 */
	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}




	public void nodesChanged(HierarchyEvent e) {
		Node node, previousParentNode, newParentNode;
		Task previousParentTask, newParentTask;
		ArrayList<LeafScheduleTransition> transitionedToLeaf = new ArrayList<>();
		int count=e.getNodes().length;
		if (count==0) return;
		for (int i=0;i<count;i++){
			node=(Node)e.getNodes()[i];
			if (!(node.getImpl() instanceof Task task)) continue;

			previousParentTask=task.getWbsParentTask();
			previousParentNode=taskOutlines.getDefaultOutline().search(previousParentTask);

			if (previousParentTask!=null) {
				previousParentTask.markAllDependentTasksAsNeedingRecalculation(true); // flag this and dependent tasks as dirty
				boolean wasSummary = previousParentTask.isWbsParent();
				RollupSpan previousSummarySpan = wasSummary ? previousParentTask.calculateRollupSpan() : null;
				double previousSummaryPercentComplete = previousParentTask.getPercentComplete();
				Map<Object, Object[]> previousParentChildren = e.getPreviousParentChildren();
				Object[] previousChildren = previousParentChildren == null ? null : previousParentChildren.get(previousParentTask);
				if (previousChildren != null) {
					SummaryRollup eventPreviousRollup = calculateSummaryRollup(previousParentTask, previousChildren);
					if (eventPreviousRollup != null) {
						wasSummary = true;
						previousSummarySpan = eventPreviousRollup.span();
						previousSummaryPercentComplete = eventPreviousRollup.percentComplete();
					}
				}
				previousParentTask.setWbsChildrenNodes(taskOutlines.getDefaultOutline().getHierarchy().getChildren(previousParentNode));
				if (wasSummary && !previousParentTask.isWbsParent() && previousParentTask instanceof NormalTask normalTask)
					transitionedToLeaf.add(new LeafScheduleTransition(normalTask, previousSummarySpan, previousSummaryPercentComplete));
			}

			NodeHierarchy hierarchy=taskOutlines.getDefaultOutline().getHierarchy();
			newParentNode = hierarchy.getParent(node);
			newParentTask = null;
			if (newParentNode!=hierarchy.getRoot()) {
				newParentTask=(Task)newParentNode.getImpl();
				newParentTask.setWbsChildrenNodes(taskOutlines.getDefaultOutline().getHierarchy().getChildren(newParentNode));
				newParentTask.restrictToValidConstraintType();
				newParentTask.markAllDependentTasksAsNeedingRecalculation(true); // flag this and dependent tasks as dirty
			}

			task.setWbsParent(newParentTask);

			final Task _newParentTask = newParentTask;
			final Object eventSource = e.getSource();

			taskOutlines.getDefaultOutline().getHierarchy().visitAll(newParentNode, arg -> {
				Node visitedNode=(Node)arg;
				if (!(visitedNode.getImpl() instanceof Task visitedTask)) return;
				DependencyService.getInstance().removeAnyDependencies(visitedTask, _newParentTask,eventSource);
			});
		}
		if (!e.isVoid()) { // if the event was not the promotion of a void node
			e.consume();
			for (LeafScheduleTransition transition : transitionedToLeaf) {
				initializeFormerSummaryLeaf(transition.task(), true, transition.span(), transition.percentComplete());
			}
			updateScheduling(e.getSource(),this,ObjectEvent.CREATE); // calculate final leaf state before publishing schedule events
		}
	}
	public void nodesInserted(HierarchyEvent e) {
		nodesChanged(e);
	}
	public void nodesRemoved(HierarchyEvent e) {
	}

	private record SummaryRollup(RollupSpan span, double percentComplete) { }

	private SummaryRollup calculateSummaryRollup(Task task, Object[] previousChildren) {
		RollupSpan span = task.calculateRollupSpan(previousChildren);
		if (span.getStart() == 0L || span.getFinish() == 0L || span.getFinish() < span.getStart()) return null;
		return new SummaryRollup(span, task.taskDurationPercentRollup(previousChildren));
	}

	private void initializeFormerSummaryLeaf(Task task, boolean wasSummary, RollupSpan span, double percentComplete) {
		if (wasSummary && task != null && !task.isWbsParent() && task instanceof NormalTask normalTask)
			normalTask.initializeLeafScheduleFromSummarySpan(span, percentComplete);
	}

    public void structureChanged(HierarchyEvent e) {
    }

    public List<Node> getRootNodes(List<Task> tasks){
        List<Node> roots=new LinkedList<>();
        for (Task task : tasks){
            if (task.getWbsParentTask()==null) roots.add(taskOutlines.getDefaultOutline().search(task));
        }
        return roots;
    }

    public void dump(Collection<?> tasks,String indent){
        if (tasks!=null)
        for (Object candidate : tasks){
            Node node=(Node)candidate;
            Task task=(Task)node.getImpl();
            logger.fine(indent + task.getWbsParentTask() + "->" + task);
            dump(task.getWbsChildrenNodes(),indent+"-");
        }
    }

	private transient BarClosure barClosureInstance = new BarClosure();
	public void moveInterval(Object eventSource, long start, long end, ScheduleInterval oldInterval) {
		moveInterval(eventSource, start, end, oldInterval, false);
	}
	public void consumeIntervals(IntervalConsumer consumer) {
		consumer.consumeInterval(new ScheduleInterval(getStart(),getEnd()));
	}


	public boolean equals(Object obj){
	    if (obj instanceof DataObject dataObject){
	        return getName().equals(dataObject.getName());
	    }
	    return false;
	}

	@Override
	public int hashCode() {
		// consistent with the name-only equals above
		String name = getName();
		return name == null ? 0 : name.hashCode();
	}
	Workspace workspace;
	private void writeObject(ObjectOutputStream s) throws IOException {
		workspace = (Workspace) createWorkspace(SavableToWorkspace.PERSIST);
	    s.defaultWriteObject();
	    hasKey.serialize(s);
	}
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException  {
	    s.defaultReadObject();
	    hasKey=HasKeyImpl.deserialize(s,this);
	    initializeFacades();
	    tasks = new LinkedList<>();
		objectEventManager = new ObjectEventManager();
		objectSelectionEventManager = new ObjectSelectionEventManager();
		scheduleEventManager = new ScheduleEventManager();
		eventBatchDepth = 0;
		additionalEventManagers = new ArrayList<>();
		undoBatchActivation = null;
		undoBatchCheckpoints = new ArrayList<>();
		multipleTransactionManager = new MultipleTransactionManager();
		projectListenerList=new CopyOnWriteArrayList<>();
	    taskOutlines=new OutlineCollectionImpl(Settings.numHierarchies(),this);
	    barClosureInstance = new BarClosure();

	    // Issue #227: old .pod files deserialize `created` as null; keep the
	    // previous regenerated behavior so they still load.
	    if (created == null) created = new Date();
	    identityFacade.setCreated(created);

	}
	private void initializeFacades() {
		identityFacade = new IdentityFacade();
		baselineFacade = new BaselineFacade();
		subprojectFacade = new SubprojectFacade();
		scheduleFacade = new ScheduleFacade();
		taskAggregationFacade = new TaskAggregationFacade();
		taskLifecycleFacade = new TaskLifecycleFacade();
	}

	private void initSubprojectHandler() {
		try {
			subprojectHandler = (SubprojectHandler) Class.forName(Messages.getMetaString("SubprojectHandler")).getConstructor(Project.class).newInstance(this);
		} catch (Exception e) {
			ErrorLogger.log("SubprojectHandler initialization failed", e);
			logger.warning("SubprojectHandler not valid in meta.properties");
			System.exit(-1);
		}
	}
	public void postDeserialization(){
		lastDeserialized = this;
		initSubprojectHandler();	    //this is created transiently
	    setSchedulingAlgorithm(new CriticalPath(this)); // Critical path needs objectEventManager

		bindAssignmentOutlines();

		initializeDefaultOutline();
		setInitialized(true);
		setGroupDirty(false);
	    if (workspace != null)
	    	restoreWorkspace(workspace, SavableToWorkspace.PERSIST);
	    if (calendarOption == null)
	    	calendarOption = CalendarOption.getDefaultInstance();
		if (durationDefaults == null)
			durationDefaults = DurationDefaults.PRODUCT_DEFAULTS;

	}

	public void addEndSentinelDependency(Task task) {
		if (!task.isInSubproject())  // subprojects have fixed dates, and their children do not depend on master project's sentinels
			schedulingAlgorithm.addEndSentinelDependency(task);
	}
	public boolean removeEndSentinelDependency(Task task) {
		if (!task.isInSubproject())  // subprojects have fixed dates, and their children do not depend on master project's sentinels
			return schedulingAlgorithm.removeEndSentinelDependency(task);
		return false;
	}
	public void addStartSentinelDependency(Task task) {
		if (!task.isInSubproject())  // subprojects have fixed dates, and their children do not depend on master project's sentinels
			schedulingAlgorithm.addStartSentinelDependency(task);
	}
	public boolean removeStartSentinelDependency(Task task) {
		if (!task.isInSubproject())  // subprojects have fixed dates, and their children do not depend on master project's sentinels
			return schedulingAlgorithm.removeStartSentinelDependency(task);
		return false;
	}

	/**
	 * @param date
	 */
	public void setEndConstraint(long date) {
		schedulingAlgorithm.setEndConstraint(date);
	}
	/**
	 * @param date
	 */
	public void setStartConstraint(long date) {
		schedulingAlgorithm.setStartConstraint(date);
	}
	/**
	 * @return Returns the forward.
	 */
	public boolean isForward() {
		return forward;
	}
	/**
	 * @param forward The forward to set.
	 */
	public void setForward(boolean forward) {
		if (forward == this.forward)
			return;
		this.forward = forward;
		for (Task task : tasks)
			task.setForward(forward);
		markAllTasksAsNeedingRecalculation(false);
		schedulingAlgorithm.setForward(forward);
		schedulingAlgorithm.reset();
		schedulingAlgorithm.calculate(true);
	}

	public void fireUpdateEvent(Object source, Object object) {
		if (isInitialized())
			objectEventManager.fireUpdateEvent(source,object);
	}

	int getCalculationStateCount() {
		if (schedulingAlgorithm == null)
			return 0;
		return schedulingAlgorithm.getCalculationStateCount();
	}
	/**
	 * @return Returns the multipleTransactionManager.
	 */
	public final MultipleTransactionManager getMultipleTransactionManager() {
		return multipleTransactionManager;
	}
	public int fireMultipleTransaction(int id, boolean begin) {
		return multipleTransactionManager.fire(this,id,begin);
	}


	private void repairTasks() {
		int repairedAssignments = 0;
		for (Task candidate : tasks) {
			NormalTask task = (NormalTask) candidate;
			if (task.validateConstraints())
				addRepaired(task);
			if (task.getAssignments().isEmpty()) {
				Assignment ass = task.addDefaultAssignment();
				ass.setDirty(true);
				task.setDirty(true);
				// Missing assignments are a recoverable legacy/import condition: the
				// default assignment restores a schedulable task.  Keep this visible
				// as a warning rather than reporting a simulated exception at SEVERE,
				// which made valid POD/MPO repair look like data loss.
				repairedAssignments++;
				logger.fine("added default ass for " + task);
				addRepaired(task);
			}
		}
		if (repairedAssignments > 0)
			logger.log(java.util.logging.Level.WARNING,
					"Repaired {0} task(s) with no assignments during project initialization",
					repairedAssignments);
	}
	public void setAllTasksAsUnchangedFromPersisted(boolean justSaved) {
		getTaskOutline().getHierarchy().visitAll(o -> {
			Node node=(Node)o;
			if (node.getImpl() instanceof NormalTask task){
				task.setDirty(false);
				task.setLastSavedStart(task.getStart());
				task.setLastSavedFinish(task.getEnd());
				for (Association association : task.getAssignments())
					((Assignment) association).setDirty(false);

				for (Association association : task.getDependencyList(true))
					((Dependency) association).setDirty(false);

				Node parent=(Node)node.getParent();
				if (parent==null||parent.isRoot()) task.setLastSavedParentId(-1L);
				else task.setLastSavedParentId(((Task)parent.getImpl()).getUniqueId());
				task.setLastSavedPosistion(parent.getIndex(node));
			}
		});


		if (!justSaved && repaired != null) {
			for (Task repairedTask : repaired) {
				NormalTask task = (NormalTask) repairedTask;
				task.setTaskAssignementAndPredsDirty();
			}
		    repaired = null;
		}
	}
	void addRepaired(Task t) {
		if (repaired == null)
			repaired = new LinkedList<>();
		repaired.add(t);
	}
	public void markAllTasksAsNeedingRecalculation(boolean invalidateSchedules) {
		int nextStateCount = getCalculationStateCount()+1;
		for (Object obj : tasks) {
			Task task = (Task) obj;
			task.setCalculationStateCount(nextStateCount);
			if (invalidateSchedules)
				task.invalidateSchedules();
		}
		getSchedulingAlgorithm().initEarliestAndLatest();
	}
	public void setAllChildrenDirty(boolean dirty) { // used when changing field dirties all tasks
		for (Object obj : tasks) {
			((Task) obj).setDirty(dirty);
		}
	}
	public void setAllDirty() {
		setDirty(true);
		setGroupDirty(true);
		for (Object obj : tasks) {
			NormalTask task = (NormalTask) obj;
			task.setDirty(false);
			for (Object aObj : task.getAssignments())
				((Assignment) aObj).setDirty(true);
			for (Object dObj : task.getDependencyList(true))
				((Dependency) dObj).setDirty(true);
		}
	}

	ConstraintType.Kind getDefaultConstraintTypeKind(){
		return isForward() ? ConstraintType.Kind.ASAP : ConstraintType.Kind.ALAP;
	}
	public WorkCalendar getDefaultCalendar() {
		return getWorkCalendar();
	}

	public void setWork(long work, FieldContext fieldContext) {
	}
	public void setRemainingWork(long work, FieldContext fieldContext) {
	}
	public void setActualWork(long work, FieldContext fieldContext) {
	}
    public SchedulingAlgorithm getSchedulingAlgorithm() {
        return schedulingAlgorithm;
    }
	public void setSchedulingAlgorithm(SchedulingAlgorithm schedulingAlgorithm) {
		if (this.schedulingAlgorithm != null) {
			removeObjectListener(this.schedulingAlgorithm);
			getMultipleTransactionManager().removeListener(this.schedulingAlgorithm);
		}
		this.schedulingAlgorithm = schedulingAlgorithm;
		addObjectListener(schedulingAlgorithm);
		getMultipleTransactionManager().addListener(schedulingAlgorithm);
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

	public void setFixedCost(double fixedCost, FieldContext fieldContext) {
	}

	public boolean isReadOnlyFixedCost(FieldContext fieldContext) {
		return true;
	}

		public boolean isLabor() {
			return true;
		}

	public final long getStartDate() {
		return getStart();
	}

	public final void setStartDate(long start) {
		start = getEffectiveWorkCalendar().adjustInsideCalendar(start,false);
		long newStart = start;
		runAtomicModelCommand(markApplied -> {
			Object projectState = backupDetail();
			List<TaskScheduleState> taskStates = captureTaskScheduleStates();
			try {
				markApplied.run();
				setStart(newStart);
				getSchedulingAlgorithm().setStartConstraint(newStart);
				// Project Information edits recalculate immediately after changing the boundary.
				recalculate();
				return null;
			} catch (RuntimeException | Error failure) {
				restoreScheduleState(projectState, taskStates, failure);
				throw failure;
			}
		}, ignored -> true, value -> value);
	}

	public final boolean isReadOnlyStartDate(FieldContext fieldContext) {
		return getSchedulingAlgorithm() == null || !getSchedulingAlgorithm().isForward();
	}

	public final long getFinishDate() {
		return getEnd();
	}
	public void setFinishDate(long finish) {
		finish = getEffectiveWorkCalendar().adjustInsideCalendar(finish,true);
		long newFinish = finish;
		runAtomicModelCommand(markApplied -> {
			Object projectState = backupDetail();
			List<TaskScheduleState> taskStates = captureTaskScheduleStates();
			try {
				markApplied.run();
				setEnd(newFinish);
				getSchedulingAlgorithm().setEndConstraint(newFinish);
				recalculate();
				return null;
			} catch (RuntimeException | Error failure) {
				restoreScheduleState(projectState, taskStates, failure);
				throw failure;
			}
		}, ignored -> true, value -> value);
	}

	private List<TaskScheduleState> captureTaskScheduleStates() {
		List<TaskScheduleState> states = new ArrayList<>(tasks.size());
		for (Task task : tasks) {
			TaskSchedule schedule = task.getCurrentSchedule();
			states.add(new TaskScheduleState(task, task.backupDetail(), task.isDirty(),
				schedule.getStart(), schedule.getFinish(), schedule.isForward()));
		}
		return states;
	}

	private void restoreScheduleState(Object projectState, List<TaskScheduleState> taskStates, Throwable failure) {
		try {
			restoreDetail(this, projectState, false);
			for (TaskScheduleState state : taskStates) {
				state.task().restoreDetail(this, state.detail(), false);
				TaskSchedule schedule = state.task().getCurrentSchedule();
				schedule.setForward(state.forward());
				schedule.setStart(state.start());
				schedule.setFinish(state.finish());
				state.task().setDirty(state.dirty());
			}
		} catch (RuntimeException | Error rollbackFailure) {
			if (rollbackFailure != failure) failure.addSuppressed(rollbackFailure);
		}
	}

	private record TaskScheduleState(Task task, Object detail, boolean dirty, long start, long finish, boolean forward) { }

	public boolean isReadOnlyFinishDate(FieldContext fieldContext) {
		return getSchedulingAlgorithm() == null || getSchedulingAlgorithm().isForward();
	}
	public final long getCurrentDate() {
		return currentDate;
	}
	public final void setCurrentDate(long currentDate) {
		this.currentDate = currentDate;
	}

	public long getBaselineStart(int numBaseline) {
		return baselineFacade.getBaselineStart(numBaseline);
	}

	public long getBaselineFinish(int numBaseline) {
		return baselineFacade.getBaselineFinish(numBaseline);
	}

	public long getBaselineDuration(int numBaseline) {
		return baselineFacade.getBaselineDuration(numBaseline);
	}

	public long getActualStart() {
		return taskAggregationFacade.getActualStart();
	}

	public void setActualStart(long actualStart) {
	}

	public long getActualFinish() {
		return taskAggregationFacade.getActualFinish();
	}

	public long getStop() {
		return taskAggregationFacade.getStop();
	}

	public long getEarliestStop() {
		return taskAggregationFacade.getEarliestStop();
	}

	public void setActualFinish(long actualFinish) {
	}

	public long getActualDuration() {
		long stop = getStop();
		if (stop == 0)
			return 0;
		return getEffectiveWorkCalendar().compare(stop,getStartDate(),false);
	}

	public void setActualDuration(long actualDuration) {
	}

	public long getRemainingDuration() {
		long stop = getStop();
		if (stop == 0)
			stop = getStartDate();
		return getEffectiveWorkCalendar().compare(getFinishDate(),stop,false);
	}

	public void setRemainingDuration(long remainingDuration) {
	}
	public double getPercentComplete() {
		return taskAggregationFacade.getPercentComplete();
	}
	public void setPercentComplete(double percentComplete) {
	}

	public void setDuration(long duration) {
		scheduleFacade.setDuration(duration);
	}
	public void setDuration(long duration, FieldContext fieldContext) {
		if (FieldContext.isTaskSheetUpdate(fieldContext)) {
			TaskSheetScheduleWorkflow.applyProjectDuration(this, duration);
			return;
		}
		setDuration(duration);
	}
	public long getElapsedDuration() {
		return Math.round(getEffectiveWorkCalendar().compare(getEnd(), getStart(),true) * CalendarOption.getInstance().getFractionOfDayThatIsWorking());
	}

	public long getDependencyStart() {
		return 0;
	}

	public void setDependencyStart(long dependencyStart) {
	}

	public long getResume() {
		return 0;
	}

	public void setResume(long resume) {
	}

	public void setStop(long stop) {
	}

	public void clearDuration() {
	}

	public void moveRemainingToDate(long date) {
	}

	public void moveInterval(Object eventSource, long start, long end, ScheduleInterval oldInterval, boolean isChild) {
		scheduleFacade.moveInterval(eventSource, start, end, oldInterval, isChild);
	}
	public void split(Object eventSource, long from, long to) {
	}
	public final boolean isDirty() {
		return isDirty;
	}
	public final void setDirty(boolean dirty) {
		this.isDirty = dirty;
		if (dirty)
			setGroupDirty(true);
	}

	public final boolean isGroupDirty() {
		return isGroupDirty;
	}
	public final void setGroupDirty(boolean isGroupDirty) {
		boolean old=this.isGroupDirty;
		this.isGroupDirty = isGroupDirty;
		if (old!=isGroupDirty){
			fireGroupDirtyChanged(this, old);
		}
	}

	protected transient DataFactoryUndoController undoController;
	public DataFactoryUndoController getUndoController() {
		return undoController;
	}


	public void setUndoController(DataFactoryUndoController undoController) {
		this.undoController = undoController;
		if (undoController!=null) undoController.setDataFactory(this);
	}

	public static Predicate<Object> instanceofPredicate() {
		return Project.class::isInstance;
	}


	public void addPastedTask(Task task) {
		getSchedulingAlgorithm().addObject(task);
	}
	public Document invalidateCalendar() {
		markAllTasksAsNeedingRecalculation(false);
		return this;
	}
	@Override
	public Document getCalendarOwnerDocument() {
		return this;
	}

	public WorkCalendar getBaseCalendar() {
		return getWorkCalendar();
	}

	public void setBaseCalendar(WorkCalendar baseCalendar) throws CircularDependencyException {
		setWorkCalendar(baseCalendar);
	}

	public boolean fieldHideBaseCalendar(FieldContext fieldContext) {
		return false;
	}

	public boolean isJustModified(){
		return true;
	}

	public void setComplete(boolean complete) {
		ScheduleUtil.setComplete(this,complete);
	}
	public boolean isComplete() {
		return getPercentComplete() == 1.0D;
	}


	protected boolean master=false;
	protected boolean resourcePoolProject;
	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
	}

	public boolean isResourcePoolProject() {
		return resourcePoolProject;
	}

	public void setResourcePoolProject(boolean resourcePoolProject) {
		this.resourcePoolProject = resourcePoolProject;
	}

	public boolean isLocal() {
		return identityFacade.isLocal();
	}

	public void setLocal(boolean local) {
		identityFacade.setLocal(local);
	}

	public boolean isSavable() {
		return Environment.isProjectLibre() || (!isLocal() && !isReadOnly());
	}


	protected transient boolean temporaryLocal;

	public boolean isTemporaryLocal() {
		return temporaryLocal;
	}

	public void setTemporaryLocal(boolean temporaryLocal) {
		this.temporaryLocal = temporaryLocal;
	}

	public boolean isLockable() {
		return !(temporaryLocal||isLocal());
	}

	public boolean applyTimesheet(Collection<?> fieldArray) {
		return applyTimesheet(fieldArray,System.currentTimeMillis());
	}
	public boolean applyTimesheet(Collection<?> fieldArray, long timesheetUpdateDate) {
		return TimesheetHelper.applyTimesheet(getTaskList(),fieldArray,timesheetUpdateDate);
	}
	public long getLastTimesheetUpdate() {
		return TimesheetHelper.getLastTimesheetUpdate(getTaskList());
	}

	public boolean isPendingTimesheetUpdate() {
		return TimesheetHelper.isPendingTimesheetUpdate(getTaskList());
	}

	public int getTimesheetStatus() {
		return TimesheetHelper.getTimesheetStatus(getTaskList());
	}
	public TimesheetStatus.Kind getTimesheetStatusKind() {
		return TimesheetHelper.getTimesheetStatusKind(getTaskList());
	}
	public String getTimesheetStatusName() {
		return TimesheetHelper.getStatusName(getTimesheetStatusKind());
	}

	public void rollbackUnvalidated(NodeModel nodeModel, Object object) {
	}

	public void initOutline(NodeModel nodeModel){}

	public final long getCompletedThrough	() {
		long start = getStart();
		if (start == 0)
			return 0;
		long actualDuration = DateTime.closestDate(getDuration() * getPercentComplete());
		return getEffectiveWorkCalendar().add(start,actualDuration,true);
	}
	public final void setCompletedThrough(long completedThrough) {
	}

	public final boolean isOpenedAsSubproject() {
		return subprojectFacade.isOpenedAsSubproject();
	}

	public final void setOpenedAsSubproject(boolean openedAsSubproject) {
		subprojectFacade.setOpenedAsSubproject(openedAsSubproject);
	}

	private transient ExternalTaskManager externalTaskManager = null;
	private ExternalTaskManager getExternalTaskManager() {
		if (externalTaskManager == null)
			externalTaskManager = new ExternalTaskManager();
		return externalTaskManager;
	}
	int beginExternalTaskResolutionTransaction() { return getExternalTaskManager().beginResolutionTransaction(); }
	void commitExternalTaskResolutionTransaction(int checkpoint) { getExternalTaskManager().commitResolutionTransaction(checkpoint); }
	void rollbackExternalTaskResolutionTransaction(int checkpoint) { getExternalTaskManager().rollbackResolutionTransaction(checkpoint); }
	int retainedExternalTaskResolutionCount() { return getExternalTaskManager().retainedResolutionCount(); }
	public void addExternalTask(Task task) {
		getExternalTaskManager().add(task);
		task.markTaskAsNeedingRecalculation();
	}
	public void handleExternalTasks(Project project, boolean opening, boolean saving) {
		getExternalTaskManager().handleExternalTasks(project,opening, saving);
		project.getExternalTaskManager().handleExternalTasks(this, opening,saving);

	}
	public boolean needsSaving() {
		return (isSavable() && isGroupDirty());
	}
	public long getEarliestStartingTask() {
		return subprojectFacade.getEarliestStartingTask();
	}
	public long getEarliestStartingTaskOrStart() {
		return subprojectFacade.getEarliestStartingTaskOrStart();
	}
	public long getLatestFinishingTask() {
		return subprojectFacade.getLatestFinishingTask();
	}

	public final Map<String, Object> getExtraFields() {
		if (extraFields == null)
			// LinkedHashMap preserves insertion order for stable POD serialization (issue #227)
			extraFields = new LinkedHashMap<>();
		return extraFields;
	}

	public final void setExtraFields(Map<String, Object> extraFields) {
		this.extraFields = extraFields;
	}

	public final Map<String, String> getCustomReportPresets() {
		if (customReportPresets == null)
			customReportPresets = new LinkedHashMap<>();
		return customReportPresets;
	}

	public GanttBarFormatOverrides getGanttBarFormatOverrides() {
		if (ganttBarFormatOverrides == null)
			ganttBarFormatOverrides = new GanttBarFormatOverrides();
		return ganttBarFormatOverrides;
	}

	public void setGanttBarFormatOverrides(GanttBarFormatOverrides ganttBarFormatOverrides) {
		this.ganttBarFormatOverrides = ganttBarFormatOverrides == null
				? new GanttBarFormatOverrides()
				: ganttBarFormatOverrides;
	}

	public final Hyperlink getDocumentFolderUrl() {
		return documentFolderUrl;
	}

	public final void setDocumentFolderUrl(Hyperlink documentFolderUrl) {
		this.documentFolderUrl = documentFolderUrl;
	}

	public final boolean isReadOnly() {
		return readOnly;
	}

	public final void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public final Collection<Object> getReferringSubprojectTasks() {
		return subprojectFacade.getReferringSubprojectTasks();
	}

	public final void setReferringSubprojectTasks(Collection<?> referringSubprojectTasks) {
		subprojectFacade.setReferringSubprojectTasks(referringSubprojectTasks);
	}

	public final Task getContainingSubprojectTask() {
		return subprojectFacade.getContainingSubprojectTask();
	}

	public final void setContainingSubprojectTask(Task subprojectTask) {
		subprojectFacade.setContainingSubprojectTask(subprojectTask);
	}

	public long getFinishOffset() {
		return EarnedValueCalculator.getInstance().getFinishOffset(this);
	}

	public long getStartOffset() {
		return EarnedValueCalculator.getInstance().getStartOffset(this);
	}

	public double getRisk() {
		return risk;
	}

	public void setRisk(double risk) {
		this.risk = risk;
	}

	/** @deprecated use {@link #getProjectTypeKind()} for typed access. */
	@Deprecated
	public int getProjectType() {
		return projectType;
	}

	/** @deprecated use {@link #setProjectTypeKind(ProjectType.Kind)} for typed access. */
	@Deprecated
	public void setProjectType(int projectType) {
		this.projectType = projectType;
	}

	/** @deprecated use {@link #getExpenseKind()} for domain logic. */
	@Deprecated
	public int getExpenseType() {
		return expenseType;
	}

	/** @deprecated use {@link #setExpenseKind(ExpenseType.Kind)} for domain logic. */
	@Deprecated
	public void setExpenseType(int budgetType) {
		this.expenseType = budgetType;
	}

	@Override
	public ExpenseType.Kind getEffectiveExpenseKind() {
		return getExpenseKind();
	}

	/** @deprecated use {@link #getEffectiveExpenseKind()} for typed access. */
	@Deprecated
	public int getEffectiveExpenseType() {
		return getExpenseType();
	}

	public String getDivision() {
		return division;
	}

	public void setDivision(String division) {
		this.division = division;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	/** @deprecated use {@link #getProjectStatusKind()} for typed access. */
	@Deprecated
	public int getProjectStatus() {
		return projectStatus;
	}

	/** @deprecated use {@link #setProjectStatusKind(ProjectStatus.Kind)} for typed access. */
	@Deprecated
	public void setProjectStatus(int projectStatus) {
		this.projectStatus = projectStatus;
	}
	public ImageLink getBudgetStatusIndicator() {
		return EarnedValueCalculator.getInstance().getBudgetStatusIndicator(getCpi(null));
	}

	public ImageLink getScheduleStatusIndicator() {
		return EarnedValueCalculator.getInstance().getScheduleStatusIndicator(getSpi(null));
	}

	public Object backupDetail() {
		return scheduleFacade.backupDetail();
	}

	public void restoreDetail(Object source,Object detail,boolean isChild) {
		scheduleFacade.restoreDetail(source, detail, isChild);
	}

	public boolean containsAssignments(){return true;}

	public static final class ProjectBackup {
		private final long start;
		private final long end;

		private ProjectBackup(long start, long end) {
			this.start = start;
			this.end = end;
		}

		public long getStart() {
			return start;
		}

		public long getEnd() {
			return end;
		}
	}


	public void beginUndoUpdate(){
		if (undoController!=null) undoController.beginUpdate();
	}
	public void endUndoUpdate(){
		if (undoController!=null) undoController.endUpdate();
	}

	public boolean renumber(boolean localOnly){
		boolean r=false;
		long uniqueId=getUniqueId();
        for (Iterator<Task> i=getTaskOutlineIterator();i.hasNext();){
            NormalTask task=(NormalTask)i.next(); //ResourceImpl to have the EnterpriseResource link
            if (task.getProjectId() != uniqueId) // skip if in another project
            	continue;

            r|=task.renumber(localOnly);
        }
		r|=identityFacade.renumber(localOnly);
		if (!r) return false;
		uniqueId=getUniqueId();
        for (Iterator<Task> i=getTaskOutlineIterator();i.hasNext();){
            NormalTask task=(NormalTask)i.next();
            task.setProjectId(uniqueId);
        }
        return true;
	}

	protected transient int accessControlPolicy;
	public int getAccessControlPolicy() {
		return accessControlPolicy;
	}

	public void setAccessControlPolicy(int accessControlPolicy) {
		this.accessControlPolicy = accessControlPolicy;
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

/**
 * Automatically link all siblings at all levels
 * A condition can be applied. The condition tests the task and sees whether it can be a successor task ornot
 * @param parent - should be null if whole project
 */
	public void linkAllSiblings(Node parent, Predicate<Object> canBeSuccessorCondition, Object eventSource) {
		List<Node> children = getTaskModel().getChildren(parent);

		if (children == null)
			return;
		try {
			DependencyService.getInstance().connect(NodeList.nodeListToImplList(children, NotAssignmentFilter.getInstance()),eventSource,canBeSuccessorCondition);
		} catch (InvalidAssociationException e) {
			ErrorLogger.log("Invalid association while linking siblings", e);
		}
		for (Node n : children) // recursively do children
			linkAllSiblings(n,canBeSuccessorCondition,eventSource);

	}
	public List<NormalTask> getRootTasks() {
		List<Node> children = getTaskModel().getChildren(null);
		List<Object> impls = NodeList.nodeListToImplList(children);
		List<NormalTask> tasks = new ArrayList<>(impls.size());
		for (Object impl : impls) {
			tasks.add((NormalTask) impl);
		}
		return tasks;
	}
	public boolean hasSummaryEnvelope() {
		return summaryEnvelope != null && summaryEnvelope.hasAnyManualValue();
	}
	public SummaryEnvelope getSummaryEnvelope() {
		if (summaryEnvelope == null)
			summaryEnvelope = new SummaryEnvelope();
		return summaryEnvelope;
	}
	public void clearSummaryEnvelopePart(SummaryEnvelopePart part) {
		getSummaryEnvelope().clearPart(part);
	}
	public RollupSpan calculateRollupSpan() {
		return scheduleFacade.calculateRollupSpan();
	}

	public List<Resource> getRootResources() {
		List<Node> children = getResourceModel().getChildren(null);
		List<Object> impls = NodeList.nodeListToImplList(children);
		List<Resource> resources = new ArrayList<>(impls.size());
		for (Object impl : impls) {
			resources.add((Resource) impl);
		}
		return resources;
	}

	public boolean isCriticalPathJustChanged() {
		return ((CriticalPath)getSchedulingAlgorithm()).isCriticalPathJustChanged();
	}

	public int getBenefit() {
		return benefit;
	}

	public void setBenefit(int benefit) {
		this.benefit = benefit;
	}

	public double getNetPresentValue() {
		return netPresentValue;
	}

	public void setNetPresentValue(double netPresentValue) {
		this.netPresentValue = netPresentValue;
	}

	protected transient String fileName;
	protected transient CollaborationSession collaborationSession;
	protected transient WorkspaceSetting collaborationWorkspace;
	public String getFileName(){
		return fileName;
	}
	public String getGuessedFileName(){
		if (fileName!=null) return fileName;
		String name=getName();
		if (name==null) return null;
		return getName()+"."+FileHelper.getFileExtension(fileType);
	}
	public void setFileName(String fileName){
		this.fileName=fileName;
		if (fileName!=null){
			setFileType(FileHelper.getFileType(fileName));
			refreshSubprojectReferenceMetadata();
		}
	}

	/** Rebase portable child paths once a formerly untitled master receives its save target. */
	private void refreshSubprojectReferenceMetadata() {
		if (getTaskOutline() == null)
			return;
		for (Iterator<?> tasks = getTaskOutlineIterator(); tasks.hasNext();) {
			Object value = tasks.next();
			if (value instanceof SubProj reference)
				SubprojectReferenceMetadata.record(this, reference, reference.getSubproject());
		}
	}
	public String getTitle(){
		return formatTitle(getName(), fileName);
	}

	static String formatTitle(String name, String fileName) {
		if (name == null || name.isBlank()) return fileName == null ? "" : fileName;
		if (fileName == null || fileName.isBlank()) return name;
		return name + " - " + fileName;
	}

	protected transient int fileType=FileHelper.MPO_FILE_TYPE;

	public int getFileType() {
		return fileType;
	}

	public void setFileType(int fileType) {
		this.fileType = fileType;
	}
	public CollaborationSession getCollaborationSession() {
		return collaborationSession;
	}
	public void setCollaborationSession(CollaborationSession collaborationSession) {
		this.collaborationSession = collaborationSession;
	}
	public WorkspaceSetting getCollaborationWorkspace() {
		return collaborationWorkspace;
	}
	public void setCollaborationWorkspace(WorkspaceSetting collaborationWorkspace) {
		this.collaborationWorkspace = collaborationWorkspace;
	}

	public void setBoundsAfterReadProject() {
		getSchedulingAlgorithm().setEarliestAndLatest(getStart(), getEnd());
		fireScheduleChanged(this,ScheduleEvent.SCHEDULE);
	}

	public void setAllTasksInSubproject(boolean b, Project masterProject) {
		if (masterProject == null)
			return;
		for (Task task : new ArrayList<>(tasks)) {
			task.setInSubproject(b);
			if (b) {
				task.setProject(masterProject);
				if (!masterProject.getTasks().contains(task))
					masterProject.add(task);
			} else {
				masterProject.getTasks().remove(task);
				task.setProject(this);
			}
		}
	}

	public void setAllNodesInSubproject(boolean b) {
		for (int outline = 0; outline < Settings.numHierarchies(); outline++) {
			NodeModel nodeModel = getTaskOutline(outline);
			if (nodeModel == null)
				continue;
			for (Iterator<Node> iterator = nodeModel.iterator(); iterator.hasNext();)
				iterator.next().setSubprojectLevel(b ? 1 : 0);
		}
	}

	public SubprojectHandler getSubprojectHandler() {
		return subprojectHandler;
	}

	public long getReferringSubprojectTaskDependencyDate() {
		return subprojectHandler.getReferringSubprojectTaskDependencyDate();
	}

	public String getSubprojectOf() {
		return subprojectFacade.getSubprojectOf();
	}
	public void resetRoles(boolean publicRoles) {
		try {
			Class.forName(Messages.getMetaString("ProjectRoleManager")).getDeclaredMethod("resetRoles", Project.class, Boolean.class).invoke(null, this, publicRoles);
		} catch (Exception e) {
			ErrorLogger.log("ProjectRoleManager initialization failed", e);
			logger.warning("ProjectRoleManager not valid in meta.properties");
			System.exit(-1);
		}
	}

	public class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = 6909144693873463556L;
		WorkspaceSetting spreadsheetWorkspace;
		HashMap<String, String> fieldAliasMap = new HashMap<>();
		PrintSettings printSettings;
		CalendarOption calendarOption;

	}
	public transient SpreadSheetFieldArray fieldArray = null;
	public transient PrintSettings printSettings = null;
	public transient PrintSettings tmpSettings = null;
	public transient CalendarOption calendarOption = null;
	// Persisted native projects carry these defaults in their MSPDI Project header.
	// Keep the object transient so the frozen POD serialization descriptor is unchanged.
	private transient DurationDefaults durationDefaults;

	public DurationDefaults getDurationDefaults() {
		if (durationDefaults == null)
			durationDefaults = DurationDefaults.PRODUCT_DEFAULTS;
		return durationDefaults;
	}

	public void setDurationDefaults(DurationDefaults durationDefaults) {
		this.durationDefaults = java.util.Objects.requireNonNull(durationDefaults, "durationDefaults");
	}

	public PrintSettings getPrintSettings(int context) {
		return context==SavableToWorkspace.PERSIST?printSettings:tmpSettings;
	}

	public void setPrintSettings(PrintSettings printSettings) {
	this.printSettings = printSettings;
		setGroupDirty(true);
	}
	public CalendarOption getCalendarOption() {
		return calendarOption;
	}

	public void setCalendarOption(CalendarOption calendarOption) {
		this.calendarOption = calendarOption;
		if (calendarOption != null) {
			DurationDefaults current = getDurationDefaults();
			durationDefaults = new DurationDefaults(calendarOption.getHoursPerDay() * 60.0D,
				calendarOption.getHoursPerWeek() * 60.0D, calendarOption.getDaysPerMonth(),
				current.durationEnteredIn(), current.workUnit());
		}
		setGroupDirty(true);
	}

	public PrintSettings getTmpSettings() {
		return tmpSettings;
	}

	public void setTmpSettings(PrintSettings tmpSettings) {
		this.tmpSettings = tmpSettings;
	}

	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		if (ws.spreadsheetWorkspace != null) {
			fieldArray=SpreadSheetFieldArray.restore(ws.spreadsheetWorkspace, getName(), context);
		}
		if (ws.printSettings!=null){
			printSettings=ws.printSettings;
			if (printSettings!=null){
				printSettings.init();
				tmpSettings=(PrintSettings)printSettings.clone();
			}

		}

		if (ws.fieldAliasMap != null)
			FieldDictionary.setAliasMap(ws.fieldAliasMap);
		if (ws.calendarOption != null) {
			calendarOption = ws.calendarOption;
			CalendarOption.setInstance(calendarOption);
		}

	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		if (Environment.isClientSide()){
			if (fieldArray != null)
				ws.spreadsheetWorkspace = fieldArray.createWorkspace(context);
			if (printSettings!=null){
				ws.printSettings=printSettings;
				printSettings.updateWorkspace();
			}
			if (calendarOption != null)
				ws.calendarOption = calendarOption;
		}
		ws.fieldAliasMap = FieldDictionary.getAliasMap();
		return ws;
	}

	public SpreadSheetFieldArray getFieldArray() {
		return fieldArray;
	}

	public void setFieldArray(SpreadSheetFieldArray fieldArray) {
		this.fieldArray = fieldArray;
	}

	public void forTasks(Consumer<Object> c){
		for (Iterator<Task> i=getTaskOutlineIterator();i.hasNext();){
			c.accept(i.next());
		}
	}

	private class TaskIterator implements Iterator<Task>{
		private Iterator<?> iterator;
		private Task next=null;
		private Task nextElement(){
			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (element instanceof Node node && node.getImpl() instanceof Task task) {
					return next = task;
				}
			}
			return next = null;
		}

		TaskIterator(){
			iterator=getTaskOutline().iterator(getTaskOutlineRoot());
			nextElement();
		}
		public boolean hasNext() {
			return next!=null;
		}
		public Task next() {
			if (next == null) throw new NoSuchElementException();
			Task n=next;
			nextElement();
			return n;
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	public Iterator<Task> getTaskOutlineIterator(){
		return new TaskIterator();
	}

	public Node getTaskOutlineRoot(){
    	if (isOpenedAsSubproject()) { // when doing subprojects, we must treat the suproject parent as the root node
    		if (tasks.size() > 0) {
	    		NormalTask t = (NormalTask) tasks.get(0); // get any task in the project
	   			return t.getEnclosingSubprojectNode(); // this will fetch the enclosing subproject task which will be the hierarchy root
    		}
    	}
		return null;
	}

	private transient TreeMap<DistributionData,DistributionData> distributionMap;
	private transient TreeMap<DistributionData,DistributionData> newDistributionMap;
	public TreeMap<DistributionData,DistributionData> getDistributionMap() {
		return distributionMap;
	}

	public void setDistributionMap(TreeMap<DistributionData,DistributionData> distributionMap) {
		this.distributionMap = distributionMap;
	}

	public TreeMap<DistributionData, DistributionData> getNewDistributionMap() {
		return newDistributionMap;
	}

	public void setNewDistributionMap(TreeMap<DistributionData, DistributionData> newDistributionMap) {
		this.newDistributionMap = newDistributionMap;
	}

    public void updateDistributionMap(){
    	long t=System.currentTimeMillis();
		List<DistributionData> dist=(new DistributionConverter()).createDistributionData(this,false);
    	if (dist==null) return;
		TreeMap<DistributionData, DistributionData> distMap=new TreeMap<>(new DistributionComparator());
    	setDistributionMap(distMap);
    	long projectId=getUniqueId();
		for (DistributionData d : dist){
			if (d.getProjectId()==projectId) distMap.put(d,d);
	    }
	    logger.info("DistributionMap: " + dist.size() + " elements, updated in " + (System.currentTimeMillis() - t) + " ms");
    }
    public void validateNewDistributionMap(){
    	if (distributionMap!=null) distributionMap.clear();
    	setDistributionMap(getNewDistributionMap());
    	setNewDistributionMap(null);
    	setForceNonIncrementalDistributions(false);
    }

    protected transient boolean forceNonIncremental;

	public boolean isForceNonIncremental() {
		return forceNonIncremental;
	}

	public void setForceNonIncremental(boolean forceNonIncremental) {
		this.forceNonIncremental = forceNonIncremental;
	}

	protected transient boolean forceNonIncrementalDistributions;

	public boolean isForceNonIncrementalDistributions() {
		return forceNonIncrementalDistributions;
	}

	public void setForceNonIncrementalDistributions(
			boolean forceNonIncrementalDistributions) {
		this.forceNonIncrementalDistributions = forceNonIncrementalDistributions;
	}

	public static final float CURRENT_VERSION=1.2f;
	protected float version=CURRENT_VERSION;

	public float getVersion() {
		return version;
	}

	public void setVersion(float version) {
		this.version = version;
	}

	public void recalculate() {
		markAllTasksAsNeedingRecalculation(true);
		schedulingAlgorithm.reset();
		schedulingAlgorithm.calculate(true);
	}

	public ObjectSelectionEventManager getObjectSelectionEventManager() {
		return objectSelectionEventManager;
	}

	private final class IdentityFacade implements Serializable {
		private static final long serialVersionUID = 1L;
		long getId() {
			return hasKey.getId();
		}

		void setId(long id) {
			hasKey.setId(id);
		}

		Date getCreated() {
			return hasKey.getCreated();
		}

		void setCreated(Date created) {
			hasKey.setCreated(created);
		}

		String getName() {
			return hasKey.getName();
		}

		String getName(FieldContext context) {
			return hasKey.getName(context);
		}

		void setName(String name) {
			hasKey.setName(name);
		}

		boolean isLocal() {
			return hasKey.isLocal();
		}

		void setLocal(boolean local) {
			hasKey.setLocal(local);
		}

		long getUniqueId() {
			return hasKey.getUniqueId();
		}

		void setUniqueId(long id) {
			hasKey.setUniqueId(id);
		}

		boolean renumber(boolean localOnly) {
			return hasKey.renumber(localOnly);
		}
	}

	private final class BaselineFacade implements Serializable {
		private static final long serialVersionUID = 1L;
		long getBaselineStart(int numBaseline) {
			NormalTask task;
			long result = Long.MAX_VALUE;
			long val;
			for (Task candidate : tasks) {
				task = (NormalTask) candidate;
				val = task.getBaselineStart(numBaseline);
				if (val != 0 && val < result) {
					result = val;
				}
			}
			if (result == Long.MAX_VALUE) {
				result = 0;
			}
			return result;
		}

		long getBaselineFinish(int numBaseline) {
			NormalTask task;
			long result = 0;
			long val;
			for (Task candidate : tasks) {
				task = (NormalTask) candidate;
				val = task.getBaselineFinish(numBaseline);
				if (val > result) {
					result = val;
				}
			}
			return result;
		}

		long getBaselineDuration(int numBaseline) {
			return getEffectiveWorkCalendar().compare(getBaselineFinish(numBaseline), getBaselineStart(numBaseline), false);
		}
	}

	private final class SubprojectFacade implements Serializable {
		private static final long serialVersionUID = 1L;
		boolean isOpenedAsSubproject() {
			return openedAsSubproject;
		}

		void setOpenedAsSubproject(boolean value) {
			openedAsSubproject = value;
		}

		long getEarliestStartingTask() {
			return earliestStartingTask;
		}

		long getEarliestStartingTaskOrStart() {
			if (isOpenedAsSubproject()) {
				return earliestStartingTask;
			}
			long early = ((CriticalPath) getSchedulingAlgorithm()).getEarliestStart();
			if (early == 0) {
				early = getStart();
				logger.info("0 earliest start for project. Forward = " + isForward() + " using proj start " + new Date(early));
			}
			return early;
		}

		long getLatestFinishingTask() {
			if (isOpenedAsSubproject()) {
				return latestFinishingTask;
			}
			long late = ((CriticalPath) getSchedulingAlgorithm()).getLatestFinish();
			if (late == 0 || late == Long.MAX_VALUE) {
				late = getEnd();
				logger.info("" + late + " latest finish for project. Forward = " + isForward() + " using proj end " + new Date(end));
			}
			return late;
		}

		Collection<Object> getReferringSubprojectTasks() {
			return subprojectHandler.getReferringSubprojectTasks();
		}

		void setReferringSubprojectTasks(Collection<?> referringSubprojectTasks) {
			subprojectHandler.setReferringSubprojectTasks(referringSubprojectTasks);
		}

		Task getContainingSubprojectTask() {
			return subprojectHandler.getContainingSubprojectTask();
		}

		void setContainingSubprojectTask(Task subprojectTask) {
			subprojectHandler.setContainingSubprojectTask(subprojectTask);
		}

		String getSubprojectOf() {
			return subprojectHandler.getSubprojectOf();
		}
	}

	private final class ScheduleFacade implements Serializable {
		private static final long serialVersionUID = 1L;
		void setStart(long start) {
			Project.this.start = start;
		}

		void setEnd(long end) {
			Project.this.end = end;
		}

		void setDuration(long duration) {
		}

		void moveInterval(Object eventSource, long start, long end, ScheduleInterval oldInterval, boolean isChild) {
			long newStart = start;
			long newEnd = end;

			if (newEnd < newStart) {
				if (start != oldInterval.getStart()) {
					newEnd = newStart;
				} else {
					newStart = newEnd;
				}
			}

			boolean startChanged = newStart != getStart();
			boolean endChanged = newEnd != getEnd();
			if (!startChanged && !endChanged) {
				return;
			}

			if (startChanged) {
				setStart(newStart);
				if (getSchedulingAlgorithm() != null) {
					getSchedulingAlgorithm().setStartConstraint(newStart);
				}
			}
			if (endChanged) {
				setEnd(newEnd);
				if (getSchedulingAlgorithm() != null) {
					getSchedulingAlgorithm().setEndConstraint(newEnd);
				}
			}
		}

		Object backupDetail() {
			return new ProjectBackup(start, end);
		}

		void restoreDetail(Object source, Object detail, boolean isChild) {
			ProjectBackup backup = (ProjectBackup) detail;
			setStart(backup.start);
			setEnd(backup.end);
			if (getSchedulingAlgorithm() != null) {
				getSchedulingAlgorithm().setStartConstraint(backup.start);
				getSchedulingAlgorithm().setEndConstraint(backup.end);
			}
		}

		RollupSpan calculateRollupSpan() {
			return RollupSpanCalculator.forProject(Project.this);
		}
	}

	private final class TaskAggregationFacade implements Serializable {
		private static final long serialVersionUID = 1L;
		long getActualStart() {
			long result = Long.MAX_VALUE;
			for (Task task : tasks) {
				long actualStart = task.getActualStart();
				if (actualStart != 0 && actualStart < result) {
					result = actualStart;
				}
			}
			return result == Long.MAX_VALUE ? 0 : result;
		}

		long getActualFinish() {
			long result = 0;
			for (Task task : tasks) {
				long actualFinish = task.getActualFinish();
				if (actualFinish == 0) {
					break;
				}
				if (actualFinish > result) {
					result = actualFinish;
				}
			}
			return result;
		}

		long getStop() {
			long result = 0;
			for (Task task : tasks) {
				long stop = task.getStop();
				if (stop == 0) {
					return 0;
				}
				if (stop > result) {
					result = stop;
				}
			}
			return result;
		}

		long getEarliestStop() {
			long result = Long.MAX_VALUE;
			for (Task task : tasks) {
				long earliestStop = task.getEarliestStop();
				if (earliestStop < result) {
					result = earliestStop;
				}
				if (earliestStop == 0) {
					break;
				}
			}
			return result == Long.MAX_VALUE ? 0 : result;
		}

		double getPercentComplete() {
			long actual = 0L;
			long total = 0L;
			for (Task task : tasks) {
				actual += Duration.millis(task.getActualDuration());
				total += Duration.millis(task.getDuration());
			}
			return total == 0L ? 0D : ((double) actual) / total;
		}
	}

	private final class TaskLifecycleFacade implements Serializable {
		private static final long serialVersionUID = 1L;
		void initializeId(Task task) {
			long id = ++taskIdCounter;
			task.setId(id);
		}

		NormalTask newNormalTaskInstance(boolean userCreated) {
			NormalTask newOne = new NormalTask(Project.this);
			add(newOne);
			initializeId(newOne);
			if (userCreated)
				objectEventManager.fireCreateEvent(Project.this, newOne);
			return newOne;
		}

		NormalTask newStandaloneNormalTaskInstance() {
			NormalTask newOne = new NormalTask(Project.this);
			initializeId(newOne);
			return newOne;
		}

		NormalTask createScriptedTask() {
			NormalTask task = newStandaloneNormalTaskInstance();
			connectTask(task);
			taskOutlines.addToAll(task, null);
			task.markTaskAsNeedingRecalculation();
			updateScheduling(Project.this, task, ObjectEvent.CREATE);
			return task;
		}

		void connectTask(Task task) {
			if (!isOpenedAsSubproject() || !tasks.contains(task))
				add(task);

			if (task.getOwningProject() == null)
				task.setOwningProject(Project.this);
			if (task.getProjectId() == 0) {
				task.setProjectId(getUniqueId());
			}
			Project masterProject = (Project) task.getMasterDocument();
			if (masterProject == Project.this) {
				task.setProject(Project.this);
				if (task.getSuccessorList().size() == 0)
					addEndSentinelDependency(task);
				if (task.getPredecessorList().size() == 0)
					addStartSentinelDependency(task);
			} else {
				masterProject.add(task);
				task.setProject(masterProject);
			}
		}

		Object createUnvalidatedObject(NodeModel nodeModel, Object parent) {
			NormalTask task = newStandaloneNormalTaskInstance();
			task.setWbsParent((Task) parent);
			return task;
		}

		void addUnvalidatedObject(Object object, NodeModel nodeModel, Object parent) {
			if (!(object instanceof NormalTask))
				return;
			NormalTask task = (NormalTask) object;
			task.setWbsParent((Task) parent);
			task.setInSubproject(task.liesInSubproject());
		}

		void validateObject(Object newlyCreated, NodeModel nodeModel, Object eventSource, Object hierarchyInfo, boolean isNew) {
			if (!(newlyCreated instanceof Task))
				return;
			Task newTask = (Task) newlyCreated;
			newTask.setProject((Project) getSchedulingAlgorithm().getMasterDocument());
			newTask.setOwningProject(Project.this);
			connectTask(newTask);
			taskOutlines.addToAll(newlyCreated, nodeModel);
			Task parentTask = newTask.getWbsParentTask();
			Node parentNode = (parentTask == null) ? null : nodeModel.search(newTask.getWbsParentTask());
			Node childNode = nodeModel.search(newTask);
			setDefaultRelationship(parentNode, childNode);
			newTask.markTaskAsNeedingRecalculation();
			updateScheduling(Project.this, newlyCreated, ObjectEvent.CREATE);
		}

		void remove(Object toRemove, NodeModel nodeModel, boolean deep, boolean undo, boolean cleanDependencies) {
			Object eventSource = nodeModel;
			if (toRemove instanceof SubProj && !(toRemove instanceof Task)) {
				Project subproject = ((SubProj) toRemove).getSubproject();
				if (subproject != null)
					ProjectFactory.getInstance().removeProject(subproject, false, false, true);
				objectEventManager.fireDeleteEvent(eventSource, toRemove);
				return;
			}
			if (!(toRemove instanceof Task))
				return;
			Task task = (Task) toRemove;
			Project owningProject = task.getOwningProject();
			if (owningProject != Project.this) {
				owningProject.taskOutlines.removeFromAll(toRemove, null);
				owningProject.tasks.remove(task);
			}
			task.cleanUp(eventSource, deep, undo, cleanDependencies);
			tasks.remove(task);
			taskOutlines.removeFromAll(task, nodeModel);
			if (task instanceof SubProj) {
				Project sub = ((SubProj) task).getSubproject();
				if (sub != null)
					ProjectFactory.getInstance().removeProject(sub, false, false, true);
			}
			objectEventManager.fireDeleteEvent(eventSource, task);
		}
	}


}
