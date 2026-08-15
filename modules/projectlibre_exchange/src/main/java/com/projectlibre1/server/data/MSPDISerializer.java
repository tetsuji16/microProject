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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.projectlibre1.exchange.ImportedCalendarService;
import com.projectlibre1.server.data.linker.Linker;
import com.projectlibre1.server.data.linker.ResourceLinker;
import com.projectlibre1.server.data.linker.TaskLinker;
import com.projectlibre1.server.data.mspdi.ModifiedMSPDIWriter;
import com.projectlibre1.association.AssociationList;
import com.projectlibre1.configuration.Settings;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.grouping.core.VoidNodeImpl;
import com.projectlibre1.grouping.core.model.NodeModelUtil;
import com.projectlibre1.job.JobRunnable;
import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.assignment.Assignment;
import com.projectlibre1.pm.calendar.WorkCalendar;
import com.projectlibre1.pm.calendar.WorkingCalendar;
import com.projectlibre1.pm.dependency.Dependency;
import com.projectlibre1.pm.key.uniqueid.UniqueIdException;
import com.projectlibre1.pm.resource.ResourceImpl;
import com.projectlibre1.pm.snapshot.Snapshottable;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.pm.task.TaskSnapshot;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Alert;

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
    		ModifiedMSPDIWriter projectData=(ModifiedMSPDIWriter)transformedParent;
    		ResourceImpl resource=(ResourceImpl)child;
//    		resource.setId(count++); // enumerate them
    		net.sf.mpxj.Resource resourceData=projectData.getProjectFile().addResource();
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
    		ModifiedMSPDIWriter projectData=(ModifiedMSPDIWriter)transformedParent;
    		NormalTask task=(NormalTask)child;
    		
    		net.sf.mpxj.Task taskData=projectData.getProjectFile().addTask();
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
                 
                        
                        projectData.putOPPrAssignmentMap(assignmentData,assignment);
                        projectData.putOPPrSnapshotIdMap(assignmentData,Integer.valueOf(s));
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
    		net.sf.mpxj.Task taskData=(net.sf.mpxj.Task)getTransformationMap().get(outlineChild);
    		net.sf.mpxj.Task parentData=(outlineParent==null)?null:((net.sf.mpxj.Task)getTransformationMap().get(outlineParent));
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
    protected Map<ResourceImpl, net.sf.mpxj.Resource> saveResources(Project project,ModifiedMSPDIWriter projectData) throws Exception{

		NodeModelUtil.enumerateNonAssignments(project.getResourcePool().getResourceOutline());
    	resourceLinker.setParent(project);
    	resourceLinker.setTransformedParent(projectData);
    	//resourceLinker.setGlobalIdsOnly(globalIdsOnly);
    	resourceLinker.init();
    	resourceLinker.addTransformedObjects(ResourceImpl.getUnassignedInstance());
    	resourceLinker.addTransformedObjects();
    	resourceLinker.addOutline(null);
//    	resourceLinker.getTransformationMap().put(new Long(ResourceImpl.getUnassignedInstance().getUniqueId()),ResourceImpl.getUnassignedInstance());
        return (Map<ResourceImpl, net.sf.mpxj.Resource>) resourceLinker.getTransformationMap();
    }

    protected Map<net.sf.mpxj.Task, Task> saveTasks(Project project,ModifiedMSPDIWriter projectData,Map<ResourceImpl, net.sf.mpxj.Resource> resourceMap) throws Exception{
		NodeModelUtil.enumerateNonAssignments(project.getTaskOutline()); // to fix bug, I moved this before tasks are saved. 16.2.06 hk
    	taskLinker.setParent(project);
    	taskLinker.setTransformedParent(projectData);
    	//taskLinker.setGlobalIdsOnly(globalIdsOnly);
    	taskLinker.setArgs(new Object[]{resourceMap});
    	taskLinker.init();
    	taskLinker.addTransformedObjects();
    	taskLinker.addOutline(null);

    	//dependencies
		// mpxj uses default options when importing link leads and lags
		CalendarOption oldOptions = CalendarOption.getInstance();
		CalendarOption.setInstance(CalendarOption.getDefaultInstance());

		int taskCount = 0;
		LinkedList<Object> voidTasksQueue=new LinkedList<>(); // we do not want to export nulls lines at end, so once all tasks done, stop
    	for (Iterator i=project.getTaskOutline().iterator();i.hasNext();){
    		Object obj = ((Node)i.next()).getImpl();
    		if (voidTasksQueue.size()>0 && !(obj instanceof VoidNodeImpl)){
    			//insert voids
    			for (Object voidTask:voidTasksQueue){
            		net.sf.mpxj.Task taskData=projectData.getProjectFile().addTask();
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
//	            task.setUniqueId(task.getId()); // set unique id and id to the same thing on export. Ensures unique id is unique
	            net.sf.mpxj.Task taskData=(net.sf.mpxj.Task)taskLinker.getTransformationMap().get(task);
	            projectData.putOPPrTaskMap(taskData,task);
		        
	            for (Iterator j=task.getPredecessorList().iterator();j.hasNext();){
	            	Dependency dependency=(Dependency)j.next();
	            	Task pred=(Task)dependency.getPredecessor();
	            	net.sf.mpxj.Task predData=(net.sf.mpxj.Task)taskLinker.getTransformationMap().get(pred);
	            	
	            	Relation rel=taskData.addPredecessor(predData,RelationType.getInstance(dependency.getDependencyType()),MPXConverter.toMPXDuration(dependency.getLag())); //claur
	            }
	            taskCount++;
    		}
        }
    	
		CalendarOption.setInstance(oldOptions);
        return taskLinker.getTransformationMap();
    }

    public ModifiedMSPDIWriter serializeProject(Project project) throws Exception{
    	return serializeProject(project,false);
    }
	public ModifiedMSPDIWriter serializeProject(Project project,boolean globalIdsOnly) throws Exception{
        if (globalIdsOnly) 
        	makeGLobal(project);
        MPXConverter.beginExport();
        try {
	        ModifiedMSPDIWriter projectData=new ModifiedMSPDIWriter();
	        ProjectFile projectFile = new ProjectFile();
	        projectData.setProjectFile(projectFile);
	        
	        projectData.setOPPrProject(project);
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
        Map<ResourceImpl, net.sf.mpxj.Resource> resourceMap=saveResources(project,projectData);
	        if (job!=null) job.setProgress(0.5f);
	        
	        //tasks
	        saveTasks(project,projectData,resourceMap);
	        if (job!=null) job.setProgress(0.7f);

	        return projectData;
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
			for (com.projectlibre1.pm.resource.Resource resource : project.getResourcePool().getResourceList()) {
				collectCalendar(resource.getWorkCalendar(), calendars, visited);
			}
		}
		for (com.projectlibre1.pm.task.Task task : project.getTaskList()) {
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
			if (saveProject(project, fos)
					 && tmpFile.length()>0){
				if (!file.equals(tmpFile)){
					file.delete();
					tmpFile.renameTo(file);
				}
				return true;
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to save MSPDI file " + fileName, e);
		}
		if (file.equals(tmpFile))
			Alert.error(Messages.getString("Message.saveError"));
		else Alert.error(Messages.getString("Message.saveErrorTmpFile")+tmpFileName);
		return false;
	}

	public boolean saveProject(Project project,OutputStream out) {
		try {
			//MSPDISerializer serializer=new MSPDISerializer();
			ModifiedMSPDIWriter data=/*serializer.*/serializeProject(project);
			if (job!=null) job.setProgress(0.9f);
			data.write(data.getProjectFile(),out);
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
