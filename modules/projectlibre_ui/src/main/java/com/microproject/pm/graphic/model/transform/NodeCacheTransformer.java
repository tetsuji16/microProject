/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.microproject.pm.graphic.model.transform;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.document.Document;
import com.microproject.grouping.core.GroupNodeImpl;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.grouping.core.transform.HierarchicObject;
import com.microproject.grouping.core.transform.ViewConfiguration;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.grouping.core.transform.filtering.BaseFilter;
import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.grouping.core.transform.grouping.NodeGroup;
import com.microproject.grouping.core.transform.grouping.NodeGrouper;
import com.microproject.grouping.core.transform.sorting.NodeSorter;
import com.microproject.grouping.core.transform.transformer.NodeTransformer;

/**
 *
 */
public class NodeCacheTransformer implements CacheTransformer {
    protected ViewTransformer transformer;

    protected ReferenceNodeModelCache refCache;
    protected int levelOffset=0;
    protected String viewName;
    protected ViewConfiguration view;

    public NodeCacheTransformer(String viewName,ReferenceNodeModelCache refCache,Consumer<Object> transformerClosure){
    	//System.out.println("viewName="+viewName);
    	view=ViewConfiguration.getView(viewName);
        if (view == null || view.getTransform() == null) {
            view = new ViewConfiguration();
            view.setName(viewName);
            transformer = new ViewTransformer();
        } else {
            transformer=view.getTransform();
        }
        if (transformerClosure!=null) transformerClosure.accept(transformer);

    	this.refCache=refCache;
    	this.viewName=viewName;
    }

	public ViewTransformer getTransformer() {
		return transformer;
	}



    public void transfrom(List list){
    	model.clear();

        if (list==null) return;

        boolean preserveHierarchy=transformer.isPreserveHierarchy();

        if (!transformer.isShowSummary()){
        	preserveHierarchy=false;
        	removeSummaries(list);
        }
        Map<GraphicNode,List<GraphicNode>> assignmentsMap=null;
        if (!transformer.isShowAssignments()) removeAssignments(list);
        if (!transformer.isShowEmptyLines()) removeVoids(list);
        if (transformer.isShowEmptyLines()&&!transformer.isShowEndEmptyLines()) removeEndVoids(list);

        NodeTransformer composition=transformer.getTransformer();

        NodeFilter hiddenFilter=transformer.getHiddenFilter();
        if (hiddenFilter instanceof BaseFilter && !((BaseFilter)hiddenFilter).isActive()) hiddenFilter=null; //to avoid useless filtering in case of BaseFilter
        NodeFilter userFilter=(transformer.isNoneFilter())?null:transformer.getUserFilter();
        boolean filtering=hiddenFilter!=null||userFilter!=null;

        NodeSorter sorter1=transformer.getHiddenSorter();
        NodeSorter sorter2=transformer.getUserSorter();
        boolean sorting=!(sorter1==null&&transformer.isNoneSorter());

        NodeGrouper grouper=transformer.getUserGrouper();
        boolean grouping=!transformer.isNoneGrouper();

        if (!filtering&&!sorting&&!grouping) return;

        if (transformer.isShowAssignments()&&preserveHierarchy&&!transformer.isTreatAssignmentsAsTasks()) assignmentsMap=extractAssignments(list);


        List<GraphicNode> localList=null;
        Stack<GraphicNode> parents=null;
        boolean alreadyExcluded;
        if (preserveHierarchy){
         	localList=new ArrayList<>();
         	parents=new Stack<>();
        }else localList=list;

        GraphicNode gnode,previous=null;
        Object current;
        for (Iterator i=list.iterator();i.hasNext();){
            gnode=(GraphicNode)i.next();
            gnode.setFiltered(false);
            if (!gnode.isVoid()){
	            current=(composition==null)?gnode.getNode():composition.evaluate(gnode.getNode());
	            alreadyExcluded=false;
	             if (hiddenFilter!=null){
	                 if(!hiddenFilter.evaluate(current)){
	                    if (!gnode.isSummary() || !preserveHierarchy){
	                    	i.remove();
		                    continue;
	                    }
                    	if (gnode.isSummary()&&preserveHierarchy) gnode.setFiltered(true);
                    	alreadyExcluded=true;
	                }
	            }
	             if (userFilter!=null&&!alreadyExcluded){
	                 if(!userFilter.evaluate(current)){
	                	 if (!gnode.isSummary() || !preserveHierarchy){
	                		 i.remove();
	 		                continue;
	                	 }
	                    if (gnode.isSummary()&&preserveHierarchy) gnode.setFiltered(true);
	                }
	             }
            }
             if (preserveHierarchy){
	             //contruct a temporary tree for sorting and grouping
//            	 if (parents==null||previous==null){
//            		 System.out.println("null");
//            	 }
	             if (gnode.getLevel()==1){
	            	 localList.add(gnode);
	            	 parents.clear();
	             }else{
	                 if (previous.getLevel()<gnode.getLevel()){
	                	 parents.push(previous);
	                 }else if (previous.getLevel()>=gnode.getLevel()){
	                	 while (parents.size()>=gnode.getLevel()) parents.pop();
	                 }
	                 ((GraphicNode)parents.peek()).getChildren().add(gnode);
	             }
	             previous=gnode;
             }
         }

        //remove parents without children
        if (preserveHierarchy){
        	list.clear();
        	if (transformer.isShowEmptySummaries()){
        		if ("TaskUsage".equals(viewName)) filterBadBranches(localList);
        	}else filterEmptySummaries(localList,false);
        }




        if (sorting){
	        if (sorter1!=null)
	            sorter1.sortList(localList,new GraphicNodeComparator(sorter1,composition),preserveHierarchy);
	        if (!transformer.isNoneSorter())
	            sorter2.sortList(localList,new GraphicNodeComparator(sorter2,composition),preserveHierarchy);
        }



        if (grouping){
        	List<NodeGroup> groups=grouper.getGroups();
        	levelOffset=groups.size();
        	List<GraphicNode> groupedList=new LinkedList<>();
        	groupList(localList,groupedList,groups.listIterator(),null,composition,preserveHierarchy);
        	localList.clear();
        	localList.addAll(groupedList);
        }


        if (preserveHierarchy){ //converts tmp tree to list
         	treeToList(localList, list);
        }

        if (assignmentsMap!=null) recoverAssignments(list, assignmentsMap);

//        if (transformer.isShowEmptyLines())
//        	placeVoidNodes(list);


    }

	private boolean filterEmptySummaries(List<GraphicNode> list,boolean filterVoids){
		boolean containsNonSummaries=false;
		boolean containsVoids=false;
		for (Iterator<GraphicNode> i=list.iterator();i.hasNext();){
			GraphicNode gnode=i.next();
			if (gnode.isVoid()){
				containsVoids=true;
				continue;
			}
			if (!gnode.isSummary()||(gnode.getChildren().size()>0 && filterEmptySummaries(gnode.getChildren(),true))) containsNonSummaries=true;
			else i.remove();
		}
		if (!containsNonSummaries&&(!containsVoids || filterVoids)) list.clear();
		return containsNonSummaries;
	}

	private void filterBadBranches(List<GraphicNode> list){
		filterBadBranches(list, false);
	}
	private boolean filterBadBranches(List<GraphicNode> list,boolean cut){
		boolean keep=false;
		for (Iterator<GraphicNode> i=list.iterator();i.hasNext();){
			GraphicNode gnode=i.next();
			boolean cutBranch=cut|gnode.isFiltered();
			if (!gnode.isSummary()||(!gnode.isFiltered()&&gnode.getChildren().size()==0)||filterBadBranches(gnode.getChildren(),cutBranch)||!gnode.isFiltered()) keep=true;
			else i.remove();
		}
		return keep;
	}

	private void treeToList(List<GraphicNode> in,List<GraphicNode> out){
		for (Iterator<GraphicNode> i=in.iterator();i.hasNext();){
			HierarchicObject gnode=i.next();
			GraphicNode graphicNode=(GraphicNode)gnode;
			out.add(graphicNode);
			if (graphicNode.getChildren().size()>0) treeToList(graphicNode.getChildren(),out);
			graphicNode.getChildren().clear();
		}
	}



	public int getLevelOffset() {
		return levelOffset;
	}


	private class GraphicNodeComparator implements Comparator{
	    protected NodeSorter comparator;
	    protected NodeTransformer composition;
	    private GraphicNodeComparator(NodeSorter comparator){
	        this(comparator,null);
	    }
	    private GraphicNodeComparator(NodeSorter comparator,NodeTransformer composition){
	        this.comparator=comparator;
	        this.composition=composition;
	    }
	    public int compare(Object o1, Object o2) {
	    	GraphicNode n1=(GraphicNode)o1;
	    	GraphicNode n2=(GraphicNode)o2;
	    	if (n1==n2) return 0;
	    	else if (n1.isVoid()) return 1;
	    	else if (n2.isVoid()) return -1;
	    	if (composition==null)
	    		return comparator.compare(n1.getNode(),n2.getNode());
	    	else return comparator.compare(composition.evaluate(n1.getNode()),composition.evaluate(n2.getNode()));
	    }
	    public ListIterator getCurrentSorter(){
	        return comparator.getCurrentSorter();
	    }
	}

	protected static GraphicNode createGroupWithName(int level,NodeGroup group,String name){
		Node node=NodeFactory.getInstance().createGroup(group,name);
		GraphicNode gnode=new GraphicNode(node,level);
		gnode.setComposite(true);
		gnode.setSummary(true);
		gnode.setCollapsed(false);
		return gnode;
	}

	protected  GraphicNode createGroup(int level,NodeGroup group,NodeSorter sorter,Node node){
	    return createGroupWithName(level,group,sorter.getGroupName(node));
	}

    private void groupList(List<GraphicNode> list,List<GraphicNode> destList,ListIterator<NodeGroup> groupIterator,Node parentGroup,NodeTransformer composition, boolean preserveHierarchy){
    	NodeGroup group=groupIterator.next();
    	NodeSorter sorter=group.getSorter();
    	GraphicNodeComparator gcomp=new GraphicNodeComparator(sorter,composition);
    	sorter.sortList(list,gcomp,preserveHierarchy);
    	GraphicNode last=null;
    	List<GraphicNode> nodes=null;
    	GraphicNode current;
    	for (ListIterator<GraphicNode> i=list.listIterator();i.hasNext();){
    		current=i.next();
    		if (last==null){
    			nodes=new LinkedList<>();
    		}else if (gcomp.compare(last,current)!=0){
    			handleGroup(destList,groupIterator,parentGroup,group,last,nodes,composition,preserveHierarchy);
    			nodes=new LinkedList<>();
    		}
    		nodes.add(current);
    		last=current;
    	}
    	if (nodes!=null&&nodes.size()>0){
			handleGroup(destList,groupIterator,parentGroup,group,last,nodes,composition,preserveHierarchy);
    	}
    	groupIterator.previous();
    }
    private void handleGroup(List<GraphicNode> destList,ListIterator<NodeGroup> groupIterator,Node parentGroup,NodeGroup group,GraphicNode last, List<GraphicNode> nodes,NodeTransformer composition, boolean preserveHierarchy){
		GraphicNode groupNode=createGroup(groupIterator.nextIndex(),group,group.getSorter(),last.getNode());
		destList.add(groupNode);
		model.addRelationship(parentGroup,groupNode.getNode());
		if (groupIterator.hasNext()){
			groupList(nodes,destList,groupIterator,groupNode.getNode(),composition,preserveHierarchy);
		}
		else{
    		for (Iterator<GraphicNode> j=nodes.iterator();j.hasNext();)
    			model.addRelationship(groupNode.getNode(),j.next().getNode());
    		destList.addAll(nodes);
    	}
    }






    private void placeVoidNodes(List<GraphicNode> list){
    	ListIterator<GraphicNode> i=list.listIterator();
    	while (i.hasNext()){
        	GraphicNode gnode=i.next();
        	//if (!gnode.isGroup()){ //Already disabled if sorters or groupers are working
        		placeVoidNodes(i,gnode.getNode());
        	//}
    	}
    	placeVoidNodes(i,(Node)refCache.getModel().getRoot());
    }
    private void placeVoidNodes(ListIterator<GraphicNode> i,Node node){
    		Node current;
        	for (Enumeration e=node.children();e.hasMoreElements();){
            	current=(Node)e.nextElement();
        		if (current.isVoid()){
        			GraphicNode gcurrent=refCache.getGraphicNode(current);
        			i.add(gcurrent);
        		}
        	}
    }



//    private void placeVoidNodes(ListIterator i,int maxLevel){
//        List nodes;
//        GraphicNode previous,child;
//        Iterator j;
//        List parentLevelVoidNodes=null;
//
//        while (i.hasNext()){
//            previous=(GraphicNode)i.next();
//            if (previous.getLevel()<=maxLevel){
//                i.previous();
//                return;
//            }
//            nodes=refCache.getVoidNodes(previous);
//            if (nodes!=null)
//            for (j=nodes.iterator();j.hasNext();){
//                child=(GraphicNode)j.next();
//                if (child.getLevel()>previous.getLevel()){
//                    if (!(previous.isComposite()&&previous.isCollapsed())) i.add(child);
//                }
//                else{
//                    if (parentLevelVoidNodes==null)
//                        parentLevelVoidNodes=new LinkedList();
//                    parentLevelVoidNodes.add(child);
//                }
//            }
//            if (parentLevelVoidNodes!=null){ //adds voids nodes at previous level
//                placeVoidNodes(i,previous.getLevel());
//                for (j=parentLevelVoidNodes.iterator();j.hasNext();)
//                    i.add(j.next());
//                parentLevelVoidNodes=null;
//            }
//        }
//    }
//
//    private void placeVoidNodes(List list){
//        placeVoidNodes(list.listIterator(),0);
//
//        List nodes=refCache.getVoidNodes(NodeCache.BEGIN_VOIDNODES);
//        if (nodes!=null) list.addAll(0,nodes);
//
//        nodes=refCache.getVoidNodes(NodeCache.END_VOIDNODES);
//        if (nodes!=null) list.addAll(nodes);
//
//
//    }

    private void removeVoids(List<GraphicNode> list){
    	GraphicNode current;
        for (ListIterator<GraphicNode> i=list.listIterator();i.hasNext();){
            current=i.next();
            if (current.isVoid()){
            	i.remove();
            }
        }
    }
    private void removeEndVoids(List<GraphicNode> list){
    	GraphicNode current;
        for (ListIterator<GraphicNode> i=list.listIterator(list.size());i.hasPrevious();){
            current=i.previous();
            if (current.isVoid()){
            	i.remove();
            }else break;
        }
    }

    private void removeSummaries(List<GraphicNode> list){
    	GraphicNode current;
        for (ListIterator<GraphicNode> i=list.listIterator();i.hasNext();){
            current=i.next();
            if (current.isSummary()){
            	i.remove();
            }
        }
    }
    private void removeAssignments(List<GraphicNode> list){
    	GraphicNode current;
        for (ListIterator<GraphicNode> i=list.listIterator();i.hasNext();){
            current=i.next();
            if (current.isAssignment()){
            	i.remove();
            }
        }
    }
    private Map<GraphicNode,List<GraphicNode>> extractAssignments(List<GraphicNode> list){
    	Map<GraphicNode,List<GraphicNode>> map=new HashMap<GraphicNode, List<GraphicNode>>();
    	GraphicNode current,last;
    	Stack<GraphicNode> path=new Stack<GraphicNode>();
        for (ListIterator<GraphicNode> i=list.listIterator();i.hasNext();){
            current=i.next();
            if (current.getLevel()==1){
            	path.clear();
            	path.push(current);
            	continue;
            }
            while ((last=path.peek()).getLevel()>=current.getLevel()) path.pop();
            if (current.isAssignment()){
            	GraphicNode task=path.peek();
            	List<GraphicNode> ass=map.get(task);
            	if (ass==null){
            		ass=new LinkedList<GraphicNode>();
            		map.put(task, ass);
            	}
            	ass.add(current);
            	i.remove();
            }
            path.push(current);
        }
        return map;
    }
    private void recoverAssignments(List<GraphicNode> list,Map<GraphicNode,List<GraphicNode>> map){
    	GraphicNode current=null;
        for (ListIterator<GraphicNode> i=list.listIterator();i.hasNext();){
            current=i.next();
            if (map.containsKey(current)){
            	List<GraphicNode> ass=map.get(current);
            	for (GraphicNode a : ass) {
					i.add(a);
				}
            }
        }
     }



	public WalkersNodeModel getWalkersModel(){
		return model;
	}

	private TransformerNodeModel model=new TransformerNodeModel();
	class TransformerNodeModel implements WalkersNodeModel{
	    protected MultiMap childrenMap=new MultiHashMap();
	    protected Map<Node,Node> parentMap=new HashMap<>();

		public List<GraphicNode> getChildren(Node node) {
			if (node.getImpl() instanceof GroupNodeImpl){
				return (List<GraphicNode>)childrenMap.get(node);
			}
			else return refCache.getModel().getChildren(node);
		}

		public Node getParent(Node child) {
			if (parentMap.containsKey(child)){
				return (Node)parentMap.get(child);
			}
			else return refCache.getModel().getParent(child);
		}
		public boolean isSummary(Node node) {
			if (node.getImpl() instanceof GroupNodeImpl) return true;
			else return refCache.getModel().isSummary(node);
		}
		public Node search(Object key) {
			Node node=refCache.getModel().search(key);
			return null;
		}

		public void addRelationship(Node parent,Node child){
			if (parent!=null) childrenMap.put(parent,child);
			parentMap.put(child,parent);
		}

		public void clear(){
			childrenMap.clear();
			parentMap.clear();
		}



        public Document getDocument() {
            return refCache.getDocument();
        }
	}

}

