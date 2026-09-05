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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.microproject.util.SafeFileReplace;

import com.microproject.exchange.ImportedCalendarService;
import com.microproject.server.data.linker.Linker;
import com.microproject.server.data.linker.ResourceLinker;
import com.microproject.server.data.linker.TaskLinker;
import net.sf.mpxj.mspdi.MSPDIWriter;
import com.microproject.association.AssociationList;
import com.microproject.configuration.Settings;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.VoidNodeImpl;
import com.microproject.grouping.core.model.NodeModelUtil;
import com.microproject.job.JobRunnable;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.key.uniqueid.UniqueIdException;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ProjectProperties;
import net.sf.mpxj.Relation;
import net.sf.mpxj.RelationType;
import net.sf.mpxj.ResourceAssignment;

/**
 *
 */
public class MSPDISerializer implements ProjectSerializer {
    public static final boolean TMP_FILES=false;
    private static final Logger logger = Logger.getLogger(MSPDISerializer.class.getName());
    protected JobRunnable job=null;
    
    
    
    protected Linker resourceLinker=new ResourceLinker(){
//    	int count = 0; // unassigned should start at 0
    	public Object addTransformedObjects(Object child) throws Exception{
    		Project project=(Project)parent;
    		ProjectFile projectFile=(ProjectFile)transformedParent;
    		ResourceImpl resource=(ResourceImpl)child;
//    		resource.setId(count++); // enumerate them
    		net.sf.mpxj.Resource resourceData=projectFile.addResource();
    		MPXConverter.toMPXResource(resource,resourceData);
    		            
            transformationMap.put(resource,resourceData);
            return resourceData;
    	}
    	public boolean addOutlineElement(Object outlineChild,Object outlineParent,long position){
    		return true;
    	}
    		
    };
    protected Linker taskLinker=new TaskLinker(){
    	public Object addTransformedObjects(Object child) throws Exception{
    		Project project=(Project)parent;
    		ProjectFile projectFile=(ProjectFile)transformedParent;
    		NormalTask task=(NormalTask)child;
			// A linked child project owns these tasks, but the master MPO stores
			// only its SubProject placeholder and reloads the child from its file.
			if (task.isInSubproject() && !project.isOpenedAsSubproject())
				return null;
    		
    		net.sf.mpxj.Task taskData=projectFile.addTask();
    		MPXConverter.toMPXTask(task,taskData);

    		@SuppressWarnings("unchecked")
			Map<ResourceImpl, net.sf.mpxj.Resource> resourceMap=(Map<ResourceImpl, net.sf.mpxj.Resource>)args[0];
            for (int s=0;s<Settings.numBaselines();s++){
                TaskSnapshot snapshot=(TaskSnapshot)task.getSnapshot(Integer.valueOf(s));
                if (snapshot==null) continue;
                AssociationList snapshotAssignments=snapshot.getHasAssignments().getAssignments();
                if (snapshotAssignments.size()>0){
                    for (Iterator j=snapshotAssignments.iterator();j.hasNext();){
                        Assignment assignment=(Assignment)j.next();
                        ResourceImpl r=(ResourceImpl)assignment.getResource();
                        if (s!=Snapshottable.CURRENT.intValue()) continue;
                        net.sf.mpxj.Resource resourceData=(net.sf.mpxj.Resource)resourceMap.get(r);
                        
                        ResourceAssignment assignmentData=taskData.addResourceAssignment(resourceData);
                        if (s==Snapshottable.CURRENT.intValue()){
                        	MPXConverter.toMPXAssignment(assignment,assignmentData);
                        }
                        
                    }
                }
			}
			MPXConverter.toMPXTaskTracking(task, taskData);
			transformationMap.put(task,taskData);
            return taskData;
    	}
    	public boolean addOutlineElement(Object outlineChild,Object outlineParent,long position){
    		if (outlineChild instanceof VoidNodeImpl) // skip void nodes
    			return false;
			Project project = (Project) parent;
			if (project.isOpenedAsSubproject() && project.getTaskOutlineRoot() != null
					&& outlineChild == project.getTaskOutlineRoot().getImpl())
				return false;
			if (outlineChild instanceof Task && ((Task) outlineChild).isInSubproject()
					&& !project.isOpenedAsSubproject())
				return false;
    		net.sf.mpxj.Task taskData=(net.sf.mpxj.Task)getTransformationMap().get(outlineChild);
    		net.sf.mpxj.Task parentData=(outlineParent==null)?null:((net.sf.mpxj.Task)getTransformationMap().get(outlineParent));
			if (taskData == null || (outlineParent != null && parentData == null))
				return false;
   			taskData.setOutlineLevel(Integer.valueOf(((parentData==null)?1:(parentData.getOutlineLevel().intValue()+1)))); // outline levels start at 1
   			//fix from vitaliff
   			//setSummary is normally done in mpxj post processing
   			if (parentData != null) 
   				parentData.setSummary(true);
			taskData.setOutlineNumber(((parentData==null)?"":(parentData.getOutlineNumber()+"."))+(position+1));
			return true;
    	}
        
        
    };
    
    @SuppressWarnings("unchecked")
    protected Map<ResourceImpl, net.sf.mpxj.Resource> saveResources(Project project,ProjectFile projectFile) throws Exception{

		NodeModelUtil.enumerateNonAssignments(project.getResourcePool().getResourceOutline());
    	resourceLinker.setParent(project);
    	resourceLinker.setTransformedParent(projectFile);
    	//resourceLinker.setGlobalIdsOnly(globalIdsOnly);
    	resourceLinker.init();
    	resourceLinker.addTransformedObjects(ResourceImpl.getUnassignedInstance());
    	resourceLinker.addTransformedObjects();
		// New resources can be assigned before the UI has inserted them into a
		// resource outline. They are still valid pool members and must be
		// serialized; otherwise their assignments are exported as unassigned.
		for (com.microproject.pm.resource.Resource resource : project.getResourcePool().getResourceList()) {
			ResourceImpl resourceImpl = (ResourceImpl) resource;
			if (!resourceLinker.getTransformationMap().containsKey(resourceImpl)) {
				resourceLinker.addTransformedObjects(resourceImpl);
			}
		}
    	resourceLinker.addOutline(null);
//    	resourceLinker.getTransformationMap().put(new Long(ResourceImpl.getUnassignedInstance().getUniqueId()),ResourceImpl.getUnassignedInstance());
        return (Map<ResourceImpl, net.sf.mpxj.Resource>) resourceLinker.getTransformationMap();
    }

    protected Map<net.sf.mpxj.Task, Task> saveTasks(Project project,ProjectFile projectFile,Map<ResourceImpl, net.sf.mpxj.Resource> resourceMap) throws Exception{
		NodeModelUtil.enumerateNonAssignments(project.getTaskOutline()); // to fix bug, I moved this before tasks are saved. 16.2.06 hk
    	taskLinker.setParent(project);
    	taskLinker.setTransformedParent(projectFile);
    	//taskLinker.setGlobalIdsOnly(globalIdsOnly);
    	taskLinker.setArgs(new Object[]{resourceMap});
    	taskLinker.init();
    	taskLinker.addTransformedObjects();
		taskLinker.addOutline(project.getTaskOutlineRoot());

    	//dependencies
		// mpxj uses default options when importing link leads and lags
		CalendarOption oldOptions = CalendarOption.getInstance();
		CalendarOption.setInstance(CalendarOption.getDefaultInstance());

		int taskCount = 0;
		Map<Task, net.sf.mpxj.Task> externalTasks=new HashMap<Task, net.sf.mpxj.Task>();
		LinkedList<Object> voidTasksQueue=new LinkedList<>(); // we do not want to export nulls lines at end, so once all tasks done, stop
		// An opened subproject shares the master's outline so it can appear in the
		// consolidated Gantt.  Exporting the whole outline loses its own tasks
		// (they are marked inSubproject) and can accidentally serialize siblings.
		// Start at this project's enclosing SubProj node instead; omit that
		// structural reference itself and retain its concrete child tasks.
		Node exportRoot = project.getTaskOutlineRoot();
		boolean savingOpenedSubproject = exportRoot != null;
		for (Iterator i=project.getTaskOutline().iterator(exportRoot);i.hasNext();){
			Object obj = ((Node)i.next()).getImpl();
    		if (voidTasksQueue.size()>0 && !(obj instanceof VoidNodeImpl)){
    			//insert voids
    			for (Object voidTask:voidTasksQueue){
            		net.sf.mpxj.Task taskData=projectFile.addTask();
            		MPXConverter.toMPXVoid((VoidNodeImpl)voidTask,taskData);
    			}
    			voidTasksQueue.clear();
    		}
    		if (obj instanceof Assignment)
    			continue;
    		if (obj instanceof VoidNodeImpl) {
    			if (taskCount == 0)
    				continue;
    			voidTasksQueue.add(obj);
		} else {
	            Task task=(Task)obj; //ResourceImpl to have the EnterpriseResource link
			if (savingOpenedSubproject && task == exportRoot.getImpl())
				continue;
			if (task.isInSubproject() && !savingOpenedSubproject)
				continue;
//	            task.setUniqueId(task.getId()); // set unique id and id to the same thing on export. Ensures unique id is unique
	            net.sf.mpxj.Task taskData=(net.sf.mpxj.Task)taskLinker.getTransformationMap().get(task);
		        
	            for (Iterator j=task.getPredecessorList().iterator();j.hasNext();){
	            	Dependency dependency=(Dependency)j.next();
	            	Task pred=(Task)dependency.getPredecessor();
	            	net.sf.mpxj.Task predData=(net.sf.mpxj.Task)taskLinker.getTransformationMap().get(pred);
				if (predData==null)
					predData=externalTask(projectFile,externalTasks,pred);
	            	Relation rel=taskData.addPredecessor(predData,RelationType.getInstance(dependency.getDependencyType()),MPXConverter.toMPXDuration(dependency.getLag())); //claur
	            }
	            taskCount++;
    		}
        }
    	
		CalendarOption.setInstance(oldOptions);
        return taskLinker.getTransformationMap();
    }

	private net.sf.mpxj.Task externalTask(ProjectFile projectFile,Map<Task, net.sf.mpxj.Task> externalTasks,Task task) {
		net.sf.mpxj.Task taskData=externalTasks.get(task);
		if (taskData!=null)
			return taskData;
		taskData=projectFile.addTask();
		MPXConverter.toMPXTask((NormalTask)task,taskData);
		taskData.setID(nextMpxTaskIdentifier(projectFile,false));
		taskData.setUniqueID(nextMpxTaskIdentifier(projectFile,true));
		taskData.setOutlineLevel(Integer.valueOf(1));
		taskData.setExternalTask(true);
		String externalProjectReference = task.getExternalProjectFile();
		taskData.setExternalTaskProject(Long.toString(task.getProjectId()));
		taskData.setSubprojectFile(externalProjectReference == null || externalProjectReference.isBlank()
			? Long.toString(task.getProjectId()) : externalProjectReference);
		taskData.setSubprojectTaskID(Integer.valueOf((int)task.getId()));
		externalTasks.put(task,taskData);
		return taskData;
	}

	private Integer nextMpxTaskIdentifier(ProjectFile projectFile,boolean unique) {
		int max=0;
		for (net.sf.mpxj.Task task : projectFile.getTasks()) {
			Integer value=unique ? task.getUniqueID() : task.getID();
			if (value!=null)
				max=Math.max(max,value.intValue());
		}
		return Integer.valueOf(max+1);
	}

    public ProjectFile serializeProject(Project project) throws Exception{
    	return serializeProject(project,false);
    }
	public ProjectFile serializeProject(Project project,boolean globalIdsOnly) throws Exception{
        if (globalIdsOnly) 
        	makeGLobal(project);
        MPXConverter.beginExport();
        try {
	        ProjectFile projectFile = new ProjectFile();
	//this doesn't appear in 2007 version of mpxj        projectData.setMicrosoftProjectCompatibleOutput(true);
	        projectFile.getProjectConfig().setAutoTaskUniqueID(true);
	        projectFile.getProjectConfig().setAutoResourceUniqueID(true);
	        //project
	        ProjectProperties projectHeader=projectFile.getProjectProperties();
	        
			MPXConverter.toMPXOptions(projectHeader);

			MPXConverter.toMPXProject(project,projectHeader);
	        if (job!=null) job.setProgress(0.2f);
	        
	        //calendars
	//        WorkCalendar calendar=project.getWorkCalendar();
	//        if (calendar!=null){
	//            ProjectCalendar calendarData=projectData.addDefaultBaseCalendar();
	//            calendarData.setName(calendar.getName());
	//        }
			projectFile.getProjectConfig().setAutoCalendarUniqueID(true);
			ImportedCalendarService.cleanUp();
			LinkedHashSet<WorkingCalendar> calendars = collectProjectCalendars(project);
			for (WorkingCalendar workCalendar : calendars) {
				ProjectCalendar cal = projectFile.addCalendar();
				MPXConverter.toMpxCalendar(workCalendar, cal);
				ImportedCalendarService.getInstance().addExportedCalendar(cal, workCalendar);
			}
			setDefaultCalendar(project, projectHeader);
	        if (job!=null) job.setProgress(0.3f);
	        
	        //resources
        Map<ResourceImpl, net.sf.mpxj.Resource> resourceMap=saveResources(project,projectFile);
	        if (job!=null) job.setProgress(0.5f);
	        
	        //tasks
	        saveTasks(project,projectFile,resourceMap);
	        if (job!=null) job.setProgress(0.7f);

	        return projectFile;
        } finally {
        	MPXConverter.endExport();
        }
        
    }

	private void setDefaultCalendar(Project project, ProjectProperties projectHeader) {
		WorkCalendar baseCalendar = project.getBaseCalendar();
		if (baseCalendar == null) {
			return;
		}
		ProjectCalendar exportedCalendar = ImportedCalendarService.getInstance().findExportedCalendar(baseCalendar);
		if (exportedCalendar != null) {
			projectHeader.setDefaultCalendar(exportedCalendar);
		}
	}

	private LinkedHashSet<WorkingCalendar> collectProjectCalendars(Project project) {
		LinkedHashSet<WorkingCalendar> calendars = new LinkedHashSet<WorkingCalendar>();
		Set<WorkCalendar> visited = new HashSet<WorkCalendar>();
		if (project == null) {
			return calendars;
		}
		collectCalendar(project.getWorkCalendar(), calendars, visited);
		if (project.getResourcePool() != null) {
			for (com.microproject.pm.resource.Resource resource : project.getResourcePool().getResourceList()) {
				collectCalendar(resource.getWorkCalendar(), calendars, visited);
			}
		}
		for (com.microproject.pm.task.Task task : project.getTaskList()) {
			collectCalendar(task.getWorkCalendar(), calendars, visited);
		}
		return calendars;
	}

	private void collectCalendar(WorkCalendar calendar, LinkedHashSet<WorkingCalendar> calendars, Set<WorkCalendar> visited) {
		if (!(calendar instanceof WorkingCalendar)) {
			return;
		}
		if (!visited.add(calendar)) {
			return;
		}
		WorkingCalendar workingCalendar = (WorkingCalendar) calendar;
		WorkCalendar baseCalendar = workingCalendar.getBaseCalendar();
		if (baseCalendar != null) {
			collectCalendar(baseCalendar, calendars, visited);
		}
		calendars.add(workingCalendar);
	}
    

    
    
    
    public void makeGLobal(DataObject data) throws UniqueIdException{
    	CommonDataObject.makeGlobal(data);
     }
    
	public boolean saveProject(Project project,String fileName) {
		String extension="";
		String name=fileName;
		String tmpFileName=fileName;
		int i=fileName.lastIndexOf('.');
		if (i>0){
			extension=fileName.substring(i);
			name=fileName.substring(0, i);
		}
		
		File file=new File(fileName);
		File tmpFile=file;
		for (int count=0;tmpFile.exists();count++){
			tmpFileName=name+"_tmp"+count+extension;
			tmpFile=new File(tmpFileName);
		}
		
		try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
			if (!saveProject(project, fos) || tmpFile.length()==0){
				// Serialization failed or produced an empty file: discard the
				// partial temp so it cannot accumulate, and keep the original.
				tmpFile.delete();
				Alert.error(Messages.getString("Message.saveError"));
				return false;
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to save MSPDI file " + fileName, e);
			tmpFile.delete();
			Alert.error(Messages.getString("Message.saveError"));
			return false;
		}
		// Replace the original with the temp file only after the move has
		// actually succeeded, so a failed rename can no longer delete the user's
		// data (issue #354). On failure the temp is discarded to avoid an
		// ever-growing pile of _tmpN files.
		if (file.equals(tmpFile)) {
			return true;
		}
		if (!SafeFileReplace.replace(tmpFile, file)) {
			tmpFile.delete();
			Alert.error(Messages.format("Format.join", Messages.getString("Message.saveErrorTmpFile"), tmpFileName));
			return false;
		}
		return true;
	}

	public boolean saveProject(Project project,OutputStream out) {
		try {
			//MSPDISerializer serializer=new MSPDISerializer();
			ProjectFile data=serializeProject(project);
			if (job!=null) job.setProgress(0.9f);
			new MSPDIWriter().write(data,out);
			if (job!=null) job.setProgress(1.0f);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to save ProjectLibre project as MSPDI", e);
			return false;
		}
		return true;
	}

	public JobRunnable getJob() {
		return job;
	}

	public void setJob(JobRunnable job) {
		this.job = job;
	}
	
	

}
