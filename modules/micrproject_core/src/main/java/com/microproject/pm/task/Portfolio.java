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

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.collections.Predicate;

import com.microproject.document.Document;
import com.microproject.document.ObjectEventManager;
import com.microproject.document.ObjectSelectionEventManager;
import com.microproject.document.ObjectEvent.Listener;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.NodeModelFactory;
import com.microproject.grouping.core.summaries.DeepChildSearcher;
import com.microproject.job.Job;
import com.microproject.job.JobRunnable;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;

/**
 *
 */
public class Portfolio implements Document, NodeModelDataFactory {
	private static final Logger logger = Logger.getLogger(Portfolio.class.getName());
	NodeModel nodeModel;
	ObjectEventManager objectEventManager;
	private transient ObjectSelectionEventManager objectSelectionEventManager = new ObjectSelectionEventManager();
	boolean creating = false;
	private transient boolean dirty;
	ProjectFactory projectFactory = null;
	/**
	 *
	 */
	public Portfolio(ProjectFactory projectFactory) {
		super();
		this.projectFactory = projectFactory;
		objectEventManager = new ObjectEventManager();
		nodeModel = NodeModelFactory.getInstance().createNodeModel(this);
		nodeModel.getHierarchy().setNbEndVoidNodes(0);
	}

	public Project findByUniqueId(long uniqueId) {
		return (Project) DeepChildSearcher.searchForUniqueId(nodeModel,uniqueId);
	}

	private class ResourcePoolFinder implements Predicate {
		public boolean evaluate(Object arg) {
			Project project = (Project)arg;
			if (project.isMaster() && !project.isReadOnly())
				return true;
			return false;
		};
	}

	public boolean isResourcePoolOpenAndWritable() {
		Project p = (Project) DeepChildSearcher.search(nodeModel,new ResourcePoolFinder());
		return (p != null);
	}


	void addProject(final Project project,boolean createJob,boolean verify) {
		if (!verify){
			_addProject(project);
			return;
		}
//		if (Environment.getStandAlone())
//			createJob = false;
		Job job=null;
		if (creating)
			return;
		Node oldNode=nodeModel.search(project,comparator);
		if (oldNode!=null){
			if (!shouldReplaceExistingProject(Alert.confirm(Messages.getString("Message.projectAlreadyExists")))){
				//TODO be sure all references are removed
				return;
			}else{
			    //removeProject((Project)oldNode.getImpl());
				job=getRemoveProjectJob((Project)oldNode.getImpl(),true);
				if (job!=null&&!createJob){
					//job.addSync(); //sync leads to a lock
			    	SessionFactory.getInstance().getSession(project.isLocal()).schedule(job);
				}
			}
		}

		if (!createJob){
			_addProject(project);
			return;
		}

	   	Job addProjectJob=new Job(SessionFactory.getInstance().getSession(project.isLocal()).getJobQueue(),"addProject","Adding project...",false);
	   	addProjectJob.addSwingRunnable(new JobRunnable("Local: addProject",1.0f){
    		public Object run() throws Exception{
    			_addProject(project);
    			setProgress(1.0f);
    			return null;
    		}
    	});
//    	job.addExceptionRunnable(new JobRunnable("Local: exception"){
//    		public Object run() throws Exception{
//    			Alert.error(Messages.getString("Message.serverError"));
//    			return null;
//    		}
//    	});
	   	if (job==null) job=addProjectJob;
	   	else job.addJob(addProjectJob);
    	SessionFactory.getInstance().getSession(project.isLocal()).schedule(job);

	}

	static boolean shouldReplaceExistingProject(int confirmation) {
		return confirmation == JOptionPane.YES_OPTION;
	}

	private void _addProject(Project project){
    	nodeModel.add(NodeFactory.getInstance().createNode(project),NodeModel.SILENT);
    	handleExternalTasks(project,true, false); 		// external link handling

    	objectEventManager.fireCreateEvent(this,project);
    	project.getResourcePool().addProject(project);
	}

	void handleExternalTasks(final Project project, final boolean opening, final boolean saving) {
		// external link handling
		forProjects(new Consumer<Object>() { public void accept(Object arg0) {
				Project p = (Project)arg0;
				if (p != project)
					p.handleExternalTasks(project, opening, saving);
			}});

	}
	public void addSubproject(final Project child, Project parent, Project owning) {
		// parent is no longer used.
//System.out.println("addSubproject child " + child + " parent " + parent + " owning " +  owning)	;
		Node childNode = nodeModel.search(child);
		boolean modified = false;
		if (childNode == null) {
			addProject(child,false,true);
			childNode = nodeModel.search(child);
		} else {
			modified = true;
			objectEventManager.fireCreateEvent(this,child); // for mainframe to get rid of any open one
		}
		Node owningNode = nodeModel.search(owning);
		nodeModel.getHierarchy().move(childNode, owningNode, NodeModel.SILENT);

		objectEventManager.fireCreateEvent(this,child); // fire a second time too for projects view
	}




	public static final Comparator<Object> comparator = new ImplComparator();
	public static class ImplComparator implements Comparator<Object> {
		ImplComparator() {}
		@Override
		public int compare(Object node, Object impl) {
			if (node == null) // why  is node null?
				return impl == null ? 0 : 1;
			if (((Node)node).getImpl().equals(impl))
				return 0;
			else
				return 1;
		}
	}


	public Job getRemoveProjectJob(final Project project,boolean calledFromSwing) {
		return projectFactory.getRemoveProjectJob(project,true,true,calledFromSwing);
	}

	public Job getRemoveAllProjectsJob(JobRunnable exitRunnable,boolean calledFromSwing,boolean[] closeStatus){
		boolean exitOnClose=true;
		if (closeStatus!=null&&closeStatus.length>0) closeStatus[0]=true;
    	final Job job=new Job(SessionFactory.getInstance().getLocalSession().getJobQueue(),"removeAllProjects","Removing projects...",true);
		List<Node> toRemove = new ArrayList<>();
		for (Iterator<Node> i=nodeModel.iterator(); i.hasNext();){
			Node node=i.next();
			if (!node.isRoot()) toRemove.add(node);
		}
		for (Node node : toRemove){
			Project p = (Project)node.getImpl();
			if (p.isOpenedAsSubproject()) // subprojects are saved with their parents
				continue;
			Job rJob=getRemoveProjectJob(p,calledFromSwing);
			if (rJob==null) {
				if (calledFromSwing) exitOnClose=false; //close cancelled.
				if (closeStatus!=null&&closeStatus.length>0){
					logger.fine("Close cancelled");
					closeStatus[0]=false;
				}
			}
			else job.addJob(rJob);
		}
		if (exitOnClose) job.addRunnable(exitRunnable,!calledFromSwing,false,calledFromSwing,false);
		return job;
	}

	public void addObjectListener(Listener listener) {
		objectEventManager.addListener(listener);
	}
	public void removeObjectListener(Listener listener) {
		objectEventManager.removeListener(listener);
	}
	public ObjectEventManager getObjectEventManager() {
		return objectEventManager;
	}


	public Object createUnvalidatedObject(NodeModel nodeModel, Object parent) {
		creating = true;
		Project project = projectFactory.createProject();
		creating = false;
		return project;
	}
	public void addUnvalidatedObject(Object object,NodeModel nodeModel, Object parent) {
	}
	public void validateObject(Object newlyCreated, NodeModel nodeModel, Object eventSource, Object hierarchyInfo,boolean isNew) {
		//objectEventManager.fireCreateEvent(this,(Project)newlyCreated);
	}
//	public void fireCreated(Object newlyCreated){
//		//objectEventManager.fireCreateEvent(this,newlyCreated);
//	}
	public void remove(Object toRemove, NodeModel nodeModel,boolean deep,boolean undo,boolean removeDependencies){
		//removeProject((Project) toRemove);
	}

	public NodeModel getNodeModel() {
		return nodeModel;
	}

	public void forProjects(Consumer<Object> c){
    	Object impl;
    	for (Iterator i=getNodeModel().iterator();i.hasNext();){
    		impl=((Node)i.next()).getImpl();
    		if (!(impl instanceof Project)) continue;
    		c.accept(impl);
    	}
	}

	public Collection getDirtyProjectList() {
		final ArrayList list = new ArrayList();
		forProjects(new Consumer<Object>() { public void accept(Object arg0) {
				if (((Project)arg0).needsSaving())
					list.add(arg0);
			}});
		return list;
	}

	public Collection getWritableProjectList() {
		final ArrayList list = new ArrayList();
		forProjects(new Consumer<Object>() { public void accept(Object arg0) {
				if (!((Project)arg0).isReadOnly())
					list.add(arg0);
			}});
		return list;
	}

	public void fireUpdateEvent(Object source, Object object) {
		objectEventManager.fireUpdateEvent(source,object);
	}

	public int fireMultipleTransaction(int id, boolean begin) {
		return 0;
	}

	public WorkCalendar getDefaultCalendar() {
		return CalendarService.getInstance().getDefaultInstance();
	}


	public final boolean isGroupDirty() {
		return dirty;
	}
	public final void setGroupDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public ArrayList extractCalendars() {
		return WorkingCalendar.extractCalendars(nodeModel.getHierarchy());
	}

	public DataFactoryUndoController getUndoController() {
		return null;
	}
	public void rollbackUnvalidated(NodeModel nodeModel, Object object) {
	}

	public void initOutline(NodeModel nodeModel){}

	public NodeModelDataFactory getFactoryToUseForChildOfParent(Object impl) {
		return this;
	}

	public void setAllChildrenDirty(boolean dirty) {
		setGroupDirty(dirty);
		forProjects(new Consumer<Object>() { public void accept(Object arg0) {
				((Project) arg0).setGroupDirty(dirty);
			}
		});
	}

	public boolean containsAssignments(){
		if (nodeModel == null)
			return false;
		final boolean[] result = new boolean[] {false};
		forProjects(new Consumer<Object>() { public void accept(Object arg0) {
				result[0] |= ((Project) arg0).containsAssignments();
			}
		});
		return result[0];
	}
	public boolean evaluate(Object arg0) {
		return false;
	}

	public ObjectSelectionEventManager getObjectSelectionEventManager() {
		if (objectSelectionEventManager == null)
			objectSelectionEventManager = new ObjectSelectionEventManager();
		return objectSelectionEventManager;
	}

}
