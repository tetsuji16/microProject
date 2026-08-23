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
package com.microproject.core.pm.exchange;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mpxj.Duration;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Relation;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mpx.MPXReader;
import net.sf.mpxj.mspdi.schema.TimephasedDataType;
import net.sf.mpxj.reader.AbstractProjectReader;
import net.sf.mpxj.reader.UniversalProjectReader;
import net.sf.mpxj.planner.PlannerReader;
import com.microproject.exchange.xlsx.ProjectLibreXlsxReader;

import com.microproject.core.pm.exchange.converters.mpx.MpxAssignmentConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxCalendarConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxDependencyConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxImportState;
import com.microproject.core.pm.exchange.converters.mpx.MpxOptionsConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxProjectConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxResourceConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxTaskConverter;
import com.microproject.core.pm.exchange.converters.mpx.type.MpxDurationConverter;
import com.microproject.core.pm.exchange.converters.type.DateUTCConverter;
import com.microproject.core.pm.exchange.converters.type.PercentNumberRatioDoubleConverter;
import com.microproject.exchange.ImportedCalendarService;
import com.microproject.grouping.core.Node;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.NormalTask;
import com.microproject.undo.DataFactoryUndoController;

/**
 * @author Laurent Chretienneau
 *
 */
public class MspImporter {
	private static final Logger logger = Logger.getLogger(MspImporter.class.getName());
	protected ProjectFile mpxProjectFile;
	protected MpxImportState state=new MpxImportState();
	protected AbstractProjectReader reader;
	protected long earliestTaskStart=-1L;
	protected net.sf.mpxj.Task mpxRootTask=null;
	
	public Project importProject(String name, ProgressClosure progress) throws Exception{
		progress.updateProgress(0.0f, "Start");
		parseProject(name);
		return importProject_(progress);		
	}

	public Project importProject(InputStream in, String extension, ProgressClosure progress) throws Exception{
		progress.updateProgress(0.0f, "Start");
		parseProject(in,extension);
		return importProject_(progress);		
	}

	private Project importProject_(ProgressClosure progress) throws Exception{
		progress.updateProgress(0.2f, "File parsed");
		initializeTimephasedState();

		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("imported", undo);
		Project project = Project.createProject(resourcePool, undo);

		importOptions(project);
		progress.updateProgress(0.3f, "Options converted");
		importCalendars(project);
		progress.updateProgress(0.4f, "Calendars converted");
		importResourcePool(project);
		progress.updateProgress(0.5f, "Resources converted");
		importTasks(project);
		progress.updateProgress(0.7f, "Tasks converted");
		importDependencies(project);
		progress.updateProgress(0.8f, "Dependencies converted");
		importProjectHeader(project); //must be done after tasks to correct project start
		progress.updateProgress(0.9f, "Project headers converted");

		progress.updateProgress(1f, "Completed");
		return project;
	}
	
	private void initializeTimephasedState() {
		// Identity the type of conversion. It will be used by AssignmentConverter.
		if (state.isMspdi()) {
			state.setMpxTimephasedMap(new HashMap<ResourceAssignment, List<TimephasedDataType>>());
			return;
		}
		state.setMpxTimephasedMap(new HashMap<ResourceAssignment, List<TimephasedDataType>>());
	}
	
	
	public void parseProject(InputStream in, String extension) throws Exception {
		try {
			InputStream source = prepareProjectStream(in);
			String effectiveExtension = normalizeExtension(extension, source);
			reader = createReader(effectiveExtension);
			mpxProjectFile = readProjectFile(source);
			state.setMpxProjectFile(mpxProjectFile);
		} finally {
			if (in!=null)
				in.close();
		}	
	}

	private AbstractProjectReader createReader(String extension) {
		if (extension == null) {
			throw new IllegalArgumentException("Unsupported import extension: null");
		}
		if (isMspdiExtension(extension)) {
			state.setMspdi(true);
			return new net.sf.mpxj.mspdi.MSPDIReader();
		}
		if (extension.equals("mpp"))
			return new MPPReader();
		if (extension.equals("mpx"))
			return new MPXReader();
		if (extension.equals("planner"))
			return new PlannerReader();
		if (extension.equals("xlsx"))
			return new ProjectLibreXlsxReader();
		throw new IllegalArgumentException("Unsupported import extension: " + extension);
	}

	private boolean isMspdiExtension(String extension) {
		if (extension == null) {
			return false;
		}
		return extension.equals("xml") || extension.equals("pod");
	}

	private ProjectFile readProjectFile(InputStream source) throws Exception {
		return reader.read(source);
	}

	private InputStream prepareProjectStream(InputStream in) {
		if (in instanceof BufferedInputStream) {
			return in;
		}
		return new BufferedInputStream(in);
	}

	private String normalizeExtension(String extension, InputStream in) throws Exception {
		if (!"xlsx".equals(extension) || !in.markSupported()) {
			return extension;
		}
		in.mark(256);
		byte[] header = new byte[256];
		int read = in.read(header);
		in.reset();
		if (read <= 0) {
			return extension;
		}
		String prefix = new String(header, 0, read, StandardCharsets.UTF_8).trim();
		if (prefix.startsWith("<?xml") || prefix.startsWith("<Project")) {
			return "xml"; //$NON-NLS-1$
		}
		return extension;
	}
	protected void parseProject(String fileName) throws Exception {
		fileName=fileName.trim();
		int extensionPosition=fileName.lastIndexOf("."); 
		String extension = extensionPosition==-1 ? "xml" : fileName.substring(extensionPosition+1).toLowerCase(java.util.Locale.ROOT);
		try (FileInputStream input = new FileInputStream(fileName)) {
			parseProject(input, extension);
		}
	}
	
	
	protected void importOptions(Project project) {
		// Obsolete CalendarOptions conversion removed (see issue #154).
	}
	
	protected void importProjectHeader(Project project) {
		MpxProjectConverter converter=new MpxProjectConverter();
		converter.from(mpxProjectFile.getProjectProperties(), project, state);

		if (earliestTaskStart!=-1L) //fix project start
			project.setStart(earliestTaskStart);
	}

	
	protected void importCalendars(Project project) {
		state.setProjectTitle(mpxProjectFile.getProjectProperties().getProjectTitle());

		MpxCalendarConverter converter = new MpxCalendarConverter();
		WorkCalendar standardBaseCalendar = null;
		for (ProjectCalendar mpxBaseCalendar : mpxProjectFile.getCalendars()) {
			WorkingCalendar calendar = WorkingCalendar.getStandardBasedInstance();
			if (ProjectCalendar.DEFAULT_BASE_CALENDAR_NAME.equals(mpxBaseCalendar.getName())) {
				state.setMpxStandardBaseCalendar(mpxBaseCalendar);
				standardBaseCalendar = calendar;
			}
			converter.from(mpxBaseCalendar, calendar, state);
			// register into the global calendar service via the import bridge
			ImportedCalendarService.getInstance().addImportedCalendar((WorkingCalendar) calendar, mpxBaseCalendar);
			state.mapBaseCalendar(calendar, mpxBaseCalendar);
		}
		if (standardBaseCalendar == null) {
			standardBaseCalendar = CalendarService.getInstance().getStandardInstance();
		}
		try {
			project.setBaseCalendar(standardBaseCalendar);
		} catch (com.microproject.configuration.CircularDependencyException e) {
			// ignore: a self-referential base calendar is not expected here
		}
		project.setWorkCalendar(standardBaseCalendar);
		state.setProjectBaseCalendar(standardBaseCalendar);
	}
	
	protected void importResourcePool(Project project) {
		ResourcePool resourcePool = project.getResourcePool();
		state.setResourcePool(resourcePool);
		importResources(resourcePool);
	}

	protected void importResources(ResourcePool resourcePool) {
		state.setResourcePool(resourcePool);
		MpxResourceConverter converter=new MpxResourceConverter();
		for (net.sf.mpxj.Resource mpxResource : mpxProjectFile.getResources()){
			if (shouldSkipResource(mpxResource)) {
				logSkippedResource(mpxResource);
				continue;
			}
			Resource resource;
			if (mpxResource.getID()==0) {
				resource=ResourceImpl.getUnassignedInstance();
				state.mapResource(mpxResource, resource);
				continue;
			}
			resource=resourcePool.newResourceInstance();
			converter.from(mpxResource, resource, state);
			state.mapResource(mpxResource, resource);
		}
	}

	private boolean shouldSkipResource(net.sf.mpxj.Resource mpxResource) {
		return mpxResource.getNull() || mpxResource.getID() == null;
	}

	private void logSkippedResource(net.sf.mpxj.Resource mpxResource) {
		logger.log(Level.FINE, "Skipping resource with missing identity: {0}", mpxResource);
	}
	
	protected void importTasks(Project project) {
		for (net.sf.mpxj.Task mpxTask : mpxProjectFile.getChildTasks()){
			importTasks(project,mpxTask,null);
		}
	}
	
	protected void importTasks(Project project,net.sf.mpxj.Task mpxTask, Task parentTask) {
		if (shouldSkipTask(mpxTask)) {
			logSkippedTask(mpxTask);
			return;
		}

		Task task = null;
		if (isRootTask(mpxTask)) {
			registerRootTask(mpxTask);
		} else {
			task = importRegularTask(project, mpxTask, parentTask);
		}
		
		for (net.sf.mpxj.Task mpxChildTask : mpxTask.getChildTasks()){
			importTasks(project,mpxChildTask,task);
		}
	}

	private boolean shouldSkipTask(net.sf.mpxj.Task mpxTask) {
		return mpxTask.getNull() || mpxTask.getID()==null;
	}

	private void logSkippedTask(net.sf.mpxj.Task mpxTask) {
		logger.log(Level.FINE, "Skipping task with missing identity: {0}", mpxTask);
	}

	private boolean isRootTask(net.sf.mpxj.Task mpxTask) {
		return mpxTask.getOutlineNumber()!=null && mpxTask.getOutlineLevel() == 0;
	}

	private void registerRootTask(net.sf.mpxj.Task mpxTask) {
		if (mpxRootTask==null)
			mpxRootTask=mpxTask;
	}

	private Task importRegularTask(Project project, net.sf.mpxj.Task mpxTask, Task parentTask) {
		Task task = createTask(project, mpxTask, parentTask);
		updateEarliestTaskStart(task);
		state.mapTask(mpxTask, task);
		importTaskSnapshots(mpxTask, task);
		importAssignments(mpxTask, task);
		// MS Project stores task % complete separately from assignment % work
		// complete. Restore it after assignments have been imported.
		if (task instanceof NormalTask && mpxTask.getPercentageComplete() != null)
			((NormalTask) task).setImportedPercentComplete(mpxTask.getPercentageComplete().doubleValue() / 100.0D);
		if (task instanceof NormalTask && mpxTask.getPercentageWorkComplete() != null)
			((NormalTask) task).setImportedPercentWorkComplete(mpxTask.getPercentageWorkComplete().doubleValue() / 100.0D);
		return task;
	}

	private Task createTask(Project project, net.sf.mpxj.Task mpxTask, Task parentTask) {
		MpxTaskConverter converter=new MpxTaskConverter();
		Node parentNode = parentTask == null ? null : project.getTaskModel().search(parentTask);
		Task task = (Task) project.createLocalTaskNode(parentNode).getImpl();
		converter.from(mpxTask, task, state);
		return task;
	}

	private void updateEarliestTaskStart(Task task) {
		final long taskStart = task.getStart();
		if (taskStart == 0L) {
			return;
		}
		if (earliestTaskStart == -1L || taskStart < earliestTaskStart)
			earliestTaskStart = taskStart;
	}

	private void importTaskSnapshots(net.sf.mpxj.Task mpxTask, Task task) {
		// Baseline snapshots (SnapshotList/TaskSnapshot) are not carried by the
		// microproject model; import of baselines is intentionally skipped (issue #154).
	}

	private Date getTaskBaselineStart(net.sf.mpxj.Task mpxTask, int snapshotId) {
		if (snapshotId==0)
			return mpxTask.getBaselineStart();
		return mpxTask.getBaselineStart(snapshotId);
	}

	private Date getTaskBaselineFinish(net.sf.mpxj.Task mpxTask, int snapshotId) {
		if (snapshotId==0)
			return mpxTask.getBaselineFinish();
		return mpxTask.getBaselineFinish(snapshotId);
	}

	private Duration getTaskBaselineDuration(net.sf.mpxj.Task mpxTask, int snapshotId) {
		if (snapshotId==0)
			return mpxTask.getBaselineDuration();
		return mpxTask.getBaselineDuration(snapshotId);
	}
	
	protected void importAssignments(net.sf.mpxj.Task mpxTask, Task task) {
		for (net.sf.mpxj.ResourceAssignment mpxAssignment : mpxTask.getResourceAssignments()) {
			MpxAssignmentConverter converter = new MpxAssignmentConverter();
			Assignment assignment = Assignment.getInstance(task, ResourceImpl.getUnassignedInstance(), 0, 0);
			converter.from(mpxAssignment, assignment, state, task, 0);
			((NormalTask) task).addAssignment(assignment);
		}
	}

	private Date getAssignmentBaselineStart(net.sf.mpxj.ResourceAssignment mpxAssignment, int snapshotId) {
		if (snapshotId==0)
			return mpxAssignment.getBaselineStart();
		return mpxAssignment.getBaselineStart(snapshotId);
	}

	private Date getAssignmentBaselineFinish(net.sf.mpxj.ResourceAssignment mpxAssignment, int snapshotId) {
		if (snapshotId==0)
			return mpxAssignment.getBaselineFinish();
		return mpxAssignment.getBaselineFinish(snapshotId);
	}

	private Duration getAssignmentBaselineWork(net.sf.mpxj.ResourceAssignment mpxAssignment, int snapshotId) {
		if (snapshotId==0)
			return mpxAssignment.getBaselineWork();
		return mpxAssignment.getBaselineWork(snapshotId);
	}
	
	protected void importDependencies(Project project) {
		MpxDependencyConverter converter=new MpxDependencyConverter();
		for (net.sf.mpxj.Task mpxTask : mpxProjectFile.getTasks()){
//			if (mpxTask.getNull() || mpxTask.getID()==null)
//				continue;
			if (mpxTask==mpxRootTask)
				continue;
			List<Relation> mpxRelations=mpxTask.getPredecessors();
			if (mpxRelations==null) {
				logger.log(Level.FINE, "Task has no predecessors to import: {0}", mpxTask);
				continue;
			}
			for (Relation mpxRelation : mpxRelations){
				Dependency dependency=converter.from(mpxRelation, state);
				if (dependency!=null) {
					// dependency registered via DependencyService in converter
			}
		}
	}
	}

	public interface ProgressClosure{
		public void updateProgress(float progress, String label);
	}

	
	

	
	

}
