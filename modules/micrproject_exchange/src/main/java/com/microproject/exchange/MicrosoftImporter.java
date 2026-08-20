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
import com.microproject.pm.calendar.CalendarOptions;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.scheduling.ScheduleFrom;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.configuration.CircularDependencyException;
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
import java.util.logging.Logger;
/**
 * This class is based on the project mpxj http://www.tapsterrock.com/mpxj/
 * The enumerated types in projectlibre currently correspond exactly to the types in mpx, so there is no need to convert them.
 * However, if the projectlibre enumerations change, it will be necessary to map them to mpx types.
 *
 */
public class MicrosoftImporter extends ServerFileImporter{
	private static final Logger logger = Logger.getLogger(MicrosoftImporter.class.getName());
	protected com.microproject.pm.task.Project plProject= null;
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
		logger.info("-------MicrosoftImporter ctor");
	}


	@Override
	public void importFile() throws Exception {
		logger.info("BEGIN: MicrosoftImporter.PrepareResources");
		parse();
		logger.info("END: MicrosoftImporter.PrepareResources");
		Environment.setImporting(false);
		logger.info("BEGIN: Finish import");
		convertToProjectLibre1();
		logger.info("END: Finish import");
	}

	@Override
	public Project loadProject(InputStream in)  throws Exception{
		logger.info("BEGIN: MicrosoftImporter.PrepareResources");
		parse(in, getFileExtension());
		logger.info("END: MicrosoftImporter.PrepareResources");
		Environment.setImporting(false);
		logger.info("BEGIN: Finish import");
		convertToProjectLibre1();
		logger.info("END: Finish import");
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
			logger.info("Progress " + 100 * p + "%");
		else
			jobRunnable.setProgress(p);
	}
	public void importProject(Project p) throws Exception {
		logger.info("MicrosoftImporter.importProject()");

		this.project = p;
		parse();
		convertToProjectLibre1();
	}
	public void parse(InputStream in, String extension) throws Exception {
		logger.info("MicrosoftImporter.parse()");

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
		logger.info(plProject.toString());

		setProgress(0.2f);
		setProgress(1f);

	}
	public void parse() throws Exception {
		logger.info("MicrosoftImporter.parse()");

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
		logger.info(plProject.toString());

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
		logger.info("MicrosoftImporter.getImportFileJob()");

    	subprojects = new ArrayList<>();
    	errorDescription = null;
    	lastException = null;
    	Session session=SessionFactory.getInstance().getSession(resourceMapping==null);
		Job job=new Job(session.getJobQueue(),"importFile",Messages.getString("MicrosoftImporter.Importing"),true); //$NON-NLS-1$ //$NON-NLS-2$

//    	job.addRunnable(new JobRunnable(Messages.getString("MicrosoftImporter.PrepareResources"),1.0f){ //$NON-NLS-1$
//
//			public Object run() throws Exception{
//				logger.info("BEGIN: MicrosoftImporter.PrepareResources");
//				//MicrosoftImporter.this.jobRunnable = this;
//				importFile();
//				logger.info("END: MicrosoftImporter.PrepareResources");
//				return null;
//			}
//    	});

		
    	job.addRunnable(new JobRunnable(Messages.getString("MicrosoftImporter.PrepareResources"),1.0f){ //$NON-NLS-1$

			public Object run() throws Exception{
				logger.info("BEGIN: MicrosoftImporter.PrepareResources");
				MicrosoftImporter.this.jobRunnable = this;
				parse();
				logger.info("END: MicrosoftImporter.PrepareResources");
				return null;
			}
    	});
    	
    	job.addSwingRunnable(new JobRunnable("Import resources",1.0f){ //$NON-NLS-1$
			public Object run() throws Exception{
				logger.info("BEGIN: Import resources");
				ResourceMappingForm form=getResourceMapping();
				if (form!=null&&form.isLocal()) //if form==null we are in a case were have no server access. popup not needed
					if (!job.okCancel(Messages.getString("Message.ServerUnreacheableReadOnlyProject"),true)){ //$NON-NLS-1$
						setProgress(1.0f);
						errorDescription = ABORT;
						Environment.setImporting(false); // will avoid certain popups
						throw new Exception(ABORT);
					}

// claur - Moved to convertToProjectLibre1 after import Calendar because base calendar must be imported before resources
//			logger.info("import resources");		 //$NON-NLS-1$
//				if(!importResources()){
//					setProgress(1.0f);
//					errorDescription = ABORT;
//					Environment.setImporting(false); // will avoid certain popups
//					throw new Exception(ABORT);
//				}
				setProgress(1f);
				logger.info("END: Import resources");
				return null;
	    	}
    	});
    	job.addRunnable(new JobRunnable("Finish import",1.0f){ //$NON-NLS-1$
			public Object run() throws Exception{
				logger.info("BEGIN: Finish import");
				Object r=convertToProjectLibre1();
				logger.info("END: Finish import");
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
		// The microproject model is now the single source of truth: MspImporter
		// already produces a complete microproject Project, so the legacy
		// two-model conversion pipeline is obsolete. Just adopt the imported
		// project directly (see issue #154).
		this.project = plProject;
		Environment.setImporting(false);
		setProgress(1.0f);
		return this.project;
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
		writer.write(serializer.serializeProject(project), out);
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
		return name.substring(extensionPosition + 1).toLowerCase(java.util.Locale.ROOT);
	}


	protected void importCalendars() throws Exception{
		// Obsolete two-model conversion removed; MspImporter produces the
		// microproject Project directly (see issue #154).
	}


	/**
	 * This method imports all resources defined in the file into the projectlibre1 model
	 *
	 * @param file
	 *            MPX file
	 */
	protected void importLocalResources(){
		// Obsolete two-model conversion removed; MspImporter produces the
		// microproject Project directly (see issue #154).
	}


	protected boolean importResources() throws Exception{
		// Obsolete two-model conversion removed (see issue #154).
		return true;
	}

	@SuppressWarnings("unchecked")
	protected boolean importResources(Map<Number, Object> resourceMap,Consumer<Object> importLocalResources) throws Exception{
		// Obsolete two-model conversion removed (see issue #154).
		return true;
	}

	protected void retrieveResourcesForMerge(List existingResources) throws Exception{

	}




	protected void importOptions() throws Exception{
		// Obsolete two-model conversion removed (see issue #154).
	}

	private void importProjectFields() {
		// Obsolete two-model conversion removed (see issue #154).
	}

	/**
	 * This method imports all tasks defined in the file into the projectlibre1 model
	 *
	 */
	private void importTasks() {
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
	}
	


	/**
	 * Import dependencies. Must be done after importing tasks
	 *
	 * @throws Exception
	 */
	public void importDependencies() throws Exception {
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
	}


	/**
	 * Import mpx assignments into projectlibre1 model
	 *
	 */
	protected void importAssignments() {
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
	}

	private void applyImportedTrackingFields() {
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
	}

	void applyImportedTrackingFields(Task task, NormalTask opTask) {
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
		// obsolete two-model conversion removed (issue #154)
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
