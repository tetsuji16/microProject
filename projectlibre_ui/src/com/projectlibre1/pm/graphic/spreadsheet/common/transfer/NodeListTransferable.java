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
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.projectlibre1.pm.graphic.spreadsheet.common.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.CollectionUtils;

import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.projectlibre1.field.Field;
import com.projectlibre1.field.FieldContext;
import com.projectlibre1.field.FieldParseException;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.grouping.core.NodeFactory;
import com.projectlibre1.grouping.core.model.NodeModel;
import com.projectlibre1.grouping.core.model.NodeModelDataFactory;
import com.projectlibre1.options.EditOption;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.Task;



/**
 *
 */
public class NodeListTransferable implements Transferable {
    private static final Logger logger = Logger.getLogger(NodeListTransferable.class.getName());
    private static final int NODE_LIST = 0;
    private static final int STRING = 1;
    private static final int PLAIN_TEXT = 2;
    
    public static final String NODE_LIST_MIME_TYPE=DataFlavor.javaJVMLocalObjectMimeType+";class=java.util.Vector";
    
    private DataFlavor[] flavors;
    private DataFlavor nodeListDataFlavor;
    private Set flavorSet;

	protected ArrayList nodeList;
	protected ArrayList fields;
	protected SpreadSheet spreadsheet;
	protected int[] rows,cols;
	protected boolean nodeSelection;
	//protected String sdata;

	public NodeListTransferable(ArrayList nodeList, ArrayList fields,SpreadSheet spreadSheet,int[] rows,int[] cols, boolean nodeSelection) {
		this.nodeSelection=nodeSelection;
		try {
			nodeListDataFlavor=new DataFlavor(NODE_LIST_MIME_TYPE);
		} catch (ClassNotFoundException e) {}
		if (nodeSelection){
				flavors=new DataFlavor[]{
						nodeListDataFlavor,
						DataFlavor.stringFlavor,
						DataFlavor.getTextPlainUnicodeFlavor()}; //TODO isRepresentationClassReader(||InputStream)||isFlavorTextType+flavor.getReaderForText()
			this.nodeList=nodeList;
			this.fields=fields;
		}else{
			flavors=new DataFlavor[]{
					DataFlavor.stringFlavor,
					DataFlavor.getTextPlainUnicodeFlavor()}; //TODO isRepresentationClassReader(||InputStream)||isFlavorTextType+flavor.getReaderForText()
			//sdata=nodeListToString(nodeList,spreadSheet,fields);
		}
		flavorSet=new HashSet();
		//Collections.addAll(flavorSet,flavors); //jdk 1.5
		//for (int i=0;i<flavors.length;i++) flavorSet.add(flavors[i]);
		CollectionUtils.addAll(flavorSet,flavors); //replaced JDK 1.5 code with this call
		this.spreadsheet=spreadSheet;
		this.rows=rows;
		this.cols=cols;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return (DataFlavor[])flavors.clone();
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (int i = 0; i < flavors.length; i++) {
    	    if (flavor.equals(flavors[i])) {
    	        return true;
    	    }
    	}
    	return false;
	}

	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (!flavorSet.contains(flavor)) throw new UnsupportedFlavorException(flavor);
		if (nodeListDataFlavor.equals(flavor)){
			NodeModel model=((CommonSpreadSheetModel)spreadsheet.getModel()).getCache().getModel();
//			ArrayList nl =nodeList;
//			nodeList=new Vector(nl.size());
//			nodeList.addAll(model.copy(nl,NodeModel.SILENT));
			return model.copy(nodeList,NodeModel.SILENT);
		}else if (DataFlavor.stringFlavor.equals(flavor))
		    return selectionToString(spreadsheet,rows,cols);
//		    return (sdata==null)?nodeListToString(nodeList,spreadsheet,fields):sdata;
		else if (DataFlavor.getTextPlainUnicodeFlavor().equals(flavor))
		    return new StringReader(selectionToString(spreadsheet,rows,cols));
	    	//return new StringReader((sdata==null)?nodeListToString(nodeList,spreadsheet,fields):sdata);
		else throw new UnsupportedFlavorException(flavor);
	}
	
//	public Object getTransferData(DataFlavor[] flavors) throws UnsupportedFlavorException, IOException {
//		for (int i=0;i<flavors.length;i++){
//			if (isDataFlavorSupported(flavors[i]))
//				return getTransferData(flavors[i]);
//		}
//		throw new UnsupportedFlavorException(flavors[0]);
//}
	
	public static String nodeListToString(List nodeList,SpreadSheet spreadsheet,List fields){
		StringBuilder sb = new StringBuilder();
		for (Iterator i=nodeList.iterator();i.hasNext();){
			nodeToString((Node)i.next(),sb,spreadsheet,fields);
		}
		return sb.toString();
	}
	public static void nodeToString(Node node, StringBuilder sb, SpreadSheet spreadsheet, List fields){
		CommonSpreadSheetModel model=(CommonSpreadSheetModel)spreadsheet.getModel();
		Object value;
		Field field;
		Iterator fieldsIterator=fields.iterator();
		boolean first=true;
		//String s=null;
		while(fieldsIterator.hasNext()){
			field=(Field)fieldsIterator.next();
			value=field.getValue(node,model.getCache().getWalkersModel(),model.getFieldContext());
			if (first) first=false;
			else sb.append('\t');
			sb.append((value==null)?"":value.toString());
			//s=sb.toString();
			//System.out.println("s="+s);
		}
		sb.append('\n');
		for (Iterator i=node.childrenIterator();i.hasNext();)
			nodeToString((Node)i.next(),sb,spreadsheet,fields);
	}
	
	
	
	public static String selectionToString(SpreadSheet spreadsheet,int[] rows, int[] cols){
		StringBuilder sb = new StringBuilder();
		Object value;
		for (int r=0;r<rows.length;r++){
			for (int c=0;c<cols.length;c++){
				value=spreadsheet.getValueAt(rows[r],cols[c]);
				if (value!=null&&!(value instanceof Task))
					if (value instanceof Date)
						sb.append(EditOption.getInstance().getDateFormat().format((Date)value));
					else sb.append(value.toString());
				if (c<cols.length-1) sb.append('\t');
				else sb.append('\n');
			}
		}
		return sb.toString();
	}
	
	
	

	public static ArrayList stringToNodeList(String s,SpreadSheet spreadsheet,List fields,NodeModelDataFactory factory){
		ArrayList list = new ArrayList();
		StringTokenizer st=new StringTokenizer(s,"\n\r");
		Node node;
		while (st.hasMoreTokens()){
			node=stringToNode(st.nextToken(),spreadsheet,fields,factory);
			if (node!=null) list.add(node);
		}
		return list;
	}
	public static Node stringToNode(String s,SpreadSheet spreadsheet,List fields,NodeModelDataFactory factory){
		String category=spreadsheet.getSpreadSheetCategory();
		Node node=null;
		String delim="\t";
		StringTokenizer st=new StringTokenizer(s,delim,true);
		if (st.hasMoreTokens()){
			if (SpreadSheet.TASK_CATEGORY.equals(category)) node=NodeFactory.getInstance().createTask((Project)factory);
			else if (SpreadSheet.RESOURCE_CATEGORY.equals(category)) node=NodeFactory.getInstance().createResource((ResourcePool)factory);
			else return null;
			
			CommonSpreadSheetModel model=(CommonSpreadSheetModel)spreadsheet.getModel();
			String valueS;
			Field field;
			Iterator fieldsIterator=fields.iterator();
			while(st.hasMoreTokens()&&fieldsIterator.hasNext()){
				valueS=st.nextToken();
				if (delim.equals(valueS)) valueS="";
				else if (st.hasMoreTokens()) st.nextToken();
				field=(Field)fieldsIterator.next();
				try {
					field.setValue(node,model.getCache().getWalkersModel(),spreadsheet,valueS,model.getFieldContext());
				} catch (FieldParseException e) {}
			}
		}
		return node;
	}

	public static boolean pasteString(String s,SpreadSheet spreadsheet){
		int[] rows=spreadsheet.getSelectedRows();
		int[] cols=spreadsheet.getSelectedColumns();
		if (rows.length>0&&cols.length>0){
			int result=pasteStringIntoSelection(s,spreadsheet,rows,cols);
			if (result==PASTE_APPLIED||result==PASTE_FAILED)
				return result==PASTE_APPLIED;
		}
		if (rows.length>0&&cols.length>0)
			return pasteString(s,spreadsheet,rows[0],cols[0]);
		return false;
	}

	static final int PASTE_NOT_APPLICABLE = 0;
	static final int PASTE_APPLIED = 1;
	static final int PASTE_FAILED = 2;

	static int pasteStringIntoSelection(String s,SpreadSheet spreadsheet,int[] rows,int[] cols){
		String[][] values=parseClipboardTable(s);
		if (values.length==0||values[0].length==0)
			return PASTE_NOT_APPLICABLE;
		if (rows.length==1&&cols.length==1)
			return PASTE_NOT_APPLICABLE;
		CommonSpreadSheetModel model=(CommonSpreadSheetModel)spreadsheet.getModel();
		FieldContext fieldContext=model.getFieldContext();
		boolean round=fieldContext.isRound();
		boolean parseOnly=fieldContext.isParseOnly();
		fieldContext.setRound(true);
		try {
			fieldContext.setParseOnly(true);
			if (!applyClipboardValues(model, values, rows, cols)) {
				return PASTE_FAILED;
			}
			fieldContext.setParseOnly(false);
			if (!applyClipboardValues(model, values, rows, cols)) {
				return PASTE_FAILED;
			}
			return PASTE_APPLIED;
		} finally {
			fieldContext.setParseOnly(parseOnly);
			fieldContext.setRound(round);
		}
	}

	private static boolean applyClipboardValues(CommonSpreadSheetModel model, String[][] values, int[] rows, int[] cols) {
		boolean ok = true;
		if (values.length==1&&values[0].length==1){
			for (int i=0;i<rows.length;i++)
				for (int j=0;j<cols.length;j++)
					ok &= setValueAt(model,values[0][0],rows[i],cols[j]);
			return ok;
		}
		if (values.length==1&&values[0].length==cols.length){
			for (int i=0;i<rows.length;i++)
				for (int j=0;j<cols.length;j++)
					ok &= setValueAt(model,values[0][j],rows[i],cols[j]);
			return ok;
		}
		if (values[0].length==1&&values.length==rows.length){
			for (int i=0;i<rows.length;i++)
				for (int j=0;j<cols.length;j++)
					ok &= setValueAt(model,values[i][0],rows[i],cols[j]);
			return ok;
		}
		if (values.length==rows.length&&values[0].length==cols.length){
			for (int i=0;i<rows.length;i++)
				for (int j=0;j<cols.length;j++)
					ok &= setValueAt(model,values[i][j],rows[i],cols[j]);
			return ok;
		}
		return false;
	}

	private static String[][] parseClipboardTable(String s){
		if (s==null)
			return new String[0][0];
		String[] rows=s.replace("\r\n","\n").replace('\r','\n').split("\n",-1);
		if (rows.length>0&&rows[rows.length-1].length()==0){
			String[] trimmed=new String[rows.length-1];
			System.arraycopy(rows,0,trimmed,0,trimmed.length);
			rows=trimmed;
		}
		String[][] values=new String[rows.length][];
		for (int i=0;i<rows.length;i++)
			values[i]=rows[i].split("\t",-1);
		return values;
	}

	private static boolean setValueAt(CommonSpreadSheetModel model,String value,int row,int column){
		try{
			model.setValueAt(value,row,column+1);
			return true;
		}catch(Exception e){
			logger.log(Level.FINE, "Failed to paste cell value at row {0}, col {1}", new Object[]{row, column});
			return false;
		}
	}
	public static boolean pasteString(String s,SpreadSheet spreadsheet,int row0, int col0){
		String[][] values=parseClipboardTable(s);
		if (values.length==0||values[0].length==0)
			return false;
		CommonSpreadSheetModel model=(CommonSpreadSheetModel)spreadsheet.getModel();
		FieldContext fieldContext=model.getFieldContext();
		boolean round=fieldContext.isRound();
		boolean parseOnly=fieldContext.isParseOnly();
		fieldContext.setRound(true);
		try {
			fieldContext.setParseOnly(true);
			if (!applyClipboardValues(model, values, row0, col0)) {
				return false;
			}
			fieldContext.setParseOnly(false);
			return applyClipboardValues(model, values, row0, col0);
		} finally {
			fieldContext.setParseOnly(parseOnly);
			fieldContext.setRound(round);
		}
	}
	public static void pasteStringLine(String s,SpreadSheet spreadsheet,int row0, int col0){
		pasteString(s,spreadsheet,row0,col0);
	}

	private static boolean applyClipboardValues(CommonSpreadSheetModel model, String[][] values, int row0, int col0) {
		boolean ok = true;
		for (int i=0;i<values.length;i++) {
			for (int j=0;j<values[i].length;j++) {
				ok &= setValueAt(model, values[i][j], row0+i, col0+j);
			}
		}
		return ok;
	}

	
//	public boolean isNodeSelection() {
//		return nodeSelection;
//	}
//	
//	public ArrayList getSelectedFields(){
//		return (nodeSelection)?spreadsheet.getSelectableFields():spreadsheet.getSelectedFields();
//	}

}
