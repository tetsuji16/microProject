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
import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;



import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeBridge;
import com.microproject.grouping.core.model.AssignmentNodeModel;
import com.microproject.grouping.core.model.DefaultNodeModel;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelUtil;
import com.microproject.grouping.core.summaries.DeepChildWalker;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.job.JobRunnable;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourcePoolFactory;
import com.microproject.util.FilePathUtils;
import com.microproject.session.CreateOptions;
import com.microproject.session.FileHelper;
import com.microproject.session.LoadOptions;
import com.microproject.session.LocalSession;
import com.microproject.session.SaveOptions;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.Environment;

import com.microproject.util.UiDispatch;

/**
 *
 */
public class ProjectFactory {
	private static final Logger logger = Logger.getLogger(ProjectFactory.class.getName());
	private static int untitledCount = 0;
	private Runnable afterSubprojectProjection = () -> {};
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

	void setAfterSubprojectProjection(Runnable hook) {
		afterSubprojectProjection = hook == null ? () -> {} : hook;
	}

	public Project createProject() {
		CreateOptions opt=new CreateOptions();
		opt.setLocal(true);
		opt.setName(Messages.format("Format.words", Messages.getString("Text.Untitled"), ++untitledCount));
		return createProject(opt);
	}
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

		project.setInitialized(true);
		//two following lines inverted to have a NodeModel with dataFactory set in DocumentFrame
		((DefaultNodeModel)project.getTaskOutline()).setDataFactory(project);
		addProject(project,!opt.isSync(),opt.isVerify());
		logger.fine("Project returned");
		return project;
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
	protected Set<Long> loadingProjects = new HashSet<>();
	private final Map<Long, Integer> loadingProjectReferences = new HashMap<>();
	protected Set<Long> closingProjects = new HashSet<>();
	private final Map<Long, List<Runnable>> projectClosedCallbacks = new HashMap<>();
	public synchronized Set<Long> getOpenOrLoadingProjects(){
		Set<Long> projectIds = new HashSet<>();
		ProjectFactory.getInstance().getPortfolio().forProjects(project ->
				projectIds.add(Long.valueOf(project.getUniqueId())));
    	projectIds.addAll(loadingProjects);
    	projectIds.removeAll(closingProjects);
    	return projectIds;
	}
	public synchronized void addLoadingProject(long id){
		Long projectId = Long.valueOf(id);
		loadingProjectReferences.merge(projectId, 1, Integer::sum);
		loadingProjects.add(projectId);
	}
	public synchronized void removeLoadingProject(long id){
		Long projectId = Long.valueOf(id);
		Integer references = loadingProjectReferences.get(projectId);
		if (references == null || references <= 1) {
			loadingProjectReferences.remove(projectId);
			loadingProjects.remove(projectId);
		} else {
			loadingProjectReferences.put(projectId, references - 1);
		}
	}
	public synchronized void addClosingProjects(Collection<Long> ids){
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
		List<Runnable> callbacks = new ArrayList<>(ids.size());
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
		Session session=SessionFactory.getInstance().getLocalSession();
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
		Runnable terminalCleanup = () -> {
			if (!recover && opt.getId() > 0)
				removeLoadingProject(opt.getId());
		};
		AtomicReference<JobQueue.AdmissionTicket> admissionTicket = new AtomicReference<>();
		Consumer<Object> completion = registerOpenProjectCompletion(job, opt, terminalCleanup, () -> {
			JobQueue.AdmissionTicket ticket = admissionTicket.get();
			if (ticket != null)
				ticket.close();
		});
		job.addSwingRunnable(new JobRunnable("Local: addProject"){
			public Object run() throws Exception{
				Project project=(Project)getPreviousResult();
				return finishProjectOpen(project, completion, loadedProject -> {
					if (!recover){
						if (loadedProject!=null) {
							// A child belongs to the master outline; it must not create a
							// separate top-level document frame while loading.
							if (opt.isSubproject()) loadedProject.setOpenedAsSubproject(true);
							addProject(loadedProject,false,true);
						}
					}
					// A canonical local file can be recovered from the portfolio instead
					// of imported again.  Both that recovery route and a first import must
					// reattach a master's persisted child references.
					if (loadedProject != null && !opt.isSubproject())
						restoreLinkedLocalSubprojects(loadedProject);

					if (loadedProject != null && opt.isOpenAs() && loadedProject.isMaster())
						loadedProject.setReadOnly(true); // don't allow copy of master

					if (loadedProject != null && opt.isOpenAs()) {
						loadedProject.setReadOnly(true);
						loadedProject.setLocal(true);
					}
				});
			}
		},false);
		// Keep the admission key reserved until project registration and linked
		// child scheduling have completed on the EDT. Child files use their own
		// canonical keys, so this does not reject nested loads.
		job.addRunnable(new JobRunnable("Local: finish project registration") {
			@Override public Object run() { return getPreviousResult(); }
		});
		// Local file import failures occur on the job before the project can be
		// registered.  Without an exception handler they were only logged, leaving
		// an asynchronous open with neither a visible explanation nor a cleared
		// loading identity.  Keep existing documents untouched and report the
		// failing path directly to the user.
		job.addExceptionRunnable(new JobRunnable("Local: report project load failure") {
			public Object run() throws Exception {
				Exception failure = job.getFailureException();
				logger.log(Level.WARNING, "Project could not be opened: " + opt.getFileName(), failure);
				completion.accept(null);
				Alert.error(projectLoadFailureMessage(opt.getFileName(), failure));
				return null;
			}
		});
		if (opt.isSync()) job.addSync();
		session.getJobQueue().scheduleOrJoinRetained(job,
				() -> openAdmissionKey(opt),
				ticket -> admissionTicket.set(ticket), (activeJob, ticket) -> {
					try {
						completion.accept(null);
					} finally {
						ticket.close();
					}
				});
		try {
			return (opt.isSync())?(Project)job.waitResult():null;
		} catch (Exception e) {//Forward exception + Alert
			return null;
		}
	}

	/**
	 * Admission key of an open request. A request that carries its own input stream (the XML
	 * recovery of an unreadable file, started from inside the load of that same file) is a
	 * different load and must not join the active file load, which would never publish it.
	 */
	static String openAdmissionKey(LoadOptions opt) {
		String key = LocalSession.resolveProjectLoadAdmissionKey(opt.getFileName(), opt.getId());
		return key != null && opt.getFileInputStream() != null ? key + ":stream" : key;
	}

	@FunctionalInterface
	interface OpenProjectRegistration {
		void register(Project project) throws Exception;
	}

	static Project finishProjectOpen(Project project, Consumer<Object> completion,
			OpenProjectRegistration registration) throws Exception {
		try {
			registration.register(project);
			completion.accept(project);
			return project;
		} catch (Exception | Error failure) {
			completion.accept(null);
			throw failure;
		}
	}

	/**
	 * Routes all open outcomes through one callback and supplies a null result if
	 * cancellation prevents either the success or failure runnable from running.
	 */
	static Consumer<Object> registerOpenProjectCompletion(Job job, LoadOptions options, Runnable terminalCleanup) {
		return registerOpenProjectCompletion(job, options, terminalCleanup, () -> { });
	}

	static Consumer<Object> registerOpenProjectCompletion(Job job, LoadOptions options, Runnable terminalCleanup,
			Runnable afterUiDelivery) {
		Consumer<Object> endSwingClosure = options.getEndSwingClosure();
		AtomicBoolean delivered = new AtomicBoolean();
		Consumer<Object> completion = project -> {
			if (!delivered.compareAndSet(false, true))
				return;
			try {
				if (terminalCleanup != null)
					terminalCleanup.run();
			} finally {
				Runnable callback = () -> {
					try {
						if (endSwingClosure != null)
							endSwingClosure.accept(project);
					} finally {
						afterUiDelivery.run();
					}
				};
				if (UiDispatch.isDispatchThread())
					callback.run();
				else {
					try {
						UiDispatch.invokeLater(callback);
					} catch (RuntimeException | Error e) {
						afterUiDelivery.run();
						throw e;
					}
				}
			}
		};
		job.addCompletionRunnable(() -> {
			if (job.isCanceled() || job.isInterrupted() || job.getFailureException() != null) {
				completion.accept(null);
				return;
			}
			if (delivered.get())
				return;
			// The final Swing runnable is free and may still be queued. Check after
			// it on the UI dispatcher so a later cancel or EDT-side registration error
			// cannot leave the open request without a terminal callback.
			UiDispatch.invokeLater(() -> {
				if (!delivered.get())
					completion.accept(null);
			});
		});
		return completion;
	}

	static boolean scheduleOpenProjectJob(JobQueue queue, Job job, Consumer<Object> completion) {
		if (queue.trySchedule(job))
			return true;
		completion.accept(null);
		return false;
	}

	/** Produces an actionable standalone-file error without exposing importer internals as the primary message. */
	public static String projectLoadFailureMessage(String fileName, Throwable cause) {
		SubProj.LoadStatus status = subprojectLoadFailureStatus(cause);
		String file = fileName == null || fileName.isBlank() ? "The selected project" : "The project file '" + fileName + "'";
		String message = switch (status) {
		case ACCESS_DENIED -> file + " could not be opened because access was denied.";
		case MISSING -> file + " could not be found.";
		default -> file + " is invalid or could not be imported.";
		};
		return message + failureDetail(cause) + " Projects that are already open were not changed.";
	}

	/** Reattaches path-persisted child references after a local master has loaded. */
	public void restoreLinkedLocalSubprojects(Project master) {
		if (master == null || master.isOpenedAsSubproject())
			return;
		LocalSession localSession = SessionFactory.getInstance().getLocalSession();
		// Loading a child attaches its nodes below the reference.  Do not iterate the
		// live master outline while those asynchronous loads are allowed to mutate it,
		// otherwise a later sibling reference can be skipped during master reopen.
		List<Node> references = new ArrayList<>(master.getTaskList().size());
		for (Iterator<Node> iterator = master.getTaskOutline().iterator(master.getTaskOutlineRoot()); iterator.hasNext();) {
			Node node = iterator.next();
			if (node.getImpl() instanceof SubProj subproject && !subproject.isSubprojectOpen()
					&& subproject.getLoadStatus() == SubProj.LoadStatus.NOT_LOADED)
				references.add(node);
		}
		for (Node node : references) {
			SubProj subproject = (SubProj) node.getImpl();
			Task task = (Task) subproject;
			String linkedFile = task.getSubprojectFile();
			if (linkedFile == null || linkedFile.isBlank())
				continue;
			if (!new File(linkedFile).isFile()) {
				subproject.setLoadStatus(SubProj.LoadStatus.MISSING);
				Alert.warn(MessageFormat.format(Messages.getString("Message.subprojectMissing"), linkedFile));
				continue;
			}
			// A path-persisted child is a local project even when the legacy POD
			// identity did not retain its local flag.
			master.setLocal(true);
			long localId = localSession.registerProjectFile(linkedFile);
			if (localId <= 0L) {
				subproject.setLoadStatus(SubProj.LoadStatus.UNAVAILABLE);
				continue;
			}
			subproject.setSubprojectUniqueId(localId);
			openSubproject(master, node, false);
			// Local jobs share a critical section.  Starting another child from this
			// master-load callback would block the EDT until the first child ends.
			// The child completion callback below resumes this scan; if the child was
			// already open, continue immediately for the next reference.
			if (!subproject.isSubprojectOpen())
				return;
		}
	}

	public Project findFromId(long id) {
		return portfolio.findByUniqueId(id);
	}

	public Project openSubproject(final Project parent, final Node subprojectNode, final boolean creating) {
		final SubProj subprojectTask = (SubProj)subprojectNode.getImpl();
		final boolean requestedReadOnly = ((Task)subprojectTask).isSubprojectReadOnly();
		final long id = subprojectTask.getSubprojectUniqueId();
		Project openSubproject = portfolio.findByUniqueId(id);
		if (openSubproject != null) {
			if (parent.getSubprojectHandler().wouldCreateCircularReference(openSubproject)) {
				subprojectTask.setLoadStatus(SubProj.LoadStatus.CYCLE);
				Alert.error(circularSubprojectReferenceMessage(parent, openSubproject));
				return null;
			}
			if (subprojectTask.getSubprojectFile() != null && !subprojectTask.getSubprojectFile().isBlank())
				openSubproject.setFileName(subprojectTask.getSubprojectFile());
			parent.getSubprojectHandler().addSubproject(openSubproject, subprojectNode,creating, true);
			parent.fireUpdateEvent(this, subprojectTask);
			portfolio.handleExternalTasks(openSubproject,true, false);  // resolve external links if any
			return openSubproject;
		}

		final Session session=SessionFactory.getInstance().getLocalSession();
		if (!session.projectExists(id)) {
			subprojectTask.setLoadStatus(SubProj.LoadStatus.MISSING);
			Alert.error(Messages.getString("Error.projectDoesNotExist"));
			return null;
		}

		addLoadingProject(id);

		LoadOptions opt=subprojectLoadOptions(parent, id);
		Job job=session.getLoadProjectJob(opt);
		subprojectTask.setFetching(true);
		AtomicBoolean requestFinished = new AtomicBoolean();
		Consumer<JobQueue.AdmissionTicket> finishRequest = ticket -> {
			Runnable finishOnEdt = () -> {
				try {
					if (!requestFinished.compareAndSet(false, true))
						return;
					if (!subprojectTask.isSubprojectOpen()
							&& subprojectTask.getLoadStatus() == SubProj.LoadStatus.NOT_LOADED)
						subprojectTask.setLoadStatus(SubProj.LoadStatus.UNAVAILABLE);
					subprojectTask.setFetching(false);
					removeLoadingProject(id);
					if (!parent.isOpenedAsSubproject())
						restoreLinkedLocalSubprojects(parent);
				} finally {
					ticket.close();
				}
			};
			try {
				UiDispatch.invokeLater(finishOnEdt);
			} catch (RuntimeException | Error e) {
				ticket.close();
				throw e;
			}
		};

		job.addSwingRunnable(new JobRunnable("Local: insertProject"){
			public Object run() throws Exception{
				Project subproject = null;
				try {
					subproject = (Project)getPreviousResult();
					if (subproject == null)
						return null;

					//add assignments in the outline, paste uses only assignments present in the nodeModel
					AssignmentNodeModel parentModel = (AssignmentNodeModel)subproject.getTaskOutline();
					parentModel.addAssignments(parentModel.iterator()); // assignments

					attachLoadedSubproject(parent, subprojectNode, creating, subproject, requestedReadOnly);
				} catch (Exception | Error e) {
					SubProj.LoadStatus status = subprojectLoadFailureStatus(e);
					subprojectTask.setLoadStatus(status);
					logger.log(Level.WARNING, "Linked subproject could not be loaded (" + status + ")", e);
					Alert.warn(subprojectLoadFailureMessage(status, e));
				}
				return subproject;
			}
		},false);

		session.getJobQueue().scheduleOrJoinRetained(job,
			() -> LocalSession.resolveProjectLoadAdmissionKey(opt.getFileName(), id), ticket ->
			job.addCompletionRunnable(() -> finishRequest.accept(ticket)), (activeJob, ticket) -> {
			UiDispatch.invokeLater(() -> {
				try {
					Project loaded = activeJob.isCanceled() || activeJob.isInterrupted()
							|| activeJob.getFailureException() != null
							? null : (Project) activeJob.getCompletionResult();
					if (loaded == null) {
						if (subprojectTask.getLoadStatus() == SubProj.LoadStatus.NOT_LOADED)
							subprojectTask.setLoadStatus(SubProj.LoadStatus.UNAVAILABLE);
					} else {
						attachLoadedSubproject(parent, subprojectNode, creating, loaded, requestedReadOnly);
					}
				} catch (Exception | Error e) {
					SubProj.LoadStatus status = subprojectLoadFailureStatus(e);
					subprojectTask.setLoadStatus(status);
					logger.log(Level.WARNING, "Linked subproject could not be attached (" + status + ")", e);
					Alert.warn(subprojectLoadFailureMessage(status, e));
				} finally {
					finishRequest.accept(ticket);
				}
			});
		});
		return ((Task)subprojectTask).getProject();
	}

	private void attachLoadedSubproject(Project parent, Node subprojectNode, boolean creating,
			Project subproject, boolean requestedReadOnly) {
		SubProj subprojectTask = (SubProj) subprojectNode.getImpl();
		if (subprojectTask.getSubproject() == subproject && subprojectTask.isSubprojectOpen())
			return;
		if (subprojectTask.getSubprojectFile() != null && !subprojectTask.getSubprojectFile().isBlank())
			subproject.setFileName(subprojectTask.getSubprojectFile());
		if (parent.getSubprojectHandler().wouldCreateCircularReference(subproject)) {
			subprojectTask.setLoadStatus(SubProj.LoadStatus.CYCLE);
			Alert.error(circularSubprojectReferenceMessage(parent, subproject));
			return;
		}
		if (requestedReadOnly)
			subproject.setReadOnly(true);
		parent.getSubprojectHandler().addSubproject(subproject, subprojectNode, creating, false);
		parent.fireUpdateEvent(this, subprojectTask);
		if (portfolio.findByUniqueId(subproject.getUniqueId()) == null)
			addProject(subproject, false, true);
		portfolio.handleExternalTasks(subproject, true, false);
		subprojectTask.setLoadStatus(SubProj.LoadStatus.OPEN);
		if (subproject.isReadOnly())
			Alert.warn(MessageFormat.format(Messages.getString("Message.readOnlySubproject"), new Object[]{subproject.getName()}));
	}

	/** Maps loader failures onto stable, repairable master-reference states. */
	static SubProj.LoadStatus subprojectLoadFailureStatus(Throwable failure) {
		for (Throwable current = failure; current != null && current.getCause() != current; current = current.getCause()) {
			if (current instanceof AccessDeniedException || current instanceof SecurityException)
				return SubProj.LoadStatus.ACCESS_DENIED;
			if (current instanceof FileNotFoundException)
				return SubProj.LoadStatus.MISSING;
		}
		// A file that exists but cannot be parsed/imported is not an unavailable
		// project.  Preserve the master reference as INVALID so the user can
		// locate a repaired file or remove only the reference.
		return SubProj.LoadStatus.INVALID;
	}

	private static String subprojectLoadFailureMessage(SubProj.LoadStatus status, Throwable cause) {
		String message = switch (status) {
		case ACCESS_DENIED -> "The linked project could not be opened because access was denied.";
		case MISSING -> "The linked project file is missing.";
		default -> "The linked project file is invalid or could not be imported.";
		};
		return message + failureDetail(cause) + " The master reference was kept so it can be repaired or removed.";
	}

	static String failureDetail(Throwable cause) {
		if (cause == null || cause.getMessage() == null || cause.getMessage().isBlank())
			return "";
		return " Details: " + cause.getMessage().replaceAll("[\\r\\n]+", " ").trim() + ".";
	}

	private static String circularSubprojectReferenceMessage(Project parent, Project candidate) {
		String chain = parent == null ? "" : parent.getSubprojectHandler().describeCircularReference(candidate);
		return chain == null || chain.isBlank()
				? "The selected subproject would create a circular master/subproject reference."
				: "The selected subproject would create a circular master/subproject reference: " + chain;
	}

	/**
	 * Installs a freshly loaded child in place of a dirty linked child after the
	 * user explicitly chose Discard.  This is intentionally separate from the
	 * best-effort refresh merge: discard must also remove local-only task rows
	 * and hierarchy edits.
	 */
	public boolean replaceOpenSubproject(Project parent, SubProj reference, Project replacement) {
		return com.microproject.transaction.ProjectMutationState.coordinated(
			() -> replaceOpenSubprojectCoordinated(parent, reference, replacement));
	}

	private boolean replaceOpenSubprojectCoordinated(Project parent, SubProj reference, Project replacement) {
		if (parent == null || reference == null || replacement == null)
			return false;
		Project previous = reference.getSubproject();
		Node referenceNode = parent.getTaskOutline().search(reference);
		if (previous == null || referenceNode == null)
			return false;
		List<com.microproject.transaction.ProjectMutationState.Scope> mutations = new ArrayList<>(3);
		boolean replaced = false;
		try {
			mutations.add(parent.getMutationState().begin());
			mutations.add(previous.getMutationState().begin());
			mutations.add(replacement.getMutationState().begin());
			replacement.setUniqueId(previous.getUniqueId());
			replacement.setLocal(previous.isLocal());
			replacement.setFileName(previous.getFileName());
			replacement.setReadOnly(previous.isReadOnly());
			parent.getSubprojectHandler().replaceSubproject(previous, replacement, referenceNode);
			try {
				afterSubprojectProjection.run();
			} catch (RuntimeException | Error failure) {
				try {
					parent.getSubprojectHandler().replaceSubproject(replacement, previous, referenceNode);
				} catch (RuntimeException | Error rollbackFailure) {
					failure.addSuppressed(rollbackFailure);
				}
				throw failure;
			}
			boolean portfolioReplaced;
			try {
				portfolioReplaced = portfolio.replaceProject(previous, replacement, false);
			} catch (RuntimeException | Error failure) {
				try {
					parent.getSubprojectHandler().replaceSubproject(replacement, previous, referenceNode);
				} catch (RuntimeException | Error rollbackFailure) {
					failure.addSuppressed(rollbackFailure);
				}
				throw failure;
			}
			if (!portfolioReplaced) {
				parent.getSubprojectHandler().replaceSubproject(replacement, previous, referenceNode);
				return false;
			}
			reference.setLoadStatus(SubProj.LoadStatus.OPEN);
			for (com.microproject.transaction.ProjectMutationState.Scope mutation : mutations)
				mutation.commit(true);
			replaced = true;
		} finally {
			com.microproject.transaction.ProjectMutationState.closeAll(mutations);
		}
		if (replaced) {
			parent.fireUpdateEvent(this, reference);
			portfolio.publishProjectReplacement(replacement);
		}
		return replaced;
	}

	static LoadOptions subprojectLoadOptions(Project parent, long id) {
		LoadOptions options = new LoadOptions();
		options.setSubproject(true);
		options.setLocal(parent != null && parent.isLocal());
		options.setId(id);
		return options;
	}





	//SAVE PROJECTS



	public void saveProject(final Project project, final SaveOptions opt) {
		Job job=getSaveProjectJob(project,opt);
		Session session=SessionFactory.getInstance().getLocalSession();
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
		DeepChildWalker.recursivelyTreatBranch(portfolio.getNodeModel(), project, arg0 -> {
			Node n=(Node)arg0;
			Object impl = n.getImpl();
			if (impl instanceof Project p){
				// A child may be modified while the master's own document stays
				// clean.  Save each dirty member of the branch independently;
				// inheriting only the root's dirty flag silently lost that child.
				if (shouldIncludeInBranchSave(p, opt)) {
					projects.add(p);
				}
			}
		});
		// A locally reopened linked child can be materialized beneath its SubProj
		// reference before it is represented as a portfolio branch.  Include that
		// real master hierarchy as well, otherwise Save on a clean master loses a
		// dirty child that is visible in the consolidated Gantt.
		collectLinkedProjectsForSave(project, opt, projects, new HashSet<>());
		// Recovery currently stores and restores one project file. Saving a linked
		// branch would send children after the root to their original file names,
		// overwriting user data instead of creating recovery snapshots.
		if (opt.isRecoverySnapshot() && (projects.size() > 1 || hasRecoveryExternalReferences(project))) {
			throw new IllegalStateException("Recovery snapshot skipped: linked project branches are not supported yet");
		}
		// Normalize dates only after the recovery branch has passed its safety check.
		for (Project savedProject : projects)
			savedProject.setEarliestAndLatestDatesFromSchedule();  // external constraints determine subproject dates
		if (projects.size()>0){
			Session session=SessionFactory.getInstance().getLocalSession();
			final SaveOptions o=(SaveOptions)opt.clone();
			// A master-save job can contain only a dirty child, or both the
			// master and several children.  In either case each member must retain
			// its own file location; applying the master's requested path would
			// overwrite the wrong project file.
			// Keep the explicit Save As target for the root document. Linked
			// children retain their own persisted file names; LocalSession assigns
			// the requested name only to the first project in this branch.
			o.setPostSaving(obj -> {
				Project p = (Project)obj;
				if (!opt.isRecoverySnapshot()) {
					p.setAllTasksAsUnchangedFromPersisted(true);
					p.validateNewDistributionMap();
					portfolio.handleExternalTasks(p,false, true); 		// external link handling
				}
				if (opt.getPostSaving()!=null) opt.getPostSaving().accept(obj); //id, combobox update
			});
			Job job=session.getSaveProjectJob(projects,o);
			return job;
		}
		return null;
	}

	/** Until branch capture is supported, never publish a snapshot that could
	 * restore a linked document or pool from an original on-disk path. */
	private static boolean hasRecoveryExternalReferences(Project project) {
		if (project == null) return false;
		String pool = project.getSharedResourcePoolFile();
		if (pool != null && !pool.isBlank()) return true;
		for (Iterator<Task> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = iterator.next();
			if (task instanceof SubProj) return true;
			String external = task.getExternalProjectFile();
			if (external != null && !external.isBlank()) return true;
		}
		return false;
	}

	/** Determines whether one project in a master-save branch must be persisted. */
	static boolean shouldIncludeInBranchSave(Project project, SaveOptions options) {
		return project != null && (project.needsSaving() || options.isSaveAs() || options.getImporter() != null);
	}

	private static void collectLinkedProjectsForSave(Project project, SaveOptions options,
			List<Project> projects, Set<Project> visited) {
		if (project == null || !visited.add(project))
			return;
		if (shouldIncludeInBranchSave(project, options) && !projects.contains(project)) {
			projects.add(project);
		}
		for (Iterator<Task> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = iterator.next();
			if (task instanceof SubProj reference)
				collectLinkedProjectsForSave(reference.getSubproject(), options, projects, visited);
		}
	}




	//CLOSE PROJECTS



	public Job getCloseProjectsOnServerJob(Project project){
		// Save the project and all of its subprojects
		final List<Project> projects=new ArrayList<>();
		DeepChildWalker.recursivelyTreatBranch(portfolio.getNodeModel(), project, arg0 -> {
			Object impl = ((Node)arg0).getImpl();
			if (impl instanceof Project childProject){
				projects.add(childProject);
			}
		});
		if (projects.size()>0){
			Session session=SessionFactory.getInstance().getLocalSession();
			Job job=session.getCloseProjectsJob(projects);
			return job;
		}
		return null;
	}


	public Job getCloseProjectsOnServerJob(Collection<Project> projects) {
		int projectCount = projects == null ? 0 : projects.size();
		List<Project> localProjects = new ArrayList<>(projectCount);
		List<Project> serverProjects = new ArrayList<>(projectCount);
		for (Project project : projects) {
			if (project.isReadOnly()) continue;
			if (project.isLocal()) localProjects.add(project);
			else serverProjects.add(project);
		}
		Job job=null;
		if (localProjects.size()>0) job=SessionFactory.getInstance().getLocalSession().getCloseProjectsJob(projects);
		if (serverProjects.size()>0){
			Job j=SessionFactory.getInstance().getLocalSession().getCloseProjectsJob(projects);
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

	static SaveOptions prepareLocalCloseSaveOptions(Project project, Function<String, String> chooseTarget) {
		if (project == null || !project.isLocal()) return null;
		String sourceFileName = project.getFileName();
		boolean needsSaveAs = sourceFileName == null || FileHelper.requiresSaveAs(sourceFileName);
		String fileName = sourceFileName;
		if (needsSaveAs) {
			String suggested = sourceFileName == null ? project.getGuessedFileName()
				: FileHelper.defaultNativeSaveAsFileName(sourceFileName, false);
			fileName = chooseTarget == null ? null : chooseTarget.apply(suggested);
		}
		if (fileName == null) return null;

		SaveOptions options = new SaveOptions();
		options.setLocal(true);
		options.setSaveAs(needsSaveAs || !fileName.equals(sourceFileName));
		options.setFileName(fileName);
		options.setImporter(LocalSession.getImporter(FileHelper.getFileType(fileName)));
		return options;
	}

	static String getDisplayNameForSavePrompt(Project project) {
		if (project == null)
			return Messages.getString("Text.Untitled");
		String fileName = project.getFileName();
		if (fileName != null && fileName.trim().length() > 0)
			return FilePathUtils.fileName(fileName);
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

			int promptResult = promptForSave(project,allowCancel);
			if (promptResult == Alert.YES_OPTION){
				SaveOptions opt=new SaveOptions();
				opt.setLocal(project.isLocal());
				if (project.isLocal()){
					opt=prepareLocalCloseSaveOptions(project, suggested ->
						SessionFactory.getInstance().getLocalSession().chooseFileName(true, suggested));
					if (opt == null) return null;
				}
				job=getSaveProjectJob(project, opt);
			}
			else if (promptResult == Alert.CANCEL_OPTION)
				return null;
		}

		final List<Node> toRemove = new ArrayList<>();
		final List<Project> projects = new ArrayList<>();
		DeepChildWalker.recursivelyTreatBranch(portfolio.getNodeModel(), project, arg0 -> {
			Node node = (Node)arg0;
			if (node == null)
				return;
			Object impl = node.getImpl();
			if (!(impl instanceof Project p))
				return;
			toRemove.add(node);
			projects.add(p);
		});


		Job closeProjectJob=getCloseProjectsOnServerJob(projects);
		if (closeProjectJob==null){
			closeProjectJob=new Job(SessionFactory.getInstance().getJobQueue(),"closeProjects","Closing...",false);

		}
		if (job==null) job=closeProjectJob;
		else job.addJob(closeProjectJob);


		job.addRunnable(new JobRunnable("Local: closeProjects"){
			public Object run() throws Exception{
				com.microproject.transaction.ProjectMutationState.coordinated(() -> {
					List<Project> closingProjects = new ArrayList<>(toRemove.size());
					for (Node node : toRemove) {
						Project p = (Project)node.getImpl();
						portfolio.handleExternalTasks(p,false,false); 	// external link handling
						p.getResourcePool().removeProject(p);
						p.disconnect();
						closingProjects.add(p);
					}
					removePortfolioNodesAndPublish(portfolio.getNodeModel(), toRemove);
					for (Project p : closingProjects)
						com.microproject.transaction.ProjectMutationState.publishAfterAllScopes(
							() -> portfolio.getObjectEventManager().fireDeleteEvent(this,p));
				});
				System.gc(); // clean up memory used by projects
   	    	return null; //return not used anyway
			}
		},/*!calledFromSwing*/false,false,calledFromSwing,false);
		return job;
	}

	static void removePortfolioNodesAndPublish(NodeModel nodeModel, List<Node> nodes) {
		java.util.IdentityHashMap<Node, Boolean> seen = new java.util.IdentityHashMap<>();
		List<Node> removedNodes = new ArrayList<>();
		for (Node node : nodes) {
			for (java.util.Enumeration<?> enumeration = ((NodeBridge)node).postorderEnumeration(); enumeration.hasMoreElements();) {
				Node removed = (Node)enumeration.nextElement();
				if (seen.put(removed, Boolean.TRUE) == null)
					removedNodes.add(removed);
			}
		}
		for (Node node : nodes)
			nodeModel.remove(node, NodeModel.SILENT);
		com.microproject.transaction.ProjectMutationState.publishAfterAllScopes(
			() -> nodeModel.getHierarchy().fireRemoval(removedNodes.toArray(Node[]::new)));
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
				if (calledFromSwing && !UiDispatch.isDispatchThread())
					UiDispatch.invokeLater(complete);
				else
					complete.run();
			});
			Session session=SessionFactory.getInstance().getLocalSession();
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
		DeepChildWalker.recursivelyTreatBranch(portfolio.getNodeModel(), project, value -> {
			Node node = (Node) value;
			if (node == null)
				return;
			if (node.getImpl() instanceof Project descendant)
				ids.add(Long.valueOf(descendant.getUniqueId()));
		});
		return ids;
	}

	public void doRemoveProject(Project project,boolean calledFromSwing) {
		Job job=projectFactory.getPortfolio().getRemoveProjectJob(project,calledFromSwing);
		if (job!=null) {
			SessionFactory.getInstance().getLocalSession().schedule(job);
			com.microproject.transaction.ProjectMutationState.coordinated(
				() -> portfolio.handleExternalTasks(project,false, false)); // external link handling
		}

	}
	/**
	 * @return Returns the portfolio.
	 */
	public Portfolio getPortfolio() {
		return portfolio;
	}

	public Collection<Project> getDirtyProjectList() {
		return portfolio.getDirtyProjectList();
	}
	public Collection<Project> getWritableProjectsList() {
		return portfolio.getWritableProjectList();
	}
	public boolean isResourcePoolOpenAndWritable() {
		return portfolio.isResourcePoolOpenAndWritable();
	}

}
