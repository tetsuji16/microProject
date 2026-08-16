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
package com.microproject.exchange;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.microproject.core.hierarchy.Hierarchy;
import com.microproject.core.hierarchy.HierarchyNode;
import com.microproject.core.pm.exchange.MspImporter;
import com.microproject.core.pm.exchange.ProjectConverter;
import com.microproject.core.pm.exchange.converters.op.OpAssignmentConverter;
import com.microproject.core.pm.exchange.converters.op.OpDependencyConverter;
import com.microproject.core.pm.exchange.converters.op.OpImportState;
import com.microproject.core.pm.exchange.converters.op.OpProjectConverter;
import com.microproject.core.pm.exchange.converters.op.OpResourceConverter;
import com.microproject.core.pm.exchange.converters.op.OpTaskConverter;
import com.microproject.pm.calendar.CalendarOptions;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.scheduling.ScheduleFrom;
import com.microproject.pm.tasks.SnapshotList;
import com.microproject.pm.tasks.Task;
import com.microproject.pm.tasks.TaskSnapshot;
import com.microproject.configuration.CircularDependencyException;
import com.microproject.contrib.util.Log;
import com.microproject.contrib.util.LogFactory;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.job.Job;
import com.microproject.job.JobRunnable;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.criticalpath.TaskSchedule;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.server.data.EnterpriseResourceData;
import com.microproject.server.data.MSPDISerializer;
import com.microproject.server.data.Serializer;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.DateTime;
import com.microproject.util.Environment;

import net.sf.mpxj.writer.ProjectWriter;
import com.microproject.exchange.mpxj.ProjectWriterFactory;
import com.microproject.exchange.xlsx.ProjectLibreXlsxWriter;
import com.microproject.exchange.xlsx.ProjectLibreXlsxReader;
/**
 * This class is based on the project mpxj http://www.tapsterrock.com/mpxj/
 * The enumerated types in projectlibre currently correspond exactly to the types in mpx, so there is no need to convert them.
 * However, if the projectlibre enumerations change, it will be necessary to map them to mpx types.
 *
 */
public class MicrosoftImporter extends ServerFileImporter{
	static Log log = LogFactory.getLog(MicrosoftImporter.class);
	protected com.microproject.pm.tasks.Project plProject= null;
	protected OpImportState state=new OpImportState();
	List<Object> allTasks = null;
	ArrayList<Object> subprojects;
	private Date earliestStart = DateTime.getMaxDate();
	protected Map<Object, Object> taskMap = new HashMap<>(); // keeps track of mapping mpx tasks to projectlibre1 tasks
	private Map<Number, Object> resourceMap = new HashMap<>(); // keeps track of mappy mpx resources to projectlibre1 resources
	List<Object> allResources=null;
	public static boolean ADD_SUMMARY_TASK = false; //Environment.isAddSummaryTask(); // whether to automatically add an extra project summary task or not //claur
	private static final String ABORT = "Job aborted"; //$NON-NLS-1$
	private String errorDescription = null;
	private Exception lastException = null;
	private JobRunnable jobRunnable = null;

	protected Context context = new Context();
	public MicrosoftImporter() {
		log.info("-------MicrosoftImporter ctor");
	}


	@Override
	public void importFile() throws Exception {
		log.info("BEGIN: MicrosoftImporter.PrepareResources");
		parse();
		log.info("END: MicrosoftImporter.PrepareResources");
		Environment.setImporting(false);
		log.info("BEGIN: Finish import");
		convertToProjectLibre1();
		log.info("END: Finish import");
	}

	@Override
	public Project loadProject(InputStream in)  throws Exception{
		log.info("BEGIN: MicrosoftImporter.PrepareResources");
		parse(in, getFileExtension());
		log.info("END: MicrosoftImporter.PrepareResources");
		Environment.setImporting(false);
		log.info("BEGIN: Finish import");
		convertToProjectLibre1();
		log.info("END: Finish import");
    	return project;
	}
    
    @Override
	public boolean saveProject(Project project,OutputStream out) throws Exception{
		return saveProject(project, out, fileName);
	}

	@Override
	public void exportFile() throws Exception {
		String extension = ""; //$NON-NLS-1$
		String name = fileName;
		String tmpFileName = fileName;
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
			extension = fileName.substring(i);
			name = fileName.substring(0, i);
		}

		File file = new File(fileName);
		File tmpFile = file;
		for (int count = 0; tmpFile.exists(); count++) {
			tmpFileName = name + "_tmp" + count + extension; //$NON-NLS-1$
			tmpFile = new File(tmpFileName);
		}

		try (FileOutputStream out = new FileOutputStream(tmpFile)) {
			if (!saveProject(project, out, fileName)) {
				throw new Exception("Failed to save project: " + fileName); //$NON-NLS-1$
			}
		}

		if (!file.equals(tmpFile)) {
			file.delete();
			tmpFile.renameTo(file);
		}
	}


	private void setProgress(float p) {
		if (jobRunnable == null)
			log.info("Progress " + 100 * p + "%");
		else
			jobRunnable.setProgress(p);
	}
	public void importProject(Project p) throws Exception {
		log.info("MicrosoftImporter.importProject()");

		this.project = p;
		parse();
		convertToProjectLibre1();
	}
	public void parse(InputStream in, String extension) throws Exception {
		log.info("MicrosoftImporter.parse()");

		Environment.setImporting(true); // will avoid certain popups
		
		setProgress(0.1f);
		
		
		MspImporter plImporter=new MspImporter();
		if ("xlsx".equalsIgnoreCase(extension)) {
			byte[] data = in.readAllBytes();
			Project nativeProject = ProjectLibreXlsxReader.readProjectLibreProject(new java.io.ByteArrayInputStream(data));
			if (nativeProject != null) {
				project = nativeProject;
				plProject = null;
				setProgress(1f);
				return;
			}
			in = new java.io.ByteArrayInputStream(data);
		}
		plProject=plImporter.importProject(in, extension, new MspImporter.ProgressClosure() {
			@Override
			public void updateProgress(float progress, String label) {
				setProgress(progress*0.1f);
				
			}
		});
		if (plProject == null) {
			String errorText = (errorDescription == null) ? Messages.getString("Message.ImportError") : errorDescription; //$NON-NLS-1$
			if (jobRunnable != null) {
				jobRunnable.getJob().error(errorText,false);
				jobRunnable.getJob().cancel();
			}

			Environment.setImporting(false); // will avoid certain popups
			throw lastException == null ? new Exception("Failed to import file") : lastException; //$NON-NLS-1$
		}
		log.info(plProject.toString());

		setProgress(0.2f);
		setProgress(1f);

	}
	public void parse() throws Exception {
		log.info("MicrosoftImporter.parse()");

		Environment.setImporting(true); // will avoid certain popups
		
		setProgress(0.1f);
		
		
		MspImporter plImporter=new MspImporter();
		if (fileInputStream == null && "xlsx".equals(getFileExtension())) {
			Project nativeProject = ProjectLibreXlsxReader.readProjectLibreProject(new File(fileName));
			if (nativeProject != null) {
				project = nativeProject;
				plProject = null;
				setProgress(1f);
				return;
			}
		}
		if (fileInputStream==null)
			plProject=plImporter.importProject(fileName, new MspImporter.ProgressClosure() {
				@Override
				public void updateProgress(float progress, String label) {
					setProgress(progress*0.1f);
					
				}
			});
		else plProject=plImporter.importProject(fileInputStream, getFileExtension(), new MspImporter.ProgressClosure() {
			@Override
			public void updateProgress(float progress, String label) {
				setProgress(progress*0.1f);
				
			}
		});

		if (plProject == null) {
			String errorText = (errorDescription == null) ? Messages.getString("Message.ImportError") : errorDescription; //$NON-NLS-1$
			if (jobRunnable != null) {
				jobRunnable.getJob().error(errorText,false);
				jobRunnable.getJob().cancel();
			}

			Environment.setImporting(false); // will avoid certain popups
			throw lastException == null ? new Exception("Failed to import file") : lastException; //$NON-NLS-1$
		}
		log.info(plProject.toString());

		setProgress(0.2f);
		setProgress(1f);

	}
	/**
	 * This method imports an entire mpx, mpp or xml file
	 *
	 * @param filename
	 *            name of the inputfile
	 * @throws Exception
	 *             on file read error
	 */
    public Job getImportFileJob(){
		log.info("MicrosoftImporter.getImportFileJob()");

    	subprojects = new ArrayList<>();
    	errorDescription = null;
    	lastException = null;
    	Session session=SessionFactory.getInstance().getSession(resourceMapping==null);
		Job job=new Job(session.getJobQueue(),"importFile",Messages.getString("MicrosoftImporter.Importing"),true); //$NON-NLS-1$ //$NON-NLS-2$

//    	job.addRunnable(new JobRunnable(Messages.getString("MicrosoftImporter.PrepareResources"),1.0f){ //$NON-NLS-1$
//
//			public Object run() throws Exception{
//				log.info("BEGIN: MicrosoftImporter.PrepareResources");
//				//MicrosoftImporter.this.jobRunnable = this;
//				importFile();
//				log.info("END: MicrosoftImporter.PrepareResources");
//				return null;
//			}
//    	});

		
    	job.addRunnable(new JobRunnable(Messages.getString("MicrosoftImporter.PrepareResources"),1.0f){ //$NON-NLS-1$

			public Object run() throws Exception{
				log.info("BEGIN: MicrosoftImporter.PrepareResources");
				MicrosoftImporter.this.jobRunnable = this;
				parse();
				log.info("END: MicrosoftImporter.PrepareResources");
				return null;
			}
    	});
    	
    	job.addSwingRunnable(new JobRunnable("Import resources",1.0f){ //$NON-NLS-1$
			public Object run() throws Exception{
				log.info("BEGIN: Import resources");
				ResourceMappingForm form=getResourceMapping();
				if (form!=null&&form.isLocal()) //if form==null we are in a case were have no server access. popup not needed
					if (!job.okCancel(Messages.getString("Message.ServerUnreacheableReadOnlyProject"),true)){ //$NON-NLS-1$
						setProgress(1.0f);
						errorDescription = ABORT;
						Environment.setImporting(false); // will avoid certain popups
						throw new Exception(ABORT);
					}

// claur - Moved to convertToProjectLibre1 after import Calendar because base calendar must be imported before resources
//			log.info("import resources");		 //$NON-NLS-1$
//				if(!importResources()){
//					setProgress(1.0f);
//					errorDescription = ABORT;
//					Environment.setImporting(false); // will avoid certain popups
//					throw new Exception(ABORT);
//				}
				setProgress(1f);
				log.info("END: Import resources");
				return null;
	    	}
    	});
    	job.addRunnable(new JobRunnable("Finish import",1.0f){ //$NON-NLS-1$
			public Object run() throws Exception{
				log.info("BEGIN: Finish import");
				Object r=convertToProjectLibre1();
				log.info("END: Finish import");
				return r;
    		}
    	});
    	return job;
    }

    private Project convertToProjectLibre1() throws Exception {
		if (plProject == null && project != null) {
			Environment.setImporting(false);
			return project;
		}

		log.info("import options"); //$NON-NLS-1$
		importOptions();
		setProgress(0.3f);
		
		log.info("import calendars"); //$NON-NLS-1$
		importCalendars();
		setProgress(0.4f);
		
		log.info("import resources");		 //$NON-NLS-1$
		//claur - moved here because calendars must be imported first
		importLocalResources();
		setProgress(0.5f);
		
		log.info("import tasks");		 //$NON-NLS-1$
		importTasks();
		setProgress(0.6f);
		
		log.info("import project fields");		 //$NON-NLS-1$
		importProjectFields();
		setProgress(0.7f);
		
		log.info("import dependencies");		 //$NON-NLS-1$
		importDependencies();
		setProgress(0.8f);
		
		log.info("import assignments"); //$NON-NLS-1$
		importAssignments();
		setProgress(0.9f);
				
		log.info("about to initialize");		 //$NON-NLS-1$
			if (project.getName() == null)
				project.setName("error - name not set on import"); //$NON-NLS-1$

//			CalendarService.getInstance().renameImportedBaseCalendars(project.getName());
			try {
				project.initialize(false,false); // will run critical path
			} catch (RuntimeException e) {
				if (e.getMessage()==CircularDependencyException.RUNTIME_EXCEPTION_TEXT) {
					Environment.setImporting(false); // will avoid certain popups
					Alert.error(e.getMessage());
					plProject = null;
					project = null;
					throw new Exception(e.getMessage());
				}
			}
			applyImportedTrackingFields();
			//project.setGroupDirty(!Environment.getStandAlone());
			if (!Environment.getStandAlone()) project.setAllDirty();

			project.setBoundsAfterReadProject();
			
			if (plProject.getPropertyValue("scheduleFrom") == ScheduleFrom.FINISH) {
				project.setForward(false);
			}
			Environment.setImporting(false); // will avoid certain popups
			setProgress(1.0f);
			plProject=null;// remove reference
//			project.setWasImported(true); //claur
		return project;
    }

	private boolean saveProject(Project project, OutputStream out, String targetFileName) throws Exception {
		String extension = getFileExtension(targetFileName);
		if ("xlsx".equals(extension)) {
			new ProjectLibreXlsxWriter().writeProjectLibreProject(project, out);
			return true;
		}
		MSPDISerializer serializer = new MSPDISerializer();
		if ("xml".equals(extension) || extension.length() == 0) {
			return serializer.saveProject(project, out);
		}

		ProjectWriter writer = ProjectWriterFactory.forFile(targetFileName);
		writer.write(serializer.serializeProject(project).getProjectFile(), out);
		return true;
	}

	private String getFileExtension() {
		return getFileExtension(fileName);
	}

	private String getFileExtension(String name) {
		if (name == null) {
			return "xml"; //$NON-NLS-1$
		}
		int extensionPosition = name.lastIndexOf('.');
		if (extensionPosition == -1 || extensionPosition == name.length() - 1) {
			return "xml"; //$NON-NLS-1$
		}
		return name.substring(extensionPosition + 1).toLowerCase();
	}


	protected void importCalendars() throws Exception{
		state.setCalendarManager(plProject.getCalendarManager());
		
		for (WorkCalendar plCalendar : plProject.getCalendarManager()) {
			WorkingCalendar opCalendar=WorkingCalendar.getStandardBasedInstance();
			ProjectConverter.getInstance().convert("op",ProjectConverter.Type.CALENDAR,false,opCalendar,plCalendar,state);
			if (CalendarService.findBaseCalendar(opCalendar.getName())!= null){
				//rename imported calendar if a calendar with the same name exists
				opCalendar.setName(opCalendar.getName() + "[Imported]");
			}
			CalendarService.getInstance().add(opCalendar);
			state.mapBaseCalendar(plCalendar,opCalendar);
		}
	}


	/**
	 * This method imports all resources defined in the file into the projectlibre1 model
	 *
	 * @param file
	 *            MPX file
	 */
	protected void importLocalResources(){
		ResourcePool resourcePool = project.getResourcePool();
		project.setLocal(true);
		resourcePool.setLocal(true);
		resourcePool.setMaster(false);
        resourcePool.updateOutlineTypes();
		ResourceImpl opResource;
		OpResourceConverter converter=new OpResourceConverter();
		for (com.microproject.pm.resources.Resource plResource : plProject.getResourcePool().getResources()){
			opResource = resourcePool.newResourceInstance();
			converter.to(opResource,plResource,state);
			state.mapOpResource(plResource, opResource);
			// Add to resource hierarchy.  MSProject does not actually have a hierarchy
			Node opResourceNode = NodeFactory.getInstance().createNode(opResource); // get a node for this resource
			resourcePool.addToDefaultOutline(null,opResourceNode);			
			state.mapOpResourceNode(opResource, opResourceNode);

		}
		//insertResourceVoids();
	}


	protected boolean importResources() throws Exception{
		return importResources(resourceMap,new Consumer<Object>() { public void accept(Object arg0) {
				importLocalResources();
			}
		});
	}

	@SuppressWarnings("unchecked")
	protected boolean importResources(Map<Number, Object> resourceMap,Consumer<Object> importLocalResources) throws Exception{
		ResourceMappingForm form=getResourceMapping();



		if (form==null||form.isLocal()){ //claur
				importLocalResources.accept(null);
		}else{
			if (!form.execute()) return false;
			if (form.isLocal()){
				importLocalResources.accept(null);
				return true;
			}

			com.microproject.pm.resource.Resource projectlibre1Resource=null;
			int projectlibre1ResourceCount=0;
			ResourcePool resourcePool = project.getResourcePool();
			project.setTemporaryLocal(true);
			Object srcResource;
			EnterpriseResourceData data;
			Map enterpriseResourceDataMap=new HashMap();
			for (EnterpriseResourceData enterpriseResource:(List<EnterpriseResourceData>)(List<?>)form.getResources()){
				if (enterpriseResource.isLocal()) {
					projectlibre1Resource=ResourceImpl.getUnassignedInstance();
				} else {
					projectlibre1Resource=Serializer.deserializeResourceAndAddToPool(enterpriseResource,resourcePool,null);

					//Handles only flat outlines
					Node node=NodeFactory.getInstance().createNode(projectlibre1Resource);
					resourcePool.addToDefaultOutline(null,node,projectlibre1ResourceCount++,false);
	                ((ResourceImpl)projectlibre1Resource).getGlobalResource().setResourcePool(resourcePool);
				}
				enterpriseResourceDataMap.put(enterpriseResource,projectlibre1Resource);

			}
			for (int i = 0; i < form.getImportedResources().size(); i++) {
				srcResource = form.getImportedResources().get(i);
				data = (EnterpriseResourceData) form.getSelectedResources().get(i);
				projectlibre1Resource=(com.microproject.pm.resource.Resource)enterpriseResourceDataMap.get(data);
				mapResource((long)projectlibre1Resource.getUniqueId(),projectlibre1Resource );
			}

			resourcePool.setMaster(false);
			resourcePool.updateOutlineTypes();

			project.setAccessControlPolicy(form.getAccessControlType());
			project.resetRoles(form.getAccessControlType()==0);


			
		}
		return true;
	}


	protected void retrieveResourcesForMerge(List existingResources) throws Exception{

	}



	protected void importOptions() throws Exception{
		ProjectConverter converter=ProjectConverter.getInstance();
		CalendarOption opOptions=CalendarOption.getInstance();
		CalendarOptions options=plProject.getCalendarOptions();
		converter.convert("op", ProjectConverter.Type.OPTIONS, false, opOptions, options, state);
	}

	private void importProjectFields() {
		OpProjectConverter opConverter=new OpProjectConverter();
		opConverter.to(project, plProject, state);
	}
	
	/**
	 * This method imports all tasks defined in the file into the projectlibre1 model
	 *
	 */
	private void importTasks() {
		final OpTaskConverter converter=new OpTaskConverter();
		plProject.getHierarchy().visit(new Hierarchy.Visitor(){ //pre-order visitor, parents must be treated before children
			@Override
			public void visit(HierarchyNode hierarchyNode) {
				com.microproject.core.nodes.Node node=hierarchyNode.getNode();
				if (!(node instanceof Task)) //ignore assignments present in task hierarchy
					return;
				Task task=(Task)node;
				HierarchyNode parentHierarchyNode=hierarchyNode.getParent();
				Task parentTask=null;
				if (!parentHierarchyNode.isRoot())
					parentTask=(Task)parentHierarchyNode.getNode();

				//op task conversion
				NormalTask opTask=project.newNormalTaskInstance(false);
				opTask.setOwningProject(project);
				opTask.setProjectId(project.getUniqueId());
				converter.to(opTask, task, state);
				
				//op task node conversion
				Node opTaskNode=NodeFactory.getInstance().createNode(opTask);
				
				//op node hierarchy
				NormalTask opParentTask=parentTask==null? null : state.getOpTask(parentTask);
				Node opParentTaskNode=opParentTask==null? null : state.getOpTaskNode(opParentTask);
				project.addToDefaultOutline(opParentTaskNode,opTaskNode);
				
				
				SnapshotList snapshots=task.getSnapshotList();
				for (int snapshotId=0;snapshotId<SnapshotList.BASELINE_COUNT;snapshotId++){
					TaskSnapshot s=snapshots.getSnapshot(snapshotId);
					if (s!=null && s.getStart()!=null && s.getFinish()!=null){
						com.microproject.pm.task.TaskSnapshot opSnapshot=new com.microproject.pm.task.TaskSnapshot();
						opSnapshot.getHasAssignments(); //init hasAssignments
						TaskSchedule schedule=new TaskSchedule();//(TaskSchedule)opTask.getCurrentSchedule().clone();
						schedule.setStart(s.getStart().getTime());
						schedule.setFinish(s.getFinish().getTime());
						opSnapshot.setCurrentSchedule(schedule);
						opTask.setSnapshot(snapshotId, opSnapshot);
					}
				}


				
				state.mapOpTask(task, opTask);
				state.mapOpTaskNode(opTask, opTaskNode);
			}
		});
		

	}
	


	/**
	 * Import dependencies. Must be done after importing tasks
	 *
	 * @throws Exception
	 */
	public void importDependencies() throws Exception {
		// mpxj uses default options when importing link leads and lags, even when mpp format
		CalendarOption oldOptions = CalendarOption.getInstance();
		CalendarOption.setInstance(CalendarOption.getDefaultInstance());


		final OpDependencyConverter converter=new OpDependencyConverter();
		for (com.microproject.pm.tasks.Dependency plDependency : plProject.getDependencies()){
			converter.to(plDependency,state);
		}
		CalendarOption.setInstance(oldOptions);
	}


	/**
	 * Import mpx assignments into projectlibre1 model
	 *
	 */
	protected void importAssignments() {
		OpAssignmentConverter converter=new OpAssignmentConverter();
		for (Task task : plProject.getTasks()){
			NormalTask opTask=state.getOpTask(task);
			for (com.microproject.pm.assignment.Assignment assignment : task.getAssignments()){
				Assignment opAssignment=converter.to(assignment, state);
				AssignmentService.getInstance().connect(opAssignment, null);
			}
			SnapshotList snapshots=task.getSnapshotList();
			for (int snapshotId=0;snapshotId<SnapshotList.BASELINE_COUNT;snapshotId++){
				TaskSnapshot s=snapshots.getSnapshot(snapshotId);
				com.microproject.pm.task.TaskSnapshot opSnapshot=(com.microproject.pm.task.TaskSnapshot)opTask.getSnapshot(snapshotId);
				if (s!=null && opSnapshot!=null){
					for (com.microproject.pm.assignment.Assignment assignment : s.getAssignments()){
						Assignment opAssignment=converter.to(assignment, state);
						opSnapshot.addAssignment(opAssignment);
					}
				}
			}
		}
	}

	private void applyImportedTrackingFields() {
		for (Task task : plProject.getTasks()) {
			NormalTask opTask = state.getOpTask(task);
			if (opTask != null)
				applyImportedTrackingFields(task, opTask);
		}
	}

	void applyImportedTrackingFields(Task task, NormalTask opTask) {
		Number percentComplete = (Number) task.getPropertyValue("percentComplete");
		Number percentWorkComplete = (Number) task.getPropertyValue("percentWorkComplete");
		if (percentComplete != null)
			opTask.setPercentComplete(clampProgress(percentComplete.doubleValue()));
		else if (percentWorkComplete != null)
			opTask.setPercentWorkComplete(clampProgress(percentWorkComplete.doubleValue()));

		Number physicalPercentComplete = (Number) task.getPropertyValue("physicalPercentComplete");
		if (physicalPercentComplete != null)
			opTask.setPhysicalPercentComplete(clampProgress(physicalPercentComplete.doubleValue()));

		Date actualStart = (Date) task.getPropertyValue("actualStart");
		if (actualStart != null)
			opTask.setActualStart(actualStart.getTime());

		Date actualFinish = (Date) task.getPropertyValue("actualFinish");
		if (actualFinish != null && opTask.getPercentComplete() >= 1.0d)
			opTask.setActualFinish(actualFinish.getTime());
	}

	private double clampProgress(double value) {
		return Math.max(0.0d, Math.min(1.0d, value));
	}

	protected double assignmentPercentFactor() {
		return 100.0;
	}




	/**
	 * Currently not implemented
	 */
	public Job getExportFileJob(){
    	Session session=SessionFactory.getInstance().getLocalSession();
		Job job=new Job(session.getJobQueue(),"exportFile","Exporting...",true); //$NON-NLS-1$ //$NON-NLS-2$
    	job.addRunnable(new JobRunnable("Local: export",1.0f){ //$NON-NLS-1$
    		public Object run() throws Exception{
				exportFile();
    			return null;
    		}
    	});
		//session.schedule(job);
    	return job;

	}
	protected void makeValidResourceId(Resource res) {

	}
	protected void mapResource(Number id, Object value) {
//		System.out.println("Mapping res " + id + "   " + value);
		resourceMap.put(id, value);
	}
	public Map<Number, Object> getResourceMap() {
		return resourceMap;
	}
}
