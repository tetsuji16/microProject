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

import com.microproject.util.DataUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.CollectionUtils;

import com.microproject.server.data.linker.Linker;
import com.microproject.server.data.linker.ResourceLinker;
import com.microproject.server.data.linker.TaskLinker;
import com.microproject.association.AssociationList;
import com.microproject.association.InvalidAssociationException;
import com.microproject.company.ApplicationUser;
import com.microproject.company.UserUtil;
import com.microproject.configuration.CircularDependencyException;
import com.microproject.configuration.Configuration;
import com.microproject.configuration.FieldDictionary;
import com.microproject.configuration.Settings;
import com.microproject.field.FieldValues;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.VoidNodeImpl;
import com.microproject.grouping.core.model.DefaultNodeModel;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.key.HasId;
import com.microproject.pm.key.HasKey;
import com.microproject.pm.key.uniqueid.UniqueIdException;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourcePoolFactory;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.server.access.ErrorLogger;
import com.microproject.session.LocalSession;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/**
 *
 */
public class Serializer {
    public static final boolean TMP_FILES=false;
    private static final Logger logger = Logger.getLogger(Serializer.class.getName());

    protected Linker resourceLinker=new ResourceLinker(){
    	public Object addTransformedObjects(Object child) throws IOException, UniqueIdException{
    		Project project=(Project)parent;
    		ResourceImpl resource=(ResourceImpl)child;

    		ResourceData resourceData=new ResourceData();
    		resourceData.setUniqueId(resource.getUniqueId());
    		resourceData.setRole(resource.getRole());
            //ResourceImpl doesn't contain anything. Not serialized in V1
            //ResourceData resourceData=(ResourceData)serialize((ResourceImpl)child,ResourceData.FACTORY,null);

            EnterpriseResourceData enterpriseResourceData;
            if (resource.isDefault())
            	return null;
//        	return transformationMap;//enterpriseResourceData=null;
            else if (project.isMaster()){
            	enterpriseResourceData=(EnterpriseResourceData)serialize(resource.getGlobalResource(),EnterpriseResourceData.FACTORY,null);
            }else{
            	enterpriseResourceData=new EnterpriseResourceData(); //no need to save data
            	enterpriseResourceData.setUniqueId(resource.getGlobalResource().getUniqueId());
            }
            String emailAddress=resource.getGlobalResource().getEmailAddress();
            enterpriseResourceData.setEmailAddress((emailAddress==null||emailAddress.length()==0)?null:emailAddress); //this is used to map a new user to an existing resource
            resourceData.setEnterpriseResource(enterpriseResourceData);

            transformationMap.put(Long.valueOf(resource.getUniqueId()),resourceData); // the resource map uses ids now
            return resourceData;
    	}
    	public void executeFinally(){
    		((ProjectData)getTransformedParent()).setResources(transformed);
    	}
    	public boolean addOutlineElement(Object outlineChild,Object outlineParent,long position){
			if (outlineChild instanceof VoidNodeImpl) return false;
		ResourceData resourceData=(ResourceData)getTransformationMap().get(Long.valueOf(((Resource)outlineChild).getUniqueId()));
			ResourceData parentData=(outlineParent==null)?null:(ResourceData)getTransformationMap().get(Long.valueOf(((Resource)outlineParent).getUniqueId()));
			EnterpriseResourceData enterpriseResourceData=resourceData.getEnterpriseResource(); //enterprise resource version
			enterpriseResourceData.setParentResource((parentData==null)?null:parentData.getEnterpriseResource()); //enterprise resource version
			enterpriseResourceData.setChildPosition(position); //enterprise resource version
			return true;
    	}

    };
    @SuppressWarnings("unchecked")
    public Map<Long, ResourceData> saveResources(Project project,ProjectData projectData) throws Exception{
        resourceLinker.setParent(project);
    	resourceLinker.setTransformedParent(projectData);
    	resourceLinker.init();
    	resourceLinker.addTransformedObjects();
    	resourceLinker.addOutline(null); // root is null
        return (Map<Long, ResourceData>) resourceLinker.getTransformationMap();
    }
    
    public static interface AssignmentClosure{
    	public void execute(Assignment assignment,int snapshotId) throws IOException;
    }
    public static void forAssignments(NormalTask task,AssignmentClosure c) throws IOException{
        for (int s=0;s<Settings.numBaselines();s++){
            TaskSnapshot snapshot=(TaskSnapshot)task.getSnapshot(Integer.valueOf(s));
            if (snapshot==null) continue;
            AssociationList snapshotAssignments=snapshot.getHasAssignments().getAssignments();
            if (snapshotAssignments.size()>0){
                for (Object assignment : snapshotAssignments){
                    c.execute((Assignment)assignment,s);
                }
            }
        }
    }


    protected TaskLinker taskLinker=new TaskLinker(){
    	public Object addTransformedObjects(Object child) throws IOException, UniqueIdException{
    		//Project project=(Project)parent;
    		//ProjectData projectData=(ProjectData)transformedParent;
    		NormalTask task=(NormalTask)child;
    		final Project project = ((Project)getParent());
	        if (task.getOwningProject() != project||task.isExternal()) // don't do tasks in subprojects, dont include externals
	        	return null;
    		@SuppressWarnings("unchecked")
			final Map<Long, ResourceData> resourceMap=(Map<Long, ResourceData>)args[0];
            final TaskData taskData;
            final boolean taskDirty=!incremental||task.isDirty();
            if (taskDirty/*||Environment.isNoPodServer()*/) { //claur
//            	if (Environment.isNoPodServer()){
//            		final List persistedAssignments=new Vector();
//                    Project.forAssignments(task, new Project.AssignmentClosure(){
//                    	public void execute(Assignment assignment,int s){
//        						ResourceImpl r=(ResourceImpl)assignment.getResource();
//        						//if (r.isDefault()&&s==Snapshottable.CURRENT){
//        						if (r.isDefault()) persistedAssignments.add(new PersistedAssignment(assignment,s));//save the default assignment in the task
//        						else if (s!=Snapshottable.CURRENT){
//        							persistedAssignments.add(new PersistedAssignment(assignment,s,r.getUniqueId()));
//        						}
//                    	}
//                    });
//                    if (persistedAssignments.size()>0)
//                    	task.setPersistedAssignments(persistedAssignments);
//            	}
            	taskData=(TaskData)serialize(task,TaskData.FACTORY,null);
            	//task.setPersistedAssignments(null); //claur

    	        taskData.setNotes(task.getNotes()); //assignments notification
// this code is to set fields which are exposed in database
//    	        taskData.setStart(task.getStart());
//    	        taskData.setFinish(task.getEnd());
//    	        taskData.setBaselineStart(task.getBaselineStartOrZero());
//    	        taskData.setBaselineFinish(task.getBaselineFinishOrZero());
//    	        taskData.setCompletedThrough(task.getCompletedThrough());
//    	        taskData.setPercentComplete(task.getPercentComplete());
    	       // if (!taskDirty&&Environment.isNoPodServer()) taskData.setSerialized(null); //claur
            }
            else{
            	taskData=new TaskData();
            	taskData.setUniqueId(task.getUniqueId());
//            	getUnchanged().add(task.getUniqueId());
//            	return null;
            }
	        // set the status of the task using dirty flag
	        taskData.setStatus(taskDirty ? SerializedDataObject.UPDATE : 0);

        	taskData.setProjectId(task.getProjectId());
	        if (task.isSubproject()) {
	        	taskData.setSubprojectId(((SubProj)task).getSubprojectUniqueId());
	        }


            //assignments
            final Collection<AssignmentData> assignments=(flatAssignments==null)?new ArrayList<AssignmentData>():flatAssignments;
            if (taskDirty)
            forAssignments(task, new AssignmentClosure(){ //claur
                public void execute(Assignment assignment,int s) throws IOException {
						ResourceImpl r=(ResourceImpl)assignment.getResource();
						AssignmentData assignmentData=(AssignmentData)serialize(assignment,AssignmentData.FACTORY,null);
						assignmentData.setStatus(SerializedDataObject.UPDATE);

						if (flatAssignments==null) assignmentData.setTask(taskData);
						else assignmentData.setTaskId(taskData.getUniqueId());
						EnterpriseResourceData enterpriseResourceData=(r.isDefault())?
						    	null:
								resourceMap.get(Long.valueOf(r.getUniqueId())).getEnterpriseResource();
						if (flatAssignments==null) assignmentData.setResource(enterpriseResourceData);
						else assignmentData.setResourceId((enterpriseResourceData==null)?-1L:enterpriseResourceData.getUniqueId());
						assignmentData.setSnapshotId(s);

						assignmentData.setCachedStart(new Date(assignment.getStart()));
						assignmentData.setCachedEnd(new Date(assignment.getEnd()));
						assignmentData.setTimesheetStatus(assignment.getTimesheetStatus());
						assignmentData.setLastTimesheetUpdate(new Date(assignment.getLastTimesheetUpdate()));
						assignmentData.setWorkflowState(assignment.getWorkflowState());
						assignmentData.setPercentComplete(assignment.getPercentComplete()); //assignments notification
						assignmentData.setDuration(assignment.getDuration()); //assignments notification

						assignments.add(assignmentData);
            	}
            });
            if (flatAssignments==null) taskData.setAssignments(assignments);
//~            taskData.setStart(new Date(task.getStart()));
//~            taskData.setEnd(new Date(task.getEnd()));

            transformationMap.put(task,taskData);
            return taskData;
    	}
    	public void executeFinally(){
    		((ProjectData)getTransformedParent()).setTasks(transformed);
    	}
    	public boolean addOutlineElement(Object outlineChild,Object outlineParent,long position){
			TaskData taskData=(TaskData)getTransformationMap().get(outlineChild);

			//voidNodes
			if (outlineChild instanceof VoidNodeImpl){
				taskData=new TaskData();
				@SuppressWarnings("unchecked")
				Collection<TaskData> parentTasks=(Collection<TaskData>)(Collection<?>)((ProjectData)getTransformedParent()).getTasks();
				parentTasks.add(taskData);
			}

			if (taskData == null) // in case belongs to different project
				return false;

			TaskData parentData=(outlineParent==null)?null:((TaskData)getTransformationMap().get(outlineParent));
//			System.out.println("parent "+parentData);
			if (parentData != null && parentData.isSubproject()) {
//				System.out.println("sub " + parentData.getName());
				parentData = null;
			}
			//if (taskData.isDirty()){
				taskData.setParentTask(parentData);
				taskData.setChildPosition(position);
			//}
			if (outlineChild instanceof Task){
				Task task=(Task)outlineChild;
				long parentId=parentData==null?-1L:parentData.getUniqueId();
				if (parentId!=task.getLastSavedParentId()||position!=task.getLastSavedPosistion()) taskData.setMoved(true);
			}

			return true;
    	}


    };

    private void markAncestorsOfDirtyTasksDirty(Project project) {
    	for(Task task : project.getTaskList()) {
    		if(task.isDirty()) {
    			Task parent = task.getWbsParentTask();
    			while(parent != null && !parent.isDirty()) {
    				parent.setDirty(true);
    				parent = parent.getWbsParentTask();
    			}

    		}
    	}
    }

    //flatAssignments and flatLinks mustn't be null if incremental
    protected void saveTasks(Project project,ProjectData projectData,Map<Long, ResourceData> resourceMap,Collection<DataObject> flatAssignments,Collection<DataObject> flatLinks,boolean incremental,SerializeOptions options) throws Exception{
    	ArrayList<Long> unchangedTasks = null;
    	ArrayList<Long> unchangedLinks = null;
    	if (incremental){
		unchangedTasks=new ArrayList<Long>(project.getTaskList().size());
		unchangedLinks=new ArrayList<Long>(project.getTaskList().size());
    		//taskLinker.setUnchanged(unchangedTasks);
    	}
    	this.markAncestorsOfDirtyTasksDirty(project);

    	taskLinker.setIncremental(incremental);
    	taskLinker.setFlatAssignments(flatAssignments);
    	taskLinker.setParent(project);
    	taskLinker.setTransformedParent(projectData);
    	//taskLinker.setGlobalIdsOnly(globalIdsOnly);
            taskLinker.setArgs(new Object[]{resourceMap});
    	taskLinker.init();
    	taskLinker.setOptions(options);
		taskLinker.addTransformedObjects();
		taskLinker.addOutline(project.getTaskOutlineRoot());
		long projectId = project.getUniqueId();
		Collection<TaskData> taskDataCollection=getTaskDataCollection(projectData);
		Map<Task, TaskData> externalTaskData=new HashMap<Task, TaskData>();
        //dependencies
        //Count depCount=new Count("Dependencies");
        for (Iterator<?> i=project.getTaskOutlineIterator();i.hasNext();){
            NormalTask task=(NormalTask)i.next(); //ResourceImpl to have the EnterpriseResource link
            if (task.getProjectId() != projectId||task.isExternal()) // skip if in another project, don't write externals to server
            	continue;
	        TaskData taskData=(TaskData)taskLinker.getTransformationMap().get(task);
	        if (taskData == null)
	        	continue;

            Iterator<?> j=task.getPredecessorList().iterator();
	        if (j.hasNext()){
	            List<LinkData> predecessors=new ArrayList<>(task.getPredecessorList().size());
	            while (j.hasNext()){
	                Dependency dependency=(Dependency)j.next();
	                LinkData linkData;
	                boolean dirty=!incremental||dependency.isDirty();
	                if (dirty) {
	                	linkData=(LinkData)serialize(dependency,LinkData.FACTORY,null);
	                }
	                else{
	                	//linkData=new LinkData();
	                	unchangedLinks.add(dependency.getPredecessorId());
	                	unchangedLinks.add(dependency.getSuccessorId());
	                	continue;
	                }
                	linkData.setDirty(dependency.isDirty());
	                //linkData.setExternalId(dependency.getExternalId());

	                if (flatLinks==null)
	                	linkData.setSuccessor(taskData);
	                else
	                	linkData.setSuccessorId(taskData.getUniqueId());

	                Task pred=(Task)dependency.getPredecessor();
	                TaskData predData=(TaskData)taskLinker.getTransformationMap().get(pred);

	                if (flatLinks==null){
		                if (predData != null && !predData.isExternal())
		                	linkData.setPredecessor(predData);
		                else {
					linkData.setPredecessor(externalTaskData(pred, externalTaskData, taskDataCollection));
		                }
		                predecessors.add(linkData);
	                } else {
	                	linkData.setPredecessorId(pred.getUniqueId());
	                	flatLinks.add(linkData);

	                }
	            }
	            if (flatLinks==null)
		            taskData.setPredecessors(predecessors);

	        }
			if (flatLinks==null){
				for (Iterator<?> successorIterator=task.getSuccessorList().iterator();successorIterator.hasNext();){
					Dependency dependency=(Dependency)successorIterator.next();
					Task successor=(Task)dependency.getSuccessor();
					TaskData successorData=(TaskData)taskLinker.getTransformationMap().get(successor);
					if (successorData != null && !successorData.isExternal())
						continue;
					LinkData linkData=(LinkData)serialize(dependency,LinkData.FACTORY,null);
					linkData.setDirty(dependency.isDirty());
					linkData.setPredecessor(taskData);
					linkData.setSuccessor(externalTaskData(successor, externalTaskData, taskDataCollection));
					linkData.getSuccessor().addPredecessor(linkData);
				}
			}

        }

        //depCount.dump();

    	if (incremental){
    		//if (unchangedTasks.size()>0){
	        Collection<TaskData> tasks=getTaskDataCollection(projectData);
    		if (tasks!=null)
    		for(Iterator<TaskData> i=tasks.iterator();i.hasNext();){
    			TaskData t=i.next();
    			if (!t.isDirty()&&!t.isMoved()){
    				unchangedTasks.add(t.getUniqueId());
    				i.remove();
    			}
    		}
    		if (unchangedTasks.size()>0){
    			long[] a=new long[unchangedTasks.size()];
    			int i=0;
    			for (long l:unchangedTasks) a[i++]=l;
    			projectData.setUnchangedTasks(a);
    		}
    		if (unchangedLinks.size()>0){
    			long[] a=new long[unchangedLinks.size()];
    			int i=0;
    			for (long l:unchangedLinks) a[i++]=l;
    			projectData.setUnchangedLinks(a);
    		}
    	}

        return; //taskLinker.getTransformationMap();
    }

	private TaskData externalTaskData(Task task,Map<Task, TaskData> externalTaskData,Collection<TaskData> taskDataCollection) throws IOException{
		TaskData taskData=externalTaskData.get(task);
		if (taskData!=null)
			return taskData;
		taskData=(TaskData)serialize(task,TaskData.FACTORY,null);
		taskData.setExternal(true);
		taskData.setProjectId(task.getProjectId());
		taskData.setChildPosition(taskDataCollection.size());
		externalTaskData.put(task,taskData);
		taskDataCollection.add(taskData);
		return taskData;
	}

    public DocumentData serializeDocument(Project project) throws Exception{
    	return serializeProject(project,null,null,false,null);
    }

    public ProjectData serializeProject(Project project) throws Exception{
    	return serializeProject(project,null,null,false,null);
    }
    public ProjectData serializeProject(Project project,Collection<DataObject> flatAssignments,Collection<DataObject> flatLinks,boolean incremental,SerializeOptions options) throws Exception{
    	if (TMP_FILES) initTmpDir();
    	if (project.isForceNonIncremental()) incremental=false;
    	boolean incrementalDistributions=incremental&&!project.isForceNonIncrementalDistributions();

 //   	calendars.clear();
        Count projectCount=new Count("Project");
        //if (globalIdsOnly) makeGLobal(project);
        ProjectData projectData=(ProjectData)serialize(project,ProjectData.FACTORY,projectCount);
        if (project.isForceNonIncremental()) projectData.setVersion(0);
        projectData.setMaster(project.isMaster());
//        projectData.setExternalId(project.getExternalId());

        //exposed attributes
//        projectData.setAttributes(SpreadSheetFieldArray.convertFields(project, "projectExposed", new Transformer(){
//        	public Object transform(Object value) {
//        		if (value instanceof Money) return ((Money)value).doubleValue();
//        		return null;
//        	}
//        }));

        projectCount.dump();


        //resources
        Map<Long, ResourceData> resourceMap=saveResources(project,projectData);

        //tasks
        saveTasks(project,projectData,resourceMap,flatAssignments,flatLinks,incremental,options);

        //distribution
        long t=System.currentTimeMillis();
        Collection<DistributionData> dist=(Collection<DistributionData>)(new DistributionConverter()).createDistributionData(project,incrementalDistributions);
    	if (dist==null){
    		dist=new ArrayList<DistributionData>();
    	}
		projectData.setDistributions(dist);
		projectData.setIncrementalDistributions(incrementalDistributions);

    	TreeMap<DistributionData, DistributionData> distMap=project.getDistributionMap();
    	if (distMap==null){
    		distMap=new TreeMap<DistributionData, DistributionData>(new DistributionComparator());
    		project.setDistributionMap(distMap);
    	}
    	TreeMap<DistributionData, DistributionData> newDistMap=new TreeMap<DistributionData, DistributionData>(new DistributionComparator());
    	//ArrayList<DistributionData> toInsertInOld=new ArrayList<DistributionData>();

    	//insert, update dist
    	for (Iterator<DistributionData> i=dist.iterator();i.hasNext();){
    		DistributionData d=i.next();
    		if (incrementalDistributions){
	    		DistributionData oldD=distMap.get(d);
	    		if (oldD==null){
	    			d.setStatus(DistributionData.INSERT);
	    		}else{
	    			if (oldD.getWork()==d.getWork()&&oldD.getCost()==d.getCost()){
	    				//System.out.println(d+" did not change");
	    				d.setStatus(0);
	    				i.remove();
	    			}
	    			else d.setStatus(DistributionData.UPDATE);
	    		}
    		}else{
    			d.setStatus(DistributionData.INSERT);
    		}
			newDistMap.put(d,d);
    	}
    	//remove dist
    	if (incrementalDistributions&&distMap.size()>0){
        	Set<Long> noChangeTaskIds=new HashSet<Long>();

			Task task;
			for(Iterator i = project.getTaskOutlineIterator();i.hasNext();) {
				task = (Task)i.next();
				if(incremental&&!task.isDirty()) noChangeTaskIds.add(task.getUniqueId());
			}
//        	for (Iterator i=projectData.getTasks().iterator();i.hasNext();){
//        		TaskData task=(TaskData)i.next();
//        		if (!task.isDirty()) noChangeTaskIds.add(task.getUniqueId());
//        	}
        	for (Iterator<DistributionData> i=distMap.values().iterator();i.hasNext();){
        		DistributionData d=i.next();
        		if (newDistMap.containsKey(d)) continue;
        		if (noChangeTaskIds.contains(d.getTaskId())){
        			d.setStatus(0);
        			newDistMap.put(d, d);
        		}else{
        			d.setStatus(DistributionData.REMOVE);
        			dist.add(d);
        		}
        	}
    	}
    	project.setNewDistributionMap(newDistMap);
    	logger.log(Level.INFO, "Distributions generated in {0} ms", System.currentTimeMillis() - t);


    	// send project field values to server too
        HashMap fieldValues = FieldValues.getValues(FieldDictionary.getInstance().getProjectFields(),project);
        if (project.getContainingSubprojectTask() != null) { // special case in which we want to use the duration from subproject task
        	Object durationFieldValue = Configuration.getFieldFromId("Field.duration").getValue(project.getContainingSubprojectTask(), null);
        	fieldValues.put("Field.duration", durationFieldValue);
        }
        projectData.setFieldValues(fieldValues);
        projectData.setGroup(project.getGroup());
        projectData.setDivision(project.getDivision());
        projectData.setExpenseType(project.getExpenseType());
        projectData.setProjectType(project.getProjectType());
        projectData.setProjectStatus(project.getProjectStatus());
        projectData.setExtraFields(project.getExtraFields());
        projectData.setCustomReportPresets(project.getCustomReportPresets());
        projectData.setAccessControlPolicy(project.getAccessControlPolicy());
        projectData.setCreationDate(project.getCreationDate());
        projectData.setLastModificationDate(project.getLastModificationDate());
		Collection<DataObject> referringSubprojectTasks = new ArrayList<DataObject>();
		for (Object value : project.getReferringSubprojectTasks()) {
			if (!(value instanceof Task))
				continue;
			Task referringTask = (Task)value;
			TaskData referringTaskData = (TaskData)serialize(referringTask, TaskData.FACTORY, null);
			referringTaskData.setNotes(referringTask.getNotes());
			referringTaskData.setProjectId(referringTask.getProjectId());
			if (referringTask.isSubproject())
				referringTaskData.setSubprojectId(((SubProj)referringTask).getSubprojectUniqueId());
			referringSubprojectTasks.add(referringTaskData);
		}
		projectData.setReferringSubprojectTasks(referringSubprojectTasks);
        //  	System.out.println("done serialize project " + project);

//        Collection<DistributionData> dis=(Collection<DistributionData>)projectData.getDistributions();
//        for (DistributionData d: dis) System.out.println("Dist: "+d.getTimeId()+", "+d.getType()+", "+d.getStatus());

//        project.setNewTaskIds(null);
//        if (projectData.getTasks()!=null){
//        	Set<Long> ids=new HashSet<Long>();
//        	project.setNewTaskIds(ids);
//        	for (TaskData task:(Collection<TaskData>)projectData.getTasks()){
//        		ids.add(task.getUniqueId());
//        	}
//        }
//        long[] unchangedTasks=projectData.getUnchangedTasks();
//        if (unchangedTasks!=null){
//        	Set<Long> ids=project.getNewTaskIds();
//        	if (ids==null){
//        		ids=new HashSet<Long>();
//        		project.setNewTaskIds(ids);
//        	}
//        	for (int i=0;i<unchangedTasks.length;i++) ids.add(unchangedTasks[i]);
//        }
//
//        project.setNewLinkIds(null);
//        if (flatLinks!=null){
//        	Set<DependencyKey> ids=new HashSet<DependencyKey>();
//        	project.setNewLinkIds(ids);
//        	for (LinkData link:(Collection<LinkData>)flatLinks){
//        		ids.add(new DependencyKey(link.getPredecessorId(),link.getSuccessorId()/*,link.getExternalId()*/));
//        	}
//        }
//        long[] unchangedLinks=projectData.getUnchangedLinks();
//        if (unchangedLinks!=null){
//        	Set<DependencyKey> ids=project.getNewLinkIds();
//        	if (ids==null){
//        		ids=new HashSet<DependencyKey>();
//        		project.setNewLinkIds(ids);
//        	}
//        	for (int i=0;i<unchangedLinks.length;i+=2) ids.add(new DependencyKey(unchangedLinks[i],unchangedLinks[i+1]));
//        }


        //project.setNewIds(); //claur - useful ?

        return projectData;

    }
//    public ProjectData serializeResources(Project project) throws Exception{
//    	//if (globalIdsOnly) makeGLobal(project);
//    	ProjectData projectData=(ProjectData)serialize(project,ProjectData.FACTORY,null);
//        //projectData just here to hold resources
//        saveResources(project,projectData);
//        return projectData;
//    }



   //incremental serialization
/*
    public IncrementalData serializeIncrementalProject(Project project) throws Exception{
        if (!project.isGroupDirty()) return null;
        final IncrementalData newData=new IncrementalData();
        final IncrementalData oldData=(IncrementalData)project.getPublishedData().clone();
    	ProjectData projectData=serializeProject(project,null,null);
    	@SuppressWarnings("unchecked")
    	final Set<AssignmentData> oldAssignments = oldData.getAssignments();
    	@SuppressWarnings("unchecked")
    	final Set<LinkData> oldLinks = oldData.getLinks();
    	@SuppressWarnings("unchecked")
    	final Set<ResourceData> oldResources = oldData.getResources();
    	final Map<TaskData, TaskData> oldTasks = oldData.getTasks();

    	for (TaskData t : projectData.getTasks()){
    		for (AssignmentData data : t.getAssignments()){
        		if (oldAssignments.contains(data)){
        			if (data.isDirty()){
        				newData.addAssignment(data);
        			}
        			oldAssignments.remove(data);
        		}else{
        			data.setStatus(SerializedDataObject.INSERT);
    				newData.addAssignment(data);
        		}
    		}
    		for (LinkData data : t.getPredecessors()){
        		if (oldLinks.contains(data)){
        			if (data.isDirty()){
        				newData.addLink(data);
        			}
        			oldLinks.remove(data);
        		}else{
        			data.setStatus(SerializedDataObject.INSERT);
    				newData.addLink(data);
        		}
    		}

    		if (oldTasks.containsKey(t)){
    			TaskData oldT=oldTasks.get(t);
    			if (t.getParentTaskId()==oldT.getParentTaskId()&&t.getCalendarId()==t.getChildPosition()){
        			if (t.isDirty()){
        				newData.addTask(t);
        			}
    			}else{
    				t.setStatus(t.getStatus()|SerializedDataObject.MOVE);
    				newData.addTask(t);
    			}
    			oldTasks.remove(t);
    		}else{
    			t.setStatus(SerializedDataObject.INSERT);
				newData.addTask(t);
    		}
    	}
    	for (ResourceData r : projectData.getResources()){
    		if (oldResources.contains(r)){
    			if (r.isDirty()){
    				newData.addResource(r);
    			}
    			oldResources.remove(r);
    		}else{
    			r.setStatus(SerializedDataObject.INSERT);
				newData.addResource(r);
    		}
    	}

    	//REMOVE
    	for (ResourceData data:oldResources){
    		data.setStatus(SerializedDataObject.REMOVE);
    		newData.addResource(data);
    	}
    	for (TaskData data:oldTasks.keySet()){
    		data.setStatus(SerializedDataObject.REMOVE);
    		newData.addTask(data);
    	}
    	for (AssignmentData data:oldAssignments){
    		data.setStatus(SerializedDataObject.REMOVE);
    		newData.addAssignment(data);
    	}
    	for (LinkData data:oldLinks){
    		data.setStatus(SerializedDataObject.REMOVE);
    		newData.addLink(data);
    	}




		if (oldResources.contains(projectData)){
			if (projectData.isDirty()){
//				System.out.println("UPDATE: "+projectData);
				newData.setProject(projectData);
			}
		}
        return newData;
    }
 */

    //deserialization

    public Project deserializeLocalDocument(DocumentData documentData) throws IOException, ClassNotFoundException {
    	Session local = SessionFactory.getInstance().getLocalSession();
    	if (local instanceof LocalSession) ((LocalSession) local).resetLocalSeed(); // issue #227/#268: deterministic local ids
    	return deserializeProject((ProjectData)documentData,false,local,null,null);
    }

    /**
     * enterpriseResources to use instead of enterprise resources given by projectData
     */
    public Project deserializeProject(ProjectData projectData, final boolean subproject, final Session reindex, Map<Long, EnterpriseResourceData> enterpriseResources) throws IOException, ClassNotFoundException {
    	return deserializeProject(projectData, subproject, reindex, enterpriseResources,null,true);
    }
    public Project deserializeProject(ProjectData projectData, final boolean subproject, final Session reindex, Map<Long, EnterpriseResourceData> enterpriseResources,Consumer<Object> loadResources) throws IOException, ClassNotFoundException {
    	return deserializeProject(projectData, subproject, reindex, enterpriseResources,loadResources,true);
    }

    //DEF165936: 	.pod file import fails mapped to resource with modified calendar
    //the only way i found to make this work was to pass over the original ResourceImpls mapped by selected resource Id
//    private Project _existingProject = null;
//    Map<Long, Resource> _localResourceMap;
//    public void SetStuffForPODDeserialization(Project existingProject, Map<Long, Resource> localResourceMap)
//    {
//    	_existingProject = existingProject;
//    	_localResourceMap = localResourceMap;
//    }
    public Project deserializeProject(ProjectData projectData, final boolean subproject, final Session reindex, Map<Long, EnterpriseResourceData> enterpriseResources,Consumer<Object> loadResources,boolean updateDistribution) throws IOException, ClassNotFoundException {
    	DataFactoryUndoController undoController=new DataFactoryUndoController();
    	Project project=(Project)deserialize(projectData,reindex);
    	// Issue #227: for a local document the project's uniqueId is part of its persistent
    	// identity and is re-persisted into fieldValues["Field.uniqueId"]. Deserialize mints a
    	// fresh LocalSession id (localSeed++) above, so reloading would change the id every time
    	// and permanently grow the .pod. Restore the id stored in the file instead.
    	if (projectData.isLocal() && projectData.getUniqueId() > 0) {
    		project.setUniqueId(projectData.getUniqueId());
    	}
    	project.setUndoController(undoController);
    	project.setMaster(projectData.isMaster()); //not necessary
    	project.setLocal(projectData.isLocal());
    	project.setReadOnly(!projectData.canBeUsed());
    	project.setCreationDate(projectData.getCreationDate());
    	project.setLastModificationDate(projectData.getLastModificationDate());
    	//project.setExternalId(projectData.getExternalId());
    	boolean fixCorruption=false;

    	//IncrementalData incremental=new IncrementalData();

    	//calendar
//  	WorkCalendar calendar = project.getWorkCalendar();
//  	if (projectData.getCalendar()==null) {
//  	System.out.println("deserializing null project calendar");
//  	calendar= CalendarService.getInstance().getStandardBasedInstance(project);
//  	} else {
//  	calendar= (WorkingCalendar)deserializeCalendar(projectData.getCalendar());
//  	calendar.setDocument(project);
//  	CalendarService.getInstance().add(calendar);
//  	}
//  	CalendarService.getInstance().add((WorkingCalendar) calendar);


    	WorkCalendar calendar=project.getWorkCalendar();
    	if (calendar==null)
    		calendar = CalendarService.getInstance().getDefaultInstance();

    	project.setWorkCalendar(calendar); // needed for objects using
    	project.setExtraFields(projectData.getExtraFields());

    	project.setGroup(projectData.getGroup());
    	project.setDivision(projectData.getDivision());
    	project.setExpenseType(projectData.getExpenseType());
    	project.setProjectType(projectData.getProjectType());
    	project.setProjectStatus(projectData.getProjectStatus());
    	project.setAccessControlPolicy(projectData.getAccessControlPolicy());
		project.getCustomReportPresets().clear();
		if (projectData.getCustomReportPresets() != null)
			project.getCustomReportPresets().putAll(projectData.getCustomReportPresets());

    	project.postDeserialization();


    	//resources
	    ResourcePool resourcePool = ResourcePoolFactory.getInstance().createResourcePool(project.getName(),undoController);
    	resourcePool.setMaster(project.isMaster());
    	resourcePool.setLocal(project.isLocal());
    	resourcePool.updateOutlineTypes();
	    Collection<ResourceData> resources=(Collection<ResourceData>)(Collection<?>)projectData.getResources();
    final Map<Object, Node> resourceNodeMap = resources == null ? new HashMap<>() : new HashMap<>(resources.size() * 4 / 3 + 1);
    	if (resources!=null)
    		for (ResourceData resourceData:sortResourcesByChildPosition(resources)){
    			ResourceImpl resource=deserializeResourceAndAddToPool(resourceData,resourcePool,reindex,enterpriseResources);
    			
       			//Change for DEF165936 but doesn't work
    			//Resource origImpl =  _localResourceMap.get(resourceData.getUniqueId());
    			//resourceNodeMap.put(resourceData.getEnterpriseResource(),NodeFactory.getInstance().createNode(origImpl));
     			resourceNodeMap.put(resourceData.getEnterpriseResource(),NodeFactory.getInstance().createNode(resource));
    		}
    	project.setResourcePool(resourcePool);

    	//resource outline
    	/* version with outline on project resource
    	 * if (resources!=null){
            for (Iterator i=resources.iterator();i.hasNext();){
                ResourceData resourceData=(ResourceData)i.next();
                ResourceData parentData=(ResourceData)resourceData.getParentResource();
                Node node=(Node)resourceNodeMap.get(resourceData.getEnterpriseResource());
                Node parentNode=(parentData==null)?
                		null:
                		((Node)resourceNodeMap.get(parentData.getEnterpriseResource()));
                project.getResourcePool().addToDefaultOutline(parentNode,node,(int)resourceData.getChildPosition());
            }
        }*/
    	if (resources!=null){

    		for (ResourceData resourceData:resources){
    			EnterpriseResourceData enterpriseResourceData=resourceData.getEnterpriseResource();
    			EnterpriseResourceData parentData=enterpriseResourceData.getParentResource();
    			Node node=resourceNodeMap.get(enterpriseResourceData);
    			Node parentNode=(parentData==null)?
    					null:
    						resourceNodeMap.get(parentData);
    			project.getResourcePool().addToDefaultOutline(parentNode,node,(int)enterpriseResourceData.getChildPosition(),false);
    			((ResourceImpl)node.getImpl()).getGlobalResource().setResourcePool(project.getResourcePool());
    		}
    		project.getResourcePool().getResourceOutline().getHierarchy().cleanVoidChildren();

    		//renumber resources
    		project.getResourcePool().getResourceOutline().getHierarchy().visitAll(new Consumer<Object>(){
    			int id=1;
    			public void accept(Object o) {
    				Node node=(Node)o;
    				if (node.getImpl() instanceof HasId){
    					HasId impl=(HasId)node.getImpl();
    					if (impl.getId()>0) impl.setId(id++); //if id=0 means id not used
    				}
    			}

    		});
    	}

    	if (loadResources!=null){
    		loadResources.accept(project);
    		resourceNodeMap.clear();
    		project.getResourcePool().getResourceOutline().getHierarchy().visitAll(new Consumer<Object>() { public void accept(Object o) {
    				Node node=(Node)o;
    				resourceNodeMap.put(((ResourceImpl)node.getImpl()).getGlobalResource(), node);
    			}

    		});
    	}


    	//tasks
    	Collection<TaskData> tasks=getTaskDataCollection(projectData);
    Map<TaskData, Node> taskNodeMap = tasks == null ? new HashMap<>() : new HashMap<>(tasks.size() * 4 / 3 + 1);
    	long projectId = project.getUniqueId();
    	NormalTask task;

    	if (tasks!=null){
    		//Set<Long> initialTaskIds=new HashSet<Long>();
    		//project.setInitialTaskIds(initialTaskIds);
    		for (TaskData taskData:sortTasksByChildPosition(tasks)){
    			task = null;
//  			initialTaskIds.add(taskData.getUniqueId());
    			if (taskData.isDirty()) fixCorruption=true; //recovers errors
//    			if (Environment.isAddSummaryTask()&&taskData.getUniqueId()==Task.SUMMARY_UNIQUE_ID&&taskData.getSerialized()==null){ //claur
//					System.out.println("Fixing null binary summary task");
//					task = new NormalTask(project);
//					task.setName(taskData.getName());
//					task.setUniqueId(taskData.getUniqueId());
//    			}else
    			if (taskData.getSerialized()==null) {
     				if (taskData.isTimesheetCreated()) {
     					task = new NormalTask(project);
     					task.setName(taskData.getName());
    					logger.log(Level.FINE, "made new task in serializer {0} parent {1}",
    						new Object[] { task, taskData.getParentTask() == null ? null : taskData.getParentTask().getName() });
     				} else {
     					continue; // void node
     				}
    			} else {
    				try {
    					task = (NormalTask)deserialize(taskData,reindex);
    				} catch (Exception e) {
    					if (taskData.isSubproject()){ //For migration
     						try {
     							task = (NormalTask) Class.forName(Messages.getMetaString("Subproject")).getConstructor(new Class[]{Project.class,Long.class}).newInstance(project,taskData.getSubprojectId());
     						} catch (Exception e1) {
    							logger.log(Level.WARNING, "Failed to instantiate subproject task", e1);
     						}

//  						task=new Subproject(project,taskData.getSubprojectId());
    						task.setUniqueId(taskData.getUniqueId());
    						task.setName(taskData.getName());
    						((SubProj)task).setSubprojectFieldValues(taskData.getSubprojectFieldValues());
    					}
     					else{
    						logger.log(Level.WARNING, "Failed to deserialize task", e);
    						throw new IOException("Subproject:"+e);
     					}
    				}
    			}
    			taskNodeMap.put(taskData,NodeFactory.getInstance().createNode(task));
    			task.setProject(project);
    			project.initializeId(task);
    			project.add(task);
    			if (taskData.isExternal()) {
    				task.setExternal(true);
    				task.setProjectId(taskData.getProjectId());
    				task.setAllSchedulesToCurrentDates();
    				project.addExternalTask(task);
    			} else {
    				task.setOwningProject(project);
    				task.setProjectId(projectId);
    			}
    			if (taskData.isSubproject()) {
    				SubProj sub = (SubProj)task;
    				sub.setSubprojectUniqueId(taskData.getSubprojectId());
    				sub.setSubprojectFieldValues(taskData.getSubprojectFieldValues());
    				sub.setSchedulesFromSubprojectFieldValues();
    			}
//    			if (task.isRoot()){ //claur
//    				project.setSummaryTaskEnabled(true);
//    			}

    			WorkingCalendar cal=(WorkingCalendar) task.getWorkCalendar();
    			if (cal!=null){ // use global one
    				WorkingCalendar newCal = (WorkingCalendar) CalendarService.findBaseCalendar(cal.getName());
    				if (newCal != null && newCal != cal)
    					task.setWorkCalendar(newCal);
    			}

    			//project.addToDefaultOutline(null,);


    			//assignments
				List<AssignmentData> assignments = new ArrayList<>(
						taskData.getAssignments() == null ? 0 : taskData.getAssignments().size());
//    			if (Environment.isNoPodServer()&&task.getPersistedAssignments()!=null){ //claur
//    				assignments.addAll(task.getPersistedAssignments());
//    			}
    			if (taskData.getAssignments()!=null) assignments.addAll(taskData.getAssignments());

    			if (assignments.size()>0)
				for (Iterator<AssignmentData> j=assignments.iterator();j.hasNext();){
					AssignmentData assignmentData=j.next();
//					if (loadResources!=null&&obj instanceof PersistedAssignment){ //claur
//					}else{
    						if (assignmentData.getSerialized() == null) { // timesheet created
								logger.log(Level.FINE, "==== no cached start found {0}", task.getName());
								if (assignments.size()==1)
									assignmentData.setResourceId(-1L);
								else j.remove();
						}
//					}
				}

    			if (assignments.size()>0)
    				for (Iterator<AssignmentData> j=assignments.iterator();j.hasNext();){
    					AssignmentData assignmentData=j.next();
    					Assignment assignment=null;
    					Resource resource;
    					boolean assigned=true;
    					int s;
//    					if (loadResources!=null&&obj instanceof PersistedAssignment){ //claur
//    						PersistedAssignment pa=(PersistedAssignment)obj;
//    						assignment=pa.getAssignment();
//    						s=pa.getSnapshot();
//
//   							long resId=pa.getResourceId();
//							Node node=(Node)resourceNodeMap.get(resId);
//							resource=node==null?ResourceImpl.getUnassignedInstance():(Resource)node.getImpl();
//
//							if (resource==null) assigned=false;
//    					}else{
    						if (loadResources==null){
    							EnterpriseResourceData r=assignmentData.getResource();
    							if (r==null) assigned=false;
    							resource=(r==null)?ResourceImpl.getUnassignedInstance():(Resource)resourceNodeMap.get(r).getImpl();
    						}else{
    							long resId=assignmentData.getResourceId();
    							Node node=resourceNodeMap.get(resId);
    							resource=node==null?ResourceImpl.getUnassignedInstance():(Resource)node.getImpl();
    						}
    						if (assignmentData.getSerialized() != null){
    							try {
    								assignment=(Assignment)deserialize(assignmentData,reindex);
    							} catch (Exception e) {
     								logger.log(Level.WARNING, "Failed to deserialize assignment", e);
     							}
    						}
    						if (assignmentData.getSerialized() == null||(assignmentData.getSerialized() != null&&assignment==null)) { // timesheet created
    							assignment = Assignment.getInstance(task,resource,	1.0, 0);
    							if (assignment.getCachedStart() == null) { //doesn't occur filtered above
    								logger.log(Level.FINE, "==== no cached start found {0}", task.getName());

    							} else {
    								task.setActualStart(assignment.getCachedStart().getTime());
    								task.setActualFinish(assignment.getCachedEnd().getTime());
    							}
    						}
    						assignment.setCachedStart(assignmentData.getCachedStart());
    						assignment.setCachedEnd(assignmentData.getCachedEnd());
    						assignment.setTimesheetStatus(assignmentData.getTimesheetStatus());
    						long lastUpdate = (assignmentData.getLastTimesheetUpdate() == null) ? 0 : assignmentData.getLastTimesheetUpdate().getTime();
    						assignment.setLastTimesheetUpdate(lastUpdate);
    						assignment.setWorkflowState(assignmentData.getWorkflowState());
    						s=assignmentData.getSnapshotId();
//    					}

    					assignment.getDetail().setTask(task);
    					assignment.getDetail().setResource(resource);
						Object snapshotId=Integer.valueOf(s);
    					TaskSnapshot snapshot=(TaskSnapshot)task.getSnapshot(snapshotId);

    					if (snapshot==null){
    						snapshot=new TaskSnapshot();
    						snapshot.setCurrentSchedule(task.getCurrentSchedule());
    						task.setSnapshot(snapshotId,snapshot);
    					}
    					if (Snapshottable.TIMESHEET.equals(snapshotId)) {
    						assignment.setTimesheetAssignment(true);
    					}
    					//

    					snapshot.addAssignment(assignment);

    					if (assigned&&Snapshottable.CURRENT.equals(snapshotId)) resource.addAssignment(assignment);

    					if (assignmentData!=null) assignmentData.emtpy();
    					//incremental.addAssignment(assignmentData);
    				}
//    			task.setPersistedAssignments(null);
    		}


			//dependencies
    		//Set<DependencyKey> initialLinkIds=null;
    		for (TaskData successorssorData:getTaskDataCollection(projectData)){
    			if (successorssorData.getPredecessors()!=null){
    				final Task successor=(Task)taskNodeMap.get(successorssorData).getImpl();
    				for (LinkData linkData:successorssorData.getPredecessors()){
//  					if (initialLinkIds==null){
//  					initialLinkIds=new HashSet<DependencyKey>();
//  					project.setInitialLinkIds(initialLinkIds);
//  					}
//  					initialLinkIds.add(new DependencyKey(linkData.getPredecessorId(),linkData.getSuccessorId()/*,externalId*/));
    					Dependency dependency=(Dependency)deserialize(linkData,reindex);

     					if (linkData.getPredecessor() == null) {
     						logger.warning("null pred - this shouldn't happen. skipping"); // todo treat it
     						continue;
     					}
    					final Task predecessor=(Task)taskNodeMap.get(linkData.getPredecessor()).getImpl();
    					connectDependency(dependency,predecessor,successor);

    					linkData.emtpy(); //why is this there?
    				}
    			}
    		}

		}

		//task outline
    	if (tasks!=null){

    		//add missing summary task
    		Node summaryNode=null;


			Map<Long, Node> subprojectsMap=new HashMap<Long, Node>(tasks.size() * 4 / 3 + 1);
    		for (TaskData taskData:tasks){
    			TaskData parentData=taskData.getParentTask();
//  			if (taskData.isTimesheetCreated())
//  			System.out.println("timesheet created parent is  " + parentData == null ? null : parentData.getName());
    			Node node;
    			if (taskData.getSerialized()==null /*&& taskData.getUniqueId()!=Task.SUMMARY_UNIQUE_ID*/ &&!taskData.isTimesheetCreated()) //void node //claur
    				node=NodeFactory.getInstance().createVoidNode();
    			else node=taskNodeMap.get(taskData);
    			Node parentNode=null;
    			int position=-1;
    			if (taskData.isExternal()){
    				Node previous=subprojectsMap.get(taskData.getProjectId());
    				if (previous!=null) parentNode=(Node)previous.getParent();
    				if (parentNode!=null){
    					position=parentNode.getIndex(previous)+1;
    					if (parentNode.isRoot()) parentNode=null;
    				}
    			}
    			if (position==-1){
    				if (parentData==null&&summaryNode!=null)
    					parentNode=summaryNode;
    				else
    					parentNode=(parentData==null)?
    						null:
    					taskNodeMap.get(parentData);
    				position=(int)taskData.getChildPosition();
    			}
    			if (taskData.isTimesheetCreated())
    				logger.log(Level.FINE, "new task {0} parent node is {1}", new Object[] { node, parentNode });
    			if (node.getImpl() instanceof SubProj){
    				SubProj sub=(SubProj)node.getImpl();
    				subprojectsMap.put(sub.getSubprojectUniqueId(), node);
    			}

    			project.addToDefaultOutline(parentNode,node,position,false);

    			taskData.emtpy();
    			//incremental.addTask(taskData);

    		}
    		//renumber tasks and save outline
    		project.getTaskOutline().getHierarchy().visitAll(new Consumer<Object>(){
    			int id=1;
    			public void accept(Object o) {
    				Node node=(Node)o;
    				if (node.getImpl() instanceof HasId){ //renumber
    					HasId impl=(HasId)node.getImpl();
    					if (impl.getId()>0) impl.setId(id++); //if id=0 means id not used
    				}
//  				if (node.getImpl() instanceof Task){ //save outline
//  				Task t=(Task)node.getImpl();
//  				Node parent=(Node)node.getParent();
//  				if (parent==null||parent.isRoot()) t.setLastSavedParentId(-1L);
//  				else t.setLastSavedParentId(((Task)parent.getImpl()).getUniqueId());
//  				t.setLastSavedPosistion(parent.getIndex(node));
//  				}
    				//done in setAllTasksAsUnchangedFromPersisted
    			}

    		});


    	}


    	if (resources!=null)
    		for (ResourceData resourceData:resources){
    			EnterpriseResourceData enterpriseResourceData=resourceData.getEnterpriseResource();
    			resourceData.emtpy();
    			//incremental.addResource(resourceData);
    			enterpriseResourceData.emtpy();
    			//incremental.addEnterpriseResource(enterpriseResourceData);

    		}



    	((DefaultNodeModel)project.getTaskOutline()).setDataFactory(project);



		project.initialize(subproject,updateDistribution&&!fixCorruption);
		// This relationship also exists for an empty child project. Restore it after
		// initialization, which rebuilds the transient subproject handler.
		Collection<TaskData> referringSubprojectTaskData=(Collection<TaskData>)(Collection<?>)projectData.getReferringSubprojectTasks();
		if (referringSubprojectTaskData!=null){
			ArrayList<Task> referringSubprojectTasks = new ArrayList<>(referringSubprojectTaskData.size());
			for (TaskData taskData:referringSubprojectTaskData){
				String projectName = taskData.getName(); // it was set to the referrig project name by synchronizer
				NormalTask referringTask;
				try {
					referringTask = (NormalTask)deserialize(taskData,reindex);
				} catch (Exception e) {
					if (taskData.isSubproject()){ //For migration
						referringTask=(NormalTask) project.getSubprojectHandler().createSubProj(taskData.getSubprojectId());
						referringTask.setUniqueId(taskData.getUniqueId());
						referringTask.setName(taskData.getName());
						((SubProj)referringTask).setSubprojectFieldValues(taskData.getSubprojectFieldValues());
					}
					else throw new IOException("Subproject:"+e);
				}
				referringTask.setName(projectName);
				referringTask.setProjectId(taskData.getProjectId());
				referringSubprojectTasks.add(referringTask);
			}
			project.setReferringSubprojectTasks(referringSubprojectTasks);
		}

		projectData.emtpy();
    	//incremental.setProject(projectData); //remove

    	(new DistributionConverter()).substractDistributionFromProject(project);


    	//distribution map
    	//project.updateDistributionMap();


    	if (fixCorruption) project.setForceNonIncremental(true);
    	if (project.getVersion()<1.2){
    		project.setForceNonIncrementalDistributions(true);
    	}
    	project.setVersion(Project.CURRENT_VERSION);

    	return project;
    	}


    public static void connectDependency(Dependency dependency,Task predecessor,Task successor){
		try {
			DependencyService.getInstance().initDependency(dependency,predecessor,successor,null);
    		} catch (InvalidAssociationException e) {
    			dependency.setDisabled(true);
    			try { // try a second time now that it's disabled
    				DependencyService.getInstance().initDependency(dependency,predecessor,successor,null);
    			} catch (InvalidAssociationException e1) {
    				logger.log(Level.WARNING, "Failed to initialize dependency after disabling", e1);
    			}
    			DependencyService.warnCircularCrossProjectLinkMessage(predecessor, successor);
    		}

    }


//    protected Map calendars=new Hashtable();
//    protected CalendarData serializeCalendar(WorkCalendar calendar,boolean globalIdsOnly) throws IOException,UniqueIdException{
//        Count calendarsCount=new Count("Calendars");
//        if (calendars.containsKey(calendar))
//            return (CalendarData)calendars.get(calendar);
//    	if (globalIdsOnly) makeGLobal(calendar);
//        CalendarData calendarData=(CalendarData)serialize(calendar,CalendarData.FACTORY,calendarsCount);
//        if (calendar instanceof WorkingCalendar){
//            WorkCalendar baseCalendar=((WorkingCalendar)calendar).getBaseCalendar();
//            if (baseCalendar!=null){
//            	if (globalIdsOnly) makeGLobal(baseCalendar);
//                CalendarData baseCalendarData=(CalendarData)serialize(baseCalendar,CalendarData.FACTORY,calendarsCount);
//                calendarData.setBaseCalendar(baseCalendarData);
//            }
//        }
//        calendarsCount.dump();
//        calendars.put(calendar,calendarData);
//        return calendarData;
//
//				/*((WorkingCalendar)calendar)*/calendar.setBaseCalendar(baseCalendar);
//				baseCalendar.setDocument(null);
//			} catch (CircularDependencyException e) {
//				e.printStackTrace();
//			}
//        }
//        if (calendar.isBaseCalendar()) {
//        	calendar.setDocument(null);
//        	CalendarService.getInstance().add(calendar);
//        }
//        return calendar;
//    }
//
    public static ResourceImpl deserializeResourceAndAddToPool(EnterpriseResourceData enterpriseResourceData,ResourcePool resourcePool,Session reindex) throws IOException, ClassNotFoundException{
    	ResourceData resourceData=new ResourceData();
    	resourceData.setEnterpriseResource(enterpriseResourceData);
    	ResourceImpl resource=deserializeResourceAndAddToPool(resourceData,resourcePool,reindex,null);
        setRoles(resource, resourceData);
        return resource;

    }
    public static ResourceImpl deserializeResourceAndAddToPool(ResourceData resourceData,ResourcePool resourcePool,Session reindex,Map<Long, EnterpriseResourceData> enterpriseResources) throws IOException, ClassNotFoundException{
        EnterpriseResourceData enterpriseResourceData=resourceData.getEnterpriseResource();
        EnterpriseResource enterpriseResource;
        if (enterpriseResources==null){
        	enterpriseResource =(EnterpriseResource)deserialize(enterpriseResourceData,reindex);
        	enterpriseResource.setUserAccount(enterpriseResourceData.getUserAccount());
        }else{
        	EnterpriseResourceData e=enterpriseResources.get(Long.valueOf(enterpriseResourceData.getUniqueId()));
        	if (e==null) {
        		throw new IllegalStateException("Missing enterprise resource mapping for " + enterpriseResourceData.getUniqueId());
        	}
        	enterpriseResource =(EnterpriseResource)deserialize(e,reindex);
        	enterpriseResource.setUserAccount(e.getUserAccount());
        }
        enterpriseResource.setGlobalWorkVector(enterpriseResourceData.getGlobalWorkVector());
        enterpriseResource.setMaster(resourcePool.isMaster());
        ResourceImpl resource=(resourceData.getSerialized()==null)?
                createResourceFromEnterpriseResource(enterpriseResource):
                (ResourceImpl)deserialize(resourceData,reindex);

        resource.setGlobalResource(enterpriseResource);
        setRoles(resource, resourceData);


        // to ensure older projects import correctly
        WorkingCalendar cal = (WorkingCalendar) enterpriseResource.getWorkCalendar();
        if (cal==null)
            enterpriseResource.setWorkCalendar(WorkingCalendar.getInstanceBasedOn(resourcePool.getDefaultCalendar()));
        else {
			try {
//				cal.setBaseCalendar(CalendarService.findBaseCalendar(cal.getBaseCalendar().getName()));// avoids multiple instances
        		WorkCalendar baseCal=CalendarService.findBaseCalendar(cal.getBaseCalendar().getName());
        		if (baseCal!=null) cal.setBaseCalendar(baseCal);// avoids multiple instances

         	} catch (CircularDependencyException e) {
				logger.log(Level.WARNING, "Failed to restore base calendar", e);
			}
        }
        resourcePool.initializeId(enterpriseResource);
        resourcePool.add(resource);
        return resource;
    }

    private static void setRoles(ResourceImpl resource,ResourceData resourceData){
        resource.setRole(resourceData.getRole());

        int[] authRoles=resourceData.getEnterpriseResource().getAuthorizedRoles();
        if (authRoles!=null){
        	EnterpriseResource globalResource=resource.getGlobalResource();
        	globalResource.setDefaultRole(authRoles.length>0?authRoles[0]:ApplicationUser.INACTIVE);
        	Set<Integer> roles=new HashSet<Integer>(authRoles.length * 4 / 3 + 1);
        	for (int i=0;i<authRoles.length;i++) {
				roles.add(UserUtil.toExtendedRole(authRoles[i],resource.isUser()));
			}
        	globalResource.setAuthorizedRoles(roles);
        	globalResource.setLicense(resourceData.getEnterpriseResource().getLicense());
        	globalResource.setLicenseOptions(resourceData.getEnterpriseResource().getLicenseOptions());
        }

    }

    private static ResourceImpl createResourceFromEnterpriseResource(EnterpriseResource enterpriseResource){
        ResourceImpl resource=new ResourceImpl();
        return resource;
    }


    //call referenceCache.update() after
//    public void updateEnterpriseResources(Collection resources,NodeModel model) throws IOException, ClassNotFoundException{
//        model.removeAll(NodeModel.EVENT);
//    	Map resourceNodeMap=new Hashtable();
//        if (resources!=null)
//        for (Iterator i=resources.iterator();i.hasNext();){
//            EnterpriseResourceData resourceData=(EnterpriseResourceData)i.next();
//            EnterpriseResource resource=(EnterpriseResource)deserialize(resourceData);
//            resourceNodeMap.put(resourceData,NodeFactory.getInstance().createNode(resource));
//        }
//
//        //resource outline
//        if (resources!=null){
//            for (Iterator i=resources.iterator();i.hasNext();){
//                EnterpriseResourceData resourceData=(EnterpriseResourceData)i.next();
//                EnterpriseResourceData parentData=(EnterpriseResourceData)resourceData.getParentResource();
//                Node node=(Node)resourceNodeMap.get(resourceData);
//                Node parentNode=(parentData==null)?
//                		null:
//                		((Node)resourceNodeMap.get(parentData));
//                model.add(parentNode,node,(int)resourceData.getChildPosition(),NodeModel.SILENT); //global update instead
//            }
//            Alert.error("cleanNullChildren not implemented");
//            //model.getHierarchy().cleanNullChildren();
//            model.getHierarchy().fireUpdate();
//        }
//    }
    public static void setEnterpriseResources(Collection<EnterpriseResourceData> resources,ResourcePool resourcePool,Session reindex) throws IOException, ClassNotFoundException{
        if (resources!=null){
        Map<EnterpriseResourceData, Node> resourceNodeMap = new HashMap<>(resources.size() * 4 / 3 + 1);
            for (EnterpriseResourceData resourceData : resources){
                ResourceImpl resource=deserializeResourceAndAddToPool(resourceData,resourcePool,reindex);
                resourceNodeMap.put(resourceData,NodeFactory.getInstance().createNode(resource));
            }
            //NodeModel model=resourcePool.getResourceOutline();
            for (EnterpriseResourceData resourceData : resources){
                EnterpriseResourceData parentData=resourceData.getParentResource();
                Node node=resourceNodeMap.get(resourceData);
                Node parentNode=(parentData==null)?
                		null:
                		resourceNodeMap.get(parentData);
                //model.add(parentNode,node,(int)resourceData.getChildPosition(),NodeModel.SILENT); //global update instead
                resourcePool.addToDefaultOutline(parentNode,node,(int)resourceData.getChildPosition(),false);
                ((ResourceImpl)node.getImpl()).getGlobalResource().setResourcePool(resourcePool);
            }
            resourcePool.getResourceOutline().getHierarchy().cleanVoidChildren();
        }

    }



    public static void forProjectDataDo(ProjectData project,Consumer<Object> c){
    	c.accept(project);
    	if (project.getCalendar()!=null){
    		c.accept(project.getCalendar());
    		//base calendars to handle?
    	}
    	for (Object value : project.getResources()){
    		ResourceData r=(ResourceData)value;
    		c.accept(r);
    		c.accept(r.getEnterpriseResource());
    		//calendars?
    	}
    	for (Object value : getTaskDataCollection(project)){
    		TaskData t=(TaskData)value;
    		c.accept(t);
    		DataUtils.forAllDo(t.getAssignments().iterator(), c);
    		DataUtils.forAllDo(t.getPredecessors().iterator(), c);
    		//calendars?
    	}
    }
    public static void forProjectDataReversedDo(ProjectData project,Consumer<Object> c){
    	for (Object value : getTaskDataCollection(project)){
    		TaskData t=(TaskData)value;
    		DataUtils.forAllDo(t.getAssignments().iterator(), c);
    		DataUtils.forAllDo(t.getPredecessors().iterator(), c);
    		c.accept(t);
    		//calendars?
    	}
    	for (Object value : project.getResources()){
    		ResourceData r=(ResourceData)value;
    		c.accept(r.getEnterpriseResource());
    		c.accept(r);
    		//calendars?
    	}
    	if (project.getCalendar()!=null){
    		c.accept(project.getCalendar());
    		//base calendars to handle?
    	}
    	c.accept(project);
    }

    private static abstract class IdClosure implements Consumer<Object>{
    	long id=1;
    }
    public static void renumberProject(ProjectData project){
    	forProjectDataDo(project,new IdClosure(){
			public void accept(Object arg0) {
				((CommonDataObject)arg0).setUniqueId(id++);
			}
    	});
    }



    class Count{
    	int count;
        int size;
        int max;
        int min=Integer.MAX_VALUE;
        String typeLabel;
        public Count(String typeLabel){
        	this.typeLabel=typeLabel;
        }
        void reset(){
        	count=0;
        	size=0;
        	max=0;
        	min=Integer.MAX_VALUE;
        }
        void add(int s){
        	count++;
        	size+=s;
        	if (s<min) min=s;
        	if (s>max) max=s;
        }
    	void dump(){
        	logger.log(Level.INFO, "Serialized {0} {1}, total={2}, average={3}, min={4}, max={5}",
        		new Object[] { count, typeLabel, size, ((count==0)?0:(size/count)), min, max });
    	}
    }

    protected File tmpDir=null;
    protected void initTmpDir() throws IOException{
    	tmpDir=new File(System.getProperty("user.home"),"micrproject_tmp");
    	if (tmpDir.isDirectory()){
    		File[] files=tmpDir.listFiles();
    		if (files!=null) for (int i=0;i<files.length;i++) files[i].delete();
    	}
    	else if (!tmpDir.exists()) tmpDir.mkdir();
    }
    protected void writeTmpFile(SerializedDataObject data,Count count) throws IOException{
    	if (tmpDir!=null&&count!=null)
    	try (FileOutputStream out = new FileOutputStream(new File(tmpDir,data.getPrefix()+"_"+count.count))) {
			if (data.getSerialized()!=null) out.write(data.getSerialized());
		} catch (FileNotFoundException e) {
			logger.log(Level.WARNING, "Failed to write temp serialization file", e);
		}
    }
//    public void makeGLobal(DataObject data) throws UniqueIdException{
//    	CommonDataObject.makeGlobal(data);
//     }
    public DataObject serialize(DataObject obj,SerializedDataObjectFactory factory,Count count) throws IOException{
        SerializedDataObject data=SerializeUtil.serialize(obj,factory);
        if (TMP_FILES) writeTmpFile(data,count);
        byte[] bytes=data.getSerialized();
        if (count!=null) count.add((bytes==null)?0:bytes.length);
        return data;
    }
    public static DataObject serializeSingle(DataObject obj,SerializedDataObjectFactory factory,Count count) throws IOException{
        SerializedDataObject data=SerializeUtil.serialize(obj,factory);
        byte[] bytes=data.getSerialized();
        if (count!=null) count.add((bytes==null)?0:bytes.length);
        return data;
    }


    public static DataObject deserialize(DataObject obj,Session reindex) throws IOException, ClassNotFoundException{
        return SerializeUtil.deserialize((SerializedDataObject)obj,reindex);
    }
    protected Collection<DataObject> serialize(Collection<? extends DataObject> objs,SerializedDataObjectFactory factory) throws IOException{
    	if (objs == null)
    		return new ArrayList<>(); // a user crashed here due to null objs.

        Collection<DataObject> r=new ArrayList<>(objs.size());
        for (DataObject obj:objs)
            r.add(SerializeUtil.serialize(obj,factory));
        return r;
    }
    protected Collection<DataObject> deserialize(Collection<? extends SerializedDataObject> objs,Session reindex) throws IOException, ClassNotFoundException{
        Collection<DataObject> r=new ArrayList<>(objs.size());
        for (SerializedDataObject obj:objs)
            r.add(SerializeUtil.deserialize(obj,reindex));
        return r;
    }

    @SuppressWarnings("unchecked")
    private static Collection<TaskData> getTaskDataCollection(ProjectData projectData) {
        return (Collection<TaskData>)(Collection<?>)projectData.getTasks();
    }

    @SuppressWarnings("unchecked")
    private static Collection<ResourceData> getResourceDataCollection(ProjectData projectData) {
        return (Collection<ResourceData>)(Collection<?>)projectData.getResources();
    }


//    public static void renumber(Map renumbered){
//        HasUniqueIdImpl.update(renumbered);
//    }



    public static void buildStructure(ProjectData projectData,Collection<DataObject> resources,Collection<DataObject> tasks,Collection<DataObject> assignments,Collection<DataObject> links, Collection<DataObject> externalTasks, Collection<DataObject> referringSubprojectTasks,boolean ignoreResourcesForAssignments){
    	if (externalTasks!=null) tasks.addAll(externalTasks);
    	Map resourceMap=createIdMap(resources);
    	Map taskMap=createIdMap(tasks);
    	buildTaskStructure(projectData, tasks, taskMap);
    	projectData.setTasks(tasks);
        projectData.setResources(resources);
        projectData.setReferringSubprojectTasks(referringSubprojectTasks);

        buildAssignmentsStructure(projectData,assignments,resourceMap,taskMap,ignoreResourcesForAssignments);
        buildLinksStructure(projectData,links,taskMap);

    }

    public static void buildTaskStructure(ProjectData projectData,Collection<? extends DataObject> tasks,Map<Long, DataObject> tMap){
    	if (tasks!=null){
	        for (DataObject value : tasks){
	        	TaskData task=(TaskData)value;
	        	if (task.getParentTask()==null&task.getParentTaskId()!=-1){
	        		//not built yet, building outline
	        		TaskData parentTask=(TaskData)tMap.get(task.getParentTaskId());
	        		task.setParentTask(parentTask);
	        	}
	        }
        }
    }


    public static void buildAssignmentsStructure(ProjectData projectData,Collection<? extends DataObject> assignments){
    	buildAssignmentsStructure(projectData,assignments,null,null,false);
    }
    public static void buildAssignmentsStructure(ProjectData projectData,Collection<? extends DataObject> assignments,Map<Long, DataObject> rMap,Map<Long, DataObject> tMap,boolean ignoreResourcesForAssignments){
    	Map<Long, DataObject> resourceMap=(rMap==null)?createIdMap(getResourceDataCollection(projectData)):rMap;
    	Map<Long, DataObject> taskMap=(tMap==null)?createIdMap(getTaskDataCollection(projectData)):tMap;

    	if (assignments!=null){
	        for (DataObject value : assignments){
	        	AssignmentData assignment=(AssignmentData)value;
			ResourceData resource=(ResourceData)resourceMap.get(Long.valueOf(assignment.getResourceId()));
	        	if (!ignoreResourcesForAssignments) assignment.setResource((resource==null)?null:resource.getEnterpriseResource());
	            TaskData taskData=(TaskData)taskMap.get(Long.valueOf(assignment.getTaskId()));
	            if (taskData == null) {
	            	//System.out.println("null task data ("+assignment.getTaskId()+")- project " + projectData.getName());
	            	ErrorLogger.logOnce("null task data","null task data - project " + projectData.getName(),null);
	            } else
	            	taskData.addAssignment(assignment);
	        }
        }
    }
    public static void buildLinksStructure(ProjectData projectData,Collection<? extends DataObject> links){
    	buildLinksStructure(projectData,links,null);
    }
    public static void buildLinksStructure(ProjectData projectData,Collection<? extends DataObject> links,Map<Long, DataObject> tMap){
    	Map<Long, DataObject> taskMap=(tMap==null)?createIdMap(getTaskDataCollection(projectData)):tMap;

        if (links!=null){
	        for (DataObject value : links){
	        	LinkData link=(LinkData)value;
	            TaskData predecessor=(TaskData)taskMap.get(Long.valueOf(link.getPredecessorId()));
	            TaskData successor=(TaskData)taskMap.get(Long.valueOf(link.getSuccessorId()));
	            if (predecessor==null||successor==null) continue; //external links
	            successor.addPredecessor(link);
	            link.setPredecessor(predecessor);
	        }
        }
    }


	    public static Map<Long, DataObject> createIdMap(Collection<? extends DataObject> c){
    Map<Long, DataObject> map = c == null ? new HashMap<>() : new HashMap<>(c.size() * 4 / 3 + 1);
        if (c!=null){
	        for (DataObject d : c){
			map.put(Long.valueOf(d.getUniqueId()),d);
	        }
        }
        return map;

    }

    public void printTaskDataHierarchy(Collection<TaskData> tasks){
		if (!logger.isLoggable(Level.INFO)) return;
		StringBuilder b=new StringBuilder();
		printTaskDataHierarchy(tasks,b);
    	logger.info(b.toString());
    }
    public void printTaskDataHierarchy(Collection<TaskData> tasks,final StringBuffer b){
		StringBuilder builder = new StringBuilder();
		printTaskDataHierarchy(tasks, builder);
		b.append(builder.toString());
    }
    private void printTaskDataHierarchy(Collection<TaskData> tasks,final StringBuilder b){
    Map<Long, Set<TaskData>> taskMap = new HashMap<>(tasks.size() * 4 / 3 + 1);
    	for (TaskData taskData:tasks){
    		if (taskData == null) continue;
			Long key=Long.valueOf(taskData.getParentTaskId());
    		Set<TaskData> set=taskMap.get(key);
    		if (set==null){
    			set=new TreeSet<>(new Comparator<TaskData>(){
    				public int compare(TaskData task0, TaskData task1) {
    					int value=(task0.getChildPosition()<task1.getChildPosition())?-1:((task0.getChildPosition()==task1.getChildPosition())?0:1);
    					if (value==0){
    						b.append("Duplicates: task0="+task0.getName()+", "+task0.getParentTaskId()+", "+task0.getChildPosition()+" task1="+task1.getName()+", "+task1.getParentTaskId()+", "+task1.getChildPosition()+"\n");
    					}
    					return value;
    				}
    			});
    		}
    		set.add(taskData);
    		taskMap.put(key,set);
    	}
    	buildTaskDataHierarchy(-1L, "\t", taskMap, b);


    }
    private void buildTaskDataHierarchy(long key, String prefix, Map<Long, Set<TaskData>> taskMap,final StringBuilder b){
		Set<TaskData> o=taskMap.get(Long.valueOf(key));
    	if (o==null) return;
    	List<TaskData> children=new ArrayList<>(o);
    	children.sort(new Comparator<TaskData>(){
    		public int compare(TaskData task0, TaskData task1) {
    			return Long.compare(task0.getChildPosition(), task1.getChildPosition());
    		}
    	});
    	for (TaskData taskData:children){
    		//System.out.println("name: "+taskData.getName());
     		b.append(prefix).append(taskData.getName()).append(',').append(taskData.getUniqueId()).append('\n');
    		if (taskData.getUniqueId()!=-1L) //avoid voids
    			buildTaskDataHierarchy(taskData.getUniqueId(), prefix+"\t", taskMap, b);
    	}
    }

    private static List<ResourceData> sortResourcesByChildPosition(Collection<ResourceData> resources) {
    	List<ResourceData> sortedResources = new ArrayList<>(resources);
    	sortedResources.sort(new Comparator<ResourceData>() {
    		public int compare(ResourceData resource1, ResourceData resource2) {
    			return Long.compare(resource1.getChildPosition(), resource2.getChildPosition());
    		}
    	});
    	return sortedResources;
    }

    private static List<TaskData> sortTasksByChildPosition(Collection<TaskData> tasks) {
    	List<TaskData> sortedTasks = new ArrayList<>(tasks);
    	sortedTasks.sort(new Comparator<TaskData>() {
    		public int compare(TaskData task1, TaskData task2) {
    			if (!task1.isExternal() && task2.isExternal()) return -1; //keep external tasks at the end
    			else if (task1.isExternal() && !task2.isExternal()) return 1;
    			return Long.compare(task1.getChildPosition(), task2.getChildPosition());
    		}
    	});
    	return sortedTasks;
    }




}
