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

	static void execute(DocumentFrame frame, String command) {
		if (frame == null || command == null) {
			return;
		}
		SpreadSheet sheet = frame.getActiveSpreadSheet();
		if (sheet == null) {
			return;
		}
		switch (command) {
		case MenuActionConstants.ACTION_DELETE -> sheet.executeAction(MenuActionConstants.ACTION_DELETE);
		case MenuActionConstants.ACTION_CUT, MenuActionConstants.ACTION_COPY ->
			sheet.performAction(command, new ActionEvent(sheet, ActionEvent.ACTION_PERFORMED, command));
		case MenuActionConstants.ACTION_PASTE, MenuActionConstants.ACTION_PASTE_INSERT -> {
			if (frame.canPasteIntoCurrentSelection()) {
				sheet.performAction(command, new ActionEvent(sheet, ActionEvent.ACTION_PERFORMED, command));
			}
		}
		default -> throw new IllegalArgumentException("Unsupported edit command: " + command);
		}
	}
}
