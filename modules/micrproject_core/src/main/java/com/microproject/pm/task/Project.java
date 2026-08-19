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
import java.util.Collection;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.undo.UndoableEditSupport;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import com.microproject.algorithm.ReverseQuery;
import com.microproject.association.InvalidAssociationException;
import com.microproject.configuration.CircularDependencyException;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.FieldDictionary;
import com.microproject.configuration.Settings;
import com.microproject.datatype.Duration;
import com.microproject.datatype.Hyperlink;
import com.microproject.datatype.ImageLink;
import com.microproject.document.Document;
import com.microproject.document.ObjectEvent;
import com.microproject.document.ObjectEventManager;
import com.microproject.document.ObjectSelectionEventManager;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.HasExtraFields;
import com.microproject.functor.IntervalConsumer;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.GanttBarFormatOverrides;
import com.microproject.graphic.configuration.SpreadSheetCategories;
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
import com.microproject.options.TimesheetOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.HasTimeDistributedData;
import com.microproject.pm.assignment.TimeDistributedDataConsolidator;
import com.microproject.pm.assignment.TimeDistributedFields;
import com.microproject.pm.assignment.timesheet.TimesheetHelper;
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
import com.microproject.pm.criticalpath.HasSentinels;
import com.microproject.pm.criticalpath.SchedulingAlgorithm;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.key.HasId;
import com.microproject.pm.key.HasKey;
import com.microproject.pm.key.HasKeyImpl;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
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
import com.microproject.undo.ClearSnapshotEdit;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.SaveSnapshotEdit;
import com.microproject.util.Alert;
import com.microproject.util.DateTime;
import com.microproject.util.Environment;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;
import com.microproject.collaboration.CollaborationSession;
/**
 * Project class
 */
public class Project implements Document, BelongsToDocument, HasKey, HasPriority, MutableHasStartAndEnd, ProjectSpecificFields, HasNotes, HasBaseCalendar, HasCalendar, NodeModelDataFactory, HierarchyListener, HasTimeDistributedData, TimeDistributedFields, EarnedValueValues, EarnedValueFields, DataObject, HasSentinels, BaselineScheduleFields, Schedule,UpdatesFromTimesheet, HasExtraFields, HasExpenseType, SavableToWorkspace {
	static final long serialVersionUID = 17283790404932L;
	private static final Logger logger = Logger.getLogger(Project.class.getName());
	private long statusDate = 0;
	private String manager="";
	private transient HasKeyImpl hasKey;
	private String notes="";
	private transient LinkedList<Task> tasks = new LinkedList<Task>();
	private transient ResourcePool resourcePool = null;
	private transient SchedulingAlgorithm schedulingAlgorithm = null;
	private transient boolean initialized = false;
	private transient ScheduleEventManager scheduleEventManager = new ScheduleEventManager();
	private transient MultipleTransactionManager multipleTransactionManager = new MultipleTransactionManager();
	private transient ObjectEventManager objectEventManager = new ObjectEventManager();
	private transient ObjectSelectionEventManager objectSelectionEventManager = new ObjectSelectionEventManager();

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
	private Map extraFields = null;
	private GanttBarFormatOverrides ganttBarFormatOverrides = new GanttBarFormatOverrides();
	private double risk = 0.0D;
	private double netPresentValue = 0.0D;
	private int benefit = 0;
	transient int projectStatus = ProjectStatus.PLANNING; // exposed in database
	transient int projectType = ProjectType.OTHER; // exposed in database
	transient int expenseType = ExpenseType.NONE;// exposed in database
	transient String group;// exposed in database
	transient String division;// exposed in database

	private transient boolean openedAsSubproject = false;
	private transient Hyperlink documentFolderUrl = null;
	private transient long earliestStartingTask = 0L; // used for subprojects
	private transient long latestFinishingTask = 0L; // used for subprojects
	private static Project dummy = null;


	private transient NodeModel taskModel = null;
	private transient NodeModel resourceModel = null;
	private transient Object taskCache = null;
	private transient Object resourceCache = null;
	private transient List<Task> repaired = null;
	private transient Date creationDate,lastModificationDate;
	private transient IdentityFacade identityFacade = new IdentityFacade();
	/**
	 * Issue #227: created timestamp persisted on the object (not the transient
	 * identityFacade) so it survives load/save round-trips. See com.microproject.pm.task.Task#created.
	 */
	protected Date created = new Date();
	private transient BaselineFacade baselineFacade = new BaselineFacade();
	private transient SubprojectFacade subprojectFacade = new SubprojectFacade();
	private transient ScheduleFacade scheduleFacade = new ScheduleFacade();
	private transient TaskAggregationFacade taskAggregationFacade = new TaskAggregationFacade();
	private transient TaskLifecycleFacade taskLifecycleFacade = new TaskLifecycleFacade();

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


	public Object getResourceCache() {
		return resourceCache;
	}

	public void setResourceCache(Object resourceCache) {
		this.resourceCache = resourceCache;
	}

	public Object getTaskCache() {
		return taskCache;
	}

	public void setTaskCache(Object taskCache) {
		this.taskCache = taskCache;
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
	}


	private Project(ResourcePool resourcePool,DataFactoryUndoController undo) {
		this(resourcePool.isLocal());
		this.resourcePool = resourcePool;
		undoController=undo;
	}

	public void dispose() {
		logger.fine("disposing project " + this);
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
		int count=Settings.numHierarchies();
		for (int i=0;i<count;i++){
			NodeModel model=taskOutlines.getOutline(i);
			if (model==null) continue;
			if (model instanceof AssignmentNodeModel){
				AssignmentNodeModel aModel=(AssignmentNodeModel)model;
				aModel.setContainsLeftObjects(true);
				aModel.setDocument(this);
			}
			model.setUndoController(undoController);
		}
		initializeDefaultOutline();
	}

	public void disconnectOutlines(){
		int count=Settings.numHierarchies();
		for (int i=0;i<count;i++){
			NodeModel model=taskOutlines.getOutline(i);
			if (model instanceof AssignmentNodeModel){
				AssignmentNodeModel aModel=(AssignmentNodeModel)model;
				aModel.setDocument(null); //remove ObjectListener
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
	    	SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					recalculate();
			    	setStart(getEarliestStartingTaskOrStart());
				}});
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
		Node oldParentNode = getTaskModel().search(child.getWbsParentTask());
		if (oldParentNode != null)
			oldParentNode.getChildren().remove(childNode);
		ArrayList temp = new ArrayList();
		temp.add(childNode);
		getTaskModel().move(parentNode, temp, -1,NodeModel.NORMAL);
		setDefaultRelationship(parentNode,childNode);
	}

	public Node createLocalTaskNode(Node parentNode) {
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

	/**
	 * @param statusDate The statusDate to set.
	 */
	public void setStatusDate(long statusDate) {
		statusDate = DateTime.midnightNextDay(statusDate) -1; // last instant of today
		statusDate = workCalendar.adjustInsideCalendar(statusDate, true);
		this.statusDate = statusDate;
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
		return resourcePool;
	}
    public void setResourcePool(ResourcePool resourcePool) {
        this.resourcePool = resourcePool;
    }

	public void accept(NodeVisitor visitor) {
		visitor.accept(this);
	}

	public Class getType() throws NodeException {
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
		if (this.workCalendar != null)
			((WorkingCalendar)this.workCalendar).removeObjectUsing(this);
		this.workCalendar = workCalendar;
		((WorkingCalendar)this.workCalendar).addObjectUsing(this);
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
	public static Task findTaskById(Object idObject, Collection taskList) {
		return findTaskById(taskList, ((Number)idObject).intValue());
	}
	public Task findByUniqueId(long id) {
		return findTaskByUniqueId(getTaskOutlineIterator(), id);
	}

	private static Task findTaskById(Collection taskList, int id) {
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

	protected transient EventListenerList projectListenerList = new EventListenerList();

	public void addProjectListener(ProjectListener l) {
		projectListenerList.add(ProjectListener.class, l);
	}
	public void removeProjectListener(ProjectListener l) {
		projectListenerList.remove(ProjectListener.class, l);
	}
	public ProjectListener[] getProjectListeners() {
		return (ProjectListener[]) projectListenerList.getListeners(ProjectListener.class);
	}
    public EventListener[] getProjectListeners(Class listenerType) {
    	return projectListenerList.getListeners(listenerType);
    }

 	protected void fireNameChanged(Object source,String oldName) {
		Object[] listeners = projectListenerList.getListenerList();
		ProjectEvent e = null;
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == ProjectListener.class) {
				if (e == null) {
					e = new ProjectEvent(source,
							ProjectEvent.NAME_CHANGED, this,oldName);
				}
				((ProjectListener) listeners[i + 1]).nameChanged(e);

			}
		}
	}
 	protected void fireGroupDirtyChanged(Object source,boolean oldName) {
		Object[] listeners = projectListenerList.getListenerList();
		ProjectEvent e = null;
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == ProjectListener.class) {
				if (e == null) {
					e = new ProjectEvent(source,
							ProjectEvent.GROUP_DIRTY_CHANGED, this,Boolean.valueOf(oldName));
				}
				((ProjectListener) listeners[i + 1]).groupDirtyChanged(e);

			}
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
		if (parent == null || !(parent instanceof Task))
			return this;
		return((Task)parent).getEnclosingProject();
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



	public void saveCurrentToSnapshot(Object snapshotId, boolean entireProject, List selection, boolean undo) {
		if (entireProject) forTasks(new SnapshottableImpl.SaveCurrentToSnapshotClosure(snapshotId));
		else DataUtils.forAllDo(selection.iterator(), new SnapshottableImpl.SaveCurrentToSnapshotClosure(snapshotId));

		fireSnapshotBaselineChanged(snapshotId, true);

		if (undo){
			UndoableEditSupport undoableEditSupport=getUndoController().getEditSupport();
			if (undoableEditSupport!=null){
				undoableEditSupport.postEdit(new SaveSnapshotEdit(this,snapshotId,entireProject,selection));
			}
		}

	}

	public void restoreSnapshot(Object snapshotId, boolean entireProject, List selection, Collection snapshotDetails) {
		Iterator i = getSnapshotIterator(entireProject, selection);
		if (i == null) {
			return;
		}

		Iterator j=snapshotDetails.iterator();
		while (i.hasNext()){
			NormalTask t=(NormalTask)i.next();
			t.restoreSnapshot(snapshotId,j.next());
		}
		fireSnapshotBaselineChanged(snapshotId, true);
	}

	public void clearSnapshot(final Object snapshotId, boolean entireProject, List selection, boolean undo) {
		Iterator i = getSnapshotIterator(entireProject, selection);

		final boolean[] foundSnapshot = new boolean[1]; // no undo edit if there is no snapshot
		final Collection snapshotDetails = undo ? collectSnapshotDetails(snapshotId, i, foundSnapshot) : null;

		if (entireProject) forTasks(new SnapshottableImpl.ClearSnapshotClosure(snapshotId));
		else DataUtils.forAllDo(selection.iterator(), new SnapshottableImpl.ClearSnapshotClosure(snapshotId));
		fireSnapshotBaselineChanged(snapshotId, false);

		if (foundSnapshot[0]){
			UndoableEditSupport undoableEditSupport=getUndoController().getEditSupport();
			if (undoableEditSupport!=null){
				undoableEditSupport.postEdit(new ClearSnapshotEdit(this,snapshotId,entireProject,selection,snapshotDetails));
			}
		}

	}

	private Collection collectSnapshotDetails(Object snapshotId, Iterator tasks, boolean[] foundSnapshot) {
		if (tasks == null || !tasks.hasNext()) {
			return null;
		}
		Collection snapshotDetails = new ArrayList();
		while (tasks.hasNext()) {
			NormalTask task = (NormalTask) tasks.next();
			TaskBackup taskBackup = (TaskBackup) task.backupDetail(snapshotId);
			if (taskBackup.snapshot != null) {
				foundSnapshot[0] = true;
			}
			snapshotDetails.add(taskBackup);
		}
		return snapshotDetails;
	}

	private Iterator getSnapshotIterator(boolean entireProject, List selection) {
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
	public void fireBaselineChanged(Object source, Object object, Integer baselineNumber, boolean save) {
		scheduleEventManager.fireBaselineChanged(source, null, baselineNumber, save);
	}
	public Document getDocument() {
		return this;
	}
	public void buildReverseQuery(ReverseQuery reverseQuery) {
		Iterator i = tasks.iterator();
		while (i.hasNext()) {
			((Task)i.next()).buildReverseQuery(reverseQuery);
		}
	}

	public void forEachWorkingInterval(Consumer<Object> visitor, boolean mergeWorking, WorkCalendar workCalendar) {
		Iterator i = tasks.iterator();
		while (i.hasNext()) {
			((Task)i.next()).forEachWorkingInterval(visitor,mergeWorking, workCalendar);
		}
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
		return TimeDistributedDataConsolidator.cost(start,end,childrenToRollup());
	}

	public double actualCost(long start, long end) {
		return TimeDistributedDataConsolidator.actualCost(start,end,childrenToRollup());
	}

	public double fixedCost(long start, long end) {
		return TimeDistributedDataConsolidator.fixedCost(start,end,childrenToRollup());
	}

	public double actualFixedCost(long start, long end) {
		return TimeDistributedDataConsolidator.actualFixedCost(start,end,childrenToRollup());
	}

	public boolean fieldHideActualFixedCost(FieldContext fieldContext) {
		return false;
	}
	public long work(long start, long end) {
		return TimeDistributedDataConsolidator.work(start,end,childrenToRollup(),true);
	}

	public long actualWork(long start, long end) {
		return TimeDistributedDataConsolidator.actualWork(start,end,childrenToRollup(),true);
	}
	public long remainingWork(long start, long end) {
		return TimeDistributedDataConsolidator.remainingWork(start,end,childrenToRollup(),true);
	}
	boolean isInRange(long start, long finish) {
		long s = getStart();
		return (finish > s && start < getEnd());
	}

	private boolean isFieldHidden(FieldContext fieldContext) {
		return fieldContext != null && !isInRange(fieldContext.getStart(),fieldContext.getEnd());
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
		Task task, previousParentTask, newParentTask;
		int count=e.getNodes().length;
		if (count==0) return;
		for (int i=0;i<count;i++){
			node=(Node)e.getNodes()[i];
			if (!(node.getImpl() instanceof Task)) continue;
			task=(Task) node.getImpl();

			previousParentTask=task.getWbsParentTask();
			previousParentNode=taskOutlines.getDefaultOutline().search(previousParentTask);

			if (previousParentTask!=null) {
				previousParentTask.markAllDependentTasksAsNeedingRecalculation(true); // flag this and dependent tasks as dirty
				previousParentTask.setWbsChildrenNodes(taskOutlines.getDefaultOutline().getHierarchy().getChildren(previousParentNode));
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

			taskOutlines.getDefaultOutline().getHierarchy().visitAll(newParentNode, new Consumer<Object>() { public void accept(Object arg) {
					Node node=(Node)arg;
					if (!(node.getImpl() instanceof Task)) return;
					Task task = (Task) node.getImpl();
					DependencyService.getInstance().removeAnyDependencies(task, _newParentTask,eventSource);
				}
			});
		}
		if (!e.isVoid()) { // if the event was not the promotion of a void node
			e.consume();
			updateScheduling(e.getSource(),this,ObjectEvent.CREATE); // will cause critical path to reset and to run and send schedule events
		}
	}
	public void nodesInserted(HierarchyEvent e) {
		nodesChanged(e);
	}
	public void nodesRemoved(HierarchyEvent e) {
	}
    public void structureChanged(HierarchyEvent e) {
    }

    public List getRootNodes(List tasks){
        List roots=new LinkedList();
        for (Iterator i=tasks.iterator();i.hasNext();){
            Task task=(Task)i.next();
            if (task.getWbsParentTask()==null) roots.add(taskOutlines.getDefaultOutline().search(task));
        }
        return roots;
    }

    public void dump(Collection tasks,String indent){
        if (tasks!=null)
        for (Iterator i=tasks.iterator();i.hasNext();){
            Node node=(Node)i.next();
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
	    if (obj instanceof DataObject){
	        return getName().equals(((DataObject)obj).getName());
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
	    tasks = new LinkedList<Task>();
		objectEventManager = new ObjectEventManager();
		objectSelectionEventManager = new ObjectSelectionEventManager();
		scheduleEventManager = new ScheduleEventManager();
		multipleTransactionManager = new MultipleTransactionManager();
		projectListenerList=new EventListenerList();
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
			subprojectHandler = (SubprojectHandler) Class.forName(Messages.getMetaString("SubprojectHandler")).getConstructor(new Class[]{Project.class}).newInstance(this);
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

	    int count=Settings.numHierarchies();
		for (int i=0;i<count;i++){
			NodeModel model=taskOutlines.getOutline(i);
			if (model==null) continue;
			if (model instanceof AssignmentNodeModel){
				AssignmentNodeModel aModel=(AssignmentNodeModel)model;
				aModel.setContainsLeftObjects(true);
				aModel.setDocument(this);
			}
			model.setUndoController(undoController);
		}

		initializeDefaultOutline();
		setInitialized(true);
		setGroupDirty(false);
	    if (workspace != null)
	    	restoreWorkspace(workspace, SavableToWorkspace.PERSIST);
	    if (calendarOption == null)
	    	calendarOption = CalendarOption.getDefaultInstance();

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
		Iterator i = tasks.iterator();
		while (i.hasNext()) {
			((Task)i.next()).setForward(forward);
		}
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
		Iterator i = tasks.iterator();
		NormalTask task;
		while (i.hasNext()) {
			task = (NormalTask)i.next();
			if (task.validateConstraints())
				addRepaired(task);
			if (task.getAssignments().isEmpty()) {
				Assignment ass = task.addDefaultAssignment();
				ass.setDirty(true);
				task.setDirty(true);
				ErrorLogger.logOnce("NoAssignment","Repaired task with no assignments",null);
				logger.fine("added default ass for " + task);
				addRepaired(task);
			}
		}
	}
	public void setAllTasksAsUnchangedFromPersisted(boolean justSaved) {
		getTaskOutline().getHierarchy().visitAll(new Consumer<Object>(){
			int id=1;
			public void accept(Object o) {
				Node node=(Node)o;
				if (node.getImpl() instanceof NormalTask){
					NormalTask task=(NormalTask)node.getImpl();
					task.setDirty(false);
					task.setLastSavedStart(task.getStart());
					task.setLastSavedFinish(task.getEnd());
					Iterator j = task.getAssignments().iterator();
					while (j.hasNext())
						((Assignment)j.next()).setDirty(false);


					j=task.getDependencyList(true).iterator();
					while (j.hasNext())
						((Dependency)j.next()).setDirty(false);

					Node parent=(Node)node.getParent();
					if (parent==null||parent.isRoot()) task.setLastSavedParentId(-1L);
					else task.setLastSavedParentId(((Task)parent.getImpl()).getUniqueId());
					task.setLastSavedPosistion(parent.getIndex(node));



				}
			}
		});


		if (!justSaved && repaired != null) {
		    Iterator<Task> i = repaired.iterator();
		    while (i.hasNext()) {
		    	NormalTask t = (NormalTask)i.next();
		    	t.setTaskAssignementAndPredsDirty();
		    }
		    repaired = null;
		}
	}
	void addRepaired(Task t) {
		if (repaired == null)
			repaired = new LinkedList<Task>();
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

	int getDefaultConstraintType(){
		if (isForward())
			return ConstraintType.ASAP;
		else
			return ConstraintType.ALAP;
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
		setStart(start);
		getSchedulingAlgorithm().setStartConstraint(start);
	}

	public final boolean isReadOnlyStartDate(FieldContext fieldContext) {
		return getSchedulingAlgorithm() == null || !getSchedulingAlgorithm().isForward();
	}

	public final long getFinishDate() {
		return getEnd();
	}
	public void setFinishDate(long finish) {
		finish = getEffectiveWorkCalendar().adjustInsideCalendar(finish,true);
		setEnd(finish);
		getSchedulingAlgorithm().setEndConstraint(finish);
	}

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

	public static Predicate instanceofPredicate() {
		return new Predicate() {
			public boolean evaluate(Object arg0) {
				return arg0 instanceof Project;
			}};
	}


	public void addPastedTask(Task task) {
		getSchedulingAlgorithm().addObject(task);
	}
	public Document invalidateCalendar() {
		markAllTasksAsNeedingRecalculation(false);
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
	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
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

	public boolean applyTimesheet(Collection fieldArray) {
		return applyTimesheet(fieldArray,System.currentTimeMillis());
	}
	public boolean applyTimesheet(Collection fieldArray, long timesheetUpdateDate) {
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
	public String getTimesheetStatusName() {
		return TimesheetHelper.getTimesheetStatusName(getTimesheetStatus());
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

	public final Map getExtraFields() {
		if (extraFields == null)
			// LinkedHashMap preserves insertion order for stable POD serialization (issue #227)
			extraFields = new LinkedHashMap();
		return extraFields;
	}

	public final void setExtraFields(Map extraFields) {
		this.extraFields = extraFields;
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

	public final Collection getReferringSubprojectTasks() {
		return subprojectFacade.getReferringSubprojectTasks();
	}

	public final void setReferringSubprojectTasks(Collection referringSubprojectTasks) {
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

	public int getProjectType() {
		return projectType;
	}

	public void setProjectType(int projectType) {
		this.projectType = projectType;
	}

	public int getExpenseType() {
		return expenseType;
	}

	public void setExpenseType(int budgetType) {
		this.expenseType = budgetType;
	}

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

	public int getProjectStatus() {
		return projectStatus;
	}

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
        for (Iterator i=getTaskOutlineIterator();i.hasNext();){
            NormalTask task=(NormalTask)i.next(); //ResourceImpl to have the EnterpriseResource link
            if (task.getProjectId() != uniqueId) // skip if in another project
            	continue;

            r|=task.renumber(localOnly);
        }
		r|=identityFacade.renumber(localOnly);
		if (!r) return false;
		uniqueId=getUniqueId();
        for (Iterator i=getTaskOutlineIterator();i.hasNext();){
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
	public void linkAllSiblings(Node parent, Predicate canBeSuccessorCondition, Object eventSource) {
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

	protected transient int fileType=FileHelper.PROJECTLIBRE_FILE_TYPE;

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
	}

	public void setAllNodesInSubproject(boolean b) {
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
			Class.forName(Messages.getMetaString("ProjectRoleManager")).getDeclaredMethod("resetRoles", new Class[] {Project.class, Boolean.class}).invoke(null, new Object[] {this,publicRoles});
		} catch (Exception e) {
			ErrorLogger.log("ProjectRoleManager initialization failed", e);
			logger.warning("ProjectRoleManager not valid in meta.properties");
			System.exit(-1);
		}
	}

	public class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = 6909144693873463556L;
		WorkspaceSetting spreadsheetWorkspace;
		HashMap fieldAliasMap = new HashMap();
		PrintSettings printSettings;
		CalendarOption calendarOption;

	}
	public transient SpreadSheetFieldArray fieldArray = null;
	public transient PrintSettings printSettings = null;
	public transient PrintSettings tmpSettings = null;
	public transient CalendarOption calendarOption = null;

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
			fieldArray = (SpreadSheetFieldArray) Alert.getGraphicManagerMethod("getCurrentFieldArray");
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
		for (Iterator i=getTaskOutlineIterator();i.hasNext();){
			c.accept(i.next());
		}
	}

	private class TaskIterator implements Iterator<Task>{
		private Iterator iterator;
		private Task next=null;
		private Task nextElement(){
	        Node node=null;
	        while(iterator.hasNext() && !((node=(Node)iterator.next()).getImpl() instanceof Task));
	        if (node!=null && node.getImpl() instanceof Task) next=(Task)node.getImpl();
	        else next=null;
	        return next;
		}

		TaskIterator(){
			iterator=getTaskOutline().iterator(getTaskOutlineRoot());
			nextElement();
		}
		public boolean hasNext() {
			return next!=null;
		}
		public Task next() {
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
    	List dist=(new DistributionConverter()).createDistributionData(this,false);
    	if (dist==null) return;
    	TreeMap<DistributionData, DistributionData> distMap=new TreeMap<DistributionData, DistributionData>(new DistributionComparator());
    	setDistributionMap(distMap);
    	long projectId=getUniqueId();
	    for (Iterator i=dist.iterator();i.hasNext();){
	    	DistributionData d=(DistributionData)i.next();
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

	public int getRowHeight(SortedSet baseLines){
        for (Iterator i=getTaskOutlineIterator();i.hasNext();){
            Task task=(Task)i.next();
            int current=Snapshottable.CURRENT.intValue();
            for (int s=0;s<Settings.numGanttBaselines();s++){
                if (s==current) continue;
                TaskSnapshot snapshot=(TaskSnapshot)task.getSnapshot(Integer.valueOf(s));
                if (snapshot!=null) baseLines.add(Integer.valueOf(s));
            }
        }
		int num=(baseLines.size()==0)?0:(((Integer)baseLines.last()).intValue()+1);
		int rowHeight=GraphicConfiguration.getInstance().getRowHeight()
				+num*GraphicConfiguration.getInstance().getBaselineHeight();
		return rowHeight;
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
			for (Iterator i = tasks.iterator(); i.hasNext();) {
				task = (NormalTask) i.next();
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
			for (Iterator i = tasks.iterator(); i.hasNext();) {
				task = (NormalTask) i.next();
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

		Collection getReferringSubprojectTasks() {
			return subprojectHandler.getReferringSubprojectTasks();
		}

		void setReferringSubprojectTasks(Collection referringSubprojectTasks) {
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
			return TaskSheetScheduleWorkflow.calculateProjectRollup(Project.this);
		}
	}

	private final class TaskAggregationFacade implements Serializable {
		private static final long serialVersionUID = 1L;
		long getActualStart() {
			long result = Long.MAX_VALUE;
			for (Iterator i = tasks.iterator(); i.hasNext();) {
				Task task = (Task) i.next();
				long actualStart = task.getActualStart();
				if (actualStart != 0 && actualStart < result) {
					result = actualStart;
				}
			}
			return result == Long.MAX_VALUE ? 0 : result;
		}

		long getActualFinish() {
			long result = 0;
			for (Iterator i = tasks.iterator(); i.hasNext();) {
				Task task = (Task) i.next();
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
			for (Iterator i = tasks.iterator(); i.hasNext();) {
				Task task = (Task) i.next();
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
			for (Iterator i = tasks.iterator(); i.hasNext();) {
				Task task = (Task) i.next();
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
			for (Iterator i = tasks.iterator(); i.hasNext();) {
				Task task = (Task) i.next();
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
			newOne.getCurrentSchedule().setStart(getWorkCalendar().adjustInsideCalendar(newOne.getCurrentSchedule().getStart(), false));
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
