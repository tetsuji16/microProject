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
package com.projectlibre1.pm.graphic.spreadsheet.common;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JTable;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableColumnModel;

import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetPopupMenu;
import java.awt.Dimension;
import com.projectlibre1.util.FlatUiSupport;

/**
 *
 */
public class SpreadSheetRowHeader extends JTable {
	protected CommonSpreadSheet table;
	private int dropRow=-1;
	private boolean dropAfter;
	private boolean mouseHandlersInstalled;
	//protected SpreadSheetPopupMenu popup=null;
	public SpreadSheetRowHeader(CommonSpreadSheet table) {
		super();
		setGridColor(FlatUiSupport.tableGridColor());
		this.table=table;
		if (table instanceof SpreadSheet){
			final SpreadSheet spreadSheet=(SpreadSheet)table;
			
			getActionMap().put("cut",new AbstractAction(){
				public void actionPerformed(java.awt.event.ActionEvent e) {
					spreadSheet.prepareAction(MenuActionConstants.ACTION_CUT).actionPerformed(new ActionEvent(spreadSheet,e.getID(),e.getActionCommand()));
				}
			});
			getActionMap().put("copy",new AbstractAction(){
				public void actionPerformed(java.awt.event.ActionEvent e) {
					spreadSheet.prepareAction(MenuActionConstants.ACTION_COPY).actionPerformed(new ActionEvent(spreadSheet,e.getID(),e.getActionCommand()));
				}
			});
			getActionMap().put("paste",new AbstractAction(){
				public void actionPerformed(java.awt.event.ActionEvent e) {
					spreadSheet.prepareAction(MenuActionConstants.ACTION_PASTE).actionPerformed(new ActionEvent(spreadSheet,e.getID(),e.getActionCommand()));
				}
			});
			getActionMap().put("insertClipboard",new AbstractAction(){
				public void actionPerformed(java.awt.event.ActionEvent e) {
					spreadSheet.prepareAction(MenuActionConstants.ACTION_PASTE_INSERT).actionPerformed(new ActionEvent(spreadSheet,e.getID(),e.getActionCommand()));
				}
			});
			InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
			inputMap.put(KeyStroke.getKeyStroke("ctrl X"), "cut");
			inputMap.put(KeyStroke.getKeyStroke("ctrl C"), "copy");
			inputMap.put(KeyStroke.getKeyStroke("ctrl V"), "paste");
			inputMap.put(KeyStroke.getKeyStroke("shift ctrl V"), "insertClipboard");
			spreadSheet.installTaskMoveBindings(this);
			
		}
		setFont(FlatUiSupport.headerFont());
		setForeground(FlatUiSupport.headerForeground());
		setBackground(FlatUiSupport.spreadsheetHeaderBackground());
		setBorder(FlatUiSupport.tableHeaderBorder());
		setShowHorizontalLines(true);
		setShowVerticalLines(false);
		setIntercellSpacing(new Dimension(0, 0));
		setRowMargin(0);
		
	}
	
	protected SpreadSheetPopupMenu getPopup(){
//		if (popup==null){
//			SpreadSheet spreadSheet=(SpreadSheet)table;
//			popup = spreadSheet.hasRowPopup() ? new SpreadSheetPopupMenu(spreadSheet) : null;
//		}
//		return popup;
		return ((SpreadSheet)table).getPopup();
	}
//	public void clearPopup(){
//		popup=null;
//	}
	
	public void setModel(CommonSpreadSheetModel spreadSheetModel, DefaultTableColumnModel spreadSheetColumnModel) {
	    setModel(spreadSheetModel);
	    setColumnModel(spreadSheetColumnModel);
	    if (table.getSelection() != null) {
	    	setSelectionModel(table.getSelection().getRowSelection());
	    }
	    
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		if (table instanceof SpreadSheet){
			final SpreadSheet spreadSheet=(SpreadSheet)table;
			if (!mouseHandlersInstalled){
			mouseHandlersInstalled=true;
			MouseInputAdapter handler=new MouseInputAdapter() {
				private Point pressPoint;
				private boolean dragging;
			public void mousePressed(MouseEvent e) {
				SpreadSheetPopupMenu popup=getPopup();
				if (SwingUtilities.isLeftMouseButton(e)){
					int row = rowAtPoint(e.getPoint());
					if (row < 0) {
						return;
					}
					boolean keepExisting=isRowSelected(row)&&getSelectedRowCount()>1&&!e.isControlDown()&&!e.isShiftDown();
					selectRowForMove(row,e.isShiftDown(),e.isControlDown(),keepExisting);
					pressPoint=e.getPoint();
					dragging=false;
					if (e.getClickCount()==2){
						spreadSheet.doDoubleClick(row,0);
//						Component comp=SpreadSheetRowHeader.this;
//						while(!((comp=comp.getParent()) instanceof MainFrame));
//						MainFrame mainFrame=(MainFrame)comp;
//						mainFrame.doInformationDialog(false);
//
					}
				}else if (popup!=null&&SwingUtilities.isRightMouseButton(e)){ //e.isPopupTrigger() can be used too
					int row = rowAtPoint(e.getPoint());
					table.selectRowAndAllColumns(row);
					popup.setRow(row);
					popup.setCol(0);
					popup.show(SpreadSheetRowHeader.this,e.getX(),e.getY());
				}
			}
			public void mouseDragged(MouseEvent e) {
				if (pressPoint==null||(e.getModifiersEx()&MouseEvent.BUTTON1_DOWN_MASK)==0) return;
				if (!dragging&&pressPoint.distance(e.getPoint())<4.0d) return;
				dragging=true;
				updateDropLocation(e);
			}
			public void mouseReleased(MouseEvent e) {
				if (dragging&&dropRow>=0) spreadSheet.moveSelectedTaskRowsTo(dropRow,dropAfter);
				pressPoint=null;
				dragging=false;
				dropRow=-1;
				setCursor(Cursor.getDefaultCursor());
				repaint();
			}
			private void updateDropLocation(MouseEvent e){
				int row=rowAtPoint(e.getPoint());
				if (row<0){
					if (getRowCount()==0) return;
					row=e.getY()<0?0:getRowCount()-1;
					dropAfter=e.getY()>=0;
				}else{
					java.awt.Rectangle bounds=getCellRect(row,0,true);
					dropAfter=e.getY()>=bounds.y+bounds.height/2;
				}
				dropRow=row;
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				repaint();
			}
			};
			addMouseListener(handler);
			addMouseMotionListener(handler);
			}
		}

	}	

	private void selectRowForMove(int row,boolean extend,boolean toggle,boolean keepExisting){
		if (extend){
			int anchor=getSelectionModel().getAnchorSelectionIndex();
			if (anchor<0) anchor=row;
			getSelectionModel().setSelectionInterval(Math.min(anchor,row),Math.max(anchor,row));
		}else if (toggle){
			if (isRowSelected(row)) getSelectionModel().removeSelectionInterval(row,row);
			else getSelectionModel().addSelectionInterval(row,row);
		}else if (!keepExisting){
			getSelectionModel().setSelectionInterval(row,row);
		}
		if (table.getColumnCount()>0)
			table.getColumnModel().getSelectionModel().setSelectionInterval(0,table.getColumnCount()-1);
	}

	@Override
	protected void paintComponent(Graphics graphics){
		super.paintComponent(graphics);
		if (dropRow<0||dropRow>=getRowCount()) return;
		java.awt.Rectangle bounds=getCellRect(dropRow,0,true);
		int y=dropAfter?bounds.y+bounds.height-1:bounds.y;
		graphics.setColor(getSelectionBackground().darker());
		graphics.fillRect(0,y,getWidth(),2);
	}
	
	public CommonSpreadSheet getSpreadSheet(){
		return table;
	}
	public void updateUI() {
		super.updateUI();
		setFont(FlatUiSupport.headerFont());
		setForeground(FlatUiSupport.headerForeground());
		setBackground(FlatUiSupport.spreadsheetHeaderBackground());
		setGridColor(FlatUiSupport.tableGridColor());
		setBorder(FlatUiSupport.tableHeaderBorder());
		setShowHorizontalLines(true);
		setShowVerticalLines(false);
		setIntercellSpacing(new Dimension(0, 0));
		setRowMargin(0);
	}
}

