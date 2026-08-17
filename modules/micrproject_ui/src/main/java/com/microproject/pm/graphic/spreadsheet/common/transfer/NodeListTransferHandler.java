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
			spreadSheet.setTransferHandler(handler);
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

