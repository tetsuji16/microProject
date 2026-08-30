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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.AssignmentNodeModel;
import com.microproject.grouping.core.model.DefaultNodeModel;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelUtil;
import com.microproject.grouping.core.summaries.DeepChildWalker;
import com.microproject.job.Job;
import com.microproject.job.JobRunnable;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourcePoolFactory;
import com.microproject.server.data.DataUtil;
import com.microproject.session.CreateOptions;
import com.microproject.session.LoadOptions;
import com.microproject.session.LocalSession;
import com.microproject.session.SaveOptions;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.Environment;

/**
 *
 */
public class ProjectFactory {
	private static final Logger logger = Logger.getLogger(ProjectFactory.class.getName());
	private static int untitledCount = 0;
	private String server = null;
	Portfolio portfolio; // for now just one portfolio.  Perhaps portfolio should reference project factory and not like this
	private static ProjectFactory projectFactory;
	public static ProjectFactory getInstance() {
		if (projectFactory==null) projectFactory=new ProjectFactory();
		return projectFactory;
	}
	public static ProjectFactory createInstance() {
		return new ProjectFactory();
	}

	private ProjectFactory() {
		portfolio = new Portfolio(this);
	}

	//CREATE PROJECTS

//	public Project createProject(String name,boolean local) {
//		return createProject(name,local,true,true);
//	}
//	public Project createProject(String name, boolean local, boolean addResources,boolean verify) {
//		Project project = createProject(null,local,name,addResources,verify);
//		return project;
//	}
//
	public Project createProject() {
		CreateOptions opt=new CreateOptions();
		opt.setLocal(Environment.getStandAlone());
		opt.setName(Messages.format("Format.words", Messages.getString("Text.Untitled"), ++untitledCount));
		return createProject(opt);
	}
//	public Project createProject(boolean addResources,boolean local) {
//		Project project = createProject(Messages.getString("Text.Untitled") + " " + ++untitledCount,local,addResources,true);
//		return project;
//	}


//	public Project createProject(ResourcePool resourcePool, boolean local, String name) {
//		return createProject(resourcePool,local,name,!local,true);
//	}
//	public Project createProject(ResourcePool resourcePool, boolean local, String name, boolean addResources,boolean verify) {
	private Project createProjectAsync(CreateOptions opt) {
		DataFactoryUndoController undoController=new DataFactoryUndoController();
		ResourcePool resourcePool=opt.getResourcePool();
		if (resourcePool == null){
			resourcePool = ResourcePoolFactory.getInstance().createResourcePool(opt.getName(),undoController);
			resourcePool.setLocal(opt.isLocal());
		}
		Project project = Project.createProject(resourcePool,undoController);
		undoController.setDataFactory(project);
		project.setName(opt.getName());
		if (opt.isLocal()) project.setMaster(true);

		//Don't forget to modify Serializer.deserialize too
		if (opt.isAddResources() &&!project.isLocal()){
			try {
				Session session=SessionFactory.getInstance().getSession(false);
				Object localAccess = hasSessionMethod(session, "isLocalAccess")
					? SessionFactory.callNoEx(session,"isLocalAccess",null,null) : null;
				List resources;
				if (Boolean.TRUE.equals(localAccess))
					resources=(List)SessionFactory.call(session,"retrieveResourceHierarchy",null,null);
				else if (hasSessionMethod(session, "getLoadResourceHierarchyJob", boolean.class, List.class)){
					resources=new ArrayList();

					Job job=(Job)SessionFactory.callNoEx(session,"getLoadResourceHierarchyJob", new Class[]{boolean.class,List.class},new Object[]{true,resources});
					if (job != null) {
						job.addSync();
						session.schedule(job);
					}
					//job.waitResult();
				} else {
					resources=new ArrayList();
				}
				DataUtil.setEnterpriseResources(resources,resourcePool);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Unexpected error", e);
			}
		}

		project.setInitialized(true);
		//two following lines inverted to have a NodeModel with dataFactory set in DocumentFrame
		((DefaultNodeModel)project.getTaskOutline()).setDataFactory(project);
		addProject(project,!opt.isSync(),opt.isVerify());
		logger.fine("Project returned");
		return project;
	}

	private static boolean hasSessionMethod(Session session, String methodName, Class<?>... parameterTypes) {
		if (session == null) return false;
		try {
			session.getClass().getMethod(methodName, parameterTypes);
			return true;
		} catch (NoSuchMethodException | SecurityException ignored) {
			return false;
		}
	}
	public Project createProject(final CreateOptions opt) {
		JobRunnable runnable=new JobRunnable("Local: create Project"){
			public Object run() throws Exception{
				return createProjectAsync(opt);
			}
		};
		if (opt.isSync()){
	    	Job job=new Job(SessionFactory.getInstance().getJobQueue(),"createProject","Creating project...",false);
	    	job.addRunnable(runnable);
			job.addSync();
			SessionFactory.getInstance().schedule(job);
			try {
				Project project=(Project)job.waitResult();
				logger.fine("Project returned end lock");
				return project;
			} catch (Exception e) {//Forward exception + Alert
				logger.log(Level.SEVERE, "Unexpected error", e);
				return null;
			}

		}else{
			try {
				return (Project)runnable.run();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Unexpected error", e);
				return null;
			}
		}
	}




	public void addProject(Project project,boolean verify) {
		addProject(project,true,verify);
	}
	public void addProject(Project project,boolean createJob,boolean verify) {
		portfolio.addProject(project,createJob,verify);
	}




//	public Project openDownloadedProject() {
//		return null;
//	}

	protected Set loadingProjects=new HashSet();
	protected Set closingProjects=new HashSet();
	private final Map<Long, List<Runnable>> projectClosedCallbacks = new HashMap<>();
	public synchronized Set getOpenOrLoadingProjects(){
		final Set projectIds=new HashSet();
    	ProjectFactory.getInstance().getPortfolio().forProjects(new Consumer<Object>() { public void accept(Object impl) {
    			Project project=(Project)impl;
        		projectIds.add(Long.valueOf(project.getUniqueId()));
    		}
    	});
    	projectIds.addAll(loadingProjects);
    	projectIds.removeAll(closingProjects);
    	return projectIds;
	}
	public synchronized void addLoadingProject(long id){
		loadingProjects.add(Long.valueOf(id));
	}
	public synchronized void removeLoadingProject(long id){
		loadingProjects.remove(Long.valueOf(id));
	}
	public synchronized void addClosingProjects(Collection ids){
		closingProjects.addAll(ids);
	}
	private synchronized boolean beginClosingProjects(Collection<Long> ids) {
		for (Long id : ids) {
			if (closingProjects.contains(id))
				return false;
		}
		closingProjects.addAll(ids);
		return true;
	}
	public synchronized void addClosingProject(long id){
		closingProjects.add(Long.valueOf(id));
	}
	public void removeClosingProject(long id){
		completeProjectClosing(id);
	}
	public synchronized boolean isProjectClosing(long id) {
		return closingProjects.contains(Long.valueOf(id));
	}
	public void runAfterProjectClosed(long id, Runnable callback) {
		boolean runNow;
		synchronized (this) {
			runNow = !closingProjects.contains(Long.valueOf(id));
			if (!runNow)
				projectClosedCallbacks.computeIfAbsent(Long.valueOf(id), ignored -> new ArrayList<>()).add(callback);
		}
		if (runNow)
			callback.run();
	}
	void completeProjectClosing(long id) {
		completeProjectClosings(java.util.Collections.singleton(Long.valueOf(id)));
	}
	void completeProjectClosings(Collection<Long> ids) {
		List<Runnable> callbacks = new ArrayList<>();
		synchronized (this) {
			for (Long id : ids) {
				closingProjects.remove(id);
				List<Runnable> projectCallbacks = projectClosedCallbacks.remove(id);
				if (projectCallbacks != null)
					callbacks.addAll(projectCallbacks);
			}
		}
		for (Runnable callback : callbacks) {
			try {
				callback.run();
			} catch (RuntimeException e) {
				logger.log(Level.WARNING, "Project close callback failed for " + ids, e);
			}
		}
	}


	//OPEN PROJECTS


	public Project openProject(final LoadOptions opt) {
		Session session=SessionFactory.getInstance().getSession(opt.isLocal());
		Job job=null;
		final boolean recover;
		if (opt.getId()>0){
			Project p = findFromId(opt.getId());
			if (p != null && !opt.isOpenAs()){
				job=session.getEmptyJob("Recover project",p);
				recover=true;
			}else{
				addLoadingProject(opt.getId());
				recover=false;
			}
		}else recover=false;

		if (job==null) job=session.getLoadProjectJob(opt);
		job.addSwingRunnable(new JobRunnable("Local: addProject"){
			public Object run() throws Exception{
				Project project=(Project)getPreviousResult();
				if (!recover){
					if (project!=null) addProject(project,false,true);
					if (opt.getId()>0) removeLoadingProject(opt.getId());
				}
				if (opt.getEndSwingClosure()!=null) opt.getEndSwingClosure().accept(project);


				if (project != null && opt.isOpenAs() && project.isMaster())
					project.setReadOnly(true); // don't allow copy of master

				if (project != null && opt.isOpenAs()) {
					project.setReadOnly(true);
					project.setLocal(true);
				}
				return project;
			}
		},false);
		if (opt.isSync()) job.addSync();
		session.schedule(job);
		try {
			return (opt.isSync())?(Project)job.waitResult():null;
		} catch (Exception e) {//Forward exception + Alert
			return null;
		}
	}

	public Project findFromId(long id) {
		return portfolio.findByUniqueId(id);
	}

	public Project openSubproject(final Project parent, final Node subprojectNode, final boolean creating) {
		final SubProj subprojectTask = (SubProj)subprojectNode.getImpl();
		final long id = subprojectTask.getSubprojectUniqueId();
		Project openSubproject = portfolio.findByUniqueId(id);
		if (openSubproject != null) {
			parent.getSubprojectHandler().addSubproject(openSubproject, subprojectNode,creating, true);
			portfolio.handleExternalTasks(openSubproject,true, false);  // resolve external links if any
			return openSubproject;
		}

		final Session session=SessionFactory.getInstance().getSession(false); // never local
		if (!session.projectExists(id)) {
			Alert.error(Messages.getString("Error.projectDoesNotExist"));
			return null;
		}

		addLoadingProject(id);

		LoadOptions opt=new LoadOptions();
		opt.setSubproject(true);
		opt.setId(id);
		Job job=session.getLoadProjectJob(opt);
		subprojectTask.setFetching(true);

		job.addSwingRunnable(new JobRunnable("Local: insertProject"){
			public Object run() throws Exception{
				try {
					Project subproject = (Project)getPreviousResult();

					//add assignments in the outline, paste uses only assignments present in the nodeModel
					AssignmentNodeModel parentModel = (AssignmentNodeModel)subproject.getTaskOutline();
					parentModel.addAssignments(parentModel.iterator()); // assignments

					if (subproject != null) {// is it possible it can be null?
						parent.getSubprojectHandler().addSubproject(subproject, subprojectNode,creating, false);
						if (subproject.isReadOnly()){
							Alert.warn(MessageFormat.format(Messages.getString("Message.readOnlySubproject"),new Object[]{subproject.getName()}));
						}
//
//						subproject.setGroupDirty(true);
//						//TODO something more precise here
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Unexpected error", e);
					throw e;
				} finally {
					subprojectTask.setFetching(false);
					removeLoadingProject(id);

				}
    	    	return null; //return not used anyway
			}
		},false);

		session.schedule(job);
		return ((Task)subprojectTask).getProject();
	}





	//SAVE PROJECTS



	public void saveProject(final Project project, final SaveOptions opt) {
		Job job=getSaveProjectJob(project,opt);
		Session session=SessionFactory.getInstance().getSession(opt.isLocal());
		if (job!=null){
			if (opt.isSync()) job.addSync();
			session.schedule(job);
			try {
				if (opt.isSync()) job.waitResult();
			} catch (Exception e) {
				if (e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				logger.log(Level.WARNING, "Error while waiting for job result", e);
			}
		}
	}

	public Job getSaveProjectJob(final Project project, final SaveOptions opt){
		// Save the project and all of its subprojects
		final List<Project> projects=new ArrayList<>();
		DeepChildWalker.recursivelyTreatBranch(portfolio.getNodeModel(), project,  new Consumer<Object>() {
			boolean dirty=false;
			public void accept(Object arg0) {
				Node n=(Node)arg0;
				Object impl = n.getImpl();
				if (impl instanceof Project){
					Project p=(Project)impl;
					if (((Node)n.getParent()).isRoot()) dirty=p.needsSaving(); //&& !p.isReadOnly();
					if (dirty||opt.isSaveAs()||opt.getImporter()!=null) {
						p.setEarliestAndLatestDatesFromSchedule();  // we want subprojects to have their dates set by external constraints if any
						projects.add(p);
					}
				}
			}
		});
		if (projects.size()>0){
			Session session=SessionFactory.getInstance().getSession(opt.isLocal());
			final SaveOptions o=(SaveOptions)opt.clone();
			o.setPostSaving(new Consumer<Object>() { public void accept(Object obj) {
					Project p = (Project)obj;
					if (!opt.isRecoverySnapshot()) {
						p.setAllTasksAsUnchangedFromPersisted(true);
						p.validateNewDistributionMap();
						portfolio.handleExternalTasks(p,false, true); 		// external link handling
					}
					if (opt.getPostSaving()!=null) opt.getPostSaving().accept(obj); //id, combobox update
				}
			});
			Job job=session.getSaveProjectJob(projects,o);
			return job;
		}
		return null;
	}




	//CLOSE PROJECTS



	public Job getCloseProjectsOnServerJob(Project project){
		// Save the project and all of its subprojects
		final List<Project> projects=new ArrayList<>();
		DeepChildWalker.recursivelyTreatBranch(portfolio.getNodeModel(), project,  new Consumer<Object>() { public void accept(Object arg0) {
				Object impl = ((Node)arg0).getImpl();
				if (impl instanceof Project){
					projects.add((Project) impl);
				}
			}
		});
		if (projects.size()>0){
			Session session=SessionFactory.getInstance().getSession(project.isLocal()); //assume same type for subprojets
			Job job=session.getCloseProjectsJob(projects);
			return job;
		}
		return null;
	}


	public Job getCloseProjectsOnServerJob(Collection projects) {
		int projectCount = projects == null ? 0 : projects.size();
		List<Project> localProjects=new ArrayList<Project>(projectCount);
		List<Project> serverProjects=new ArrayList<Project>(projectCount);
		for (Project project : (Collection<Project>)projects) {
			if (project.isReadOnly()) continue;
			if (project.isLocal()) localProjects.add(project);
			else serverProjects.add(project);
		}
		Job job=null;
		if (localProjects.size()>0) job=SessionFactory.getInstance().getLocalSession().getCloseProjectsJob(projects);
		if (serverProjects.size()>0){
			Job j=SessionFactory.getInstance().getSession(false).getCloseProjectsJob(projects);
			if (job==null) job=j;
			else job.addJob(j);
		}
		return job;
	}

	public int promptForSave(Project project, boolean allowCancel) {
		String text = Messages.format("Format.threeParts",
				Messages.getString("Message.saveProjectBeforeClosing1"), getDisplayNameForSavePrompt(project),
				Messages.getString("Message.saveProjectBeforeClosing2"));
		if (allowCancel)
			return Alert.confirm(text);
		else
			return Alert.confirmYesNo(text);
	}

	static String getDisplayNameForSavePrompt(Project project) {
		if (project == null)
			return Messages.getString("Text.Untitled");
		String fileName = project.getFileName();
		if (fileName != null && fileName.trim().length() > 0)
			return new File(fileName).getName();
		String projectName = project.getName();
		if (projectName != null && projectName.trim().length() > 0)
			return projectName;
		return Messages.getString("Text.Untitled");
	}

	/**
	 * @param project
	 * @param allowCancel
	 * @param prompt
	 * @return null if cancelled
	 */
	public Job getRemoveProjectJob(final Project project, boolean allowCancel, boolean prompt,boolean calledFromSwing) {
		Job job=null;
		if (prompt && project.needsSaving()) {
//			final boolean[] lock=new boolean[]{false};
//				SwingUtilities.invokeLater(new Runnable(){
//					public void run(){
//						Alert.okCancel("test");
//						synchronized (lock) {
//							lock[0]=true;
//							lock.notifyAll();
//						}
//				    }
//				});
//			synchronized(lock){
//				while (!lock[0]){
//					try{
//							lock.wait();
//						}catch (InterruptedException e) {}
//				}
//			}

			int promptResult = promptForSave(project,allowCancel);
			if (promptResult == JOptionPane.YES_OPTION){
				SaveOptions opt=new SaveOptions();
				opt.setLocal(project.isLocal());
				if (project.isLocal()){
					String fileName=project.getFileName();
					if (fileName==null){
						fileName=SessionFactory.getInstance().getLocalSession().chooseFileName(true,project.getGuessedFileName());
					}
					if (fileName==null) return null;
					project.setFileName(fileName);
					opt.setFileName(fileName);
					opt.setImporter(LocalSession.getImporter(project.getFileType()));
				}
				job=getSaveProjectJob(project, opt);
			}
			else if (promptResult == JOptionPane.CANCEL_OPTION)
				return null;
		}

		final ArrayList toRemove = new ArrayList();
		final ArrayList projects = new ArrayList();
		DeepChildWalker.recursivelyTreatBranch(portfolio.getNodeModel(), project,  new Consumer<Object>() { public void accept(Object arg0) {
				Node node = (Node)arg0;
				Object impl = node.getImpl();
				if (!(impl instanceof Project))
					return;
				final Project p = (Project)impl;
				toRemove.add(node);
				if (Environment.getStandAlone()||project.isLockable()){
					projects.add(p);
				}
			}
		});


		Job closeProjectJob=getCloseProjectsOnServerJob(projects);
		if (closeProjectJob==null){
			closeProjectJob=new Job(SessionFactory.getInstance().getJobQueue(),"closeProjects","Closing...",false);

		}
		if (job==null) job=closeProjectJob;
		else job.addJob(closeProjectJob);


		job.addRunnable(new JobRunnable("Local: closeProjects"){
			public Object run() throws Exception{
				Iterator i = toRemove.iterator();
				while (i.hasNext()) {
					Node node = (Node)i.next();
					Project p = (Project)node.getImpl();
					portfolio.handleExternalTasks(p,false,false); 		// external link handling
					p.getResourcePool().removeProject(p);
					p.disconnect();
					portfolio.getObjectEventManager().fireDeleteEvent(this,p);
					portfolio.getNodeModel().remove(node,NodeModel.EVENT);

				}
				System.gc(); // clean up memory used by projects
   	    	return null; //return not used anyway
			}
		},/*!calledFromSwing*/false,false,calledFromSwing,false);
		return job;
	}

	public void removeProject(final Project project, boolean allowCancel, boolean prompt,boolean calledFromSwing) {
		if (project == null)
			return;
		final Set<Long> closingIds = collectProjectBranchIds(project);
		if (closingIds.isEmpty() || containsClosingProject(closingIds))
			return;
		Job job=getRemoveProjectJob(project,allowCancel,prompt,calledFromSwing);
		if (job != null) { // if not cancelled
			if (!beginClosingProjects(closingIds))
				return;
			job.addCompletionRunnable(() -> {
				Runnable complete = () -> completeProjectClosings(closingIds);
				if (calledFromSwing && !SwingUtilities.isEventDispatchThread())
					SwingUtilities.invokeLater(complete);
				else
					complete.run();
			});
			Session session=SessionFactory.getInstance().getSession(project.isLocal());
			try {
				session.schedule(job);
			} catch (RuntimeException e) {
				completeProjectClosings(closingIds);
				throw e;
			}
		}
	}

	private synchronized boolean containsClosingProject(Collection<Long> ids) {
		for (Long id : ids) {
			if (closingProjects.contains(id))
				return true;
		}
		return false;
	}

	private Set<Long> collectProjectBranchIds(final Project project) {
		final Set<Long> ids = new HashSet<>();
		ids.add(Long.valueOf(project.getUniqueId()));
		DeepChildWalker.recursivelyTreatBranch(portfolio.getNodeModel(), project, new Consumer<Object>() { public void accept(Object value) {
				Node node = (Node) value;
				if (node.getImpl() instanceof Project descendant)
					ids.add(Long.valueOf(descendant.getUniqueId()));
			}
		});
		return ids;
	}

	public void doRemoveProject(Project project,boolean calledFromSwing) {
		Job job=projectFactory.getPortfolio().getRemoveProjectJob(project,calledFromSwing);
		if (job!=null) {
			SessionFactory.getInstance().getSession(project.isLocal()).schedule(job);
			portfolio.handleExternalTasks(project,false, false); 		// external link handling
		}

	}
	/**
	 * @return Returns the portfolio.
	 */
	public Portfolio getPortfolio() {
		return portfolio;
	}
	/**
	 * @return Returns the server.
	 */
	public final String getServer() {
		return server;
	}
	/**
	 * @param server The server to set.
	 */
	public final void setServer(String server) {
		this.server = server;
	}

	public Collection getDirtyProjectList() {
		return portfolio.getDirtyProjectList();
	}
	public Collection getWritableProjectsList() {
		return portfolio.getWritableProjectList();
	}
	public static Object getProjectData(long projectId) {
		Session session = SessionFactory.getInstance().getSession(false);
		return SessionFactory.callNoEx(session, "getProjectData", new Class[] {Long.class}, new Object[] {projectId});


	//	getProjectData(projectId);

	}
	public boolean isResourcePoolOpenAndWritable() {
		return portfolio.isResourcePoolOpenAndWritable();
	}

}
