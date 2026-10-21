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
package com.microproject.pm.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.configuration.Settings;
import com.microproject.document.Document;
import com.microproject.document.ObjectEvent;
import com.microproject.document.ObjectEventManager;
import com.microproject.document.ObjectSelectionEventManager;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeList;
import com.microproject.grouping.core.OutlineCollection;
import com.microproject.grouping.core.OutlineCollectionImpl;
import com.microproject.grouping.core.model.AssignmentNodeModel;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.task.Project;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.transaction.MultipleTransactionManager;
import com.microproject.transaction.MultipleTransaction;
import com.microproject.undo.DataFactoryUndoController;


/**
 *
 */
public class ResourcePool implements Document, NodeModelDataFactory {
	private static final Logger logger = Logger.getLogger(ResourcePool.class.getName());
	private String name = "";
    private final ArrayList<Resource> resourceList = new ArrayList<>();
	private final List<Project> projects = new ArrayList<>();
	private ObjectEventManager objectEventManager = new ObjectEventManager();
	private transient MultipleTransactionManager multipleTransactionManager = new MultipleTransactionManager();
	private long resourceIdCounter = 0;
	private transient volatile long coordinationRevision;
	private WorkingCalendar defaultCalendar;
    private static ResourcePool globalPool = null;
	protected ResourcePool(String name,DataFactoryUndoController undo) {
		this.name = name;
		globalPool = this;
		defaultCalendar = CalendarService.getInstance().getDefaultInstance();
		undoController=undo;
		
	}
	public static ResourcePool createRourcePool(String name,DataFactoryUndoController undo) {
		ResourcePool pool=new ResourcePool(name,undo);
		pool.initializeOutlines();
		return pool;
	}
	
    private transient HashMap<Long,Resource> idMap = null;
	public Resource findById(long id) {
		return ProjectMutationState.coordinated(() -> {
			if (idMap == null) {
				idMap = HashMap.newHashMap(resourceList.size());
				for (Resource resource : resourceList)
					idMap.put(resource.getUniqueId(), resource);
			}
			return idMap.get(id);
		});
	}
	public void initializeId(Resource resource) {
		ProjectMutationState.coordinated(() -> {
			long id;
			do {
				id = ++resourceIdCounter;
			} while (containsResourceId(id, resource));
			resource.setId(id);
			idMap = null;
			advanceCoordinationRevisionCoordinated();
		});
	}

	private boolean containsResourceId(long id, Resource excluded) {
		for (Resource resource : resourceList) {
			if (resource != excluded && resource.getId() == id)
				return true;
		}
		return false;
	}
	
	public void initializeOutlines(){
		int count=Settings.numHierarchies();
		for (int i=0;i<count;i++){
			NodeModel model=resourceOutlines.getOutline(i);
			if (model==null) continue;
			if (model instanceof AssignmentNodeModel aModel){
				aModel.setDocument(this);
			}
			initOutline(model);
		}
	}
	
	
	public void addAndInitializeId(Resource resource) {
		ProjectMutationState.coordinated(() -> {
			add(resource);
			initializeId(resource);
		});
	}
	public void add(Resource resource) {
		ProjectMutationState.coordinated(() -> {
			if (containsResourceIdentity(resource)) return;
			resourceList.add(resource);
			resourceIdCounter = Math.max(resourceIdCounter, resource.getId());
			idMap = null;
			advanceCoordinationRevisionCoordinated();
		});
	}

	/** Registers an imported resource ID without allowing a later allocation to reuse it. */
	public void setResourceUniqueId(Resource resource, long uniqueId) {
		if (resource == null || uniqueId < 1L) throw new IllegalArgumentException("Invalid resource unique ID");
		ProjectMutationState.coordinated(() -> {
			boolean changed = resource.getUniqueId() != uniqueId;
			if (changed) resource.setUniqueId(uniqueId);
			resourceIdCounter = Math.max(resourceIdCounter, uniqueId);
			if (changed) {
				idMap = null;
				advanceCoordinationRevisionCoordinated();
			}
		});
	}
	public void remove(Resource resource) {
		ProjectMutationState.coordinated(() -> {
			if (removeResourceIdentity(resource)) {
				idMap = null;
				advanceCoordinationRevisionCoordinated();
			}
		});
	}
	
	public ResourceImpl newResourceInstance() {
		EnterpriseResource globalResource = new EnterpriseResource(isLocal(),this);
		ResourceImpl newOne = new ResourceImpl(globalResource);
		
		addAndInitializeId(newOne);
		return newOne;
	}
	
	public Resource createScriptedResource() {
		Resource res = newResourceInstance();
		resourceOutlines.addToAll(res,null); // update all node models 
		return res;
	}
	/**
	 * @return Returns the resourceList.
	 */
	public List<Resource> getResourceList() {
		return ProjectMutationState.coordinated(() -> List.copyOf(resourceList));
	}
	
	public static Resource findResourceByName(Object idObject, Object resourcePoolObject) {
		String id = (String)idObject;
		for (Resource resource : ((ResourcePool)resourcePoolObject).getResourceList()) {
			if (resource.getName().equals(id))
				return resource;
		}
		return null;
	}
	public static Resource findResourceByInitials(Object idObject, Object resourcePoolObject) {
		int id = ((Integer)idObject).intValue();
		for (Resource resource : ((ResourcePool)resourcePoolObject).getResourceList()) {
			if (resource.getId() == id)
				return resource;
		}
		return null;
	}
	
	private OutlineCollection resourceOutlines = new OutlineCollectionImpl(Settings.numHierarchies(),this); 
	
	public NodeModel getResourceOutline() {
		NodeModel model=resourceOutlines.getOutline();
		return model;
	}
	public NodeModel getResourceOutline(int outlineNumber) {
		NodeModel model=resourceOutlines.getOutline(outlineNumber);
		return model;
	}
	
	public void addToDefaultOutline(Node parentNode, Node childNode) {
		resourceOutlines.addToDefaultOutline(parentNode,childNode);
	}
	public void addToDefaultOutline(Node parentNode, Node childNode,int position,boolean event) {
		resourceOutlines.addToDefaultOutline(parentNode,childNode,position,event);
	}
	public Object createUnvalidatedObject(NodeModel nodeModel, Object parent) {
		EnterpriseResource globalResource = new EnterpriseResource(isLocal(),this);
		ResourceImpl newOne = new ResourceImpl(globalResource);
		newOne.getGlobalResource().setMaster(isMaster());
		newOne.getGlobalResource().setLocal(isLocal());
		addUnvalidatedObject(newOne,nodeModel,parent);
		return newOne;
	}
	public void addUnvalidatedObject(Object object, NodeModel nodeModel, Object parent) {
	}
	public void validateObject(Object newlyCreated, NodeModel nodeModel, Object eventSource, Object hierarchyInfo,boolean isNew) {
		if (!(newlyCreated instanceof Resource)) return;// avoids VoidNodes
		ProjectMutationState.coordinated(() -> {
			Resource resource=(Resource)newlyCreated;
			((ResourceImpl)resource).getGlobalResource().setResourcePool(this);
			add(resource);
			if (isNew) initializeId(resource);
			resourceOutlines.addToAll(newlyCreated,nodeModel); // update all node models except the one passed in
		});
	}
	public void remove(Object toRemove, NodeModel nodeModel,boolean deep,boolean undo,boolean removeDependencies){
		ProjectMutationState.coordinated(() -> {
			remove((Resource)toRemove);
			resourceOutlines.removeFromAll(toRemove,nodeModel); // update all node models except the one passed in
		});
	}

	public void addProject(Project project) {
		ProjectMutationState.coordinated(() -> {
			if (containsProjectIdentity(project)) return;
			projects.add(project);
			advanceCoordinationRevisionCoordinated();
		});
//		initUndoControlerForAllOutines(project);
	}
	public void removeProject(Project project) {
		ProjectMutationState.coordinated(() -> {
			if (removeProjectIdentity(project)) advanceCoordinationRevisionCoordinated();
		});
	}
	/** Revision for shared-pool membership and assignment publication, independent of any project. */
	public long getCoordinationRevision() {
		return coordinationRevision;
	}
	public long advanceCoordinationRevision() {
		return ProjectMutationState.coordinated(this::advanceCoordinationRevisionCoordinated);
	}
	private long advanceCoordinationRevisionCoordinated() {
		return ProjectMutationState.coordinatedRevision(this, () -> ++coordinationRevision,
			value -> coordinationRevision = value);
	}

	/** Captures the identity-preserving mutable state needed by a shared-pool transaction. */
	public PoolState captureSharedPoolState() {
		return ProjectMutationState.coordinated(() -> new PoolState(List.copyOf(resourceList), List.copyOf(projects),
			resourceIdCounter, coordinationRevision));
	}

	/** Restores a previously captured shared-pool state without publishing intermediate revisions. */
	public void restoreSharedPoolState(PoolState state) {
		ProjectMutationState.coordinated(() -> {
			resourceList.clear(); resourceList.addAll(state.resources());
			projects.clear(); projects.addAll(state.projects());
			resourceIdCounter = state.resourceIdCounter();
			coordinationRevision = state.coordinationRevision();
			idMap = null;
			for (Resource resource : resourceList)
				if (resource instanceof ResourceImpl implementation) implementation.getGlobalResource().setResourcePool(this);
		});
	}

	public record PoolState(List<Resource> resources, List<Project> projects, long resourceIdCounter,
		long coordinationRevision) { }
	private boolean containsResourceIdentity(Resource expected) {
		for (Resource resource : resourceList)
			if (resource == expected) return true;
		return false;
	}
	private boolean removeResourceIdentity(Resource expected) {
		for (int index = 0; index < resourceList.size(); index++) {
			if (resourceList.get(index) == expected) {
				resourceList.remove(index);
				return true;
			}
		}
		return false;
	}
	private boolean containsProjectIdentity(Project expected) {
		for (Project project : projects)
			if (project == expected) return true;
		return false;
	}
	private boolean removeProjectIdentity(Project expected) {
		for (int index = 0; index < projects.size(); index++) {
			if (projects.get(index) == expected) {
				projects.remove(index);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return Returns the projects.
	 */
	public List<Project> getProjects() {
		return ProjectMutationState.coordinated(() -> List.copyOf(projects));
	}
	/**
	 * @param listener
	 */
	public void addObjectListener(ObjectEvent.Listener listener) {
		objectEventManager.addListener(listener);
	}
	/**
	 * @param listener
	 */
	public void removeObjectListener(ObjectEvent.Listener listener) {
		objectEventManager.removeListener(listener);
	}	

	public ObjectEventManager getObjectEventManager() {
		return objectEventManager;
	}
	public void fireUpdateEvent(Object source, Object object) {
		objectEventManager.fireUpdateEvent(source,object);
	}
	
	
	public String toString() {
		return name;
	}	
	
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	public int fireMultipleTransaction(int id, boolean begin) {
		if (multipleTransactionManager == null)
			multipleTransactionManager = new MultipleTransactionManager();
		return multipleTransactionManager.fire(this, id, begin);
	}

	public void addMultipleTransactionListener(MultipleTransaction.Listener listener) {
		if (multipleTransactionManager == null)
			multipleTransactionManager = new MultipleTransactionManager();
		multipleTransactionManager.addListener(listener);
	}

	public void removeMultipleTransactionListener(MultipleTransaction.Listener listener) {
		if (multipleTransactionManager != null)
			multipleTransactionManager.removeListener(listener);
	}
	
	

	
	
	
	/**
	 * @return Returns the defaultCalendar.
	 */
	public final WorkCalendar getDefaultCalendar() {
		return defaultCalendar;
	}
	
	public ArrayList<WorkingCalendar> extractCalendars() {
		return WorkingCalendar.extractCalendars(resourceList);
	}
	
	private transient boolean isDirty=false;
	public final boolean isGroupDirty() {
//		for (Iterator i=getProjects().iterator();i.hasNext();){
//			Project project=(Project)i.next();
//			if (project.isGroupDirty()) return true;
//		}
//		return false;
		return isDirty;
	}
	public final void setGroupDirty(boolean isDirty) {
		logger.log(Level.FINE, "ResourcePool.setGroupDirty({0})", isDirty);
		this.isDirty = isDirty;
		if (isDirty)
		for (Project project : getProjects()){
			project.setGroupDirty(true);
		}
	}
	
	protected boolean master;
	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
		updateOutlineTypes();
	}
	protected boolean local;
	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
		updateOutlineTypes();
	}
	
	
	public void updateOutlineTypes(){
		NodeModel[] models=resourceOutlines.getOutlines();
		for (int i=0;i<models.length;i++){
			initOutline(models[i]);
		}
	}
	public void initOutline(NodeModel nodeModel){
		if (nodeModel!=null){
			nodeModel.setLocal(local);
			nodeModel.setMaster(master);			
		}
	}
	
	//Undo
	protected transient DataFactoryUndoController undoController;
	public DataFactoryUndoController getUndoController() {
		return undoController;
	}
	public void setUndoController(DataFactoryUndoController undoController) {
		this.undoController = undoController;
	}

	public void rollbackUnvalidated(NodeModel nodeModel, Object object) {
	}
	public NodeModelDataFactory getFactoryToUseForChildOfParent(Object impl) {
		return this;
	}

	public void setAllChildrenDirty(boolean dirty) {
	}

	
	public boolean containsAssignments(){return true;}

	public static final Object[] userResources() {
		ArrayList<Resource> result = new ArrayList<>(globalPool.getResourceList().size());
		for (Resource resource : globalPool.getResourceList()) {
			if (resource.isUser())
				result.add(resource);
		}
		return result.toArray();
	}
	public static final Resource findResource(String name) {
		return findResourceByName(name, globalPool);
	}
	public List<Resource> getChildrenResoures(Resource parent) {
		NodeModel resourceModel = getResourceOutline();
		Node node = resourceModel.search(parent);
		List<?> children = NodeList.nodeListToImplList(resourceModel.getChildren(node));
		List<Resource> resources = new ArrayList<>(children.size());
		for (Object child : children) {
			resources.add((Resource) child);
		}
		return resources;
	}
	public Resource getRbsParentResource(Resource child) {
		NodeModel resourceModel = getResourceOutline();
		Node node = resourceModel.search(child);
		Node parent = resourceModel.getParent(node);
		if (parent == null || parent.isVoid())
			return null;
		return (Resource)parent.getImpl();
	}
	public void setLocalParent(Resource child, Resource parent) {
		Node childNode = getResourceOutline().search(child);
		Node parentNode = parent == null ? null : getResourceOutline().search(parent);
		setLocalParent(childNode,parentNode);
	}

	public void setLocalParent(Node childNode, Node parentNode) {
		Resource child = (Resource) childNode.getImpl();
		Resource parent = (Resource) (parentNode == null ? null : parentNode.getImpl());
		if (getRbsParentResource(child) == parent)
			return;
		Node oldParentNode = getResourceOutline().search(getRbsParentResource(child));
		if (oldParentNode != null)
			oldParentNode.getChildren().remove(childNode);
		ArrayList<Node> temp = new ArrayList<>(1);
		temp.add(childNode);
		getResourceOutline().move(parentNode, temp, -1,NodeModel.NORMAL);
	}
	public ObjectSelectionEventManager getObjectSelectionEventManager() {
		return null;
	}
}

