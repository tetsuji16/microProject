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
package com.microproject.pm.graphic.spreadsheet.common.transfer;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;

import com.microproject.field.Field;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

/**
 *
 */
public class NodeListTransferHandler extends TransferHandler {
	    private static final Logger logger = Logger.getLogger(NodeListTransferHandler.class.getName());
	    public NodeListTransferHandler(SpreadSheet spreadSheet){
	    	super();
	    	this.spreadSheet=spreadSheet;
	    }
	    transient protected SpreadSheet spreadSheet;
	    private transient List<Node> pendingCutNodes;
	    private transient int[] pendingCutRows;
	    private transient int[] pendingCutColumns;
	    private transient boolean pendingCellCut;
	    
	    public SpreadSheet getSpreadSheet() {
			return spreadSheet;
		}
		public void setSpreadSheet(SpreadSheet spreadSheet) {
			this.spreadSheet = spreadSheet;
		}
		public void exportToClipboard(JComponent c, Clipboard clip, int action) {
	        boolean exportSuccess = false;
	        Transferable t = null;

	        if (action != NONE) {
	            t = createTransferable(c,action);
	            if (t != null) {
				try {
					clip.setContents(t, null);
					exportSuccess = true;
				} catch (IllegalStateException e) {
					logger.log(Level.FINE, "Clipboard is currently unavailable", e);
				}
	            }
	        }

	        if (exportSuccess) {
	            exportDone(c, t, action);
	        } else {
	            exportDone(c, null, NONE);
	        }
	    }
		private boolean transformSubprojectBranches(Node parent,NodeModelDataFactory dataFactory,Predicate p){
			if (dataFactory instanceof Project &&
					parent.getImpl() instanceof SubProj
//					&&!((Project)dataFactory).getSubprojectHandler().canInsertProject( ((SubProj)parent.getImpl()).getSubprojectUniqueId() )
			){
				if (!p.evaluate(parent)) return false;
				
			}
			for (Enumeration e=parent.children();e.hasMoreElements();){
					Node node=(Node)e.nextElement();
					if (!transformSubprojectBranches(node,dataFactory,p)) return false;

			}
			return true;
		}
		
	protected Transferable createTransferable(JComponent c, int action) {
		SpreadSheet spreadSheet=getSpreadSheet(c);
		if (spreadSheet==null) return null;
		spreadSheet.finishCurrentOperations();
		ArrayList<Node> nodes = new ArrayList<>(spreadSheet.getSelectedNodes());

		ArrayList<Field> fields = copyFields(spreadSheet.getSelectedFields());
		boolean nodeSelection=(fields==null);
		if (fields==null) fields=copyFields(spreadSheet.getSelectableFields());
		int[] rows=spreadSheet.getSelectedRows();
		int[] columns=spreadSheet.getSelectedColumns();
		NodeListTransferable transferable = new NodeListTransferable(nodes,fields,spreadSheet,
			rows,columns,nodeSelection);
		pendingCutNodes=null;
		pendingCutRows=null;
		pendingCutColumns=null;
		pendingCellCut=false;
		if (action==TransferHandler.COPY) return transferable;
		if (action!=TransferHandler.MOVE) return null;
		if (nodeSelection){
			for (Node node:nodes) {
				final boolean[] okForAll=new boolean[]{false};
				if (!transformSubprojectBranches(node,spreadSheet.getCache().getModel().getDataFactory(),new Predicate(){
					public boolean evaluate(Object arg0) {
						if (okForAll[0]) return true;
						boolean r=Alert.okCancel(Messages.getString("Message.subprojectCut"));
						if (r) okForAll[0]=true;
						return r;
					}
				})) return null;
			}
			pendingCutNodes=nodes;
		}else{
			pendingCutRows=rows;
			pendingCutColumns=columns;
			pendingCellCut=true;
		}
		return transferable;
	}

	protected void exportDone(JComponent source, Transferable data, int action) {
		try {
			if (data==null||action!=TransferHandler.MOVE) return;
			if (pendingCellCut){
				spreadSheet.cutSelectedCellValues(pendingCutRows,pendingCutColumns);
			}else if (pendingCutNodes!=null){
				spreadSheet.commitTaskCut(pendingCutNodes);
			}
		}finally{
			pendingCutNodes=null;
			pendingCutRows=null;
			pendingCutColumns=null;
			pendingCellCut=false;
		}
	}
	    public boolean importData(JComponent c, Transferable t) {
	    	SpreadSheet spreadSheet=getSpreadSheet(c);
	    	if (spreadSheet==null) return false;
	    	spreadSheet.finishCurrentOperations();
	    	DataFlavor flavor=getFlavor(t.getTransferDataFlavors());
	        if (flavor!=null) {
	            try {
	            	NodeModel model=((CommonSpreadSheetModel)spreadSheet.getModel()).getCache().getModel();
	            	Object data=t.getTransferData(flavor);
				if (data instanceof Reader reader){
					StringWriter writer=new StringWriter();
					reader.transferTo(writer);
					data=writer.toString();
				}
	        		if (data==null) return false;
	            	List nodes=null;
	        		if (data instanceof List<?>){
	        			nodes=new ArrayList<>((List<Node>)data);
	        			
	        	    	for (Iterator<Node> i=nodes.iterator();i.hasNext();) {
	        	    		Node node=i.next();
	        				transformSubprojectBranches(node,model.getDataFactory(),new Predicate(){
								public boolean evaluate(Object arg0) {
									Node parent=(Node)arg0;
									//change implementation
									NormalTask task=new NormalTask();
									Task source=((Task)parent.getImpl());
									source.cloneTo(task);
									//task.setDuration(source.getActualDuration());
									parent.setImpl(task);
									return true;
								}	 
							});
	        			}

					return spreadSheet.pasteNodesFromClipboard(nodes);
	        		}else if (data instanceof String){
					if (!spreadSheet.prepareCellPaste()) return false;
//	        			ArrayList fields =spreadSheet.getSelectedFields();
//	        			if (fields==null){
//	        				fields=spreadSheet.getSelectableFields(); //The whole line is selected
//		        			nodes=NodeListTransferable.stringToNodeList((String)data,spreadSheet,fields,model.getDataFactory());
//	        			}else{
//	        				NodeListTransferable.pasteString((String)data,spreadSheet);
//	        			}
	        			int[] rows = spreadSheet.getSelectedRows();
	        			int[] cols = spreadSheet.getSelectedColumns();
	        			int result = NodeListTransferable.pasteStringIntoSelection((String)data, spreadSheet, rows, cols);
	        			if (result == NodeListTransferable.PASTE_FAILED) {
	        				return false;
	        			}
	        			if (result == NodeListTransferable.PASTE_NOT_APPLICABLE) {
	        				return NodeListTransferable.pasteString((String)data,spreadSheet);
	        			}
	        			return true;
	        		}else return false;
	            } catch (UnsupportedFlavorException | IOException e) {
	                logger.log(Level.FINE, "Failed to import spreadsheet clipboard data", e);
	                return false;
	            }
	        }
	        return false;
	    }
	    
	    protected SpreadSheet getSpreadSheet(JComponent c){
			if (c instanceof SpreadSheet){
	    		return (SpreadSheet)c;
			}else return null;
	    }

	    @SuppressWarnings("unchecked")
	    private static ArrayList<Field> copyFields(List source) {
	    	if (source == null) {
	    		return null;
	    	}
	    	return new ArrayList<>((List<Field>) source);
	    }

	    protected DataFlavor getFlavor(DataFlavor[] flavors) {
//    		for (int i=0;i<flavors.length;i++){
//    			System.out.println("flavor #"+i+": "+flavors[i]);
//    		}
			for (int i=0;i<flavors.length;i++){
				if (NodeListTransferable.isNodeListFlavor(flavors[i])
						|| DataFlavor.stringFlavor.equals(flavors[i])
						|| DataFlavor.getTextPlainUnicodeFlavor().equals(flavors[i]))
					return flavors[i];
			}
	        return null;
	    }

	    public boolean canImport(JComponent c, DataFlavor[] flavors) {
	        return getFlavor(flavors)!=null;
	    }
	    
	    public static void registerWith(SpreadSheet spreadSheet){
	    	NodeListTransferHandler handler=new NodeListTransferHandler(spreadSheet);
//	    	if (c instanceof SpreadSheet){
//	    		SpreadSheet spreadSheet=(SpreadSheet)c;
//	    		handler.setSpreadSheet(spreadSheet);
//	    	}
			spreadSheet.setTransferHandler(handler);
			
			InputMap imap = spreadSheet.getInputMap();
			imap.put(KeyStroke.getKeyStroke("ctrl X"),
					NodeListTransferHandler.getCutAction().getValue(Action.NAME));
			imap.put(KeyStroke.getKeyStroke("ctrl C"),
					NodeListTransferHandler.getCopyAction().getValue(Action.NAME));
			imap.put(KeyStroke.getKeyStroke("ctrl V"),
					NodeListTransferHandler.getPasteAction().getValue(Action.NAME));
			//c.setInputMap(JComponent.WHEN_FOCUSED,imap);
			
			ActionMap amap = spreadSheet.getActionMap();
			amap.put(NodeListTransferHandler.getCutAction().getValue(Action.NAME),
					NodeListTransferHandler.getCutAction());
			amap.put(NodeListTransferHandler.getCopyAction().getValue(Action.NAME),
					NodeListTransferHandler.getCopyAction());
			amap.put(NodeListTransferHandler.getPasteAction().getValue(Action.NAME),
					NodeListTransferHandler.getPasteAction());

	    }
	    
	    
	    protected transient NodeListTransfertAction nodeListCutAction, nodeListCopyAction,nodeListPasteAction;
	    
	    protected void initCutAction(SpreadSheet.SpreadSheetAction a) {
	    	nodeListCutAction=new NodeListTransfertAction(getCutAction(),a,spreadSheet);
	    	nodeListCutAction.putValue("Name",Messages.getString("Spreadsheet.Action.cut"));
	    }
	    protected void initCopyAction(SpreadSheet.SpreadSheetAction a) {
	    	nodeListCopyAction=new NodeListTransfertAction(getCopyAction(),a,spreadSheet);
	    	nodeListCopyAction.putValue("Name",Messages.getString("Spreadsheet.Action.copy"));
	    }
	    protected void initPasteAction(SpreadSheet.SpreadSheetAction a) {
	    	nodeListPasteAction=new NodeListTransfertAction(getPasteAction(),a,spreadSheet);
	    	nodeListPasteAction.putValue("Name",Messages.getString("Spreadsheet.Action.paste"));
	    }
		public NodeListTransfertAction getNodeListCopyAction() {
			if (nodeListCopyAction==null) initCopyAction(spreadSheet.getCopyAction());
			return nodeListCopyAction;
		}
		public NodeListTransfertAction getNodeListCutAction() {
			if (nodeListCutAction==null) initCutAction(spreadSheet.getCutAction());
			return nodeListCutAction;
		}
		public NodeListTransfertAction getNodeListPasteAction() {
			if (nodeListPasteAction==null) initPasteAction(spreadSheet.getPasteAction());
			return nodeListPasteAction;
		}
	    

	    
	}

