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
package com.microproject.session;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.microproject.exchange.FileImporter;
import com.microproject.grouping.core.model.DefaultNodeModel;
import com.microproject.job.Job;
import com.microproject.job.JobRunnable;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourcePoolFactory;
import com.microproject.pm.task.Project;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.server.data.ProjectData;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.ClassUtils;
import com.microproject.util.Environment;

public class LocalSession extends AbstractSession{
	private static final Logger logger = Logger.getLogger(LocalSession.class.getName());
	public static final String LOCAL_PROJECT_IMPORTER = "com.microproject.exchange.LocalFileImporter";
	public static final String MPO_PROJECT_IMPORTER = "com.microproject.exchange.MpoFileImporter";
	public static final String SERVER_LOCAL_PROJECT_IMPORTER = "com.microproject.exchange.ServerLocalFileImporter";
	public static final String MICROSOFT_PROJECT_IMPORTER = "com.microproject.exchange.MicrosoftImporter";
	private static final String DESCRIPTOR_FILE_NAME = "projectlibre.fileName";
	private static final int DESCRIPTOR_SCAN_DEPTH = 2;
	private final Map<Long, String> descriptorFiles = Collections.synchronizedMap(new HashMap<Long, String>());
	
	
	protected long localSeed;
	public synchronized long getId(){
		return localSeed++;
	}
    /**
     * Issue #227/#268: reset the local id counter to a fixed deterministic base.
     * Internal/scaffolding objects (standard calendars, the scheduling algorithm's
     * task, the unassigned resource, default assignments, void nodes) mint their
     * identity from this counter during document load. Because the counter is
     * persistent across loads, those minted ids used to drift into the saved .pod,
     * making round-trips non-deterministic. Resetting to a constant base at the
     * start of each local document load makes every load produce identical ids
     * (the load creates the same internal objects in the same order), so the
     * serialized output is stable. Real (file-stored) object ids are taken from the
     * file and are unaffected.
     */
    public synchronized void resetLocalSeed() {
        localSeed = -1_000_000_000L;
    }
    public Job getCloseProjectsJob(final Collection projects){
    	Job job=new Job(jobQueue,"closeProjects","Closing...",false);
    	job.addRunnable(new JobRunnable("LocalAccess: closeProjects",0.1f){
    		public Object run() throws Exception{
				setProgress(1.0f);
    			return null;
    		}
    	});
    	job.addExceptionRunnable(new JobRunnable("Local: exception"){
    		public Object run() throws Exception{
			logJobFailure("Failed to close local projects", job.getFailureException());
    			Alert.error(Messages.getString("Message.serverError"));
    			return null;
    		}
    	});
    	return job;
    }

    public Job getLoadProjectDescriptorsJob(final boolean includeProjects, final List descriptors, final boolean allowOpenAs) {
    	final Job job = new Job(jobQueue, "loadProjectDescriptors", "Loading...", false);
    	job.addRunnable(new JobRunnable("LocalAccess: loadProjectDescriptors", 1.0f) {
    		public Object run() throws Exception {
    			List<ProjectData> loadedDescriptors = loadLocalProjectDescriptors();
    			descriptors.clear();
    			descriptors.addAll(loadedDescriptors);
    			setProgress(1.0f);
    			return loadedDescriptors;
    		}
    	});
    	return job;
    }

    public String getProjectFile(long id) {
    	synchronized (descriptorFiles) {
    		return descriptorFiles.get(Long.valueOf(id));
    	}
    }

    private List<ProjectData> loadLocalProjectDescriptors() {
    	LinkedHashSet<String> seenPaths = new LinkedHashSet<String>();
    	List<ProjectData> descriptors = new ArrayList<ProjectData>();
    	for (File root : getDescriptorRoots()) {
    		collectProjectFiles(root, seenPaths, descriptors, 0);
    	}
    	Collections.sort(descriptors, new Comparator<ProjectData>() {
    		public int compare(ProjectData left, ProjectData right) {
    			Date leftDate = left.getLastModificationDate();
    			Date rightDate = right.getLastModificationDate();
    			if (leftDate != null && rightDate != null) {
    				int byDate = rightDate.compareTo(leftDate);
    				if (byDate != 0) {
    					return byDate;
    				}
    			}
    			String leftName = left.getName();
    			String rightName = right.getName();
    			if (leftName == null) {
    				return rightName == null ? 0 : 1;
    			}
    			if (rightName == null) {
    				return -1;
    			}
    			return leftName.compareToIgnoreCase(rightName);
    		}
    	});
    	return descriptors;
    }

    private List<File> getDescriptorRoots() {
    	LinkedHashSet<File> roots = new LinkedHashSet<File>();
    	addDescriptorRoot(roots, Preferences.userNodeForPackage(FileHelper.class).get("lastDirectory", null));
    	addDescriptorRoot(roots, System.getProperty("user.dir"));
    	File userHome = new File(System.getProperty("user.home"));
    	addDescriptorRoot(roots, new File(userHome, "ProjectLibre").getAbsolutePath());
    	addDescriptorRoot(roots, new File(userHome, "Documents").getAbsolutePath());
    	addDescriptorRoot(roots, new File(System.getProperty("user.dir"), "samples").getAbsolutePath());
    	return new ArrayList<File>(roots);
    }

    private void addDescriptorRoot(Collection<File> roots, String path) {
    	if (path == null || path.length() == 0) {
    		return;
    	}
    	File root = new File(path);
    	if (root.exists() && root.isDirectory()) {
    		roots.add(root);
    	}
    }

    private void collectProjectFiles(File root, LinkedHashSet<String> seenPaths, List<ProjectData> descriptors, int depth) {
    	if (root == null || !root.exists()) {
    		return;
    	}
    	if (root.isFile()) {
    		addDescriptor(root, seenPaths, descriptors);
    		return;
    	}
    	if (depth > DESCRIPTOR_SCAN_DEPTH) {
    		return;
    	}
    	File[] children = root.listFiles();
    	if (children == null) {
    		return;
    	}
    	for (File child : children) {
    		if (child.isDirectory()) {
    			collectProjectFiles(child, seenPaths, descriptors, depth + 1);
		} else if (FileHelper.isNativeFile(child.getName())) {
    			addDescriptor(child, seenPaths, descriptors);
    		}
    	}
    }

    private void addDescriptor(File file, LinkedHashSet<String> seenPaths, List<ProjectData> descriptors) {
    	try {
    		String canonicalPath = file.getCanonicalPath();
    		if (!seenPaths.add(canonicalPath)) {
    			return;
    		}
    		ProjectData descriptor = buildDescriptor(file);
    		if (descriptor == null) {
    			return;
    		}
    		rememberDescriptor(descriptor, canonicalPath);
    		descriptors.add(descriptor);
    	} catch (Exception e) {
    		logger.log(Level.WARNING, "Failed to add project descriptor", e);
    	}
    }

    private ProjectData buildDescriptor(File file) throws Exception {
    	ProjectData descriptor = new ProjectData();
    	if (descriptor.getName() == null || descriptor.getName().trim().length() == 0) {
    		String fileName = file.getName();
    		int lastDot = fileName.lastIndexOf('.');
    		descriptor.setName(lastDot <= 0 ? fileName : fileName.substring(0, lastDot));
    	}
    	if (descriptor.getUniqueId() <= 0) {
    		descriptor.setUniqueId(Math.abs(file.getCanonicalPath().hashCode()));
    	}
    	descriptor.setMaster(true);
    	descriptor.setLocal(true);
    	BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
    	if (descriptor.getCreationDate() == null) {
    		descriptor.setCreationDate(new Date(attributes.creationTime().toMillis()));
    	}
    	if (descriptor.getLastModificationDate() == null) {
    		descriptor.setLastModificationDate(new Date(attributes.lastModifiedTime().toMillis()));
    	}
    	descriptor.setLockedById(0L);
    	descriptor.setLockedByName(null);
    	return descriptor;
    }

    private void rememberDescriptor(ProjectData descriptor, String fileName) {
    	Map attributes = descriptor.getAttributes();
    	if (attributes == null) {
    		attributes = new HashMap();
    		descriptor.setAttributes(attributes);
    	}
    	attributes.put(DESCRIPTOR_FILE_NAME, fileName);
    	synchronized (descriptorFiles) {
    		descriptorFiles.put(Long.valueOf(descriptor.getUniqueId()), fileName);
    	}
    }

    
    
    
    public Job getLoadProjectJob(final LoadOptions opt){
    	String resolvedFileName = opt.getFileName();
    	if (resolvedFileName == null && opt.getId() > 0) {
    		resolvedFileName = getProjectFile(opt.getId());
    	}
    	if (resolvedFileName == null) {
    		Job missingJob = new Job(jobQueue, "loadProject", "Loading...", false);
    		missingJob.addRunnable(new JobRunnable("LocalAccess: missingProject", 1.0f) {
    			public Object run() throws Exception {
    				Alert.error(Messages.getString("Error.projectDoesNotExist"));
    				setProgress(1.0f);
    				return null;
    			}
    		});
    		return missingJob;
    	}
    	opt.setFileName(resolvedFileName);
    	if (opt.getImporter() == null) {
			if (FileHelper.isProjectLibreFile(resolvedFileName)) {
				opt.setImporter(LOCAL_PROJECT_IMPORTER);
			} else if (FileHelper.isMpoFile(resolvedFileName)) {
				opt.setImporter(MPO_PROJECT_IMPORTER);
    		} else {
    			opt.setImporter(MICROSOFT_PROJECT_IMPORTER);
    		}
    	}
    final Job job=new Job(jobQueue,"loadProject","Loading...",true);
        job.setCancelMonitorClosure(new Consumer<Object>() { public void accept(Object o) {
				logger.fine("Monitor Canceled");
				jobQueue.endCriticalSection(job);
			}
        });
		try {
			final FileImporter importer = ClassUtils.forName(opt.getImporter()).asSubclass(FileImporter.class)
				.getDeclaredConstructor().newInstance();
	    	importer.setFileName(opt.getFileName());
	    	importer.setFileInputStream(opt.getFileInputStream());
	    	importer.setResourceMapping(opt.getResourceMapping());
	    	importer.setProjectFactory(ProjectFactory.getInstance());//used?
	    	importer.setJobQueue(jobQueue);
	        
	        job.addSwingRunnable(new JobRunnable("LocalAccess: loadProject.begin",1.0f){
	    		public Object run() throws Exception{
	    			ResourcePool resourcePool=null;
	    			if (MICROSOFT_PROJECT_IMPORTER.equals(opt.getImporter())){
	    				DataFactoryUndoController undoController=new DataFactoryUndoController();
	    				resourcePool = ResourcePoolFactory.getInstance().createResourcePool("",undoController);
	    				resourcePool.setLocal(importer.getResourceMapping()==null);
	    				Project project = Project.createProject(resourcePool,undoController);
	    				
	    				((DefaultNodeModel)project.getTaskOutline()).setDataFactory(project);		
	    				importer.setProject(project);
	    			}
	     			setProgress(1.0f);
	                return null;
	    		}
	        });
	    	job.addJob(importer.getImportFileJob());
	        job.addRunnable(new JobRunnable("LocalAccess: loadProject.end",1.0f){
	    		public Object run() throws Exception{
	    	    	Project project=importer.getProject();
	    	    	if (project == null) {
	    	    		setProgress(1.0f);
	    	    		return null;
	    	    	}
	    	    	project.setFileName(opt.getFileName()); //overrides project name
	    			if (MICROSOFT_PROJECT_IMPORTER.equals(opt.getImporter()))
	    				project.getResourcePool().setName(project.getName());
	    			if (Environment.getStandAlone()){ //force local in this case
	    				project.setMaster(true); //local project is always master
	    				project.setLocal(true);
	    			}
	     			setProgress(1.0f);
	                return project;
	 			
	    		}
	    	});
		} catch (ReflectiveOperationException | ClassCastException e) {
			logger.log(Level.WARNING, "Failed to create importer", e);
		}
     	return job;
    }

    
    public static FileImporter getImporter(String name){
		FileImporter importer=null;
		try {
			importer = ClassUtils.forName(name).asSubclass(FileImporter.class)
				.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException | ClassCastException e) {
			logger.log(Level.WARNING, "Failed to create importer", e);
		}
    	return importer;
    }
    
    
    public Job getSaveProjectJob(final List<Project> projs,final SaveOptions opt){
    	final String title="Saving";
		final Job job=new Job(jobQueue,"saveProject",title+"...",true);
        job.setCancelMonitorClosure(new Consumer<Object>() { public void accept(Object o) {
				logger.fine("Monitor Canceled");
				jobQueue.endCriticalSection(job);
			}
        });
        //claur
//        FileImporter importer=getImporter(opt.getImporter());
//		importer.setJobQueue(jobQueue);
//		importer.setProjectFactory(ProjectFactory.getInstance());//used?
		int count=projs.size();
		int i=0;
		for (final Project project : projs) {
			//if projs.size()>1 opt.getFileName() must be null
			String fileN=(opt.getFileName()==null)?project.getGuessedFileName():opt.getFileName();//+(count>1?("("+i+")"):""));
			if (!FileHelper.isFileNameAllowed(fileN, true)){
				fileN=SessionFactory.getInstance().getLocalSession().chooseFileName(true,FileHelper.changeFileExtension(fileN, FileHelper.MPO_FILE_TYPE));
			}
			// POD remains byte-compatible with the legacy format and therefore
			// intentionally has no CCPM payload. Keep CCPM projects lossless by
			// choosing the open mpo container instead.
			if (FileHelper.isProjectLibreFile(fileN) && new CriticalChainService().requiresMpo(project)) {
				fileN = FileHelper.changeFileExtension(fileN, FileHelper.MPO_FILE_TYPE);
			}
			final String fileName=fileN;
			if (fileName==null) continue;
			
			//claur saving mpp as pod was selecting xml exporter
			if (FileHelper.isProjectLibreFile(fileName)){ //$NON-NLS-1$
				opt.setFileName(fileName);
				opt.setImporter(LocalSession.LOCAL_PROJECT_IMPORTER);
			}
			else if (FileHelper.isMpoFile(fileName)) {
				opt.setFileName(fileName);
				opt.setImporter(LocalSession.MPO_PROJECT_IMPORTER);
			}
			else{
				opt.setFileName(fileName/*+((fileName.endsWith(".xml"))?"":".xml")*/);
				opt.setImporter(LocalSession.MICROSOFT_PROJECT_IMPORTER);

			}
	        FileImporter importer=getImporter(opt.getImporter());
			importer.setJobQueue(jobQueue);
			importer.setProjectFactory(ProjectFactory.getInstance());//used?

			
			
			importer.setFileName(fileName);
			importer.setProject(project);
			if (opt.getPreSaving() != null)
				opt.getPreSaving().accept(project);

			job.addJob(importer.getExportFileJob());
			job.addSwingRunnable(new JobRunnable("Local: saveProject end"){
				public Object run() throws Exception{
					if (!opt.isRecoverySnapshot()) {
						project.setFileName(fileName);
					project.setGroupDirty(false);
					}
					if (opt.getPostSaving()!=null) opt.getPostSaving().accept(project);
	    	    	return null;
				}
			});


        	//setProgress(((float)++i)/((float)count));
		}
		job.addExceptionRunnable(new JobRunnable("Local: exception"){
			public Object run() throws Exception{
				Exception failure = job.getFailureException();
				logSaveFailure(opt.getFileName(), opt.isRecoverySnapshot(), failure);
				// Recovery snapshots run in the background. Their failure must not
				// interrupt the user with a dialog.
				if (!opt.isRecoverySnapshot()) {
					Alert.error(Messages.getString("Message.saveError"));
				}
				return null;
			}
		});
    	return job;
     }

	private void logSaveFailure(String fileName, boolean recoverySnapshot, Exception failure) {
		String operation = recoverySnapshot ? "Automatic recovery snapshot save failed" : "Project save failed";
		logJobFailure(operation + "; file=" + fileName, failure);
	}

	private void logJobFailure(String operation, Exception failure) {
		if (failure == null) {
			logger.warning(operation + "; the job did not expose an exception");
		} else {
			logger.log(Level.WARNING, operation, failure);
		}
	}
    
    
    public String chooseFileName(final boolean save,String selectedFileName){
    	return com.microproject.util.UiServices.getFileChooserProvider().chooseFileName(save, selectedFileName, getJobQueue().getComponent());
    }
    
    public static String getImporter(int fileType){
    	switch (fileType) {
		case FileHelper.PROJECTLIBRE_FILE_TYPE: return LOCAL_PROJECT_IMPORTER;
		case FileHelper.MPO_FILE_TYPE: return MPO_PROJECT_IMPORTER;
		case FileHelper.MSP_FILE_TYPE: return MICROSOFT_PROJECT_IMPORTER;
		default:
			return null;
		}
    }
	public boolean projectExists(long id) {
		synchronized (descriptorFiles) {
			return descriptorFiles.containsKey(Long.valueOf(id));
		}
	}

   
}
