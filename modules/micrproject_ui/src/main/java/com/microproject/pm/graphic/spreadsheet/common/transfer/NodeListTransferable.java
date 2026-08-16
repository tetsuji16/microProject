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

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.options.EditOption;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;



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
    private Set<DataFlavor> flavorSet;

	protected ArrayList<Node> nodeList;
	protected ArrayList<Field> fields;
	protected SpreadSheet spreadsheet;
	protected int[] rows,cols;
	protected boolean nodeSelection;
	private final String stringData;
	private final NodeModel nodeModel;

	public NodeListTransferable(ArrayList<Node> nodeList, ArrayList<Field> fields,SpreadSheet spreadSheet,int[] rows,int[] cols, boolean nodeSelection) {
		this.nodeSelection=nodeSelection;
		nodeModel=((CommonSpreadSheetModel)spreadSheet.getModel()).getCache().getModel();
		try {
			nodeListDataFlavor=new DataFlavor(NODE_LIST_MIME_TYPE);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Invalid local node-list data flavor", e);
		}
		if (nodeSelection){
				flavors=new DataFlavor[]{
						nodeListDataFlavor,
						DataFlavor.stringFlavor,
						DataFlavor.getTextPlainUnicodeFlavor()};
			this.nodeList=new ArrayList<>((List<Node>)nodeModel.copy(nodeList,NodeModel.SILENT));
			this.fields=fields;
		}else{
			flavors=new DataFlavor[]{
					DataFlavor.stringFlavor,
					DataFlavor.getTextPlainUnicodeFlavor()};
			//sdata=nodeListToString(nodeList,spreadSheet,fields);
		}
		flavorSet=new HashSet<>();
		//Collections.addAll(flavorSet,flavors); //jdk 1.5
		//for (int i=0;i<flavors.length;i++) flavorSet.add(flavors[i]);
		CollectionUtils.addAll(flavorSet,flavors); //replaced JDK 1.5 code with this call
		this.spreadsheet=spreadSheet;
		this.rows=rows;
		this.cols=cols;
		stringData = nodeSelection
				? nodeListToString(nodeList, spreadSheet, fields)
				: selectionToString(spreadSheet, rows, cols);
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
//			ArrayList nl =nodeList;
//			nodeList=new Vector(nl.size());
//			nodeList.addAll(model.copy(nl,NodeModel.SILENT));
			return nodeModel.copy(nodeList,NodeModel.SILENT);
		}else if (DataFlavor.stringFlavor.equals(flavor))
		    return stringData;
		else if (DataFlavor.getTextPlainUnicodeFlavor().equals(flavor))
		    return new StringReader(stringData);
		else throw new UnsupportedFlavorException(flavor);
	}
	
//	public Object getTransferData(DataFlavor[] flavors) throws UnsupportedFlavorException, IOException {
//		for (int i=0;i<flavors.length;i++){
//			if (isDataFlavorSupported(flavors[i]))
//				return getTransferData(flavors[i]);
//		}
//		throw new UnsupportedFlavorException(flavors[0]);
//}
	
	public static String nodeListToString(List<Node> nodeList,SpreadSheet spreadsheet,List<Field> fields){
		StringBuilder sb = new StringBuilder();
		for (Node node : nodeList){
			nodeToString(node,sb,spreadsheet,fields);
		}
		return sb.toString();
	}
	public static void nodeToString(Node node, StringBuilder sb, SpreadSheet spreadsheet, List<Field> fields){
		CommonSpreadSheetModel model=(CommonSpreadSheetModel)spreadsheet.getModel();
		Object value;
		Field field;
		boolean first=true;
		//String s=null;
		for (Field currentField : fields){
			field=currentField;
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
	
	
	

	public static ArrayList<Node> stringToNodeList(String s,SpreadSheet spreadsheet,List<Field> fields,NodeModelDataFactory factory){
		ArrayList<Node> list = new ArrayList<>();
		StringTokenizer st=new StringTokenizer(s,"\n\r");
		Node node;
		while (st.hasMoreTokens()){
			node=stringToNode(st.nextToken(),spreadsheet,fields,factory);
			if (node!=null) list.add(node);
		}
		return list;
	}
	public static Node stringToNode(String s,SpreadSheet spreadsheet,List<Field> fields,NodeModelDataFactory factory){
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
			Iterator<Field> fieldsIterator=fields.iterator();
			while(st.hasMoreTokens()&&fieldsIterator.hasNext()){
				valueS=st.nextToken();
				if (delim.equals(valueS)) valueS="";
				else if (st.hasMoreTokens()) st.nextToken();
				field=fieldsIterator.next();
				try {
					field.setValue(node,model.getCache().getWalkersModel(),spreadsheet,valueS,model.getFieldContext());
				} catch (FieldParseException e) {
					logger.log(Level.FINE, "Failed to parse pasted value for field " + field.getId(), e);
					return null;
				}
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

	public static boolean isNodeListFlavor(DataFlavor flavor) {
		return flavor != null && flavor.isMimeTypeEqual(NODE_LIST_MIME_TYPE);
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

