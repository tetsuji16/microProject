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
package com.microproject.pm.graphic.spreadsheet.common;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JTable;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import javax.swing.table.DefaultTableColumnModel;

import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetPopupMenu;
import com.microproject.pm.graphic.spreadsheet.selection.event.PopupTriggerHandler;
import java.awt.Dimension;
import com.microproject.util.FlatUiSupport;

/**
 * Row header (ID column) of the task spreadsheet.
 *
 * <p>Dragging through the row header selects a range of rows and keeps the
 * Gantt chart's row highlight in sync live, matching Microsoft Project's
 * behaviour (issue #179). Reordering tasks is performed from the Move Up/Down
 * ribbon buttons and the keyboard shortcuts, not by dragging here, so a plain
 * selection drag never triggers an accidental move or a system beep.
 */
public class SpreadSheetRowHeader extends JTable {
	protected CommonSpreadSheet table;
	private boolean mouseHandlersInstalled;
	//	protected SpreadSheetPopupMenu popup=null;
	public SpreadSheetRowHeader(CommonSpreadSheet table) {
		super();
		setGridColor(FlatUiSupport.tableGridColor());
		this.table=table;
		if (table instanceof SpreadSheet){
			final SpreadSheet spreadSheet=(SpreadSheet)table;

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

	public SpreadSheetPopupMenu getPopup(){
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
				RowHeaderMouseHandler handler=new RowHeaderMouseHandler(spreadSheet);
				addMouseListener(handler);
				addMouseMotionListener(handler);
			}
		}

	}

	protected void showHeaderPopup(SpreadSheetPopupMenu popup, MouseEvent event) {
		popup.show(this, event.getX(), event.getY());
	}

	private void selectRowForMove(int row,boolean extend,boolean toggle,boolean keepExisting){
		table.setRowHeaderSelectionActive(true);
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

	/**
	 * Row-header mouse handler: left-drag extends the row selection (issue
	 * #179), a right-click/popup-trigger shows the task-table popup. The shared
	 * press/release popup-trigger dedupe lives in {@link PopupTriggerHandler}.
	 */
	private final class RowHeaderMouseHandler extends PopupTriggerHandler implements MouseInputListener {
		private final SpreadSheet spreadSheet;
		private Point pressPoint;
		private int pressRow = -1;
		private boolean dragging;

		RowHeaderMouseHandler(SpreadSheet spreadSheet) {
			this.spreadSheet = spreadSheet;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e)) {
				int row = rowAtPoint(e.getPoint());
				if (row < 0) return;
				boolean keepExisting = isRowSelected(row) && getSelectedRowCount() > 1
						&& !e.isControlDown() && !e.isShiftDown();
				selectRowForMove(row, e.isShiftDown(), e.isControlDown(), keepExisting);
				// The row header is only a selection affordance. Keep keyboard
				// input on the task table so Ctrl+C/V and typed edits continue
				// through the document's single root-pane shortcut routing.
				spreadSheet.requestFocusInWindow();
				pressRow = row;
				pressPoint = e.getPoint();
				dragging = false;
				if (e.getClickCount() == 2) {
					spreadSheet.doDoubleClick(row, 0);
				}
			} else {
				super.mousePressed(e);
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (pressPoint == null || (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) return;
			if (!dragging && pressPoint.distance(e.getPoint()) < 4.0d) return;
			dragging = true;
			// Microsoft Project selects a range of rows when the user drags
			// through the row header (ID column). Extend the selection live so
			// the Gantt chart's row highlight follows the drag immediately
			// (issue #179). Task reordering is done from the Move Up/Down
			// buttons and keyboard shortcuts, so a selection drag must never
			// relocate tasks or beep.
			int currentRow = rowAtPoint(e.getPoint());
			if (currentRow >= 0 && pressRow >= 0) {
				int first = Math.min(pressRow, currentRow);
				int last = Math.max(pressRow, currentRow);
				getSelectionModel().setSelectionInterval(first, last);
				if (getColumnCount() > 0)
					getColumnModel().getSelectionModel().setSelectionInterval(0, getColumnCount() - 1);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// Let PopupTriggerHandler handle the popup-trigger release path,
			// then clear the drag state regardless of which branch ran.
			super.mouseReleased(e);
			pressPoint = null;
			pressRow = -1;
			dragging = false;
			repaint();
		}

		@Override
		protected boolean showPopup(MouseEvent event) {
			if (!isPopupTrigger(event)) return false;
			SpreadSheetPopupMenu popup = getPopup();
			int row = rowAtPoint(event.getPoint());
			if (popup == null || row < 0) return false;
			if (!isRowSelected(row)) table.selectRowAndAllColumns(row);
			spreadSheet.requestFocusInWindow();
			popup.setRow(row);
			popup.setCol(0);
			// Delegated so subclasses/tests can intercept the actual showing
			// (headless environments have no screen to show on).
			showHeaderPopup(popup, event);
			event.consume();
			return true;
		}
	}
}
