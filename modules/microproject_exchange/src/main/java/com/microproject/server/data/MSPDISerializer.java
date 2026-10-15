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
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.microproject.util.SafeFileReplace;

import com.microproject.association.Association;
import com.microproject.association.AssociationList;
import com.microproject.core.pm.exchange.converters.mpx.MpxImportState;
import com.microproject.exchange.ImportedCalendarService;
import com.microproject.server.data.linker.Linker;
import com.microproject.server.data.linker.ResourceLinker;
import com.microproject.server.data.linker.TaskLinker;
import net.sf.mpxj.mspdi.MSPDIWriter;
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
import com.microproject.temporary.TemporaryCleanupQueue;

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
    private final Map<ResourceAssignment, Assignment> exportedAssignments = new IdentityHashMap<>();
    private record ExactWorkRow(long start, long finish, long work) { }
    private record ExactCostRow(long start, long finish, BigDecimal cost) { }
    
    
    
    protected Linker resourceLinker=new ResourceLinker(){
//    	int count = 0; // unassigned should start at 0
    	public Object addTransformedObjects(Object child) throws Exception{
    		Project project=(Project)parent;
    		ProjectFile projectFile=(ProjectFile)transformedParent;
    		ResourceImpl resource=(ResourceImpl)child;
//    		resource.setId(count++); // enumerate them
    		net.sf.mpxj.Resource resourceData=projectFile.addResource();
			MPXConverter.toMPXResource(project, resource, resourceData);
    		            
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
                    for (Association snapshotAssignment : snapshotAssignments) {
                        Assignment assignment = (Assignment) snapshotAssignment;
                        ResourceImpl r=(ResourceImpl)assignment.getResource();
                        if (s!=Snapshottable.CURRENT.intValue()) continue;
                        net.sf.mpxj.Resource resourceData=(net.sf.mpxj.Resource)resourceMap.get(r);
                        
                        ResourceAssignment assignmentData=taskData.addResourceAssignment(resourceData);
                        if (s==Snapshottable.CURRENT.intValue()){
                        	MPXConverter.toMPXAssignment(assignment,assignmentData);
							exportedAssignments.put(assignmentData, assignment);
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

    @SuppressWarnings("unchecked")
    protected Map<net.sf.mpxj.Task, Task> saveTasks(Project project,ProjectFile projectFile,Map<ResourceImpl, net.sf.mpxj.Resource> resourceMap) throws Exception{
		NodeModelUtil.enumerateNonAssignments(project.getTaskOutline()); // to fix bug, I moved this before tasks are saved. 16.2.06 hk
    	taskLinker.setParent(project);
    	taskLinker.setTransformedParent(projectFile);
    	taskLinker.setArgs(new Object[]{resourceMap});
    	taskLinker.init();
    	taskLinker.addTransformedObjects();
		taskLinker.addOutline(project.getTaskOutlineRoot());

    	//dependencies
		// mpxj uses default options when importing link leads and lags
		return withDefaultCalendarOptions(() -> {

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
		for (Iterator<?> i=project.getTaskOutline().iterator(exportRoot);i.hasNext();){
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
		        
	            for (Association predecessorAssociation : task.getPredecessorList()) {
	                Dependency dependency = (Dependency) predecessorAssociation;
	            	Task pred=(Task)dependency.getPredecessor();
	            	net.sf.mpxj.Task predData=(net.sf.mpxj.Task)taskLinker.getTransformationMap().get(pred);
				if (predData==null)
					predData=externalTask(projectFile,externalTasks,pred);
				Relation rel=taskData.addPredecessor(predData,RelationType.getInstance(dependency.getDependencyType()),MPXConverter.toMPXDependencyLag(dependency.getLag())); //claur
	            }
	            taskCount++;
    		}
        }
    	
			return (Map<net.sf.mpxj.Task, Task>) taskLinker.getTransformationMap();
		});
    }

	static <T> T withDefaultCalendarOptions(Callable<T> operation) throws Exception {
		CalendarOption previousOptions = CalendarOption.getInstance();
		CalendarOption.setInstance(CalendarOption.getDefaultInstance());
		try {
			return operation.call();
		} finally {
			CalendarOption.setInstance(previousOptions);
		}
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
		exportedAssignments.clear();
        if (globalIdsOnly) 
        	makeGLobal(project);
        MPXConverter.beginExport(project);
        try {
	        ProjectFile projectFile = new ProjectFile();
	//this doesn't appear in 2007 version of mpxj        projectData.setMicrosoftProjectCompatibleOutput(true);
	        projectFile.getProjectConfig().setAutoTaskUniqueID(true);
	        projectFile.getProjectConfig().setAutoResourceUniqueID(true);
	        //project
	        ProjectProperties projectHeader=projectFile.getProjectProperties();
	        
			MPXConverter.toMPXOptions(project, projectHeader);

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
		LinkedHashSet<WorkingCalendar> calendars = new LinkedHashSet<>();
		Set<WorkCalendar> visited = new HashSet<>();
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
		if (project == null || fileName == null || fileName.isBlank()) {
			logger.warning("Failed to save MSPDI file: project and file name are required");
			return false;
		}
		File file = new File(fileName).getAbsoluteFile();
		Path target = file.toPath();
		Path parent = target.getParent();
		if (parent == null) {
			logger.warning("Failed to save MSPDI file: target has no parent directory " + file);
			return false;
		}

		Path temporaryPath;
		try {
			// Keep the temporary file beside the target so replacement remains a
			// same-volume move and never falls back to the old racy _tmpN probing.
			temporaryPath = Files.createTempFile(parent, target.getFileName().toString() + ".tmp-", ".tmp");
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to create temporary MSPDI file beside " + file, e);
			Alert.error(Messages.getString("Message.saveError"));
			return false;
		}
		File temporaryFile = temporaryPath.toFile();
		boolean replaced = false;
		try {
			boolean saved;
			try (OutputStream output = Files.newOutputStream(temporaryPath)) {
				saved = saveProject(project, output);
			}
			if (!saved || Files.size(temporaryPath) == 0L) {
				Alert.error(Messages.getString("Message.saveError"));
				return false;
			}
			if (!SafeFileReplace.replace(temporaryFile, file)) {
				Alert.error(Messages.format("Format.join", Messages.getString("Message.saveErrorTmpFile"), temporaryFile));
				return false;
			}
			replaced = true;
			return true;
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to save MSPDI file " + fileName, e);
			Alert.error(Messages.getString("Message.saveError"));
			return false;
		} finally {
			if (!replaced) {
				TemporaryCleanupQueue.deleteOrEnqueue(temporaryPath);
			}
		}
	}

	public boolean saveProject(Project project,OutputStream out) {
		try {
			saveProjectOrThrow(project, out);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to save ProjectLibre project as MSPDI", e);
			return false;
		}
		return true;
	}

	/**
	 * Writes MSPDI while preserving the serializer's failure cause for callers
	 * that need transactional error reporting.  The legacy boolean method above
	 * intentionally keeps its historical failure contract.
	 */
	public void saveProjectOrThrow(Project project, OutputStream out) throws Exception {
		ProjectFile data = serializeProject(project);
		if (job != null) job.setProgress(0.9f);
		ByteArrayOutputStream serialized = new ByteArrayOutputStream();
		MSPDIWriter writer = new MSPDIWriter();
		writer.setWriteTimephasedData(true);
		if (exportedAssignments.values().stream().anyMatch(assignment -> assignment.getWorkContour().isPersonal()))
			writer.setSplitTimephasedAsDays(false);
		writer.write(data, serialized);
		byte[] xml = normalizeMspdiAssignmentData(serialized.toByteArray());
		out.write(xml);
		if (job != null) job.setProgress(1.0f);
	}

	private byte[] normalizeMspdiAssignmentData(byte[] xml) throws Exception {
		Map<String, Map<Integer, Assignment.BaselineValues>> expected = new java.util.HashMap<>();
		Set<String> personalContourAssignments = new java.util.HashSet<>();
		Map<String, List<ExactWorkRow>> personalActualRows = new java.util.HashMap<>();
		Map<String, List<ExactWorkRow>> personalRemainingRows = new java.util.HashMap<>();
		Map<String, Map<Integer, List<ExactWorkRow>>> baselineWorkRows = new java.util.HashMap<>();
		Map<String, Map<Integer, List<ExactCostRow>>> baselineCostRows = new java.util.HashMap<>();
		for (Map.Entry<ResourceAssignment, Assignment> exported : exportedAssignments.entrySet()) {
			Integer uid = exported.getKey().getUniqueID();
			if (exported.getValue().getWorkContour().isPersonal()) {
				if (uid == null || !personalContourAssignments.add(uid.toString()))
					throw new IOException("MPXJ did not assign a unique identity to a personal-contour assignment");
				personalActualRows.put(uid.toString(), exactWorkRows(exported.getKey().getTimephasedActualWork()));
				personalRemainingRows.put(uid.toString(), exactWorkRows(exported.getKey().getTimephasedWork()));
			}
			Map<Integer, List<ExactWorkRow>> assignmentBaselineRows = new java.util.TreeMap<>();
			Map<Integer, List<ExactCostRow>> assignmentBaselineCostRows = new java.util.TreeMap<>();
			for (int slot = 0; slot <= 10; slot++) {
				Assignment baseline = exported.getValue().getBaselineAssignment(Integer.valueOf(slot), false);
				if (baseline == null) continue;
				if (baseline.getWorkContour().isPersonal()) {
					if (uid == null)
						throw new IOException("MPXJ did not assign an identity to a timephased assignment baseline");
					assignmentBaselineRows.put(Integer.valueOf(slot), exactWorkRows(
						MPXConverter.baselineTimephasedWorkRows(baseline)));
				}
				if (baseline.getCostContour().isPersonal()) {
					if (uid == null)
						throw new IOException("MPXJ did not assign an identity to a timephased assignment baseline cost");
					assignmentBaselineCostRows.put(Integer.valueOf(slot), baselineTimephasedCostRows(baseline));
				}
			}
			if (!assignmentBaselineRows.isEmpty()) {
				if (baselineWorkRows.put(uid.toString(), Map.copyOf(assignmentBaselineRows)) != null)
					throw new IOException("MPXJ assigned duplicate identities to timephased assignment baselines");
			}
			if (!assignmentBaselineCostRows.isEmpty()) {
				if (baselineCostRows.put(uid.toString(), Map.copyOf(assignmentBaselineCostRows)) != null)
					throw new IOException("MPXJ assigned duplicate identities to timephased assignment baseline costs");
			}
			Map<Integer, Assignment.BaselineValues> zeroWorkSlots = new java.util.TreeMap<>();
			for (int slot = 0; slot <= 10; slot++) {
				Assignment.BaselineValues values = exported.getValue().getBaselineValues(slot);
				if (values != null && values.work() != null && values.work().longValue() == 0L)
					zeroWorkSlots.put(Integer.valueOf(slot), values);
			}
			if (zeroWorkSlots.isEmpty()) continue;
			if (uid == null)
				throw new IOException("MPXJ did not assign an identity to an assignment with zero baseline work");
			if (expected.put(uid.toString(), zeroWorkSlots) != null)
				throw new IOException("MPXJ assigned duplicate identities to baseline assignments");
		}
		if (expected.isEmpty() && personalContourAssignments.isEmpty() && baselineWorkRows.isEmpty()
				&& baselineCostRows.isEmpty()) return xml;
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
		String namespace = document.getDocumentElement().getNamespaceURI();
		org.w3c.dom.NodeList assignmentNodes = document.getElementsByTagNameNS(namespace, "Assignment");
		java.util.Set<String> applied = new java.util.HashSet<>();
		java.util.Set<String> appliedBaselineSlots = new java.util.HashSet<>();
		java.util.Set<String> appliedBaselineCostSlots = new java.util.HashSet<>();
		java.util.Set<String> normalizedPersonalContours = new java.util.HashSet<>();
		for (int index = 0; index < assignmentNodes.getLength(); index++) {
			org.w3c.dom.Element assignmentNode = (org.w3c.dom.Element) assignmentNodes.item(index);
			String uid = childText(assignmentNode, "UID");
			if (uid != null && personalContourAssignments.contains(uid)) {
				if (!normalizedPersonalContours.add(uid))
					throw new IOException("MSPDI output contains duplicate personal-contour assignment identities");
				normalizePersonalContourAssignmentScalars(assignmentNode, exportedAssignment(uid));
				if (!normalizePersonalContourWorkRows(assignmentNode, personalActualRows.get(uid),
					personalRemainingRows.get(uid)))
					throw new IOException("MSPDI output omitted personal-contour timephased work");
			}
			Map<Integer, Assignment.BaselineValues> zeroWorkSlots = uid == null ? null : expected.get(uid);
			if (zeroWorkSlots == null)
				continue;
			if (!applied.add(uid))
				throw new IOException("MSPDI output contains duplicate assignment identities");
			for (Map.Entry<Integer, Assignment.BaselineValues> zeroWork : zeroWorkSlots.entrySet()) {
				int slot = zeroWork.getKey().intValue();
				preserveExplicitZeroBaselineWork(document, assignmentNode, slot);
			}
		}
		if (!normalizedPersonalContours.equals(personalContourAssignments))
			throw new IOException("MSPDI output omitted a personal-contour assignment");
		if (!applied.equals(expected.keySet()))
			throw new IOException("MSPDI output omitted an assignment with zero baseline work");
		for (Map.Entry<String, Map<Integer, List<ExactWorkRow>>> entry : baselineWorkRows.entrySet()) {
			String uid = entry.getKey();
			for (Map.Entry<Integer, List<ExactWorkRow>> slotRows : entry.getValue().entrySet()) {
				org.w3c.dom.Element assignmentNode = findAssignment(assignmentNodes, namespace, uid);
				if (assignmentNode == null)
					throw new IOException("MSPDI output omitted an assignment with timephased baseline work");
				normalizeAssignmentBaselineWorkRows(document, assignmentNode, slotRows.getKey().intValue(),
					slotRows.getValue());
				appliedBaselineSlots.add(uid + ":" + slotRows.getKey());
			}
		}
		java.util.Set<String> expectedBaselineSlots = new java.util.HashSet<>();
		baselineWorkRows.forEach((uid, slots) -> slots.keySet().forEach(slot -> expectedBaselineSlots.add(uid + ":" + slot)));
		if (!appliedBaselineSlots.equals(expectedBaselineSlots))
			throw new IOException("MSPDI output omitted assignment baseline timephased work");
		for (Map.Entry<String, Map<Integer, List<ExactCostRow>>> entry : baselineCostRows.entrySet()) {
			String uid = entry.getKey();
			for (Map.Entry<Integer, List<ExactCostRow>> slotRows : entry.getValue().entrySet()) {
				org.w3c.dom.Element assignmentNode = findAssignment(assignmentNodes, namespace, uid);
				if (assignmentNode == null)
					throw new IOException("MSPDI output omitted an assignment with timephased baseline cost");
				normalizeAssignmentBaselineCostRows(document, assignmentNode, slotRows.getKey().intValue(),
					slotRows.getValue());
				appliedBaselineCostSlots.add(uid + ":" + slotRows.getKey());
			}
		}
		java.util.Set<String> expectedBaselineCostSlots = new java.util.HashSet<>();
		baselineCostRows.forEach((uid, slots) -> slots.keySet().forEach(slot -> expectedBaselineCostSlots.add(uid + ":" + slot)));
		if (!appliedBaselineCostSlots.equals(expectedBaselineCostSlots))
			throw new IOException("MSPDI output omitted assignment baseline timephased cost");
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
		transformerFactory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
		transformerFactory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
		transformer.transform(new javax.xml.transform.dom.DOMSource(document),
			new javax.xml.transform.stream.StreamResult(result));
		return result.toByteArray();
	}

	private static List<ExactCostRow> baselineTimephasedCostRows(Assignment baseline) throws IOException {
		List<ExactCostRow> result = new ArrayList<>();
		com.microproject.pm.calendar.WorkCalendar calendar = baseline.getEffectiveWorkCalendar();
		long start = calendar.adjustInsideCalendar(baseline.getStart(), false);
		long finish = baseline.getFinish();
		long cursor = start;
		while (cursor < finish) {
			long nextDay = com.microproject.util.DateTime.nextDay(com.microproject.util.DateTime.dayFloor(cursor));
			long segmentEnd = Math.min(finish, nextDay);
			long workingEnd = segmentEnd == nextDay ? calendar.adjustInsideCalendar(segmentEnd, true) : segmentEnd;
			long workingDuration = calendar.compare(workingEnd, cursor, false);
			if (workingDuration > 0L) {
				double value = baseline.cost(cursor, workingEnd);
				if (!Double.isFinite(value) || value < 0.0D)
					throw new IOException("Assignment baseline cost contour produced an invalid value");
				result.add(new ExactCostRow(cursor, workingEnd, BigDecimal.valueOf(value).stripTrailingZeros()));
			}
			if (segmentEnd <= cursor)
				throw new IOException("Cannot advance an assignment baseline cost interval");
			cursor = segmentEnd < finish ? calendar.adjustInsideCalendar(segmentEnd, false) : segmentEnd;
		}
		if (result.isEmpty()) throw new IOException("Assignment baseline cost contour has no working intervals");
		return List.copyOf(result);
	}

	private Assignment exportedAssignment(String uid) throws IOException {
		for (Map.Entry<ResourceAssignment, Assignment> exported : exportedAssignments.entrySet())
			if (exported.getKey().getUniqueID() != null && uid.equals(exported.getKey().getUniqueID().toString()))
				return exported.getValue();
		throw new IOException("MSPDI output contains an unknown personal-contour assignment");
	}

	private static void normalizePersonalContourAssignmentScalars(org.w3c.dom.Element assignmentNode,
		Assignment source) throws IOException {
		setDurationField(assignmentNode, "Work", source.getWork(null));
		setDurationField(assignmentNode, "ActualWork", source.getActualWork(null));
		setDurationField(assignmentNode, "RemainingWork", source.getRemainingWork());
	}

	private static void setDurationField(org.w3c.dom.Element parent, String name, long millis) throws IOException {
		org.w3c.dom.Element element = childElement(parent, name);
		if (element == null) {
			if (millis != 0L) throw new IOException("MSPDI output omitted nonzero assignment " + name);
			return;
		}
		element.setTextContent(durationMillisToMspdi(millis));
	}

	private static List<ExactWorkRow> exactWorkRows(List<net.sf.mpxj.TimephasedWork> rows) throws IOException {
		List<ExactWorkRow> result = new ArrayList<>();
		for (net.sf.mpxj.TimephasedWork row : rows) {
			net.sf.mpxj.Duration amount = row.getTotalAmount();
			if (amount == null || amount.getUnits() != net.sf.mpxj.TimeUnit.HOURS
					|| row.getStart() == null || row.getFinish() == null)
				throw new IOException("MPXJ personal-contour work row has no exact hour amount");
			result.add(new ExactWorkRow(
				com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(row.getStart()),
				com.microproject.core.pm.exchange.converters.type.DateUTCConverter.toModelTime(row.getFinish()),
				Math.round(amount.getDuration() * 60L * 60L * 1000L)));
		}
		return List.copyOf(result);
	}

	private boolean normalizePersonalContourWorkRows(org.w3c.dom.Element assignmentNode,
		List<ExactWorkRow> actualRows, List<ExactWorkRow> remainingRows)
			throws IOException {
		Map<String, ExactWorkRow> expectedActual = indexWorkRows(actualRows);
		Map<String, ExactWorkRow> expectedRemaining = indexWorkRows(remainingRows);
		for (org.w3c.dom.Element row : directChildren(assignmentNode, "TimephasedData")) {
			String type = childText(row, "Type");
			if (!"1".equals(type) && !"2".equals(type)) continue;
			List<org.w3c.dom.Element> units = directChildren(row, "Unit");
			if (units.size() != 1)
				throw new IOException("MSPDI personal-contour work row is missing or duplicates its unit");
			// MPXJ uses the duration display-unit enum here; MSPDI's Unit uses
			// a separate enum. These timephased values are per-day totals.
			units.get(0).setTextContent("2");
			long start = parseMspdiWallTime(childText(row, "Start"));
			long finish = parseMspdiWallTime(childText(row, "Finish"));
			Map<String, ExactWorkRow> expectedRows = "2".equals(type) ? expectedActual : expectedRemaining;
			ExactWorkRow expected = expectedRows.remove(workIntervalKey(start, finish));
			if (expected == null)
				throw new IOException("MSPDI personal-contour work row does not match its source interval");
			String exactValue = durationMillisToMspdi(expected.work());
			if (childText(row, "Value") == null) {
				if (expected.work() != 0L || childText(row, "Start") == null || childText(row, "Finish") == null)
					throw new IOException("MSPDI personal-contour work row is missing its value");
				org.w3c.dom.Element value = row.getOwnerDocument()
					.createElementNS(row.getNamespaceURI(), "Value");
				value.setTextContent(exactValue);
				row.appendChild(value);
			} else {
				childElement(row, "Value").setTextContent(exactValue);
			}
		}
		return expectedActual.isEmpty() && expectedRemaining.isEmpty()
			&& !(actualRows.isEmpty() && remainingRows.isEmpty());
	}

	private static Map<String, ExactWorkRow> indexWorkRows(List<ExactWorkRow> rows) throws IOException {
		Map<String, ExactWorkRow> result = new java.util.HashMap<>();
		for (ExactWorkRow row : rows) {
			if (result.put(workIntervalKey(row.start(), row.finish()), row) != null)
				throw new IOException("MPXJ personal-contour work rows have duplicate intervals");
		}
		return result;
	}

	private static String workIntervalKey(long start, long finish) {
		return start + ":" + finish;
	}

	private static org.w3c.dom.Element findAssignment(org.w3c.dom.NodeList assignments, String namespace,
		String uid) throws IOException {
		org.w3c.dom.Element match = null;
		for (int index = 0; index < assignments.getLength(); index++) {
			org.w3c.dom.Element assignment = (org.w3c.dom.Element) assignments.item(index);
			if (!uid.equals(childText(assignment, "UID", namespace))) continue;
			if (match != null) throw new IOException("MSPDI output contains duplicate assignment identities");
			match = assignment;
		}
		return match;
	}

	private static void normalizeAssignmentBaselineWorkRows(org.w3c.dom.Document document,
		org.w3c.dom.Element assignment, int slot, List<ExactWorkRow> rows) throws IOException {
		org.w3c.dom.Element baseline = findBaseline(assignment, slot);
		if (baseline == null) {
			baseline = document.createElementNS(assignment.getNamespaceURI(), "Baseline");
			insertBaselineAtSchemaPosition(assignment, baseline, slot);
		}
		int type = MpxImportState.assignmentBaselineWorkType(slot);
		for (org.w3c.dom.Element existing : directChildren(baseline, "TimephasedData")) {
			if (Integer.toString(type).equals(childText(existing, "Type"))) baseline.removeChild(existing);
		}
		org.w3c.dom.Element number = childElement(baseline, "Number");
		if (number == null) {
			number = document.createElementNS(assignment.getNamespaceURI(), "Number");
			number.setTextContent(Integer.toString(slot));
			baseline.appendChild(number);
		} else if (!Integer.toString(slot).equals(number.getTextContent().trim())) {
			throw new IOException("MSPDI output assignment baseline number does not match its slot");
		}
		String assignmentUid = childText(assignment, "UID");
		if (assignmentUid == null) throw new IOException("MSPDI output assignment has no UID");
		for (ExactWorkRow row : rows) {
			org.w3c.dom.Element timephased = document.createElementNS(assignment.getNamespaceURI(), "TimephasedData");
			appendTextElement(document, timephased, "Type", Integer.toString(type));
			appendTextElement(document, timephased, "UID", assignmentUid);
			appendTextElement(document, timephased, "Start", formatMspdiWallTime(row.start()));
			appendTextElement(document, timephased, "Finish", formatMspdiWallTime(row.finish()));
			appendTextElement(document, timephased, "Unit", "2");
			appendTextElement(document, timephased, "Value", durationMillisToMspdi(row.work()));
			baseline.insertBefore(timephased, number);
		}
	}

	private static void normalizeAssignmentBaselineCostRows(org.w3c.dom.Document document,
		org.w3c.dom.Element assignment, int slot, List<ExactCostRow> rows) throws IOException {
		org.w3c.dom.Element baseline = findBaseline(assignment, slot);
		if (baseline == null) {
			baseline = document.createElementNS(assignment.getNamespaceURI(), "Baseline");
			insertBaselineAtSchemaPosition(assignment, baseline, slot);
		}
		int type = MpxImportState.assignmentBaselineCostType(slot);
		for (org.w3c.dom.Element existing : directChildren(baseline, "TimephasedData")) {
			if (Integer.toString(type).equals(childText(existing, "Type"))) baseline.removeChild(existing);
		}
		org.w3c.dom.Element number = childElement(baseline, "Number");
		if (number == null) {
			number = document.createElementNS(assignment.getNamespaceURI(), "Number");
			number.setTextContent(Integer.toString(slot));
			baseline.appendChild(number);
		} else if (!Integer.toString(slot).equals(number.getTextContent().trim())) {
			throw new IOException("MSPDI output assignment baseline number does not match its slot");
		}
		String assignmentUid = childText(assignment, "UID");
		if (assignmentUid == null) throw new IOException("MSPDI output assignment has no UID");
		for (ExactCostRow row : rows) {
			org.w3c.dom.Element timephased = document.createElementNS(assignment.getNamespaceURI(), "TimephasedData");
			appendTextElement(document, timephased, "Type", Integer.toString(type));
			appendTextElement(document, timephased, "UID", assignmentUid);
			appendTextElement(document, timephased, "Start", formatMspdiWallTime(row.start()));
			appendTextElement(document, timephased, "Finish", formatMspdiWallTime(row.finish()));
			appendTextElement(document, timephased, "Unit", "2");
			appendTextElement(document, timephased, "Value", row.cost().toPlainString());
			baseline.insertBefore(timephased, number);
		}
	}

	private static void appendTextElement(org.w3c.dom.Document document, org.w3c.dom.Element parent,
		String name, String value) {
		org.w3c.dom.Element element = document.createElementNS(parent.getNamespaceURI(), name);
		element.setTextContent(value);
		parent.appendChild(element);
	}

	private static String formatMspdiWallTime(long millis) {
		return java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
			java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), java.time.ZoneOffset.UTC));
	}

	private static long parseMspdiWallTime(String value) throws IOException {
		if (value == null) throw new IOException("MSPDI personal-contour work row is missing its interval");
		try {
			return java.time.LocalDateTime.parse(value).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
		} catch (java.time.DateTimeException e) {
			throw new IOException("MSPDI personal-contour work row has an invalid interval", e);
		}
	}

	private static String durationMillisToMspdi(long millis) throws IOException {
		if (millis < 0L) throw new IOException("MSPDI work row has negative work");
		long hours = millis / 3_600_000L;
		long minutes = (millis % 3_600_000L) / 60_000L;
		long seconds = millis % 60_000L / 1_000L;
		long fractionalMillis = millis % 1_000L;
		String secondsText = Long.toString(seconds);
		if (fractionalMillis != 0L) {
			String fraction = String.format(java.util.Locale.ROOT, "%03d", Long.valueOf(fractionalMillis));
			secondsText += "." + fraction.replaceFirst("0+$", "");
		}
		return "PT" + hours + "H" + minutes + "M" + secondsText + "S";
	}

	private static org.w3c.dom.Element childElement(org.w3c.dom.Element parent, String name) {
		for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof org.w3c.dom.Element element
					&& isNamedElement(element, name, parent.getNamespaceURI())) return element;
		}
		return null;
	}

	private void preserveExplicitZeroBaselineWork(org.w3c.dom.Document document,
		org.w3c.dom.Element assignmentNode, int slot) throws IOException {
		org.w3c.dom.Element baseline = findBaseline(assignmentNode, slot);
		if (baseline == null) {
			baseline = document.createElementNS(assignmentNode.getNamespaceURI(), "Baseline");
			org.w3c.dom.Element number = document.createElementNS(assignmentNode.getNamespaceURI(), "Number");
			number.setTextContent(Integer.toString(slot));
			baseline.appendChild(number);
			insertBaselineAtSchemaPosition(assignmentNode, baseline, slot);
		}
		List<org.w3c.dom.Element> workNodes = directChildren(baseline, "Work");
		if (workNodes.size() > 1)
			throw new IOException("MSPDI output contains duplicate baseline Work fields");
		if (workNodes.isEmpty()) {
			org.w3c.dom.Element work = document.createElementNS(assignmentNode.getNamespaceURI(), "Work");
			work.setTextContent("PT0H0M0S");
			org.w3c.dom.Node followingField = firstBaselineFieldAfterWork(baseline);
			if (followingField == null) baseline.appendChild(work);
			else baseline.insertBefore(work, followingField);
		} else if (!isZeroDuration(workNodes.get(0).getTextContent())) {
			throw new IOException("MPXJ emitted a nonzero baseline Work value for a zero-work assignment");
		}
	}

	private static String childText(org.w3c.dom.Element parent, String name) {
		return childText(parent, name, parent.getNamespaceURI());
	}

	private static String childText(org.w3c.dom.Element parent, String name, String namespace) {
		for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof org.w3c.dom.Element element && isNamedElement(element, name, namespace))
				return element.getTextContent();
		}
		return null;
	}

	private static org.w3c.dom.Element findBaseline(org.w3c.dom.Element assignment, int slot) throws IOException {
		org.w3c.dom.Element match = null;
		Set<Integer> seenSlots = new HashSet<>();
		int previousSlot = -1;
		for (org.w3c.dom.Node child = assignment.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (!(child instanceof org.w3c.dom.Element element) || !isNamedElement(element, "Baseline", assignment.getNamespaceURI()))
				continue;
			String number = childText(element, "Number", assignment.getNamespaceURI());
			int existingSlot;
			try {
				existingSlot = Integer.parseInt(number);
			} catch (RuntimeException e) {
				throw new IOException("MSPDI output contains an invalid assignment baseline number", e);
			}
			if (!seenSlots.add(Integer.valueOf(existingSlot)))
				throw new IOException("MSPDI output contains duplicate assignment baseline slots");
			if (existingSlot < previousSlot)
				throw new IOException("MSPDI output contains assignment baseline slots out of order");
			previousSlot = existingSlot;
			if (existingSlot == slot) match = element;
		}
		return match;
	}

	private static void insertBaselineAtSchemaPosition(org.w3c.dom.Element assignment,
			org.w3c.dom.Element baseline, int slot) throws IOException {
		// Microsoft MSPDI assignment XSD sequence: ExtendedAttribute*, Baseline*, then
		// local custom fields (f404000...). Keep new records in the Baseline group.
		org.w3c.dom.Node firstCustomField = null;
		org.w3c.dom.Node greaterSlotBaseline = null;
		for (org.w3c.dom.Node child = assignment.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (!(child instanceof org.w3c.dom.Element element)) continue;
			if (isNamedElement(element, "Baseline", assignment.getNamespaceURI())) {
				String number = childText(element, "Number", assignment.getNamespaceURI());
				try {
					if (number != null && Integer.parseInt(number) > slot) {
						greaterSlotBaseline = child;
						break;
					}
				} catch (NumberFormatException e) {
					throw new IOException("MSPDI output contains an invalid assignment baseline number", e);
				}
				continue;
			}
			if (isAssignmentCustomField(element)) {
				firstCustomField = child;
				break;
			}
		}
		org.w3c.dom.Node insertionPoint = greaterSlotBaseline == null ? firstCustomField : greaterSlotBaseline;
		if (insertionPoint == null) assignment.appendChild(baseline);
		else assignment.insertBefore(baseline, insertionPoint);
	}

	private static boolean isAssignmentCustomField(org.w3c.dom.Element element) {
		String name = element.getLocalName() == null ? element.getTagName() : element.getLocalName();
		if (name.length() != 7 || name.charAt(0) != 'f') return false;
		for (int index = 1; index < name.length(); index++) {
			char value = name.charAt(index);
			if (!((value >= '0' && value <= '9') || (value >= 'a' && value <= 'f')
					|| (value >= 'A' && value <= 'F'))) return false;
		}
		return true;
	}

	private static boolean isZeroDuration(String value) throws IOException {
		try {
			return javax.xml.datatype.DatatypeFactory.newInstance().newDuration(value).getSign() == 0;
		} catch (javax.xml.datatype.DatatypeConfigurationException | IllegalArgumentException e) {
			throw new IOException("MSPDI output contains an invalid baseline Work duration", e);
		}
	}

	private static List<org.w3c.dom.Element> directChildren(org.w3c.dom.Element parent, String name) {
		List<org.w3c.dom.Element> result = new ArrayList<>();
		for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof org.w3c.dom.Element element && isNamedElement(element, name, parent.getNamespaceURI()))
				result.add(element);
		}
		return result;
	}

	private static boolean isNamedElement(org.w3c.dom.Element element, String name, String namespace) {
		String localName = element.getLocalName() == null ? element.getTagName() : element.getLocalName();
		return name.equals(localName) && java.util.Objects.equals(namespace, element.getNamespaceURI());
	}

	private static org.w3c.dom.Node firstBaselineFieldAfterWork(org.w3c.dom.Element baseline) {
		String[] orderedFields = { "TimephasedData", "Number", "Start", "Finish", "Work", "Cost", "BCWS", "BCWP" };
		for (int index = 0; index < orderedFields.length; index++) {
			if (!"Work".equals(orderedFields[index])) continue;
			for (int later = index + 1; later < orderedFields.length; later++) {
				for (org.w3c.dom.Node child = baseline.getFirstChild(); child != null; child = child.getNextSibling()) {
					if (child instanceof org.w3c.dom.Element element
							&& isNamedElement(element, orderedFields[later], baseline.getNamespaceURI()))
						return child;
				}
			}
		}
		return null;
	}

	public JobRunnable getJob() {
		return job;
	}

	public void setJob(JobRunnable job) {
		this.job = job;
	}
	
	

}
