/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog.assignment;

import java.awt.Component;

import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;

/** A spreadsheet inside an assignment dialog that shows the dialog's editor buttons only while a cell is edited. */
abstract class DialogEntrySpreadSheet extends SpreadSheet {
	private static final long serialVersionUID = 1L;
	private final CommonAssignmentDialog dialog;

	DialogEntrySpreadSheet(CommonAssignmentDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public Component prepareEditor(TableCellEditor editor, int row, int column) {
		dialog.setEditorButtonsVisible(true);
		return super.prepareEditor(editor, row, column);
	}

	@Override
	public void editingCanceled(ChangeEvent e) {
		dialog.setEditorButtonsVisible(false);
		super.editingCanceled(e);
	}

	@Override
	public void editingStopped(ChangeEvent e) {
		dialog.setEditorButtonsVisible(false);
		super.editingStopped(e);
	}
}
