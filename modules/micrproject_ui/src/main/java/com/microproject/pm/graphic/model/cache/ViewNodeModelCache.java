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
package com.microproject.pm.graphic.model.cache;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;


import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.model.transform.DependencyCacheTransformer;
import com.microproject.pm.graphic.model.transform.NodeCacheTransformer;
import com.microproject.association.InvalidAssociationException;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.hierarchy.HierarchyUtils;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.grouping.core.transform.TransformList;
import com.microproject.grouping.core.transform.ViewTransformerEvent;
import com.microproject.grouping.core.transform.ViewTransformerListener;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

/**
 *
 */
public class ViewNodeModelCache implements NodeModelCache, ViewTransformerListener, CacheListener {
    protected ReferenceNodeModelCache reference;
    protected VisibleNodes visibleNodes;
    protected VisibleDependencies visibleDependencies;
    protected String viewName;


    ViewNodeModelCache(ReferenceNodeModelCache reference,String viewName,Consumer<Object> transformerClosure) {
        this(reference,new VisibleNodes(viewName,new NodeCacheTransformer(viewName,reference,transformerClosure)),
                new VisibleDependencies(viewName,new DependencyCacheTransformer(viewName,reference)));
        this.viewName=viewName;
    }
    /**
     * @param reference
     * @param visibleNodes
     * @param visibleDependencies
     */
    private ViewNodeModelCache(ReferenceNodeModelCache reference,
            VisibleNodes visibleNodes, VisibleDependencies visibleDependencies) {
        this.reference = reference;
        this.visibleNodes = visibleNodes;
        this.visibleDependencies = visibleDependencies;
        addNodeModelListener(this);
        visibleDependencies.setVisibleNodes(visibleNodes);
        visibleNodes.setVisibleDependencies(visibleDependencies);
        reference.bindView(visibleNodes,visibleDependencies);
        ((NodeCacheTransformer)visibleNodes.getTransformer()).getTransformer().addViewTransformerListener(this);

    }

    public NodeModel getModel() {
        return reference.getModel();
    }
    public void setModel(NodeModel model) {
        reference.setModel(model);
    }

	public WalkersNodeModel getWalkersModel(){
		NodeCacheTransformer transformer=(NodeCacheTransformer)visibleNodes.getTransformer();
		return transformer.getWalkersModel();
	}

	public void setType(int type){
		reference.setType(type);
	}
	public int getType(){
		return reference.getType();
	}


	public String getViewName() {
		return viewName;
	}

	public void transformerChanged(ViewTransformerEvent e) {
		update();
	}

	public void update(){
//		System.out.println("ViewNodeModelCache update "+getViewName());
		reference.updateVisibleElements(visibleNodes);
	}

	public ReferenceNodeModelCache getReference(){
		return reference;
	}

	public Object getElementAt(int i) {
	    return visibleNodes.getElementAt(i);
	}
	public ListIterator getIterator(){
	    return visibleNodes.getIterator();
	}
	public ListIterator getIterator(int i){
	    return visibleNodes.getIterator(i);
	}
	public ListIterator getEdgesIterator(){
	    return visibleDependencies.getIterator();
	}
	public ListIterator getEdgesIterator(int i){
	    return visibleDependencies.getIterator(i);
	}


//	public void forEach(CacheClosure c){
//	   Stack stack=new Stack();
//	   GraphicNode node=null,nextNode=null,history;
//	   if (getSize()==0) return;
//	   ListIterator i=getIterator();
//	   node=(GraphicNode)i.next();
//	   while (i.hasNext()){
//	   		nextNode=(GraphicNode)i.next();
//	   		if (nextNode.getLevel()>node.getLevel()){
//	   			stack.push(node);
//	   		}else{
//	   			c.execute(node,nextNode.getLevel()-node.getLevel());
//	   			while (((GraphicNode)stack.peek()).getLevel()>=nextNode.getLevel())
//					c.execute((GraphicNode)stack.pop(),nextNode.getLevel()-node.getLevel());
//	   		}
//	   		node=nextNode;
//	   }
//	   if (nextNode!=null) c.execute(nextNode,nextNode.getLevel()-node.getLevel());
//	}
	public int getMaxLevel(){
		int level=0;
		GraphicNode node;
		for (Iterator i=getIterator();i.hasNext();){
			node=(GraphicNode)i.next();
			if (node.getLevel()>level) level=node.getLevel();
		}
		return level;
	}

    public List<Object> getElementsAt(int[] i) {
	    ArrayList<Object> list = new ArrayList<>(i.length);
	    for (int j=0;j<i.length;j++){
	        Object element=getElementAt(i[j]);
	        if (element!=null) list.add(element);
	    }
	    return list;
	}
	public List<Object> getNodesAt(int[] i) {
	    ArrayList<Object> list = new ArrayList<>(i.length);
	    for (int j=0;j<i.length;j++){
	        Object base=((GraphicNode)getElementAt(i[j])).getNode();
	        if (base!=null) list.add(base);
	    }
	    return list;
	}

	public Object getEdgeElementAt(int i) {
		return visibleDependencies.getElementAt(i);
	}

	public int getSize() {
		return visibleNodes.getSize();
	}
	public int getEdgesSize() {
		return visibleDependencies.getSize();
	}

	public int getRowAt(Object node){
	    return visibleNodes.getRow(node);
	}

	public Object getGraphicNode(Object base){
		return reference.getGraphicNode(base);
	}
	public Object getGraphicDependency(Object base){
		return reference.getGraphicDependency(base);
	}



    public GraphicNode getParent(GraphicNode node) {
        GraphicNode parent=reference.getParent(node);
        if (visibleNodes.getElements().contains(node)) return parent;
        else return null;
    }

    public List<Object> getChildren(GraphicNode node) {
        List<Object> children=reference.getChildren(node);
        if (children==null) return null;
        List<?> elements=visibleNodes.getElements();
        for (Iterator<Object> i=children.iterator();i.hasNext();){
            if (!elements.contains(i.next())) i.remove();
        }
        return children;
    }

    public void changeCollapsedState(GraphicNode node) {
        reference.changeCollapsedState(node);
    }


    public void createDependency(GraphicNode startNode, GraphicNode endNode)
            throws InvalidAssociationException {
       reference.createDependency(startNode,endNode);

    }

    public void createHierarchyDependency(GraphicNode startNode,
            GraphicNode endNode) throws InvalidAssociationException {
        reference.getModel().getHierarchy().move(endNode.getNode(), startNode.getNode(), NodeModel.NORMAL);
    }
    public void addNodeModelListener(CacheListener l) {
       visibleNodes.addNodeModelListener(l);
    }
    public void removeNodeModelListener(CacheListener l) {
        visibleNodes.removeNodeModelListener(l);
    }
    public CacheListener[] getNodeModelListeners() {
        return visibleNodes.getNodeModelListeners();
    }


    public void close() {
    }

    private boolean isAllowedAction(Node node,boolean isParent){
		if (node!=null && (node.getImpl() instanceof Task)){
			boolean r=true;
			Task t=(Task)node.getImpl();
			if (t instanceof SubProj){
				Project p=isParent?((SubProj)t).getSubproject():t.getOwningProject();
				if (p!=null&&p.isReadOnly()) r=false;
			}
			else r=!t.isReadOnly();
			if (!r){
				Alert.error(MessageFormat.format(Messages.getString("Message.readOnlyTask"),new Object[]{t.getName()}));
			}
			return r;
		}
		return true;
    }
    private boolean isAllowedAction(List<?> nodes,boolean checkForROSubproject){
        if (nodes!=null){
	    	for (Object o:nodes){
	    		if (o==null) continue;
	    		if (o instanceof GraphicNode) o=((GraphicNode)o).getNode();
	    		if (!isAllowedAction((Node)o,checkForROSubproject)) return false;
	    	}
    	}
    	return true;
    }

	public void newNode(GraphicNode gnode){
		Node node=gnode.getNode();
		if (!isAllowedAction(node,false)) return;
		if (node!=null && (node.getImpl() instanceof Task) && ((Task)node.getImpl()).isReadOnly()) return; //read only subprojects
		Node parent=getModel().getParent(node);
		int index=parent.getIndex(node);
		getModel().newNode(parent,index,NodeModel.NORMAL);
	}

	@Override
	public Node newNodeBefore(Node node) {
		if (!isNodeEligibleForNew(node) || !isAllowedAction(node, false))
			return null;
		Node parent = getModel().getParent(node);
		if (parent == null)
			return null;
		return getModel().newNode(parent, parent.getIndex(node), NodeModel.NORMAL);
	}

	public void newNode(List nodes) {
		if (nodes == null || nodes.isEmpty()) {
			return;
		}
		for (int i = nodes.size() - 1; i >= 0; i--) {
			Object candidate = nodes.get(i);
			if (!(candidate instanceof GraphicNode)) {
				continue;
			}
			GraphicNode gnode = (GraphicNode) candidate;
			Node node = gnode.getNode();
			if (!isNodeEligibleForNew(node)) {
				continue;
			}
			newNode(gnode);
			return;
		}
	}

	private boolean isNodeEligibleForNew(Node node) {
		if (node == null || !(node.getImpl() instanceof Task)) {
			return true;
		}
		Task task = (Task) node.getImpl();
		if (task instanceof SubProj) {
			Project project = task.getOwningProject();
			return project == null || !project.isReadOnly();
		}
		return !task.isReadOnly();
	}


	public void deleteNodes(List nodes){
		if (!isAllowedAction(nodes,false)) return;
		getModel().remove(nodes,NodeModel.NORMAL);
	}
	public void cutNodes(List nodes){
		if (!isAllowedAction(nodes,false)) return;
		List newNodes=getModel().cut(nodes,NodeModel.NORMAL);
		nodes.clear();
		nodes.addAll(newNodes);
	}
	public void copyNodes(List nodes){
		List newNodes=getModel().copy(nodes,NodeModel.NORMAL);
		nodes.clear();
		nodes.addAll(newNodes);
	}
	public boolean pasteNodes(Node parent,List nodes,int position){
		if (getModel().getDataFactory() instanceof Project project && project.isReadOnly()) return false;
		if (!isAllowedAction(parent,true)) return false;
		getModel().paste(parent,nodes,position,NodeModel.NORMAL);
		return true;
	}

	public void addNodes(Node sibling,List nodes){
		getModel().addBefore(sibling,nodes,NodeModel.NORMAL);
	}

	public boolean isTaskOrderEditable(){
		if (getModel().getDataFactory() instanceof Project project && project.isReadOnly()) return false;
		var transformer=((NodeCacheTransformer)visibleNodes.getTransformer()).getTransformer();
		return transformer.isNoneFilter()&&transformer.isNoneSorter()&&transformer.isNoneGrouper();
	}

	public boolean canMoveNodes(List nodes,int direction){
		if (!isTaskOrderEditable()||nodes==null||nodes.isEmpty()||!isAllowedActionQuietly(nodes,false)) return false;
		List validNodes=validBaseNodes(nodes);
		return !validNodes.isEmpty()&&getModel().canMoveSelectedNodes(validNodes,direction);
	}

	public boolean moveNodes(List nodes,int direction){
		if (!canMoveNodes(nodes,direction)||!isAllowedAction(nodes,false)) return false;
		return getModel().moveSelectedNodes(validBaseNodes(nodes),direction,NodeModel.NORMAL);
	}

	public boolean canRelocateNodes(List nodes,Node anchor,boolean after){
		return resolveRelocationTarget(nodes,anchor,after,false)!=null;
	}

	public boolean relocateNodes(List nodes,Node anchor,boolean after){
		RelocationTarget target=resolveRelocationTarget(nodes,anchor,after,true);
		return target!=null&&getModel().relocate(target.nodes,target.parent,target.position,NodeModel.NORMAL);
	}

	private RelocationTarget resolveRelocationTarget(List nodes,Node anchor,boolean after,boolean showReadOnlyAlert){
		if (!isTaskOrderEditable()||nodes==null||nodes.isEmpty()||anchor==null||anchor.getParent()==null) return null;
		if (!(showReadOnlyAlert?isAllowedAction(nodes,false):isAllowedActionQuietly(nodes,false))) return null;
		Node destination=(Node)anchor.getParent();
		if (!(showReadOnlyAlert?isAllowedAction(destination,true):isAllowedActionQuietly(destination,true))) return null;
		List validNodes=validBaseNodes(nodes);
		if (validNodes.isEmpty()) return null;
		ArrayList roots=new ArrayList();
		HierarchyUtils.extractParents(validNodes,roots);
		Node sourceParent=(Node)((Node)roots.get(0)).getParent();
		if (sourceParent==null||getModel().getHierarchy().getLevel(sourceParent)!=getModel().getHierarchy().getLevel(destination))
			return null;
		int position=destination.getIndex(anchor)+(after?1:0);
		for (Object value:roots){
			Node node=(Node)value;
			if (node.getParent()==destination&&destination.getIndex(node)<position) position--;
		}
		return getModel().canRelocate(validNodes,destination,position)
			?new RelocationTarget(validNodes,destination,position):null;
	}

	private List validBaseNodes(List nodes){
		ArrayList baseNodes=new ArrayList(nodes);
		convertToBase(baseNodes);
		return TransformList.getNotVoidFilter().filterList(baseNodes);
	}

	private boolean isAllowedActionQuietly(List<?> nodes,boolean checkForROSubproject){
		if (nodes==null) return true;
		for (Object value:nodes){
			if (value instanceof GraphicNode) value=((GraphicNode)value).getNode();
			if (value instanceof Node&&!isAllowedActionQuietly((Node)value,checkForROSubproject)) return false;
		}
		return true;
	}

	private boolean isAllowedActionQuietly(Node node,boolean isParent){
		if (node==null||!(node.getImpl() instanceof Task task)) return true;
		if (task instanceof SubProj subproject){
			Project project=isParent?subproject.getSubproject():task.getOwningProject();
			return project==null||!project.isReadOnly();
		}
		return !task.isReadOnly();
	}

	private static final class RelocationTarget{
		private final List nodes;
		private final Node parent;
		private final int position;
		private RelocationTarget(List nodes,Node parent,int position){
			this.nodes=nodes;
			this.parent=parent;
			this.position=position;
		}
	}


	public void expandNodes(List nodes, boolean expand){
		if (nodes==null) return;

		if (nodes.size()>0) {
			Iterator i = nodes.iterator();
			while (i.hasNext()) {
				GraphicNode gnode = (GraphicNode)i.next();
				if (expand && !gnode.isFetched()) // for subprojects
					gnode.fetch();

				if (gnode.isCollapsed() == expand)
					changeCollapsedState(gnode);
			}
		}
	}

	public void indentNodes(List nodes){
		if (nodes==null) return;
		if (!isAllowedAction(nodes,false)) return;
		List validNodes=TransformList.getNotVoidFilter().filterList(convertToBase(nodes));
		if (validNodes.size()>0) getModel().getHierarchy().indent(validNodes,1,getModel(),NodeModel.NORMAL);
	}

	public void outdentNodes(List nodes){
		if (nodes==null) return;
		if (!isAllowedAction(nodes,false)) return;
		List validNodes=TransformList.getNotVoidFilter().filterList(convertToBase(nodes));
		if (validNodes.size()>0) getModel().getHierarchy().indent(validNodes,-1,getModel(),NodeModel.NORMAL);
	}

	//returns same list with converted elements
    private List convertToBase(List gnodes){
        if (gnodes==null) return null;
        for (ListIterator i=gnodes.listIterator();i.hasNext();)
            i.set(((GraphicNode)i.next()).getNode());
        return gnodes;
    }

	private int getLastNormalRow(){
        for (int i=visibleNodes.getSize()-1;i>=0;i--){
        	GraphicNode current=(GraphicNode)visibleNodes.getElementAt(i);
	        if (!current.isVoid())
	            return i;
	    }
        return -1;
	}

//    /**
//	 * Returns the parent/previous,position identification of the position
//	 * to insert a void node.
//	 * The fist normal node preceding it.
//	 * Same level if the node isn't composite and collapsed
//	 * @param row
//	 * @return
//	 */
//	private NodeHierarchyVoidLocation getVoidNodeCreationInfoObject(GraphicNode refNode){
//	    int row=getRowAt(refNode);
//		if (row==0){
//		    int lastRow=getLastNormalRow();
//		    if (row>lastRow) return new NodeHierarchyVoidLocation(NodeHierarchyLocation.END_LOCATION,row-lastRow);
//		    return new NodeHierarchyVoidLocation(new NodeHierarchyLocation(null,null),1);
//		}
//    	GraphicNode node=(GraphicNode)visibleNodes.getElementAt(row-1);
//    	if (node.isVoid()){
//    		NodeHierarchyVoidLocation info=getVoidNodeInfoObject(row-1);
//    		info.setPosition(info.getPosition()+1);
//    		return info;
//    	}else{
//    	    Node parent;
//    	    if (node.isSummary()&&!(node.isCollapsed())){
//    	    	parent=node.getNode();
//    	    }else{
//    	    	parent=getModel().getHierarchy().getParent(node.getNode());
//    	    }
//    	    return new NodeHierarchyVoidLocation(new NodeHierarchyLocation(parent,node.getNode()),1);
//    	}
//	}
//
//	/**
//	 * Returns the parent/previous,position identification of the void node at row
//	 * Apply this to a void node row only
//	 * @param row
//	 * @return
//	 */
//	public NodeHierarchyVoidLocation getVoidNodeInfoObject(GraphicNode refNode){
//	    int row=getRowAt(refNode);
//	    return getVoidNodeInfoObject(row);
//	}
//	private NodeHierarchyVoidLocation getVoidNodeInfoObject(int row){
//
//	    int lastRow=getLastNormalRow();
//	    if (row>lastRow){
//		    GraphicNode gnode=(lastRow>=0)?(GraphicNode)visibleNodes.getElementAt(lastRow):null;
//	    	return new NodeHierarchyVoidLocation(NodeHierarchyLocation.END_LOCATION,row-lastRow,(gnode==null)?1:gnode.getLevel());
//	    }
//
//	    //Find the normal node just before the series of void nodes
//	    //It must be a sibling or a parent
//	    GraphicNode gnode=null;
//	    GraphicNode node0=(GraphicNode)visibleNodes.getElementAt(row);
//        for (int i=row-1;i>-1;i--){
//        	GraphicNode current=(GraphicNode)visibleNodes.getElementAt(i);
//	        if (!current.isVoid()&&getLevel(current)<=getLevel(node0)){
//	        	gnode=current;
//	        	break;
//	        }
//	    }
//
//	    //find the position of the void node in the series
//        //1 is the first one
//        int pos=1;
//        int voidLevel=getLevel(node0);
//        for (;pos<=row;pos++){
//           	GraphicNode current=(GraphicNode)visibleNodes.getElementAt(row-pos);
//           	if (!(current.isVoid()&&getLevel(current)==voidLevel))
//           		break;
//        }
//
//
//
//	    if (gnode==null) return new NodeHierarchyVoidLocation(new NodeHierarchyLocation(null,null),pos);
//
//	    //find the first non void node of level>level of void node
//	    //It is gnode or the parent of gnode
//	    Node parent;
//	    if (getLevel(gnode)<getLevel(node0)){
//	    	parent=gnode.getNode();
//	    }else{
//	    	parent=getModel().getHierarchy().getParent(gnode.getNode());
//	    }
//
//	    return new NodeHierarchyVoidLocation(new NodeHierarchyLocation(parent,gnode.getNode()),pos);
//	}


	public int getLevel(GraphicNode node){
		if (node.isGroup()) return node.getLevel();
		NodeCacheTransformer transformer=(NodeCacheTransformer)visibleNodes.getTransformer();
		return node.getLevel()+transformer.getLevelOffset();
	}
	public int getPertLevel(GraphicNode node){
		return node.getPertLevel();
	}
	public void setPertLevel(GraphicNode node,int level){
		node.setPertLevel(level);
	}




    public VisibleDependencies getVisibleDependencies() {
        return visibleDependencies;
    }
    public VisibleNodes getVisibleNodes() {
        return visibleNodes;
    }

    public boolean isReceiveEvents(){
    	return reference.isReceiveEvents();
    }
    public void setReceiveEvents(boolean receiveEvents){
    	reference.setReceiveEvents(receiveEvents);
    }




    //TreeModel
	public Object getChild(Object obj, int index) {
		ListIterator i=getIterator();
		GraphicNode node;
		GraphicNode ref=null;
		if (obj==getRoot()) ref=(GraphicNode)getRoot();
		else while (i.hasNext()){
			node=(GraphicNode)i.next();
			if (node==obj){
				ref=node;
				break;
			}
		}
		if (ref==null) return null;
		int count=0;
		while (i.hasNext()){
			node=(GraphicNode)i.next();
			if (node.getLevel()<=ref.getLevel()) break;
			else if (node.getLevel()==ref.getLevel()+1){
				if (count==index) return node;
				count++;
			}
		}
		return null;
	}
	public int getChildCount(Object obj) {
		ListIterator i=getIterator();
		GraphicNode node;
		GraphicNode ref=null;
		if (obj==getRoot()) ref=(GraphicNode)getRoot();
		else while (i.hasNext()){
			node=(GraphicNode)i.next();
			if (node==obj){
				ref=node;
				break;
			}
		}
		int count=0;
		if (ref!=null)
		while (i.hasNext()){
			node=(GraphicNode)i.next();
			if (node.getLevel()<=ref.getLevel()) break;
			else if (node.getLevel()==ref.getLevel()+1) count++;
		}
		return count;
	}
	public int getIndexOfChild(Object parent, Object child) {
		ListIterator i=getIterator();
		GraphicNode node;
		GraphicNode ref=null;
		if (parent==getRoot()) ref=(GraphicNode)getRoot();
		else while (i.hasNext()){
			node=(GraphicNode)i.next();
			if (node==parent){
				ref=node;
				break;
			}
		}
		if (ref==null) return -1;
		int count=0;
		while (i.hasNext()){
			node=(GraphicNode)i.next();
			if (node.getLevel()<=ref.getLevel()) break;
			else if (node.getLevel()==ref.getLevel()+1){
				if (node==child) return count;
				count++;
			}
		}
		return -1;
	}
	public Object getRoot() {
		return reference.getRoot();
	}
	public boolean isLeaf(Object obj) {
		ListIterator i=getIterator();
		GraphicNode node;
		GraphicNode ref=null;
		if (obj==getRoot()) ref=(GraphicNode)getRoot();
		else while (i.hasNext()){
			node=(GraphicNode)i.next();
			if (node==obj){
				ref=node;
				break;
			}
		}
		if (ref==null) return true;
		if (i.hasNext()){
			node=(GraphicNode)i.next();
			if (node.getLevel()>ref.getLevel()) return false;
			else return true;
		}
		return true;
	}
	public void valueForPathChanged(TreePath path, Object obj) {
	}

//TreeModel events
	protected EventListenerList treeModelListenerList = new EventListenerList();

	public void addTreeModelListener(TreeModelListener l) {
		treeModelListenerList.add(TreeModelListener.class, l);
	}
	public void removeTreeModelListener(TreeModelListener l) {
		treeModelListenerList.remove(TreeModelListener.class, l);
	}


	 protected void fireTreeModelUpdate(Object source) {
			Object[] listeners = treeModelListenerList.getListenerList();
			TreeModelEvent e = null;
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == TreeModelListener.class) {
					if (e == null) {
						e = new TreeModelEvent(source,new Object[]{getRoot()});
					}
					((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
				}
			}
		}

	    public void graphicNodesCompositeEvent(CompositeCacheEvent e){
	    	fireTreeModelUpdate(this);
	    }



}

