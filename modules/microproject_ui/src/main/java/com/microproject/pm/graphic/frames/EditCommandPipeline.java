/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.awt.event.ActionEvent;

import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;

/** Canonical execution point for spreadsheet edit commands. */
final class EditCommandPipeline {
	private EditCommandPipeline() {
	}

	static boolean execute(DocumentFrame frame, String command) {
		if (frame == null || command == null) {
			return false;
		}
		SpreadSheet sheet = frame.getActiveSpreadSheet();
		if (sheet == null) {
			return false;
		}
		switch (command) {
		case MenuActionConstants.ACTION_DELETE -> {
			sheet.executeAction(MenuActionConstants.ACTION_DELETE);
			return true;
		}
		case MenuActionConstants.ACTION_CLEAR_CONTENTS -> {
			return sheet.clearSelectedCellValues();
		}
		case MenuActionConstants.ACTION_CUT, MenuActionConstants.ACTION_COPY -> {
			return sheet.performAction(command, new ActionEvent(sheet, ActionEvent.ACTION_PERFORMED, command));
		}
		case MenuActionConstants.ACTION_PASTE -> {
			return frame.canPasteIntoCurrentSelection() && sheet.pasteClipboardContents();
		}
		case MenuActionConstants.ACTION_PASTE_INSERT -> {
			return frame.canPasteIntoCurrentSelection() && sheet.pasteClipboardInsertedContents();
		}
		default -> throw new IllegalArgumentException("Unsupported edit command: " + command);
		}
	}
}
